package app.jammes.boletim.presentation.ui.disciplina

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** O "quanto falta" embaixo da média no resumo da disciplina. */
class GraficoDosPeriodosTest {

    @Test fun `abaixo de 2 pontos fica no singular`() {
        assertEquals("Falta 0,3 ponto", quantoFalta(media = 7.7, minima = 8.0))
        assertEquals("Falta 1 ponto", quantoFalta(media = 7.0, minima = 8.0))
        assertEquals("Falta 1,75 ponto", quantoFalta(media = 6.25, minima = 8.0))
    }

    @Test fun `de 2 pontos para cima fica no plural`() {
        assertEquals("Faltam 2 pontos", quantoFalta(media = 6.0, minima = 8.0))
        assertEquals("Faltam 2,5 pontos", quantoFalta(media = 5.5, minima = 8.0))
    }

    @Test fun `a diferenca sai sem o ruido do ponto flutuante`() {
        // 8,0 − 7,7 em Double daria 0,2999…
        assertEquals("Falta 0,3 ponto", quantoFalta(media = 7.7, minima = 8.0))
        assertEquals("Falta 0,1 ponto", quantoFalta(media = 6.9, minima = 7.0))
    }

    @Test fun `na minima, acima dela ou sem media nao falta nada`() {
        assertNull(quantoFalta(media = 8.0, minima = 8.0))
        assertNull(quantoFalta(media = 9.5, minima = 8.0))
        assertNull(quantoFalta(media = null, minima = 8.0))
    }
}
