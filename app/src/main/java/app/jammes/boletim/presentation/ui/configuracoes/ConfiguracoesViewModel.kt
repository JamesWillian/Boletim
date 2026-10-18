package app.jammes.boletim.presentation.ui.configuracoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.Configuracoes
import app.jammes.boletim.domain.model.EstiloDasAbas
import app.jammes.boletim.domain.model.Tema
import app.jammes.boletim.domain.model.comHorario
import app.jammes.boletim.domain.model.semHorario
import app.jammes.boletim.domain.repository.ConfiguracoesRepository
import app.jammes.boletim.domain.usecase.AvisarNotificacoes
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * As configurações gravadas e as escolhas da tela. Um só, preso à Activity: a MainActivity tira dele
 * o tema, o AppRoot o estilo das abas, e a tela de configurações abre com ele já carregado.
 *
 * Cada escolha grava na hora: não tem Salvar, como nas configurações do aparelho.
 */
@HiltViewModel
class ConfiguracoesViewModel @Inject constructor(
    private val repo: ConfiguracoesRepository,
    private val avisarNotificacoes: AvisarNotificacoes,
) : ViewModel() {

    /** null até a primeira leitura: a splash espera por ela, para o app não abrir no tema errado. */
    val configuracoes: StateFlow<Configuracoes?> = repo.configuracoes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Se o pedido de permissão de notificações do Android já apareceu: depois dele, só as
     * configurações do aparelho liberam. null até a primeira leitura, para o boletim não pedir de novo
     * a quem já respondeu.
     */
    val permissaoDeNotificacoesPedida: StateFlow<Boolean?> = repo.permissaoDeNotificacoesPedida
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _avisosGerados = Channel<Int>(Channel.BUFFERED)

    /** Só no build de debug: o "Gerar avisos agora" terminou, com quantas notificações novas. */
    val avisosGerados: Flow<Int> = _avisosGerados.receiveAsFlow()

    fun escolherTema(tema: Tema) {
        viewModelScope.launch { repo.salvarTema(tema) }
    }

    fun escolherEstiloDasAbas(estilo: EstiloDasAbas) {
        viewModelScope.launch { repo.salvarEstiloDasAbas(estilo) }
    }

    /** A chave do backup e restauração; o BoletimBackupAgent lê ela na hora do backup. */
    fun escolherBackupAutomatico(ligado: Boolean) {
        viewModelScope.launch { repo.salvarBackupAutomatico(ligado) }
    }

    /** A chave das notificações. Desligadas, os horários continuam gravados para quando voltarem. */
    fun ativarNotificacoes(ativas: Boolean) {
        viewModelScope.launch { repo.salvarNotificacoesAtivas(ativas) }
    }

    /** Grava [novo] no lugar de [antigo], ou a mais quando [antigo] é null. */
    fun salvarHorario(novo: LocalTime, antigo: LocalTime?) {
        viewModelScope.launch { repo.editarHorariosDasNotificacoes { it.comHorario(novo, antigo) } }
    }

    fun removerHorario(horario: LocalTime) {
        viewModelScope.launch { repo.editarHorariosDasNotificacoes { it.semHorario(horario) } }
    }

    fun marcarPermissaoDeNotificacoesPedida() {
        viewModelScope.launch { repo.marcarPermissaoDeNotificacoesPedida() }
    }

    /**
     * Só no build de debug: faz agora o que o horário das notificações faria, sem esperar por ele.
     * Gera as notificações de todos os alunos e mostra no celular as que ainda não foram avisadas.
     */
    fun gerarAvisosAgora() {
        viewModelScope.launch { _avisosGerados.send(avisarNotificacoes(ZonedDateTime.now())) }
    }
}
