package app.jammes.boletim.presentation.ui.anoletivo

import app.jammes.boletim.domain.model.TipoPeriodo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Os textos do sheet de datas de um período. */
class PeriodoBottomSheetTest {

    // Atropelando os vizinhos -------------------------------------------------------------------

    @Test fun `comecar no dia em que o anterior acaba atropela ele`() {
        // prepara: a 2ª Unidade escolhida começando no último dia da 1ª
        val escolhidas = DatasPeriodo(dia(4, 30), dia(7, 15))

        // executa
        val aviso = conflitoComVizinhos(escolhidas, numero = 2, TipoPeriodo.UNIDADE, PRIMEIRA, TERCEIRA, ano = 2024)

        // confere
        assertEquals("A 1ª Unidade vai até 30 abr: comece depois disso.", aviso)
    }

    @Test fun `terminar depois que o seguinte comeca atropela ele`() {
        val escolhidas = DatasPeriodo(dia(5, 1), dia(8, 1))

        val aviso = conflitoComVizinhos(escolhidas, numero = 2, TipoPeriodo.UNIDADE, PRIMEIRA, TERCEIRA, ano = 2024)

        assertEquals("A 3ª Unidade começa em 1 ago: termine antes disso.", aviso)
    }

    @Test fun `aviso segue o genero do tipo`() {
        val escolhidas = DatasPeriodo(dia(4, 1), dia(7, 15))

        val aviso = conflitoComVizinhos(escolhidas, numero = 2, TipoPeriodo.BIMESTRE, PRIMEIRA, TERCEIRA, ano = 2024)

        assertEquals("O 1º Bimestre vai até 30 abr: comece depois disso.", aviso)
    }

    @Test fun `datas entre os vizinhos nao tem aviso, mesmo com ferias no meio`() {
        val escolhidas = DatasPeriodo(dia(5, 1), dia(7, 15))

        assertNull(conflitoComVizinhos(escolhidas, numero = 2, TipoPeriodo.UNIDADE, PRIMEIRA, TERCEIRA, ano = 2024))
    }

    @Test fun `primeiro e ultimo periodos so tem um vizinho`() {
        val muitoCedo = DatasPeriodo(dia(1, 1), dia(4, 30))

        assertNull(conflitoComVizinhos(muitoCedo, numero = 1, TipoPeriodo.UNIDADE, anterior = null, seguinte = SEGUNDA, ano = 2024))
    }

    // O que está marcado ------------------------------------------------------------------------

    @Test fun `com inicio e fim mostra o intervalo e a duracao`() {
        assertEquals("1 fev – 30 abr · 13 semanas", resumoDaEscolha(dia(2, 1), dia(4, 30), ano = 2024))
    }

    @Test fun `so com o inicio pede o ultimo dia`() {
        assertEquals("Começa em 1 fev; agora toque no último dia", resumoDaEscolha(dia(2, 1), null, ano = 2024))
    }

    @Test fun `sem nada marcado explica os dois toques`() {
        assertEquals("Toque no primeiro e no último dia", resumoDaEscolha(null, null, ano = 2024))
    }
}

private fun dia(mes: Int, dia: Int): LocalDate = LocalDate.of(2024, mes, dia)

// Os vizinhos da 2ª Unidade do seed
private val PRIMEIRA = DatasPeriodo(dia(2, 1), dia(4, 30))
private val SEGUNDA = DatasPeriodo(dia(5, 1), dia(7, 15))
private val TERCEIRA = DatasPeriodo(dia(8, 1), dia(9, 30))
