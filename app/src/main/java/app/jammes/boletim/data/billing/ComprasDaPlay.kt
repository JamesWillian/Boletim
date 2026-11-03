package app.jammes.boletim.data.billing

import app.jammes.boletim.domain.model.OfertaDoPro
import app.jammes.boletim.domain.model.ResultadoDaCompra
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/*
 * As regras da compra do Pro, sem nenhuma classe da Play: a LojaGooglePlay traduz o que a Play
 * responde para os tipos daqui, e assim as regras se testam na JVM.
 */

/** O produto de compra única do Pro, criado na Play Console. O ID é permanente: não dá para trocar nem reaproveitar. */
const val PRODUTO_PRO = "boletim_pro"

/** O estado de uma compra como a Play informa: só a comprada dá o Pro. */
enum class EstadoDaCompra { COMPRADA, PENDENTE, OUTRO }

/** Uma compra da Play, só com o que o app usa. */
data class CompraDaPlay(
    val produtos: List<String>,
    val estado: EstadoDaCompra,
    val reconhecida: Boolean, // o app já avisou a Play que entregou o Pro
    val token: String, // o que identifica a compra para a Play, no reconhecimento
)

/** Uma oferta do Pro na Play: o preço que vale e, quando há desconto, o cheio. */
data class OfertaDaPlay(
    val token: String?, // vai na compra, para a Play saber qual oferta foi escolhida; null se ela não mandar
    val precoMicros: Long, // em milionésimos: R$ 14,90 = 14_900_000
    val precoFormatado: String, // como a Play escreve, já com a moeda
    val cheioMicros: Long?, // o preço sem desconto; null quando não há desconto
    val moeda: String, // o código da moeda, como "BRL"
)

/** Se alguma das [compras] dá o Pro: uma compra dele concluída. A pendente ainda não dá. */
internal fun temPro(compras: List<CompraDaPlay>): Boolean =
    compras.any { it.ehDoPro && it.estado == EstadoDaCompra.COMPRADA }

/**
 * As compras do Pro concluídas que o app ainda não reconheceu. Sem o reconhecimento em 3 dias, a Play
 * devolve o dinheiro e tira o Pro.
 */
internal fun aReconhecer(compras: List<CompraDaPlay>): List<CompraDaPlay> =
    compras.filter { it.ehDoPro && it.estado == EstadoDaCompra.COMPRADA && !it.reconhecida }

/** Como terminou uma compra que a Play respondeu sem erro, pelas [compras] que vieram na resposta. */
internal fun resultadoDaCompra(compras: List<CompraDaPlay>): ResultadoDaCompra = when {
    temPro(compras) -> ResultadoDaCompra.Comprado
    compras.any { it.ehDoPro && it.estado == EstadoDaCompra.PENDENTE } -> ResultadoDaCompra.Pendente
    else -> ResultadoDaCompra.Cancelado
}

/** A oferta que a folha mostra e a compra usa: a mais barata das que a Play libera para a pessoa. */
internal fun melhorOferta(ofertas: List<OfertaDaPlay>): OfertaDaPlay? =
    ofertas.minByOrNull { it.precoMicros }

/**
 * A [oferta] como a folha mostra. Com desconto, vão também o preço cheio, escrito no [local], e o
 * desconto em %, arredondado para baixo: a folha nunca anuncia mais desconto do que dá.
 */
internal fun ofertaDoPro(oferta: OfertaDaPlay, local: Locale = Locale.getDefault()): OfertaDoPro {
    val cheio = oferta.cheioMicros?.takeIf { it > oferta.precoMicros }
    return OfertaDoPro(
        preco = oferta.precoFormatado,
        precoCheio = cheio?.let { formatarPreco(it, oferta.moeda, local) },
        desconto = cheio?.let { ((it - oferta.precoMicros) * 100 / it).toInt() },
    )
}

/** Um preço em milionésimos escrito com a moeda, como a Play escreve: R$ 14,90. */
internal fun formatarPreco(micros: Long, moeda: String, local: Locale): String =
    NumberFormat.getCurrencyInstance(local)
        .apply { currency = Currency.getInstance(moeda) }
        // BigDecimal, e não Double: 14_900_000 milionésimos são 14,90 exatos
        .format(BigDecimal.valueOf(micros, 6))

private val CompraDaPlay.ehDoPro: Boolean
    get() = PRODUTO_PRO in produtos
