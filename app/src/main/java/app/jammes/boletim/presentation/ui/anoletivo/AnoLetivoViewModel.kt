package app.jammes.boletim.presentation.ui.anoletivo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.repository.AlunoRepository
import app.jammes.boletim.domain.repository.AnoLetivoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    private val repository: AnoLetivoRepository,
    private val alunoRepository: AlunoRepository
): ViewModel() {

    private val selectedId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AnoLetivoUiState> =
        combine(
            repository.observeAll(),
            selectedId,
            alunoRepository.observeAluno()
        ) { anos, id, aluno ->
            val anoId = id ?: aluno?.anoLetivoId
            val anoPadrao = anos.find { it.id == aluno?.anoLetivoId }
            val periodoPadrao = anoPadrao?.periodo?.find { it.id == aluno?.periodoId }

            AnoLetivoUiState(
                items = anos,
                anoLetivoSelecionado = anos.find { it.id == anoId },
                anoLetivoPadrao = anoPadrao,
                periodoPadrao = periodoPadrao?.let { "${it.periodo} Semestre" },
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

    fun save(anoletivo: AnoLetivoDomain) {
        viewModelScope.launch {
            repository.upsert(anoletivo)
        }
    }

    fun delete(anoletivo: AnoLetivoDomain) {
        viewModelScope.launch {
            repository.delete(anoletivo)
        }
    }

    fun savePeriodo(periodo: PeriodoDomain) {
        viewModelScope.launch {
            repository.upsertPeriodo(periodo)
        }
    }

    fun deletePeriodo(periodo: PeriodoDomain) {
        viewModelScope.launch {
            repository.deletePeriodo(periodo)
        }
    }

    fun setAnoLetivoPadrao(anoLetivoId: String) {
        viewModelScope.launch {
            alunoRepository.setAnoLetivoPadrao(anoLetivoId)
        }
    }

    fun setPeriodoPadrao(periodoId: String) {
        viewModelScope.launch {
            alunoRepository.setPeriodoPadrao(periodoId)
        }
    }
}