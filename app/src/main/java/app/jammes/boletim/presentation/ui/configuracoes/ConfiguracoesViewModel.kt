package app.jammes.boletim.presentation.ui.configuracoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.Configuracoes
import app.jammes.boletim.domain.model.EstiloDasAbas
import app.jammes.boletim.domain.model.Tema
import app.jammes.boletim.domain.repository.ConfiguracoesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
}
