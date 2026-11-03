package app.jammes.boletim.data.billing

import android.content.Context
import android.util.Log
import app.jammes.boletim.data.local.preferences.PlanoPreferences
import app.jammes.boletim.domain.model.OfertaDoPro
import app.jammes.boletim.domain.model.ResultadoDaCompra
import app.jammes.boletim.domain.repository.LojaDoPro
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * A loja do Pro na Google Play, pela Play Billing Library. Fala com a Play Store do aparelho, que é
 * quem cobra e sabe o que a conta dela já comprou; o app nunca vê cartão nem e-mail.
 *
 * O que a Play confirma vai para o [PlanoPreferences], e é de lá que o plano lê: com o Pro guardado,
 * o app abre no Pro mesmo sem a Play responder. Só uma resposta da Play muda o que está guardado.
 */
@Singleton
class LojaGooglePlay @Inject constructor(
    @ApplicationContext context: Context,
    private val tela: TelaEmPrimeiroPlano,
    private val prefs: PlanoPreferences,
) : LojaDoPro {

    // Vive o tanto que o app vive: as compras que chegam pelo listener são guardadas aqui. Um erro
    // nesses trabalhos de fundo não derruba o app: fica no log, e a próxima sincronização tenta de novo
    private val escopo = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e -> Log.e(TAG, "A loja falhou", e) }
    )

    private val cliente: BillingClient = BillingClient.newBuilder(context)
        .setListener(::aoAtualizarCompras)
        // Boleto e pagamentos lentos: a compra chega PENDENTE e só depois vira COMPRADA
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        // Depois da primeira conexão, a biblioteca reconecta sozinha quando a Play Store cai
        .enableAutoServiceReconnection()
        .build()

    private val conexao = Mutex()

    @Volatile
    private var conectado = false

    // A compra aberta pelo comprar(), à espera da resposta no listener. Só mexida na thread principal
    private var compraEmAndamento: CompletableDeferred<ResultadoDaCompra>? = null

    private val _oferta = MutableStateFlow<OfertaDoPro?>(null)

    // Cada tela que passa a ler o preço pede uma consulta nova: a promoção aparece e some na hora certa
    override val oferta: Flow<OfertaDoPro?> = _oferta.onStart { escopo.launch { consultarOferta() } }

    override suspend fun comprar(): ResultadoDaCompra {
        // Um toque duplo não abre duas compras: o segundo espera a resposta do primeiro
        withContext(Dispatchers.Main) { compraEmAndamento }?.let { return it.await() }
        // Consulta de novo antes de comprar: um ProductDetails velho faz a compra falhar
        val produto = consultarOferta() ?: return ResultadoDaCompra.Indisponivel

        val espera = withContext(Dispatchers.Main) {
            val activity = tela.activity ?: return@withContext null
            val parametros = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(produto.detalhes)
                            // Qual oferta foi: a promoção, quando a folha mostrou ela
                            .apply { produto.oferta.token?.let(::setOfferToken) }
                            .build()
                    )
                )
                .build()
            val espera = CompletableDeferred<ResultadoDaCompra>()
            compraEmAndamento = espera
            // Abre a tela de pagamento da Play. A resposta chega depois, no aoAtualizarCompras
            val aberta = cliente.launchBillingFlow(activity, parametros)
            if (aberta.responseCode != BillingResponseCode.OK) {
                compraEmAndamento = null
                responderSemCompras(espera, aberta.responseCode)
            }
            espera
        } ?: return ResultadoDaCompra.Indisponivel

        return espera.await()
    }

    override suspend fun sincronizar(): Boolean? {
        if (!conectar()) return null
        val parametros = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val resposta = cliente.queryPurchasesAsync(parametros)
        // Sem resposta da Play, o que está guardado continua: quem comprou não perde o Pro offline
        if (resposta.billingResult.responseCode != BillingResponseCode.OK) return null

        // A consulta traz tudo o que a conta tem: um estorno some daqui, e o Pro sai junto
        val compras = resposta.purchasesList.map { it.paraCompraDaPlay() }
        val pro = temPro(compras)
        prefs.salvarComprado(pro)
        reconhecer(compras)
        return pro
    }

    /**
     * O que a Play responde depois de uma compra, na thread principal. Chega também sem o comprar(),
     * como um pagamento pendente que foi aprovado: por isso toda compra que vem é guardada.
     */
    private fun aoAtualizarCompras(resposta: BillingResult, compras: List<Purchase>?) {
        val espera = compraEmAndamento
        compraEmAndamento = null
        if (resposta.responseCode == BillingResponseCode.OK) {
            val daPlay = compras.orEmpty().map { it.paraCompraDaPlay() }
            escopo.launch { guardar(daPlay) }
            espera?.complete(resultadoDaCompra(daPlay))
        } else if (espera != null) {
            responderSemCompras(espera, resposta.responseCode)
        }
    }

    /** Responde ao comprar() quando a Play não trouxe compras: cancelada, já comprada ou um erro. */
    private fun responderSemCompras(espera: CompletableDeferred<ResultadoDaCompra>, codigo: Int) {
        when (codigo) {
            BillingResponseCode.USER_CANCELED -> espera.complete(ResultadoDaCompra.Cancelado)
            // A conta já tem o Pro, de uma compra que o app ainda não tinha visto: a consulta guarda ele
            BillingResponseCode.ITEM_ALREADY_OWNED -> escopo.launch {
                espera.complete(if (sincronizar() == true) ResultadoDaCompra.Comprado else ResultadoDaCompra.Indisponivel)
            }
            else -> espera.complete(ResultadoDaCompra.Indisponivel)
        }
    }

    /** Guarda o Pro quando ele vem nas [compras] e reconhece as novas. Não tira o Pro: isso é do sincronizar(). */
    private suspend fun guardar(compras: List<CompraDaPlay>) {
        if (temPro(compras)) prefs.salvarComprado(true)
        reconhecer(compras)
    }

    /**
     * Avisa a Play que o Pro foi entregue. Se falhar, a próxima consulta tenta de novo: a compra
     * continua sem reconhecimento até dar certo, e a Play só devolve o dinheiro depois de 3 dias.
     */
    private suspend fun reconhecer(compras: List<CompraDaPlay>) {
        for (compra in aReconhecer(compras)) {
            cliente.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(compra.token).build()
            )
        }
    }

    /**
     * O Pro como a Play vende agora, com a oferta mais barata, e a [oferta] atualizada com ela. null
     * sem a Play ou sem o produto à venda; num erro da Play, a [oferta] fica como estava.
     */
    private suspend fun consultarOferta(): ProdutoNaPlay? {
        if (!conectar()) return null
        val parametros = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUTO_PRO)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        val resposta = cliente.queryProductDetails(parametros)
        if (resposta.billingResult.responseCode != BillingResponseCode.OK) return null

        val detalhes = resposta.productDetailsList.orEmpty().firstOrNull { it.productId == PRODUTO_PRO }
        val oferta = detalhes?.let { melhorOferta(it.ofertas()) }
        // Sem o produto (ainda não criado na Play Console, ou desativado), a folha fica sem preço
        _oferta.value = oferta?.let { ofertaDoPro(it) }
        return if (detalhes != null && oferta != null) ProdutoNaPlay(detalhes, oferta) else null
    }

    /**
     * Conecta à Play Store, se ainda não conectou. false quando não deu: sem a Play Store no aparelho,
     * ou ela sem responder. Depois da primeira conexão, a biblioteca reconecta sozinha.
     */
    private suspend fun conectar(): Boolean = conexao.withLock {
        if (conectado) return@withLock true
        conectado = withTimeoutOrNull(TEMPO_PARA_CONECTAR) {
            suspendCancellableCoroutine { espera ->
                cliente.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(resultado: BillingResult) {
                        if (espera.isActive) espera.resume(resultado.responseCode == BillingResponseCode.OK)
                    }

                    // Caiu antes de terminar: a próxima chamada tenta de novo
                    override fun onBillingServiceDisconnected() {
                        if (espera.isActive) espera.resume(false)
                    }
                })
            }
        } ?: false
        conectado
    }

    /** O produto do Pro e a oferta escolhida dele: o que a compra leva para a Play. */
    private class ProdutoNaPlay(val detalhes: ProductDetails, val oferta: OfertaDaPlay)

    private companion object {
        const val TEMPO_PARA_CONECTAR = 15_000L
        const val TAG = "Loja"
    }
}

private fun Purchase.paraCompraDaPlay() = CompraDaPlay(
    produtos = products,
    estado = when (purchaseState) {
        Purchase.PurchaseState.PURCHASED -> EstadoDaCompra.COMPRADA
        Purchase.PurchaseState.PENDING -> EstadoDaCompra.PENDENTE
        else -> EstadoDaCompra.OUTRO
    },
    reconhecida = isAcknowledged,
    token = purchaseToken,
)

/** As ofertas de compra única do produto: a do preço normal e as promoções que valem para a pessoa. */
private fun ProductDetails.ofertas(): List<OfertaDaPlay> =
    (oneTimePurchaseOfferDetailsList ?: listOfNotNull(oneTimePurchaseOfferDetails)).map { oferta ->
        OfertaDaPlay(
            token = oferta.offerToken,
            precoMicros = oferta.priceAmountMicros,
            precoFormatado = oferta.formattedPrice,
            cheioMicros = oferta.fullPriceMicros,
            moeda = oferta.priceCurrencyCode,
        )
    }
