package app.jammes.boletim.presentation.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.LimiteDoGratisException
import app.jammes.boletim.domain.model.PerfilAluno
import app.jammes.boletim.domain.repository.ContextoRepository
import app.jammes.boletim.domain.repository.RegistroDeFalhas
import app.jammes.boletim.domain.usecase.AnoLetivoRepetidoException
import app.jammes.boletim.domain.usecase.CriarAnoLetivo
import app.jammes.boletim.domain.usecase.ObterPerfilAluno
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NovoAnoLetivoViewModel @Inject constructor(
    contextoRepo: ContextoRepository,
    obterPerfil: ObterPerfilAluno,
    private val criarAnoLetivo: CriarAnoLetivo,
    private val registroDeFalhas: RegistroDeFalhas,
) : ViewModel() {

    /** O aluno aberto, que ganha o ano novo, com os anos que ele já tem; null enquanto carrega. */
    val perfil: StateFlow<PerfilAluno?> = obterPerfil(contextoRepo.contexto.filterNotNull())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _salvando = MutableStateFlow(false)

    /** Enquanto grava, o botão espera: um toque duplo não cria dois anos. */
    val salvando: StateFlow<Boolean> = _salvando.asStateFlow()

    private val _erros = Channel<String>(Channel.BUFFERED)
    val erros: Flow<String> = _erros.receiveAsFlow()

    private val _criado = Channel<Unit>(Channel.BUFFERED)

    /** O ano foi gravado e já está aberto. */
    val criado: Flow<Unit> = _criado.receiveAsFlow()

    fun criar(formulario: FormularioNovoAnoLetivo) {
        val alunoId = perfil.value?.aluno?.id ?: return
        if (!formulario.datasCertas || _salvando.value) return
        _salvando.value = true
        viewModelScope.launch {
            try {
                criarAnoLetivo(
                    alunoId = alunoId,
                    ano = formulario.ano,
                    serie = formulario.serie.texto(),
                    tipo = formulario.tipoPeriodo,
                    periodos = formulario.periodos(),
                )
                _criado.send(Unit)
                // Deu certo: com o ano aberto, a tela sai e leva aos ajustes dele
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _salvando.value = false
                // O ano repetido e o do Pro a tela já não deixa escolher: sobra o caso de algo mudar no meio
                val motivo = (e as? AnoLetivoRepetidoException)?.message ?: (e as? LimiteDoGratisException)?.message
                if (motivo == null) registroDeFalhas.registrar(e)
                _erros.send(motivo ?: "Não deu para criar. Tente de novo.")
            }
        }
    }
}
