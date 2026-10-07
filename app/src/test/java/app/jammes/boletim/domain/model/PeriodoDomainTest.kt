package app.jammes.boletim.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}

private fun periodo(numero: Int, inicio: LocalDate, fim: LocalDate) = PeriodoDomain(
    id = numero.toLong(),
    anoLetivoId = 1,
    periodo = numero,
    dataInicio = inicio,
    dataFim = fim,
)
