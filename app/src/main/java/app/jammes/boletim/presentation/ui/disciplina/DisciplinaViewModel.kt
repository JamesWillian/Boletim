package app.jammes.boletim.presentation.ui.disciplina

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.DisciplinaDetalhe
import app.jammes.boletim.domain.repository.AvaliacaoRepository
import app.jammes.boletim.domain.repository.ContextoRepository
import app.jammes.boletim.domain.usecase.ObterDisciplinaDetalhe
import app.jammes.boletim.presentation.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.time.Clock

sealed interface DisciplinaUiState {
    data object Carregando : DisciplinaUiState
    data object NaoEncontrada : DisciplinaUiState
    data class Sucesso(val detalhe: DisciplinaDetalhe) : DisciplinaUiState
}

@HiltViewModel
class DisciplinaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    contextoRepo: ContextoRepository,
    obterDisciplinaDetalhe: ObterDisciplinaDetalhe,
    private val avaliacaoRepo: AvaliacaoRepository,
): ViewModel() {

    // Argumento da rota que abriu a tela. Cada disciplina aberta ganha seu próprio ViewModel,
    // então o id não muda durante a vida dele.
    private val disciplinaId = savedStateHandle.toRoute<Routes.Boletim.Disciplina>().disciplinaId

    val state: StateFlow<DisciplinaUiState> =
        obterDisciplinaDetalhe(disciplinaId, contextoRepo.contexto.filterNotNull())
            .map { detalhe ->
                if (detalhe == null)
                    DisciplinaUiState.NaoEncontrada
                else
                    DisciplinaUiState.Sucesso(detalhe)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DisciplinaUiState.Carregando
            )

    /** Ponto de partida do formulário: no período que está na tela, valendo 10, com peso 1 e a data de hoje. */
    fun novaAvaliacao(periodoId: Long) = AvaliacaoDomain(
        disciplinaId = disciplinaId,
        periodoId = periodoId,
        nome = "",
        nota = null,
        notaMaxima = 10.0,
        peso = 1.0,
        data = LocalDate.now(),
        criadoEm = Clock.System.now(),
    )

    fun salvar(avaliacao: AvaliacaoDomain) {
        viewModelScope.launch {
            avaliacaoRepo.upsert(avaliacao)
        }
    }

    fun excluir(avaliacao: AvaliacaoDomain) {
        viewModelScope.launch {
            avaliacaoRepo.delete(avaliacao)
        }
    }
}
