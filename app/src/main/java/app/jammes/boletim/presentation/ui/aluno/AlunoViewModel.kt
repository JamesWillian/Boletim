package app.jammes.boletim.presentation.ui.aluno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PeriodoType
import app.jammes.boletim.domain.repository.AlunoRepository
import app.jammes.boletim.domain.repository.AnoLetivoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlunoUiState(
    val aluno: AlunoDomain? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class AlunoViewModel @Inject constructor(
    private val repository: AlunoRepository
): ViewModel() {

    val uiState: StateFlow<AlunoUiState> = repository
        .observeAluno().map { aluno ->
            AlunoUiState(
                aluno = aluno,
                isLoading = false
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AlunoUiState()
        )

    fun save(aluno: AlunoDomain) {
        viewModelScope.launch {
            repository.upsert(aluno)
        }
    }

    fun savePeriodoType(periodoType: PeriodoType) {
        viewModelScope.launch {
            repository.setPeriodoType(periodoType)
        }
    }
}