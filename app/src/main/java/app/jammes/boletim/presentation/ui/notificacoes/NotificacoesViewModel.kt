package app.jammes.boletim.presentation.ui.notificacoes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.NotificacaoDomain
import app.jammes.boletim.domain.repository.AlunoRepository
import app.jammes.boletim.domain.repository.ContextoRepository
import app.jammes.boletim.domain.repository.NotificacaoRepository
import app.jammes.boletim.domain.usecase.RetirarAvisoDoAluno
import app.jammes.boletim.domain.usecase.nomeNaLista
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.Clock

/** As notificações de um dia, da mais nova para a mais antiga. */
data class DiaDeNotificacoes(
    val dia: LocalDate,
    val itens: List<ItemDeNotificacao>,
)

/** Uma notificação na lista, com a hora em que chegou. [nova]: ainda não tinha sido vista (o ponto azul). */
data class ItemDeNotificacao(
    val notificacao: NotificacaoDomain,
    val hora: LocalTime,
    val nova: Boolean,
)

sealed interface NotificacoesUiState {
    data object Carregando : NotificacoesUiState

    data class Carregadas(
        val dias: List<DiaDeNotificacoes>,
        val naoLidas: List<Long>, // ainda não gravadas como lidas: a tela marca quando mostra
        // De quem são; só com mais de um aluno no aparelho, porque nesta tela não há o card do aluno
        val nomeDoAluno: String? = null,
    ) : NotificacoesUiState {
        val total: Int get() = dias.sumOf { it.itens.size }
        val novas: Int get() = dias.sumOf { dia -> dia.itens.count { it.nova } }
    }
}

/**
 * A lista de notificações do aluno aberto; as dos outros alunos ficam guardadas, mas não aparecem.
 * Aparecer na tela é o que conta como lida: o sino do topo apaga, o aviso do aluno sai da barra do
 * celular, e o ponto azul continua nelas até a pessoa sair daqui, para dar tempo de ver quais eram as
 * novas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificacoesViewModel @Inject constructor(
    private val repo: NotificacaoRepository,
    private val contextoRepo: ContextoRepository,
    alunoRepo: AlunoRepository,
    private val retirarAvisoDoAluno: RetirarAvisoDoAluno,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // As vistas nesta visita, que continuam com o ponto azul. No SavedStateHandle, para a tela voltar
    // igual quando o Android fecha o app em segundo plano: no banco elas já estão como lidas
    private val vistas = MutableStateFlow(savedStateHandle.get<LongArray>(CHAVE_VISTAS)?.toSet().orEmpty())

    private val alunoAberto = contextoRepo.contexto
        .filterNotNull()
        .map { it.alunoId }
        .distinctUntilChanged()

    val state: StateFlow<NotificacoesUiState> = alunoAberto
        .flatMapLatest { alunoId ->
            combine(repo.notificacoes(alunoId), vistas, alunoRepo.observeAluno()) { notificacoes, vistas, alunos ->
                NotificacoesUiState.Carregadas(
                    dias = agruparPorDia(notificacoes, vistas, ZoneId.systemDefault()),
                    naoLidas = notificacoes.filterNot { it.lida }.map { it.id },
                    nomeDoAluno = nomeNaLista(alunos, alunoId),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificacoesUiState.Carregando)

    private val _limpou = Channel<List<NotificacaoDomain>>(Channel.BUFFERED)

    /** O Limpar apagou estas notificações: o aviso diz quantas, e o Desfazer põe elas de volta. */
    val limpou: Flow<List<NotificacaoDomain>> = _limpou.receiveAsFlow()

    private val _abrir = Channel<DestinoDaNotificacao>(Channel.BUFFERED)

    /** Uma notificação foi tocada: o app já está no ano e no período dela, falta a tela ir até lá. */
    val abrir: Flow<DestinoDaNotificacao> = _abrir.receiveAsFlow()

    init {
        // Abrir a lista é ver o aviso do celular: ele sai da barra
        viewModelScope.launch { retirarAvisoDoAluno(alunoAberto.first()) }
    }

    /** As [ids] apareceram na tela: ficam gravadas como lidas, e com o ponto azul até a tela fechar. */
    fun marcarComoVistas(ids: List<Long>) {
        vistas.update { it + ids }
        savedStateHandle[CHAVE_VISTAS] = vistas.value.toLongArray()
        viewModelScope.launch {
            repo.marcarComoLidas(ids)
            // Chegou um aviso com a tela aberta: ele também já foi visto aqui
            retirarAvisoDoAluno(alunoAberto.first())
        }
    }

    /**
     * Leva ao que a [notificacao] avisa. Primeiro põe o app no ano letivo e no período dela, porque a
     * tela de destino mostra o que está no contexto; só depois pede a tela. Um aviso que não aponta
     * para nada não faz nada.
     */
    fun abrir(notificacao: NotificacaoDomain) {
        val destino = destinoDa(notificacao) ?: return
        viewModelScope.launch {
            val contexto = contextoRepo.contexto.first()
            when {
                contexto?.anoLetivoId != destino.anoLetivoId ->
                    contextoRepo.selecionarAnoLetivo(destino.anoLetivoId, destino.periodoId)
                contexto?.periodoId != destino.periodoId -> contextoRepo.selecionarPeriodo(destino.periodoId)
            }
            _abrir.send(destino)
        }
    }

    /** Tira da lista as do aluno aberto; as dos outros alunos ficam. */
    fun limpar() {
        viewModelScope.launch {
            val apagadas = repo.limparDoAluno(alunoAberto.first())
            if (apagadas.isNotEmpty()) _limpou.send(apagadas)
        }
    }

    /** Põe de volta as [apagadas] por um Limpar, como estavam: o ponto azul volta nas que tinham. */
    fun desfazerLimpeza(apagadas: List<NotificacaoDomain>) {
        viewModelScope.launch { repo.restaurar(apagadas.map { it.id }) }
    }

    /**
     * Só no build de debug: exemplos para o aluno aberto, para ver a tela antes de o app criar
     * notificações de verdade.
     */
    fun criarNotificacoesDeTeste() {
        viewModelScope.launch {
            repo.inserir(notificacoesDeTeste(alunoAberto.first(), Clock.System.now(), ZoneId.systemDefault()))
        }
    }
}

private const val CHAVE_VISTAS = "vistas"

/**
 * As [notificacoes] agrupadas pelo dia em que chegaram, no fuso [zona]: os dias e, dentro de cada
 * um, as notificações, das mais novas para as mais antigas. Nova é a que ainda não foi lida ou a que
 * foi vista nesta visita ([vistas]).
 */
internal fun agruparPorDia(
    notificacoes: List<NotificacaoDomain>,
    vistas: Set<Long>,
    zona: ZoneId,
): List<DiaDeNotificacoes> =
    notificacoes
        .sortedWith(compareByDescending<NotificacaoDomain> { it.criadaEm }.thenByDescending { it.id })
        .map { notificacao ->
            val quando = java.time.Instant.ofEpochMilli(notificacao.criadaEm.toEpochMilliseconds()).atZone(zona)
            quando.toLocalDate() to ItemDeNotificacao(
                notificacao = notificacao,
                hora = quando.toLocalTime().truncatedTo(ChronoUnit.MINUTES),
                nova = !notificacao.lida || notificacao.id in vistas,
            )
        }
        // groupBy mantém a ordem em que cada dia aparece, e a das notificações dentro dele
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        .map { (dia, itens) -> DiaDeNotificacoes(dia, itens) }
