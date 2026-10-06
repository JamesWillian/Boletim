package app.jammes.boletim.presentation.ui.disciplina

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.repository.AvaliacaoRepository
import app.jammes.boletim.domain.repository.DisciplinaRepository
import app.jammes.boletim.presentation.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DisciplinaUiState(
    val disciplina: DisciplinaDomain? = null,
    val avaliacoes: List<AvaliacaoDomain> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DisciplinaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val disciplinaRepo: DisciplinaRepository,
    private val avaliacaoRepo: AvaliacaoRepository
): ViewModel() {

    // Argumento da rota que abriu a tela. Cada disciplina aberta ganha seu próprio ViewModel,
    // então o id não muda durante a vida dele.
    private val disciplinaId = savedStateHandle.toRoute<Routes.Boletim.Disciplina>().disciplinaId

    val state: StateFlow<DisciplinaUiState> = combine(
        disciplinaRepo.observeById(disciplinaId),
        avaliacaoRepo.observeByDisciplina(disciplinaId)
    ) { disciplina, avaliacoes ->
        DisciplinaUiState(disciplina = disciplina, avaliacoes = avaliacoes, isLoading = false)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DisciplinaUiState()
    )
}
