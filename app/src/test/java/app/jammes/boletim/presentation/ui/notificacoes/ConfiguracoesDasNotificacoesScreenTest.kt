package app.jammes.boletim.presentation.ui.notificacoes

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/** Os textos e as sugestões das configurações das notificações. */
class ConfiguracoesDasNotificacoesScreenTest {

    @Test fun `a parte do dia de cada horario`() {
        val esperado = mapOf(
            LocalTime.of(0, 0) to "De madrugada",
            LocalTime.of(4, 59) to "De madrugada",
            LocalTime.of(5, 0) to "De manhã",
            LocalTime.of(11, 59) to "De manhã",
            LocalTime.of(12, 0) to "À tarde",
            LocalTime.of(17, 59) to "À tarde",
            LocalTime.of(18, 0) to "À noite",
            LocalTime.of(23, 59) to "À noite",
        )

        esperado.forEach { (horario, parte) -> assertEquals("$horario", parte, parteDoDia(horario)) }
    }

    @Test fun `o relogio abre no primeiro horario sugerido que ainda nao esta na lista`() {
        assertEquals(LocalTime.of(19, 0), sugestaoDeHorario(emptyList()))
        assertEquals(LocalTime.of(6, 30), sugestaoDeHorario(listOf(LocalTime.of(19, 0))))
        assertEquals(LocalTime.of(12, 30), sugestaoDeHorario(listOf(LocalTime.of(6, 30), LocalTime.of(19, 0))))
        assertEquals(
            LocalTime.of(21, 0),
            sugestaoDeHorario(listOf(LocalTime.of(6, 30), LocalTime.of(12, 30), LocalTime.of(19, 0))),
        )
    }

    @Test fun `com outros horarios na lista, a sugestao e o primeiro livre`() {
        assertEquals(LocalTime.of(19, 0), sugestaoDeHorario(listOf(LocalTime.of(7, 0), LocalTime.of(20, 0))))
    }
}
