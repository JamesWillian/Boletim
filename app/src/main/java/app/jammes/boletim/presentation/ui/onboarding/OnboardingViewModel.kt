package app.jammes.boletim.presentation.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.data.local.DadosDeTeste
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.domain.repository.DicasRepository
import app.jammes.boletim.domain.repository.MateriaRepository
import app.jammes.boletim.domain.usecase.ComecarBoletim
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val comecarBoletim: ComecarBoletim,
    private val dadosDeTeste: DadosDeTeste,
    private val dicasRepository: DicasRepository,
    materiaRepository: MateriaRepository,
) : ViewModel() {

    /** O cadastro de matérias, para o passo das matérias. */
    val materias: StateFlow<List<MateriaDomain>> = materiaRepository.observeMaterias()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _salvando = MutableStateFlow(false)

    /** Enquanto grava, o botão espera: um toque duplo não cria dois alunos. */
    val salvando: StateFlow<Boolean> = _salvando.asStateFlow()

    private val _erros = Channel<String>(Channel.BUFFERED)
    val erros: Flow<String> = _erros.receiveAsFlow()

    private val _concluido = Channel<Unit>(Channel.BUFFERED)

    /** O aluno e o ano foram gravados e já estão abertos. */
    val concluido: Flow<Unit> = _concluido.receiveAsFlow()

    /**
     * [sugeridas]: as matérias que a série sugere, valendo enquanto a pessoa não escolhe outras.
     * [pedirDica]: mostrar a dica da primeira nota no boletim.
     */
    fun concluir(formulario: FormularioOnboarding, sugeridas: Set<Long>, pedirDica: Boolean = true) {
        val primeiro = formulario.paraGravar(sugeridas) ?: return
        if (_salvando.value) return
        _salvando.value = true
        viewModelScope.launch {
            try {
                // Antes de salvar o Contexto: o boletim já abre sabendo que tem a dica para mostrar
                if (pedirDica) dicasRepository.pedirDicaDaPrimeiraNota()
                comecarBoletim(primeiro)
                _concluido.send(Unit)
                // Deu certo: com o Contexto salvo, o AppRoot troca esta tela pelo boletim
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _salvando.value = false
                _erros.send("Não deu para salvar. Tente de novo.")
            }
        }
    }

    /** Temporário, só em debug: o botão das boas-vindas que carrega os dados de teste. */
    fun carregarDadosDeTeste() {
        viewModelScope.launch { dadosDeTeste.carregar() }
    }
}
