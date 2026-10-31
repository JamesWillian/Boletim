package app.jammes.boletim.presentation.ui.plano

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.Plano
import app.jammes.boletim.domain.repository.PlanoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** O plano do app para as telas: o que vale agora, o botão da folha do Pro e a chave "Pro de teste" do debug. */
@HiltViewModel
class PlanoViewModel @Inject constructor(
    private val repo: PlanoRepository,
) : ViewModel() {

    /** null até a primeira leitura. */
    val plano: StateFlow<Plano?> = repo.plano
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Só no build de debug: o app passa a agir como Pro ou como grátis, para testar os limites. */
    fun escolherProDeTeste(ligado: Boolean) {
        viewModelScope.launch { repo.salvarProDeTeste(ligado) }
    }

    /**
     * O botão da folha do Pro. Ainda não há a loja para comprar: por enquanto ele liga a chave "Pro de
     * teste", que só vale no debug, o único build com os limites do grátis ligados.
     */
    fun desbloquear() = escolherProDeTeste(true)
}
