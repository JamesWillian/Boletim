package app.jammes.boletim.presentation.ui.anoletivo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.AjustesAnoLetivo
import app.jammes.boletim.domain.repository.ContextoRepository
import app.jammes.boletim.domain.usecase.AjusteBloqueadoException
import app.jammes.boletim.domain.usecase.ObterAjustesAnoLetivo
import app.jammes.boletim.domain.usecase.SalvarAjustesAnoLetivo
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * As matérias do ano direto no sheet, sem a tela de ajustes (o "Adicionar matérias" do boletim vazio).
 * Grava pelo mesmo caminho do Salvar dos ajustes, só com as matérias mudadas.
 */
@HiltViewModel
class MateriasDoAnoViewModel @Inject constructor(
    private val contextoRepo: ContextoRepository,
    private val obterAjustes: ObterAjustesAnoLetivo,
    private val salvarAjustes: SalvarAjustesAnoLetivo,
) : ViewModel() {

    // null enquanto lê o banco
    private val _ajustes = MutableStateFlow<AjustesAnoLetivo?>(null)
    val ajustes: StateFlow<AjustesAnoLetivo?> = _ajustes.asStateFlow()

    private val _salvamento = MutableStateFlow<Salvamento>(Salvamento.Parado)
    val salvamento: StateFlow<Salvamento> = _salvamento.asStateFlow()

    /**
     * Lê o ano aberto no app a cada vez que o sheet abre. O ViewModel vive com o boletim, e uma
     * leitura antiga gravaria por cima do que mudou desde então (outro aluno, outro ano, um Salvar).
     */
    fun abrir() {
        _ajustes.value = null
        _salvamento.value = Salvamento.Parado
        viewModelScope.launch {
            val contexto = contextoRepo.contexto.filterNotNull().first()
            _ajustes.value = obterAjustes(contexto)
        }
    }

    fun salvar(formulario: FormularioAnoLetivo) {
        val ajustes = _ajustes.value ?: return
        val gravacao = formulario.paraGravar(ajustes) ?: return
        if (_salvamento.value == Salvamento.Salvando) return // toque duplo no Salvar

        _salvamento.value = Salvamento.Salvando
        viewModelScope.launch {
            _salvamento.value = try {
                salvarAjustes(ajustes, gravacao.anoLetivo, gravacao.regra, gravacao.materiaIds, gravacao.materiasNovas)
                Salvamento.Salvo
            } catch (e: AjusteBloqueadoException) {
                Salvamento.Falhou(e.message ?: "Não deu para salvar")
            }
        }
    }
}
