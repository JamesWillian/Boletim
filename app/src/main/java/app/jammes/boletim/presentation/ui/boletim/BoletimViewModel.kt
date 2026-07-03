package app.jammes.boletim.presentation.ui.boletim

import androidx.lifecycle.ViewModel
import app.jammes.boletim.domain.repository.AlunoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BoletimViewModel @Inject constructor(
    private val alunoRepository: AlunoRepository
): ViewModel() {
}