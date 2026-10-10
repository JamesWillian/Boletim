package app.jammes.boletim.presentation.ui.aluno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.AlunoComAno
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PerfilAluno
import app.jammes.boletim.domain.model.periodoAoAbrir
import app.jammes.boletim.domain.repository.ContextoRepository
import app.jammes.boletim.domain.usecase.AnoLetivoRepetidoException
import app.jammes.boletim.domain.usecase.CriarAnoLetivo
import app.jammes.boletim.domain.usecase.ObterPerfilAluno
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface AlunoUiState {
    data object Carregando : AlunoUiState
    data object NaoEncontrado : AlunoUiState
    data class Sucesso(val perfil: PerfilAluno) : AlunoUiState
}

/**
 * Andamento de uma troca ou de um ano novo. Trocou e CriouAnoLetivo fecham o perfil; Falhou mostra o
 * motivo e deixa tentar de novo.
 */
sealed interface Mudanca {
    data object Parada : Mudanca
    data object Mudando : Mudanca
    data object Trocou : Mudanca
    data object CriouAnoLetivo : Mudanca
    data class Falhou(val motivo: String) : Mudanca
}

@HiltViewModel
class AlunoViewModel @Inject constructor(
    private val contextoRepo: ContextoRepository,
    obterPerfil: ObterPerfilAluno,
    private val criarAnoLetivo: CriarAnoLetivo,
): ViewModel() {

    val state: StateFlow<AlunoUiState> = obterPerfil(contextoRepo.contexto.filterNotNull())
        .map { perfil ->
            if (perfil == null) AlunoUiState.NaoEncontrado else AlunoUiState.Sucesso(perfil)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AlunoUiState.Carregando
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

    /** Cria o ano com tudo no padrão e já abre ele; a tela leva então aos ajustes dele. */
    fun criarAnoLetivo(ano: Int, serie: String) {
        val alunoId = (state.value as? AlunoUiState.Sucesso)?.perfil?.aluno?.id ?: return
        mudar(Mudanca.CriouAnoLetivo) { criarAnoLetivo(alunoId, ano, serie) }
    }

    fun falhaVista() {
        _mudanca.value = Mudanca.Parada
    }

    private fun mudar(concluida: Mudanca, bloco: suspend () -> Unit) {
        if (_mudanca.value == Mudanca.Mudando) return // toque duplo
        _mudanca.value = Mudanca.Mudando
        viewModelScope.launch {
            _mudanca.value = try {
                bloco()
                concluida
            } catch (e: AnoLetivoRepetidoException) {
                Mudanca.Falhou(e.message ?: "Não deu para criar o ano letivo")
            }
        }
    }
}
