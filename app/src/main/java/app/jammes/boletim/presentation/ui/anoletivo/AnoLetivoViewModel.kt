package app.jammes.boletim.presentation.ui.anoletivo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.repository.AnoLetivoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AnoLetivoUiState(
    val items: List<AnoLetivoDomain> = emptyList(),
    val anoLetivoSelecionado: AnoLetivoDomain? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class AnoLetivoViewModel @Inject constructor(
    repository: AnoLetivoRepository
): ViewModel() {

    private val selectedId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AnoLetivoUiState> =
        combine(repository.observeAll(), selectedId) { anos, id ->
            AnoLetivoUiState(
                items = anos,
                anoLetivoSelecionado = anos.find { it.id == id },
                isLoading = false
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AnoLetivoUiState()
        )


    fun onAnoLetivoSelected(id: String) {
        selectedId.value = id
    }
}