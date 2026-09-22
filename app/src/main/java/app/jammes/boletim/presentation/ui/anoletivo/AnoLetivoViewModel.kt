package app.jammes.boletim.presentation.ui.anoletivo

import androidx.lifecycle.ViewModel
import app.jammes.boletim.domain.model.AnoLetivoDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

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