package app.jammes.boletim.data.billing

import app.jammes.boletim.domain.model.ResultadoDaCompra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** As regras da compra do Pro: o que dá o Pro, o que reconhecer e qual oferta mostrar. */
class ComprasDaPlayTest {

    // Compras -----------------------------------------------------------------------------------

    @Test fun `a compra concluida do Pro da o Pro`() {
        assertTrue(temPro(listOf(compra(EstadoDaCompra.COMPRADA))))
    }

    @Test fun `a compra pendente ainda nao da o Pro`() {
        // Boleto ou pagamento lento: só quando a Play confirmar
        assertFalse(temPro(listOf(compra(EstadoDaCompra.PENDENTE))))
    }

    @Test fun `a compra de outro produto nao da o Pro`() {
        assertFalse(temPro(listOf(compra(EstadoDaCompra.COMPRADA, produto = "outro"))))
    }

    @Test fun `sem compras nao ha Pro`() {
        assertFalse(temPro(emptyList()))
    }

    @Test fun `reconhece so as compras do Pro concluidas e ainda nao reconhecidas`() {
        val nova = compra(EstadoDaCompra.COMPRADA, token = "nova")
        val compras = listOf(
            nova,
            compra(EstadoDaCompra.COMPRADA, reconhecida = true, token = "antiga"),
            compra(EstadoDaCompra.PENDENTE, token = "pendente"),
            compra(EstadoDaCompra.COMPRADA, produto = "outro", token = "outra"),
        )

        assertEquals(listOf(nova), aReconhecer(compras))
    }

    @Test fun `a resposta da compra vira o resultado que a folha mostra`() {
        assertEquals(ResultadoDaCompra.Comprado, resultadoDaCompra(listOf(compra(EstadoDaCompra.COMPRADA))))
        assertEquals(ResultadoDaCompra.Pendente, resultadoDaCompra(listOf(compra(EstadoDaCompra.PENDENTE))))
        assertEquals(ResultadoDaCompra.Cancelado, resultadoDaCompra(emptyList()))
    }

    // Ofertas -----------------------------------------------------------------------------------

    @Test fun `a melhor oferta e a mais barata`() {
        val promocao = oferta(precoMicros = 9_900_000, cheioMicros = 14_900_000, token = "promocao")

        assertEquals(promocao, melhorOferta(listOf(oferta(precoMicros = 14_900_000), promocao)))
    }

    @Test fun `sem ofertas nao ha o que mostrar`() {
        assertNull(melhorOferta(emptyList()))
    }

    @Test fun `sem desconto a folha mostra so o preco`() {
        val oferta = ofertaDoPro(oferta(precoMicros = 14_900_000, formatado = "R$ 14,90"), PT_BR)

        assertEquals("R$ 14,90", oferta.preco)
        assertNull(oferta.precoCheio)
        assertNull(oferta.desconto)
    }

    @Test fun `com desconto vem o preco cheio e o desconto arredondado para baixo`() {
        // De 14,90 por 9,90 são 33,6% de desconto: a folha diz 33
        val oferta = ofertaDoPro(oferta(precoMicros = 9_900_000, cheioMicros = 14_900_000, formatado = "R$ 9,90"), PT_BR)

        assertEquals("R$ 9,90", oferta.preco)
        assertEquals("R$ 14,90", oferta.precoCheio?.semEspacoFixo())
        assertEquals(33, oferta.desconto)
    }

    @Test fun `um preco cheio que nao e maior que o preco nao conta como desconto`() {
        val oferta = ofertaDoPro(oferta(precoMicros = 14_900_000, cheioMicros = 14_900_000), PT_BR)

        assertNull(oferta.precoCheio)
        assertNull(oferta.desconto)
    }

    @Test fun `o preco em milionesimos sai escrito com a moeda`() {
        assertEquals("R$ 14,90", formatarPreco(14_900_000, "BRL", PT_BR).semEspacoFixo())
        assertEquals("R$ 1.234,50", formatarPreco(1_234_500_000, "BRL", PT_BR).semEspacoFixo())
    }

    private fun compra(
        estado: EstadoDaCompra,
        produto: String = PRODUTO_PRO,
        reconhecida: Boolean = false,
        token: String = "token",
    ) = CompraDaPlay(produtos = listOf(produto), estado = estado, reconhecida = reconhecida, token = token)

    private fun oferta(
        precoMicros: Long,
        cheioMicros: Long? = null,
        formatado: String = "",
        token: String = "base",
    ) = OfertaDaPlay(token = token, precoMicros = precoMicros, precoFormatado = formatado, cheioMicros = cheioMicros, moeda = "BRL")

    // O Java separa "R$" do número com um espaço que não quebra a linha; nos testes, um espaço comum
    private fun String.semEspacoFixo() = replace(' ', ' ')

    private companion object {
        val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
    }
}
