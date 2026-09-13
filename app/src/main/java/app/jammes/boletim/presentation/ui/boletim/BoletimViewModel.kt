package app.jammes.boletim.presentation.ui.boletim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.BoletimItem
import app.jammes.boletim.domain.repository.DisciplinaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BoletimUiState(
    val items: List<BoletimItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class BoletimViewModel @Inject constructor(
    disciplinaRepository: DisciplinaRepository
): ViewModel() {

    private val anoLetivoId = MutableStateFlow(0L)
    private val periodoId = MutableStateFlow(0L)

    val uiState: StateFlow<BoletimUiState> =
        disciplinaRepository
            .observarBoletim(1L, 1L)
            .map { list -> BoletimUiState(items = list, isLoading = false) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BoletimUiState()
            )

    fun setAnoLetivoId(anoLetivoId: Long) {
        this.anoLetivoId.value = anoLetivoId
    }

    fun setPeriodoId(periodoId: Long) {
        this.periodoId.value = periodoId
    }
}