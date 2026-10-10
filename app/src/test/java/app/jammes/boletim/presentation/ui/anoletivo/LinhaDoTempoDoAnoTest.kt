package app.jammes.boletim.presentation.ui.anoletivo

import app.jammes.boletim.domain.model.TipoPeriodo
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LinhaDoTempoDoAnoTest {

    @Test fun `faixa vai de janeiro a dezembro do ano letivo`() {
        // prepara: as quatro unidades do seed, todas dentro de 2024
        val periodos = UNIDADES

        // executa
        val faixa = faixaDaLinhaDoTempo(periodos, ano = 2024)

        // confere
        assertEquals(dia(2024, 1, 1), faixa.start)
        assertEquals(dia(2024, 12, 31), faixa.endInclusive)
    }

    @Test fun `periodo que passa do ano estica a faixa`() {
        val periodos = listOf(DatasPeriodo(dia(2023, 12, 1), dia(2024, 3, 1)), DatasPeriodo(dia(2024, 11, 1), dia(2025, 1, 31)))

        val faixa = faixaDaLinhaDoTempo(periodos, ano = 2024)

        assertEquals(dia(2023, 12, 1), faixa.start)
        assertEquals(dia(2025, 1, 31), faixa.endInclusive)
    }

    @Test fun `fracao vai de 0 no primeiro dia a 1 no dia seguinte ao ultimo`() {
        val faixa = dia(2024, 1, 1)..dia(2024, 12, 31) // 366 dias: 2024 é bissexto

        assertEquals(0f, fracaoDaFaixa(dia(2024, 1, 1), faixa), 0f)
        assertEquals(0.5f, fracaoDaFaixa(dia(2024, 7, 2), faixa), 0.0001f) // o 184º dia
        assertEquals(1f, fracaoDaFaixa(dia(2025, 1, 1), faixa), 0f)
    }

    @Test fun `um ano tem as doze iniciais, cada mes com os seus dias`() {
        val meses = mesesDaFaixa(dia(2024, 1, 1)..dia(2024, 12, 31))

        assertEquals("JFMAMJJASOND", meses.joinToString("") { it.inicial })
        assertEquals(dia(2024, 2, 1), meses[1].inicio)
        assertEquals(dia(2024, 2, 29), meses[1].fim)
    }

    @Test fun `mes cortado pela faixa fica so com os dias de dentro`() {
        val meses = mesesDaFaixa(dia(2024, 1, 15)..dia(2024, 3, 10))

        assertEquals(listOf("J", "F", "M"), meses.map { it.inicial })
        assertEquals(dia(2024, 1, 15), meses.first().inicio)
        assertEquals(dia(2024, 3, 10), meses.last().fim)
    }

    @Test fun `resumo diz quantos periodos e de quando a quando`() {
        assertEquals("4 unidades · de 1 fev a 15 dez", resumoDaDivisao(UNIDADES, TipoPeriodo.UNIDADE, ano = 2024))
        assertEquals("1 semestre · de 1 fev a 30 abr", resumoDaDivisao(UNIDADES.take(1), TipoPeriodo.SEMESTRE, ano = 2024))
    }

    @Test fun `data fora do ano letivo mostra o ano no resumo`() {
        val periodos = listOf(DatasPeriodo(dia(2024, 2, 1), dia(2025, 1, 31)))

        assertEquals("1 bimestre · de 1 fev a 31 jan 2025", resumoDaDivisao(periodos, TipoPeriodo.BIMESTRE, ano = 2024))
    }
}

private fun dia(ano: Int, mes: Int, dia: Int): LocalDate = LocalDate.of(ano, mes, dia)

// As quatro unidades do seed
private val UNIDADES = listOf(
    DatasPeriodo(dia(2024, 2, 1), dia(2024, 4, 30)),
    DatasPeriodo(dia(2024, 5, 1), dia(2024, 7, 15)),
    DatasPeriodo(dia(2024, 8, 1), dia(2024, 9, 30)),
    DatasPeriodo(dia(2024, 10, 1), dia(2024, 12, 15)),
)
