package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.repository.AvaliacaoNaLoja
import app.jammes.boletim.domain.repository.AvaliacaoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Quando o app pede a avaliação na loja, e o que ele lembra depois de pedir. */
class AvaliacaoNaLojaTest {

    @Test fun `com menos de 5 notas ainda nao pede`() {
        assertFalse(podePedirAvaliacaoNaLoja(notasLancadas = 0, ultimoPedido = null, hoje = HOJE))
        assertFalse(podePedirAvaliacaoNaLoja(notasLancadas = 4, ultimoPedido = null, hoje = HOJE))
    }

    @Test fun `da 5a nota em diante pede, se nunca pediu`() {
        assertTrue(podePedirAvaliacaoNaLoja(notasLancadas = 5, ultimoPedido = null, hoje = HOJE))
        assertTrue(podePedirAvaliacaoNaLoja(notasLancadas = 40, ultimoPedido = null, hoje = HOJE))
    }

    @Test fun `depois de um pedido, espera 4 meses`() {
        val pedido = LocalDate.of(2026, 3, 10)

        assertFalse(podePedirAvaliacaoNaLoja(5, pedido, hoje = pedido))
        assertFalse(podePedirAvaliacaoNaLoja(5, pedido, hoje = LocalDate.of(2026, 7, 9)))
        assertTrue(podePedirAvaliacaoNaLoja(5, pedido, hoje = LocalDate.of(2026, 7, 10)))
        assertTrue(podePedirAvaliacaoNaLoja(5, pedido, hoje = LocalDate.of(2027, 1, 1)))
    }

    @Test fun `um relogio atrasado nao repete o pedido`() {
        assertFalse(podePedirAvaliacaoNaLoja(5, ultimoPedido = HOJE, hoje = HOJE.minusYears(1)))
    }

    @Test fun `chegou a hora, mostra o cartao e guarda o dia`() = runBlocking {
        val loja = LojaFalsa()

        PedirAvaliacaoNaLoja(AvaliacoesFalsas(comNota = 5), loja)(HOJE)

        assertEquals(1, loja.cartoes)
        assertEquals(HOJE, loja.ultimoPedido())
    }

    @Test fun `antes da hora, nem chama a loja`() = runBlocking {
        val loja = LojaFalsa()

        PedirAvaliacaoNaLoja(AvaliacoesFalsas(comNota = 4), loja)(HOJE)

        assertEquals(0, loja.cartoes)
        assertNull(loja.ultimoPedido())
    }

    @Test fun `quem ja pediu ha pouco nao chama a loja e o dia nao muda`() = runBlocking {
        val semanaPassada = HOJE.minusWeeks(1)
        val loja = LojaFalsa(ultimo = semanaPassada)

        PedirAvaliacaoNaLoja(AvaliacoesFalsas(comNota = 9), loja)(HOJE)

        assertEquals(0, loja.cartoes)
        assertEquals(semanaPassada, loja.ultimoPedido())
    }

    @Test fun `a loja sem resposta nao guarda o dia, e a proxima nota tenta de novo`() = runBlocking {
        val loja = LojaFalsa(responde = false)
        val pedir = PedirAvaliacaoNaLoja(AvaliacoesFalsas(comNota = 5), loja)

        pedir(HOJE)
        pedir(HOJE)

        assertEquals(2, loja.cartoes)
        assertNull(loja.ultimoPedido())
    }

    private class LojaFalsa(private var ultimo: LocalDate? = null, private val responde: Boolean = true) : AvaliacaoNaLoja {
        var cartoes = 0

        override suspend fun ultimoPedido() = ultimo

        override suspend fun salvarUltimoPedido(dia: LocalDate) {
            ultimo = dia
        }

        override suspend fun mostrarCartao(): Boolean {
            cartoes++
            return responde
        }
    }

    private class AvaliacoesFalsas(private val comNota: Int) : AvaliacaoRepository {
        override suspend fun contarComNota() = comNota
        override fun observeByDisciplina(disciplinaId: Long): Flow<List<AvaliacaoDomain>> = emptyFlow()
        override fun observeByPeriodo(periodoId: Long): Flow<List<AvaliacaoDomain>> = emptyFlow()
        override suspend fun upsert(avaliacao: AvaliacaoDomain) = 0L
        override suspend fun delete(avaliacao: AvaliacaoDomain) = Unit
    }

    private companion object {
        val HOJE: LocalDate = LocalDate.of(2026, 11, 4)
    }
}
