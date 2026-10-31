package app.jammes.boletim.presentation.ui.aluno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.AlunoComAno
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PerfilAluno
import app.jammes.boletim.domain.model.Plano
import app.jammes.boletim.domain.model.ResumoDoAluno
import app.jammes.boletim.domain.model.periodoAoAbrir
import app.jammes.boletim.domain.model.podeCriarAlgumAno
import app.jammes.boletim.domain.model.podeCriarAluno
import app.jammes.boletim.domain.repository.AlunoRepository
import app.jammes.boletim.domain.repository.ContextoRepository
import app.jammes.boletim.domain.repository.PlanoRepository
import app.jammes.boletim.domain.usecase.ExcluirAluno
import app.jammes.boletim.domain.usecase.ObterPerfilAluno
import app.jammes.boletim.domain.usecase.RenomearAluno
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface AlunoUiState {
    data object Carregando : AlunoUiState
    data object NaoEncontrado : AlunoUiState
    data class Sucesso(
        val perfil: PerfilAluno,
        val novoAlunoSoNoPro: Boolean = false, // o grátis já tem o aluno dele: o Novo aluno abre a folha do Pro
        val novoAnoSoNoPro: Boolean = false, // o grátis não tem mais ano para criar: o Criar novo ano letivo também
    ) : AlunoUiState
}

/**
 * O que o perfil mostra: o [perfil] do aluno aberto e, pelo [plano], se um aluno novo ou um ano letivo
 * novo, em [hoje], são do Pro.
 */
internal fun estadoDoPerfil(perfil: PerfilAluno?, plano: Plano, hoje: LocalDate): AlunoUiState =
    if (perfil == null) {
        AlunoUiState.NaoEncontrado
    } else {
        AlunoUiState.Sucesso(
            perfil = perfil,
            novoAlunoSoNoPro = !plano.podeCriarAluno(perfil.alunos.size),
            novoAnoSoNoPro = !plano.podeCriarAlgumAno(perfil.anosLetivos.map { it.ano }.toSet(), hoje),
        )
    }

/**
 * Andamento de uma troca ou de uma mudança no aluno. Trocou e Excluiu fecham o perfil; Renomeou volta
 * ao início dele.
 */
sealed interface Mudanca {
    data object Parada : Mudanca
    data object Mudando : Mudanca
    data object Trocou : Mudanca
    data object Renomeou : Mudanca
    data object Excluiu : Mudanca
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AlunoViewModel @Inject constructor(
    private val contextoRepo: ContextoRepository,
    alunoRepo: AlunoRepository,
    planoRepo: PlanoRepository,
    obterPerfil: ObterPerfilAluno,
    private val renomearAluno: RenomearAluno,
    private val excluirAluno: ExcluirAluno,
): ViewModel() {

    val state: StateFlow<AlunoUiState> = combine(
        obterPerfil(contextoRepo.contexto.filterNotNull()),
        planoRepo.plano,
    ) { perfil, plano -> estadoDoPerfil(perfil, plano, LocalDate.now()) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AlunoUiState.Carregando
        )

    /**
     * O que o aluno aberto tem no aparelho, para a exclusão dizer o que sai junto. Carrega com o perfil:
     * quando a página da exclusão abre, os números já estão lá.
     */
    val resumo: StateFlow<ResumoDoAluno?> = contextoRepo.contexto
        .filterNotNull()
        .map { it.alunoId }
        .distinctUntilChanged()
        .flatMapLatest { alunoRepo.observeResumo(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    private val _mudanca = MutableStateFlow<Mudanca>(Mudanca.Parada)
    val mudanca: StateFlow<Mudanca> = _mudanca.asStateFlow()

    /** Abre o aluno no ano letivo mais recente dele. Sem nenhum ano, não há o que abrir. */
    fun trocarAluno(escolhido: AlunoComAno) {
        val ano = escolhido.anoLetivo ?: return
        mudar(Mudanca.Trocou) {
            contextoRepo.selecionarAluno(escolhido.aluno.id, ano.id, ano.periodo.periodoAoAbrir(LocalDate.now()))
        }
    }

    fun trocarAnoLetivo(ano: AnoLetivoDomain) {
        mudar(Mudanca.Trocou) {
            contextoRepo.selecionarAnoLetivo(ano.id, ano.periodo.periodoAoAbrir(LocalDate.now()))
        }
    }

    fun renomearAluno(nome: String) {
        val aluno = alunoAberto() ?: return
        mudar(Mudanca.Renomeou) { renomearAluno(aluno, nome) }
    }

    /** Apaga o aluno aberto com tudo o que é dele; o app passa para outro, ou volta ao onboarding. */
    fun excluirAluno() {
        val aluno = alunoAberto() ?: return
        mudar(Mudanca.Excluiu) { excluirAluno(aluno) }
    }

    /** A tela já reagiu ao fim da mudança (voltou ao início): pode mudar de novo. */
    fun mudancaVista() {
        _mudanca.value = Mudanca.Parada
    }

    private fun alunoAberto() = (state.value as? AlunoUiState.Sucesso)?.perfil?.aluno

    private fun mudar(concluida: Mudanca, bloco: suspend () -> Unit) {
        if (_mudanca.value == Mudanca.Mudando) return // toque duplo
        _mudanca.value = Mudanca.Mudando
        viewModelScope.launch {
            bloco()
            _mudanca.value = concluida
        }
    }
}
