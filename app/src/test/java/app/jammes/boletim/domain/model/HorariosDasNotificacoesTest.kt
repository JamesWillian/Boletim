package app.jammes.boletim.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/** A lista de horários das notificações: sempre em ordem, sem repetir, entre 1 e o máximo. */
class HorariosDasNotificacoesTest {

    // Adicionar ------------------------------------------------------------------------------

    @Test fun `um horario novo entra em ordem`() {
        assertEquals(listOf(h(7), h(19)), listOf(h(19)).comHorario(h(7)))
        assertEquals(listOf(h(7), h(12, 30), h(19)), listOf(h(7), h(19)).comHorario(h(12, 30)))
    }

    @Test fun `um horario que ja esta na lista nao entra de novo`() {
        val horarios = listOf(h(7), h(19))

        assertEquals(horarios, horarios.comHorario(h(19)))
    }

    @Test fun `os segundos ficam de fora`() {
        assertEquals(listOf(h(7, 15), h(19)), listOf(h(19)).comHorario(LocalTime.of(7, 15, 42)))
    }

    @Test fun `com a lista cheia o novo nao entra`() {
        val cheia = listOf(h(7), h(12), h(19))
        assertEquals(MAXIMO_DE_HORARIOS, cheia.size)

        assertEquals(cheia, cheia.comHorario(h(21)))
    }

    // Mudar ----------------------------------------------------------------------------------

    @Test fun `mudar troca o horario e reordena`() {
        assertEquals(listOf(h(19), h(21)), listOf(h(7), h(19)).comHorario(h(21), antigo = h(7)))
    }

    @Test fun `mudar para o mesmo horario nao muda nada`() {
        val horarios = listOf(h(7), h(19))

        assertEquals(horarios, horarios.comHorario(h(19), antigo = h(19)))
    }

    @Test fun `mudar para um horario que ja esta na lista nao perde nenhum`() {
        // A tela não deixa salvar; se chegar aqui, a lista fica como estava, em vez de juntar os dois
        val horarios = listOf(h(7), h(19))

        assertEquals(horarios, horarios.comHorario(h(7), antigo = h(19)))
    }

    @Test fun `com a lista cheia ainda da para mudar um horario`() {
        assertEquals(listOf(h(7), h(19), h(21)), listOf(h(7), h(12), h(19)).comHorario(h(21), antigo = h(12)))
    }

    // Remover --------------------------------------------------------------------------------

    @Test fun `remover tira o horario`() {
        assertEquals(listOf(h(19)), listOf(h(7), h(19)).semHorario(h(7)))
    }

    @Test fun `o ultimo horario nao sai`() {
        assertEquals(listOf(h(19)), listOf(h(19)).semHorario(h(19)))
    }

    @Test fun `remover um horario que nao esta na lista nao muda nada`() {
        assertEquals(listOf(h(7), h(19)), listOf(h(7), h(19)).semHorario(h(12)))
    }

    @Test fun `o padrao e um horario so, as 19h`() {
        assertEquals(listOf(h(19)), HORARIOS_PADRAO)
        assertEquals(HORARIOS_PADRAO, Configuracoes().horariosDasNotificacoes)
    }

    private fun h(hora: Int, minuto: Int = 0): LocalTime = LocalTime.of(hora, minuto)
}
