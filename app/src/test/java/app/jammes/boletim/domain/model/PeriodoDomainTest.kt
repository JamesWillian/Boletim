package app.jammes.boletim.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PeriodoDomainTest {

    private val periodos = listOf(
        periodo(1, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 6, 30)),
        periodo(2, LocalDate.of(2024, 8, 1), LocalDate.of(2024, 12, 15)),
    )

    @Test fun `data dentro de um periodo cai nele`() {
        assertEquals(2, periodos.periodoDe(LocalDate.of(2024, 9, 10))?.periodo)
    }

    @Test fun `primeiro e ultimo dia contam como dentro do periodo`() {
        assertEquals(1, periodos.periodoDe(LocalDate.of(2024, 6, 30))?.periodo)
        assertEquals(2, periodos.periodoDe(LocalDate.of(2024, 8, 1))?.periodo)
    }

    @Test fun `nas ferias entre dois periodos fica o que ja comecou`() {
        assertEquals(1, periodos.periodoDe(LocalDate.of(2024, 7, 15))?.periodo)
    }

    @Test fun `antes do ano comecar fica o primeiro e depois de acabar o ultimo`() {
        assertEquals(1, periodos.periodoDe(LocalDate.of(2024, 1, 10))?.periodo)
        assertEquals(2, periodos.periodoDe(LocalDate.of(2025, 1, 10))?.periodo)
    }

    @Test fun `sem periodos nao ha o que escolher`() {
        assertNull(emptyList<PeriodoDomain>().periodoDe(LocalDate.of(2024, 9, 10)))
    }

    // Filtro e intervalo ---------------------------------------------------------------------

    @Test fun `no ano letivo inteiro o filtro tem todos os periodos`() {
        assertEquals(periodos, periodos.noFiltro(periodoId = null))
    }

    @Test fun `com um periodo escolhido o filtro tem so ele`() {
        assertEquals(listOf(2), periodos.noFiltro(periodoId = 2).map { it.periodo })
    }

    @Test fun `o intervalo vai do inicio do primeiro ao fim do ultimo com as ferias no meio`() {
        val intervalo = periodos.intervalo()!!

        assertEquals(LocalDate.of(2024, 2, 1), intervalo.start)
        assertEquals(LocalDate.of(2024, 12, 15), intervalo.endInclusive)
        assertTrue(LocalDate.of(2024, 7, 15) in intervalo) // férias entre o 1º e o 2º
    }

    @Test fun `o intervalo de um periodo so sao as datas dele`() {
        val intervalo = periodos.noFiltro(periodoId = 1).intervalo()!!

        assertTrue(LocalDate.of(2024, 6, 30) in intervalo)
        assertFalse(LocalDate.of(2024, 8, 1) in intervalo)
    }

    @Test fun `sem periodos nao ha intervalo`() {
        assertNull(emptyList<PeriodoDomain>().intervalo())
    }

    // Período ao abrir o ano -----------------------------------------------------------------

    @Test fun `ano em andamento abre no periodo de hoje`() {
        assertEquals(2L, periodos.periodoAoAbrir(hoje = LocalDate.of(2024, 9, 10)))
    }

    @Test fun `nas ferias abre no periodo que ja comecou`() {
        assertEquals(1L, periodos.periodoAoAbrir(hoje = LocalDate.of(2024, 7, 15)))
    }

    @Test fun `ano que ainda nao comecou abre no primeiro periodo`() {
        assertEquals(1L, periodos.periodoAoAbrir(hoje = LocalDate.of(2024, 1, 10)))
    }

    @Test fun `ano que ja acabou abre inteiro, com o resultado final`() {
        assertNull(periodos.periodoAoAbrir(hoje = LocalDate.of(2024, 12, 16)))
    }

    @Test fun `o ultimo dia do ultimo periodo ainda abre nele`() {
        assertEquals(2L, periodos.periodoAoAbrir(hoje = LocalDate.of(2024, 12, 15)))
    }

    @Test fun `ano sem periodos abre inteiro`() {
        assertNull(emptyList<PeriodoDomain>().periodoAoAbrir(hoje = LocalDate.of(2024, 9, 10)))
    }
}

private fun periodo(numero: Int, inicio: LocalDate, fim: LocalDate) = PeriodoDomain(
    id = numero.toLong(),
    anoLetivoId = 1,
    periodo = numero,
    dataInicio = inicio,
    dataFim = fim,
)
