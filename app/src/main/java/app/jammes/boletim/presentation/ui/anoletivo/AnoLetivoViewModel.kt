package app.jammes.boletim.presentation.ui.anoletivo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.AjustesAnoLetivo
import app.jammes.boletim.domain.repository.ContextoRepository
import app.jammes.boletim.domain.usecase.AjusteBloqueadoException
import app.jammes.boletim.domain.usecase.ObterAjustesAnoLetivo
import app.jammes.boletim.domain.usecase.SalvarAjustesAnoLetivo
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface AnoLetivoUiState {
    data object Carregando : AnoLetivoUiState
    data object NaoEncontrado : AnoLetivoUiState
    data class Sucesso(val ajustes: AjustesAnoLetivo) : AnoLetivoUiState
}

/** Andamento do Salvar: Salvo fecha a tela; Falhou mostra o motivo e deixa tentar de novo. */
sealed interface Salvamento {
    data object Parado : Salvamento
    data object Salvando : Salvamento
    data object Salvo : Salvamento
    data class Falhou(val motivo: String) : Salvamento
}

@HiltViewModel
class AnoLetivoViewModel @Inject constructor(
    contextoRepo: ContextoRepository,
    obterAjustes: ObterAjustesAnoLetivo,
    private val salvarAjustes: SalvarAjustesAnoLetivo,
): ViewModel() {

    private val _state = MutableStateFlow<AnoLetivoUiState>(AnoLetivoUiState.Carregando)
    val state: StateFlow<AnoLetivoUiState> = _state.asStateFlow()

    private val _salvamento = MutableStateFlow<Salvamento>(Salvamento.Parado)
    val salvamento: StateFlow<Salvamento> = _salvamento.asStateFlow()

    init {
        // Lê uma vez só o ano letivo aberto no app: a tela edita uma cópia até o Salvar
        viewModelScope.launch {
            val contexto = contextoRepo.contexto.filterNotNull().first()
            _state.value = obterAjustes(contexto)
                ?.let { AnoLetivoUiState.Sucesso(it) }
                ?: AnoLetivoUiState.NaoEncontrado
        }
    }

    fun salvar(formulario: FormularioAnoLetivo) {
        val ajustes = (_state.value as? AnoLetivoUiState.Sucesso)?.ajustes ?: return
        val gravacao = formulario.paraGravar(ajustes) ?: return
        if (_salvamento.value == Salvamento.Salvando) return // toque duplo no Salvar

        _salvamento.value = Salvamento.Salvando
        viewModelScope.launch {
            _salvamento.value = try {
                salvarAjustes(ajustes, gravacao.anoLetivo, gravacao.regra, gravacao.materiaIds)
                Salvamento.Salvo
            } catch (e: AjusteBloqueadoException) {
                Salvamento.Falhou(e.message ?: "Não deu para salvar")
            }
        }
    }

    fun falhaVista() {
        _salvamento.value = Salvamento.Parado
    }
}
