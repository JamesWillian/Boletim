package app.jammes.boletim.presentation.ui.notificacoes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.NotificacaoDomain
import app.jammes.boletim.domain.repository.NotificacaoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    ) : NotificacoesUiState {
        val total: Int get() = dias.sumOf { it.itens.size }
        val novas: Int get() = dias.sumOf { dia -> dia.itens.count { it.nova } }
    }
}

/**
 * A lista de notificações. Aparecer na tela é o que conta como lida: o sino do topo apaga, mas o ponto
 * azul continua nelas até a pessoa sair daqui, para dar tempo de ver quais eram as novas.
 */
@HiltViewModel
class NotificacoesViewModel @Inject constructor(
    private val repo: NotificacaoRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // As vistas nesta visita, que continuam com o ponto azul. No SavedStateHandle, para a tela voltar
    // igual quando o Android fecha o app em segundo plano: no banco elas já estão como lidas
    private val vistas = MutableStateFlow(savedStateHandle.get<LongArray>(CHAVE_VISTAS)?.toSet().orEmpty())

    val state: StateFlow<NotificacoesUiState> = combine(repo.notificacoes, vistas) { notificacoes, vistas ->
        NotificacoesUiState.Carregadas(
            dias = agruparPorDia(notificacoes, vistas, ZoneId.systemDefault()),
            naoLidas = notificacoes.filterNot { it.lida }.map { it.id },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificacoesUiState.Carregando)

    private val _limpou = Channel<List<NotificacaoDomain>>(Channel.BUFFERED)

    /** O Limpar apagou estas notificações: o aviso diz quantas, e o Desfazer põe elas de volta. */
    val limpou: Flow<List<NotificacaoDomain>> = _limpou.receiveAsFlow()

    /** As [ids] apareceram na tela: ficam gravadas como lidas, e com o ponto azul até a tela fechar. */
    fun marcarComoVistas(ids: List<Long>) {
        vistas.update { it + ids }
        savedStateHandle[CHAVE_VISTAS] = vistas.value.toLongArray()
        viewModelScope.launch { repo.marcarComoLidas(ids) }
    }

    fun limpar() {
        viewModelScope.launch {
            val apagadas = repo.apagarTodas()
            if (apagadas.isNotEmpty()) _limpou.send(apagadas)
        }
    }

    /** Põe de volta as [apagadas] por um Limpar, com os mesmos ids: o ponto azul volta nas que tinham. */
    fun desfazerLimpeza(apagadas: List<NotificacaoDomain>) {
        viewModelScope.launch { repo.inserir(apagadas) }
    }

    /** Só no build de debug: exemplos para ver a tela antes de o app criar notificações de verdade. */
    fun criarNotificacoesDeTeste() {
        viewModelScope.launch { repo.inserir(notificacoesDeTeste(Clock.System.now(), ZoneId.systemDefault())) }
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
