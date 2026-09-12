package app.jammes.boletim.presentation.ui.anoletivo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.repository.AlunoRepository
import app.jammes.boletim.domain.repository.AnoLetivoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnoLetivoUiState(
    val items: List<AnoLetivoDomain> = emptyList(),
    val anoLetivoSelecionado: AnoLetivoDomain? = null,
    val anoLetivoPadrao: AnoLetivoDomain? = null,
    val periodoPadrao: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class AnoLetivoViewModel @Inject constructor(
): ViewModel() {

}