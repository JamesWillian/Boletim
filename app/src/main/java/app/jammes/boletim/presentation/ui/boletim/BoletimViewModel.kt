package app.jammes.boletim.presentation.ui.boletim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.BoletimItem
import app.jammes.boletim.domain.repository.ContextoRepository
import app.jammes.boletim.domain.repository.DisciplinaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BoletimUiState(
    val items: List<BoletimItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class BoletimViewModel @Inject constructor(
    contextoRepo: ContextoRepository,
    disciplinaRepo: DisciplinaRepository
): ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BoletimUiState> =
        contextoRepo.contexto
            .filterNotNull()
            .map { it.anoLetivoId to it.periodoId }
            .distinctUntilChanged()
            .flatMapLatest { (anoId, periodoId) ->
                disciplinaRepo
                    .observarBoletim(anoId, periodoId)
                    .map { list -> BoletimUiState(items = list, isLoading = false) }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BoletimUiState()
            )
}