package app.jammes.boletim.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class AvaliacaoDomainTest {

    private val lancadas = listOf(
        avaliacao(id = 1, periodoId = 1),
        avaliacao(id = 2, periodoId = 1, tipo = TipoAvaliacao.RECUPERACAO),
        avaliacao(id = 3, periodoId = null, tipo = TipoAvaliacao.FINAL),
    )

    @Test fun `so cabe uma recuperacao em cada periodo`() {
        assertEquals(2L, lancadas.ocupandoAVaga(TipoAvaliacao.RECUPERACAO, periodoId = 1, alemDe = 0)?.id)
        assertNull(lancadas.ocupandoAVaga(TipoAvaliacao.RECUPERACAO, periodoId = 2, alemDe = 0)) // outro período
    }

    @Test fun `so cabe uma final no ano, qualquer que seja o periodo`() {
        assertEquals(3L, lancadas.ocupandoAVaga(TipoAvaliacao.FINAL, periodoId = null, alemDe = 0)?.id)
        assertEquals(3L, lancadas.ocupandoAVaga(TipoAvaliacao.FINAL, periodoId = 2, alemDe = 0)?.id)
    }

    @Test fun `a avaliacao editada nao ocupa a propria vaga`() {
        assertNull(lancadas.ocupandoAVaga(TipoAvaliacao.RECUPERACAO, periodoId = 1, alemDe = 2))
        assertNull(lancadas.ocupandoAVaga(TipoAvaliacao.FINAL, periodoId = null, alemDe = 3))
    }

    @Test fun `normal nao tem limite`() {
        assertNull(lancadas.ocupandoAVaga(TipoAvaliacao.NORMAL, periodoId = 1, alemDe = 0))
    }
    // A pergunta de finalizar, depois da final ------------------------------------------------

    private val nova = avaliacao(id = 0, periodoId = null, nota = null)

    @Test fun `a final nova com nota foi lancada`() {
        assertTrue(avaliacao(id = 0, periodoId = null, tipo = TipoAvaliacao.FINAL).lancouAFinal(antes = nova))
    }

    @Test fun `a final agendada que ganhou nota foi lancada`() {
        val agendada = avaliacao(id = 3, periodoId = null, tipo = TipoAvaliacao.FINAL, nota = null)

        assertTrue(agendada.copy(nota = 8.0).lancouAFinal(antes = agendada))
    }

    @Test fun `a avaliacao que virou final com nota foi lancada`() {
        val normal = avaliacao(id = 4, periodoId = 2)

        assertTrue(normal.copy(tipo = TipoAvaliacao.FINAL, periodoId = null).lancouAFinal(antes = normal))
    }

    @Test fun `mudar a nota de uma final que ja tinha nao conta`() {
        val final = avaliacao(id = 3, periodoId = null, tipo = TipoAvaliacao.FINAL)

        assertFalse(final.copy(nota = 9.0).lancouAFinal(antes = final))
    }

    @Test fun `final sem nota ou normal com nota nao contam`() {
        assertFalse(avaliacao(id = 0, periodoId = null, tipo = TipoAvaliacao.FINAL, nota = null).lancouAFinal(antes = nova))
        assertFalse(avaliacao(id = 0, periodoId = 1).lancouAFinal(antes = nova))
    }
}

private fun avaliacao(
    id: Long,
    periodoId: Long?,
    tipo: TipoAvaliacao = TipoAvaliacao.NORMAL,
    nota: Double? = 7.0,
) = AvaliacaoDomain(
    id = id,
    disciplinaId = 1,
    periodoId = periodoId,
    nome = "Prova $id",
    nota = nota,
    notaMaxima = 10.0,
    peso = 1.0,
    tipo = tipo,
    data = null,
    criadoEm = Instant.fromEpochMilliseconds(0),
)
