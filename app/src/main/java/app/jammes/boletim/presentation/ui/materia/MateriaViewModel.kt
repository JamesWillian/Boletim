package app.jammes.boletim.presentation.ui.materia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.LimiteDoGratisException
import app.jammes.boletim.domain.model.MateriaDoCadastro
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.domain.model.Plano
import app.jammes.boletim.domain.repository.PlanoRepository
import app.jammes.boletim.domain.usecase.ExcluirMateria
import app.jammes.boletim.domain.usecase.MateriaProtegidaException
import app.jammes.boletim.domain.usecase.ObservarMateriasDoCadastro
import app.jammes.boletim.domain.usecase.SalvarMateria
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MateriaUiState {
    data object Carregando : MateriaUiState
    data class Sucesso(val materias: List<MateriaDoCadastro>, val plano: Plano) : MateriaUiState
}

@HiltViewModel
class MateriaViewModel @Inject constructor(
    observarMaterias: ObservarMateriasDoCadastro,
    planoRepo: PlanoRepository,
    private val salvarMateria: SalvarMateria,
    private val excluirMateria: ExcluirMateria,
) : ViewModel() {

    /** O cadastro e o plano ao vivo: o que muda aqui, e o Pro desbloqueado na folha, aparecem sem ler de novo. */
    val state: StateFlow<MateriaUiState> =
        combine<List<MateriaDoCadastro>, Plano, MateriaUiState>(observarMaterias(), planoRepo.plano) { materias, plano ->
            MateriaUiState.Sucesso(materias, plano)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MateriaUiState.Carregando)

    private val _avisos = Channel<String>(Channel.BUFFERED)

    /** O que não deu para gravar. O sheet já fechou: quem mostra é a tela. */
    val avisos: Flow<String> = _avisos.receiveAsFlow()

    fun salvar(materia: MateriaDomain) = gravar { salvarMateria(materia) }

    fun excluir(materia: MateriaDomain) = gravar { excluirMateria(materia) }

    // A tela já não deixa chegar assim nos casos de uso: sobra o caso de o Pro sair no meio do caminho
    private fun gravar(acao: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                acao()
            } catch (e: LimiteDoGratisException) {
                _avisos.send(e.message ?: NAO_DEU)
            } catch (e: MateriaProtegidaException) {
                _avisos.send(e.message ?: NAO_DEU)
            }
        }
    }

    private companion object {
        const val NAO_DEU = "Não deu para salvar"
    }
}
