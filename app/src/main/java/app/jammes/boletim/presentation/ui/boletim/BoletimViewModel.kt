package app.jammes.boletim.presentation.ui.boletim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.Boletim
import app.jammes.boletim.domain.repository.ContextoRepository
import app.jammes.boletim.domain.repository.DicasRepository
import app.jammes.boletim.domain.usecase.ObterBoletim
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BoletimUiState {
    data object Carregando : BoletimUiState
    data object SemDisciplinas : BoletimUiState
    data class Sucesso(val boletim: Boletim) : BoletimUiState
}

@HiltViewModel
class BoletimViewModel @Inject constructor(
    contextoRepo: ContextoRepository,
    obterBoletim: ObterBoletim,
    private val dicasRepository: DicasRepository,
): ViewModel() {

    /** A dica "Lance a primeira nota": logo depois do onboarding, até a pessoa dispensar. */
    val dicaDaPrimeiraNota: StateFlow<Boolean> = dicasRepository.dicaDaPrimeiraNota
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun dispensarDica() {
        viewModelScope.launch { dicasRepository.dispensarDicaDaPrimeiraNota() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BoletimUiState> =
        obterBoletim(contextoRepo.contexto.filterNotNull())
            .map { boletim ->
                if (boletim.disciplinas.isEmpty())
                    BoletimUiState.SemDisciplinas
                else
                    BoletimUiState.Sucesso(boletim)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BoletimUiState.Carregando
            )
}