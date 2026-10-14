package app.jammes.boletim.presentation.ui.disciplina

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.jammes.boletim.domain.model.EdicaoDisciplina
import app.jammes.boletim.domain.usecase.ObterEdicaoDisciplina
import app.jammes.boletim.domain.usecase.SalvarEdicaoDisciplina
import app.jammes.boletim.presentation.navigation.Routes
import app.jammes.boletim.presentation.ui.anoletivo.Salvamento
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EdicaoDisciplinaUiState {
    data object Carregando : EdicaoDisciplinaUiState
    data object NaoEncontrada : EdicaoDisciplinaUiState
    data class Sucesso(val edicao: EdicaoDisciplina) : EdicaoDisciplinaUiState
}

@HiltViewModel
class EditarDisciplinaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    obterEdicao: ObterEdicaoDisciplina,
    private val salvarEdicao: SalvarEdicaoDisciplina,
) : ViewModel() {

    // Argumento da rota que abriu a tela, como no detalhe
    private val disciplinaId = savedStateHandle.toRoute<Routes.EditarDisciplina>().disciplinaId

    private val _state = MutableStateFlow<EdicaoDisciplinaUiState>(EdicaoDisciplinaUiState.Carregando)
    val state: StateFlow<EdicaoDisciplinaUiState> = _state.asStateFlow()

    // Os mesmos passos do Salvar dos ajustes do ano: Salvo fecha a tela
    private val _salvamento = MutableStateFlow<Salvamento>(Salvamento.Parado)
    val salvamento: StateFlow<Salvamento> = _salvamento.asStateFlow()

    init {
        // Lê uma vez só: a tela edita uma cópia até o Salvar
        viewModelScope.launch {
            _state.value = obterEdicao(disciplinaId)
                ?.let { EdicaoDisciplinaUiState.Sucesso(it) }
                ?: EdicaoDisciplinaUiState.NaoEncontrada
        }
    }

    fun salvar(formulario: FormularioDisciplina) {
        val edicao = (_state.value as? EdicaoDisciplinaUiState.Sucesso)?.edicao ?: return
        val disciplina = formulario.paraGravar(edicao.disciplina) ?: return
        if (_salvamento.value == Salvamento.Salvando) return // toque duplo no Salvar

        _salvamento.value = Salvamento.Salvando
        viewModelScope.launch {
            // A disciplina e a regra própria dela vão juntas, numa transação só
            salvarEdicao(edicao, disciplina, formulario.regraParaGravar(edicao))
            _salvamento.value = Salvamento.Salvo
        }
    }
}
