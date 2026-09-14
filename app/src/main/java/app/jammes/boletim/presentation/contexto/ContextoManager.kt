package app.jammes.boletim.presentation.contexto

import app.jammes.boletim.di.ApplicationScope
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
class ContextoManager @Inject constructor(
    private val prefs: ContextoPreferences,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<ContextoUiState>(ContextoUiState.Carregando)
    val state: StateFlow<ContextoUiState> = _state.asStateFlow()

    // atalho pra quem só quer o contexto quando existe
    val contexto: Flow<Contexto> = _state
        .filterIsInstance<ContextoUiState.Definido>()
        .map { it.contexto }

    init {
        scope.launch {
            _state.value = prefs.ler()
                ?.let { ContextoUiState.Definido(it) }
                ?: ContextoUiState.Vazio
        }
    }

    fun selecionarPeriodo(periodoId: Long) = atualizar { it.copy(periodoId = periodoId) }

    fun selecionarAnoLetivo(anoLetivoId: Long, periodoId: Long) =
        atualizar { it.copy(anoLetivoId = anoLetivoId, periodoId = periodoId) }

    fun selecionarAluno(alunoId: Long, anoLetivoId: Long, periodoId: Long) {
        definir(Contexto(alunoId, anoLetivoId, periodoId))
    }

    private fun atualizar(bloco: (Contexto) -> Contexto) {
        val atual = (_state.value as? ContextoUiState.Definido)?.contexto ?: return
        definir(bloco(atual))
    }

    private fun definir(novo: Contexto) {
        _state.value = ContextoUiState.Definido(novo)
        scope.launch { prefs.salvar(novo) }
    }
}