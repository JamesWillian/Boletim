package app.jammes.boletim.presentation.ui.disciplina

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.repository.AvaliacaoRepository
import app.jammes.boletim.domain.repository.DisciplinaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DisciplinaUiState(
    val disciplina: DisciplinaDomain? = null,
    val avaliacoes: List<AvaliacaoDomain> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DisciplinaViewModel @Inject constructor(
    private val disciplinaRepo: DisciplinaRepository,
    private val avaliacaoRepo: AvaliacaoRepository
): ViewModel() {

    private val disciplinaSelecionada = MutableStateFlow(0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DisciplinaUiState> = disciplinaSelecionada
        .flatMapLatest { id ->
            combine(
                disciplinaRepo.observeById(id),
                avaliacaoRepo.observeByDisciplina(id)
            ) { disciplina, avaliacoes ->
                DisciplinaUiState(disciplina = disciplina, avaliacoes = avaliacoes, isLoading = false)
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DisciplinaUiState()
        )

    fun setDiscicplina(id: Long) {
        disciplinaSelecionada.value = id
    }
}