package app.jammes.boletim.presentation.ui.anoletivo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.repository.AnoLetivoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AnoLetivoUiState(
    val items: List<AnoLetivoDomain> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AnoLetivoViewModel @Inject constructor(
    repository: AnoLetivoRepository
): ViewModel() {

    val uiState: StateFlow<AnoLetivoUiState> =
        repository.observeAll()
            .map { AnoLetivoUiState(items = it, isLoading = false) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                AnoLetivoUiState()
            )
}