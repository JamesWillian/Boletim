package app.jammes.boletim.presentation.ui.aluno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.repository.AlunoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import jakarta.inject.Inject

data class AlunoUiState(
    val aluno: List<AlunoDomain?> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AlunoViewModel @Inject constructor(
    repository: AlunoRepository
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
}