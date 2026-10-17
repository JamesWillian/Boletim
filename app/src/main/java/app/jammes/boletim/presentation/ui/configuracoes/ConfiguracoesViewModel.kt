package app.jammes.boletim.presentation.ui.configuracoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.Configuracoes
import app.jammes.boletim.domain.model.EstiloDasAbas
import app.jammes.boletim.domain.model.Tema
import app.jammes.boletim.domain.model.comHorario
import app.jammes.boletim.domain.model.semHorario
import app.jammes.boletim.domain.repository.ConfiguracoesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * As configurações gravadas e as escolhas da tela. Um só, preso à Activity: a MainActivity tira dele
 * o tema, o AppRoot o estilo das abas, e a tela de configurações abre com ele já carregado.
 *
 * Cada escolha grava na hora: não tem Salvar, como nas configurações do aparelho.
 */
@HiltViewModel
class ConfiguracoesViewModel @Inject constructor(
    private val repo: ConfiguracoesRepository,
) : ViewModel() {

    /** null até a primeira leitura: a splash espera por ela, para o app não abrir no tema errado. */
    val configuracoes: StateFlow<Configuracoes?> = repo.configuracoes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
}
