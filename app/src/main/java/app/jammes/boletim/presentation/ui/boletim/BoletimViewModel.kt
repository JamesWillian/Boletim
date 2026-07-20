package app.jammes.boletim.presentation.ui.boletim

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.BoletimDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.repository.AlunoRepository
import app.jammes.boletim.domain.repository.AnoLetivoRepository
import app.jammes.boletim.domain.repository.BoletimRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BoletimItem(
    val id: String,
    val disciplina: String,
    val nota: Double
)

data class BoletimUiState(
    val aluno: AlunoDomain? = null,
    val periodoSelecionado: PeriodoDomain? = null,
    val boletimList: List<BoletimItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class BoletimViewModel @Inject constructor(
    alunoRepository: AlunoRepository,
    anoLetivoRepository: AnoLetivoRepository,
//    private val boletimRepository: BoletimRepository,
): ViewModel() {

    private val periodoSelecionado = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BoletimUiState> =
        combine(
            periodoSelecionado,
            alunoRepository.observeAluno(),
            anoLetivoRepository.observeAllPeriodos()
        ) { periodoId, aluno, periodos ->

            val periodoPadrao = periodoId ?: aluno?.periodoId

            BoletimUiState(
                aluno = aluno,
                periodoSelecionado = periodos.find { it.id == periodoPadrao },
                isLoading = false
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            BoletimUiState()
        )

    fun onSelectPeriodo(periodoId: String) {
        periodoSelecionado.value = periodoId
    }
}