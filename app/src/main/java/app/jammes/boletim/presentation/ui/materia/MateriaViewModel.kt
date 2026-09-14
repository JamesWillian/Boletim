package app.jammes.boletim.presentation.ui.materia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.domain.repository.MateriaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MateriaUiState(
    val items: List<MateriaDomain> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MateriaViewModel @Inject constructor(
    private val materiaRepo: MateriaRepository
): ViewModel() {

    val uiState: StateFlow<MateriaUiState> = materiaRepo
        .observeMaterias()
        .map { list -> MateriaUiState(items = list, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MateriaUiState()
        )

    fun salvar(materia: MateriaDomain) {
        viewModelScope.launch {
            materiaRepo.upsert(materia)
        }
    }

    fun deletar(materia: MateriaDomain) {
        viewModelScope.launch {
            materiaRepo.delete(materia)
        }
    }
}