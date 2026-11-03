package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.OfertaDoPro
import app.jammes.boletim.domain.model.ResultadoDaCompra
import kotlinx.coroutines.flow.Flow

/**
 * A loja do Pro: o preço, a compra e a conferência das compras da conta. Quem decide o plano continua
 * sendo o [PlanoRepository]; a loja só guarda o que a Play confirmou, e o plano lê de lá.
 */
interface LojaDoPro {

    /** O preço do Pro; null enquanto carrega, sem a loja ou sem o produto à venda. */
    val oferta: Flow<OfertaDoPro?>

    /** Abre a compra e espera a resposta. O Pro chega pelo plano, quando a loja confirma o pagamento. */
    suspend fun comprar(): ResultadoDaCompra

    /**
     * Confere as compras da conta da loja e guarda o que achar. true: achou o Pro; false: não achou;
     * null: não deu para conferir, e o que estava guardado continua.
     */
    suspend fun sincronizar(): Boolean?
}
