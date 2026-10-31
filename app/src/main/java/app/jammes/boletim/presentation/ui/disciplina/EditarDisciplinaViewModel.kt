package app.jammes.boletim.presentation.ui.disciplina

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.jammes.boletim.domain.model.EdicaoDisciplina
import app.jammes.boletim.domain.model.LimiteDoGratisException
import app.jammes.boletim.domain.model.Plano
import app.jammes.boletim.domain.model.podePersonalizarDisciplina
import app.jammes.boletim.domain.repository.PlanoRepository
import app.jammes.boletim.domain.usecase.ObterEdicaoDisciplina
import app.jammes.boletim.domain.usecase.SalvarEdicaoDisciplina
import app.jammes.boletim.presentation.navigation.Routes
import app.jammes.boletim.presentation.ui.anoletivo.Salvamento
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EdicaoDisciplinaUiState {
    data object Carregando : EdicaoDisciplinaUiState
    data object NaoEncontrada : EdicaoDisciplinaUiState
    data class Sucesso(
        val edicao: EdicaoDisciplina,
        val personalizavel: Boolean = true, // false: no grátis, o nome, a sigla, a cor e o ícone desta são do Pro
    ) : EdicaoDisciplinaUiState
}

/** A [lida] do banco com o que o [plano] deixa personalizar nela; Carregando e NaoEncontrada passam como estão. */
internal fun comPlano(lida: EdicaoDisciplinaUiState, plano: Plano): EdicaoDisciplinaUiState = when (lida) {
    is EdicaoDisciplinaUiState.Sucesso ->
        lida.copy(personalizavel = plano.podePersonalizarDisciplina(lida.edicao.fonteDaMateria))
    else -> lida
}

@HiltViewModel
class EditarDisciplinaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    obterEdicao: ObterEdicaoDisciplina,
    private val salvarEdicao: SalvarEdicaoDisciplina,
    planoRepo: PlanoRepository,
) : ViewModel() {

    // Argumento da rota que abriu a tela, como no detalhe
    private val disciplinaId = savedStateHandle.toRoute<Routes.EditarDisciplina>().disciplinaId

    // A leitura do banco, uma vez só: a tela edita uma cópia até o Salvar
    private val lida = MutableStateFlow<EdicaoDisciplinaUiState>(EdicaoDisciplinaUiState.Carregando)

    /** A leitura com o plano: o Pro desbloqueado na folha libera a personalização sem ler a disciplina de novo. */
    val state: StateFlow<EdicaoDisciplinaUiState> = combine(lida, planoRepo.plano, ::comPlano)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EdicaoDisciplinaUiState.Carregando)

    // Os mesmos passos do Salvar dos ajustes do ano: Salvo fecha a tela
    private val _salvamento = MutableStateFlow<Salvamento>(Salvamento.Parado)
    val salvamento: StateFlow<Salvamento> = _salvamento.asStateFlow()

    init {
        viewModelScope.launch {
            lida.value = obterEdicao(disciplinaId)
                ?.let { EdicaoDisciplinaUiState.Sucesso(it) }
                ?: EdicaoDisciplinaUiState.NaoEncontrada
        }
    }

    fun salvar(formulario: FormularioDisciplina) {
        val edicao = (lida.value as? EdicaoDisciplinaUiState.Sucesso)?.edicao ?: return
        val disciplina = formulario.paraGravar(edicao.disciplina) ?: return
        if (_salvamento.value == Salvamento.Salvando) return // toque duplo no Salvar

        _salvamento.value = Salvamento.Salvando
        viewModelScope.launch {
            _salvamento.value = try {
                // A disciplina e a regra própria dela vão juntas, numa transação só
                salvarEdicao(edicao, disciplina, formulario.regraParaGravar(edicao))
                Salvamento.Salvo
            } catch (e: LimiteDoGratisException) {
                // A tela já não deixa mudar o que é do Pro: sobra o caso de o Pro sair no meio da edição
                Salvamento.Falhou(e.message ?: "Não deu para salvar")
            }
        }
    }

    fun falhaVista() {
        _salvamento.value = Salvamento.Parado
    }
}
