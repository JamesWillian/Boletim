package app.jammes.boletim.data.local.preferences

import app.jammes.boletim.domain.model.HORARIOS_PADRAO
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/** Como os horários das notificações vão para o DataStore e voltam dele. */
class HorariosGravadosTest {

    @Test fun `nunca escolhidos, vale o padrao`() {
        assertEquals(HORARIOS_PADRAO, lerHorarios(null))
        assertEquals(HORARIOS_PADRAO, lerHorarios(emptySet()))
    }

    @Test fun `voltam em ordem, porque o conjunto gravado nao tem ordem`() {
        assertEquals(listOf(LocalTime.of(6, 30), LocalTime.of(19, 0)), lerHorarios(setOf("19:00", "06:30")))
    }

    @Test fun `o que nao se le fica de fora`() {
        assertEquals(listOf(LocalTime.of(19, 0)), lerHorarios(setOf("19:00", "sete da noite", "")))
    }

    @Test fun `so com textos que nao se leem, vale o padrao`() {
        assertEquals(HORARIOS_PADRAO, lerHorarios(setOf("25:00")))
    }

    @Test fun `gravados com dois digitos e sem segundos`() {
        assertEquals("07:05", escreverHorario(LocalTime.of(7, 5)))
        assertEquals("19:00", escreverHorario(LocalTime.of(19, 0, 30)))
    }

    @Test fun `o que e gravado volta igual`() {
        val horarios = listOf(LocalTime.of(6, 30), LocalTime.of(12, 0), LocalTime.of(21, 45))

        assertEquals(horarios, lerHorarios(horarios.map(::escreverHorario).toSet()))
    }
}
