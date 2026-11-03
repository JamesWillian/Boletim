package app.jammes.boletim.presentation.ui.plano

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.OfertaDoPro
import app.jammes.boletim.domain.model.Plano
import app.jammes.boletim.domain.model.ResultadoDaCompra
import app.jammes.boletim.domain.repository.LojaDoPro
import app.jammes.boletim.domain.repository.PlanoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** O que a loja tem a dizer na folha do Pro, depois de uma compra ou de um "Restaurar compra". */
enum class AvisoDaLoja { PENDENTE, INDISPONIVEL, SEM_COMPRA, SEM_RESPOSTA }

/** A loja na folha do Pro: trabalhando, com o botão esperando, e o aviso do que aconteceu. */
data class EstadoDaLoja(
    val ocupada: Boolean = false, // comprando ou conferindo: um toque duplo não abre duas vezes
    val aviso: AvisoDaLoja? = null,
)

/** O aviso de uma compra. Comprado não tem: o Pro chega pelo plano, e a folha fecha sozinha. */
internal fun avisoDaCompra(resultado: ResultadoDaCompra): AvisoDaLoja? = when (resultado) {
    ResultadoDaCompra.Comprado, ResultadoDaCompra.Cancelado -> null
    ResultadoDaCompra.Pendente -> AvisoDaLoja.PENDENTE
    ResultadoDaCompra.Indisponivel -> AvisoDaLoja.INDISPONIVEL
}

/** O aviso de um "Restaurar compra", pelo que a loja [achou]: null quando ela não conseguiu conferir. */
internal fun avisoDaRestauracao(achou: Boolean?): AvisoDaLoja? = when (achou) {
    true -> null // o Pro voltou: a folha fecha sozinha
    false -> AvisoDaLoja.SEM_COMPRA
    null -> AvisoDaLoja.SEM_RESPOSTA
}

/**
 * O plano do app para as telas: o que vale agora, o preço, a compra e a restauração do Pro, e a chave
 * "Pro de teste" do debug.
 */
@HiltViewModel
class PlanoViewModel @Inject constructor(
    private val repo: PlanoRepository,
    private val loja: LojaDoPro,
) : ViewModel() {

    /** null até a primeira leitura. */
    val plano: StateFlow<Plano?> = repo.plano
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** O preço do Pro como a loja informa; null enquanto carrega, sem a loja ou sem o produto à venda. */
    val oferta: StateFlow<OfertaDoPro?> = loja.oferta
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _estadoDaLoja = MutableStateFlow(EstadoDaLoja())
    val estadoDaLoja: StateFlow<EstadoDaLoja> = _estadoDaLoja.asStateFlow()

    /** Só no build de debug: o app passa a agir como Pro ou como grátis, para testar os limites. */
    fun escolherProDeTeste(ligado: Boolean) {
        viewModelScope.launch { repo.salvarProDeTeste(ligado) }
    }

    /** O botão da folha do Pro: abre a compra da Play. O Pro chega pelo [plano], quando ela confirma o pagamento. */
    fun desbloquear() = usarALoja { avisoDaCompra(loja.comprar()) }

    /** O "Restaurar compra": confere de novo as compras da conta da Play. */
    fun restaurar() = usarALoja { avisoDaRestauracao(loja.sincronizar()) }

    /**
     * Ao voltar para o app: confere as compras feitas fora dele (outro aparelho, um pagamento pendente
     * aprovado) e os estornos. Sem nada na tela: quem mostra o resultado é o [plano].
     */
    fun sincronizar() {
        viewModelScope.launch {
            try {
                loja.sincronizar()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // A próxima volta ao app tenta de novo
            }
        }
    }

    /** A folha fechou: o aviso era dela e não volta quando ela abrir de novo. */
    fun avisoVisto() {
        _estadoDaLoja.update { it.copy(aviso = null) }
    }

    private fun usarALoja(acao: suspend () -> AvisoDaLoja?) {
        if (_estadoDaLoja.value.ocupada) return // toque duplo
        _estadoDaLoja.value = EstadoDaLoja(ocupada = true)
        viewModelScope.launch {
            val aviso = try {
                acao()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AvisoDaLoja.INDISPONIVEL
            }
            _estadoDaLoja.value = EstadoDaLoja(aviso = aviso)
        }
    }
}
