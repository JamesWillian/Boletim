package app.jammes.boletim.presentation.ui.notificacoes

import app.jammes.boletim.domain.model.NotificacaoDomain
import app.jammes.boletim.domain.model.TipoNotificacao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * A lista de notificações: os grupos por dia, o que conta como nova e os textos da tela. Como o app
 * chama cada aluno fica nos testes das notificações do domínio, onde o nomeNaLista mora.
 */
class NotificacoesScreenTest {

    // Grupos por dia -------------------------------------------------------------------------

    @Test fun `agrupa pelo dia no fuso do aparelho, das mais novas para as mais antigas`() {
        val notificacoes = listOf(
            notificacao(id = 3, criadaEm = "2026-10-16T15:00:00Z", lida = true), // 16/10, 12:00
            notificacao(id = 1, criadaEm = "2026-10-17T13:00:45Z"), // 17/10, 10:00
            // 2h30 em Greenwich ainda é a noite do dia anterior em São Paulo
            notificacao(id = 2, criadaEm = "2026-10-17T02:30:00Z", lida = true), // 16/10, 23:30
            notificacao(id = 4, criadaEm = "2026-10-17T13:00:45Z", lida = true), // mesmo instante da 1
        )

        val dias = agruparPorDia(notificacoes, vistas = emptySet(), zona = SAO_PAULO)

        assertEquals(listOf(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 16)), dias.map { it.dia })
        // No mesmo instante, a gravada por último vem antes
        assertEquals(listOf(4L, 1L), dias[0].itens.map { it.notificacao.id })
        assertEquals(listOf(2L, 3L), dias[1].itens.map { it.notificacao.id })
        assertEquals(listOf(LocalTime.of(10, 0), LocalTime.of(10, 0)), dias[0].itens.map { it.hora })
        assertEquals(listOf(LocalTime.of(23, 30), LocalTime.of(12, 0)), dias[1].itens.map { it.hora })
    }

    @Test fun `nova e a que ainda nao foi lida ou a que foi vista nesta visita`() {
        val notificacoes = listOf(
            notificacao(id = 1, criadaEm = "2026-10-17T13:00:00Z", lida = false),
            notificacao(id = 2, criadaEm = "2026-10-17T12:00:00Z", lida = true), // vista agora
            notificacao(id = 3, criadaEm = "2026-10-17T11:00:00Z", lida = true), // vista antes
        )

        val itens = agruparPorDia(notificacoes, vistas = setOf(2L), zona = SAO_PAULO).single().itens

        assertEquals(listOf(true, true, false), itens.map { it.nova })
    }

    @Test fun `sem notificacoes nao ha dias`() {
        assertEquals(emptyList<DiaDeNotificacoes>(), agruparPorDia(emptyList(), emptySet(), SAO_PAULO))
    }

    @Test fun `o estado conta todas e as novas`() {
        val estado = NotificacoesUiState.Carregadas(
            dias = agruparPorDia(
                listOf(
                    notificacao(id = 1, criadaEm = "2026-10-17T13:00:00Z"),
                    notificacao(id = 2, criadaEm = "2026-10-15T13:00:00Z", lida = true),
                    notificacao(id = 3, criadaEm = "2026-10-15T12:00:00Z", lida = true),
                ),
                vistas = emptySet(),
                zona = SAO_PAULO,
            ),
            naoLidas = listOf(1L),
        )

        assertEquals(3, estado.total)
        assertEquals(1, estado.novas)
    }

    // O nome do dia --------------------------------------------------------------------------

    @Test fun `hoje e ontem pelo nome`() {
        assertEquals("Hoje", tituloDoDia(HOJE, HOJE))
        assertEquals("Ontem", tituloDoDia(HOJE.minusDays(1), HOJE))
    }

    @Test fun `ate uma semana atras, o dia da semana com a data`() {
        assertEquals("Quinta-feira, 15 de outubro", tituloDoDia(LocalDate.of(2026, 10, 15), HOJE))
        assertEquals("Domingo, 11 de outubro", tituloDoDia(LocalDate.of(2026, 10, 11), HOJE))
    }

    @Test fun `depois de uma semana, so a data`() {
        assertEquals("10 de outubro", tituloDoDia(LocalDate.of(2026, 10, 10), HOJE))
        assertEquals("2 de janeiro", tituloDoDia(LocalDate.of(2026, 1, 2), HOJE))
    }

    @Test fun `em outro ano, a data com o ano`() {
        assertEquals("31 de dezembro de 2025", tituloDoDia(LocalDate.of(2025, 12, 31), HOJE))
    }

    @Test fun `um dia que ainda nao chegou, so a data`() {
        // Com o relógio do aparelho mudado para trás, por exemplo
        assertEquals("18 de outubro", tituloDoDia(LocalDate.of(2026, 10, 18), HOJE))
    }

    // Textos ---------------------------------------------------------------------------------

    @Test fun `o resumo diz quantas sao e quantas sao novas`() {
        assertEquals("1 notificação", resumoDasNotificacoes(total = 1, novas = 0))
        assertEquals("1 notificação · 1 nova", resumoDasNotificacoes(total = 1, novas = 1))
        assertEquals("3 notificações · 1 nova", resumoDasNotificacoes(total = 3, novas = 1))
        assertEquals("7 notificações · 2 novas", resumoDasNotificacoes(total = 7, novas = 2))
    }

    @Test fun `com mais de um aluno, o resumo e a lista vazia dizem de quem sao`() {
        assertEquals("James · 7 notificações · 2 novas", resumoDasNotificacoes(total = 7, novas = 2, nomeDoAluno = "James"))
        assertEquals("Nenhuma notificação de James", tituloSemNotificacoes("James"))
        assertEquals("Nenhuma notificação", tituloSemNotificacoes(null))
    }

    @Test fun `o aviso do Limpar no singular e no plural`() {
        assertEquals("1 notificação apagada", textoDaLimpeza(1))
        assertEquals("7 notificações apagadas", textoDaLimpeza(7))
    }

    @Test fun `a hora com dois digitos, em 24 horas`() {
        assertEquals("07:05", textoDaHora(LocalTime.of(7, 5)))
        assertEquals("19:00", textoDaHora(LocalTime.of(19, 0)))
    }

    // Os exemplos do build de debug ----------------------------------------------------------

    @Test fun `os exemplos passam por cada jeito de dar nome ao dia`() {
        val agora = Instant.parse("2026-10-17T15:00:00Z") // meio-dia em São Paulo

        val dias = agruparPorDia(notificacoesDeTeste(alunoId = 1, agora, SAO_PAULO), vistas = emptySet(), zona = SAO_PAULO)

        assertEquals(
            listOf("Hoje", "Ontem", "Quarta-feira, 14 de outubro", "5 de outubro", "12 de setembro de 2025"),
            dias.map { tituloDoDia(it.dia, HOJE) },
        )
        assertEquals(2, dias.sumOf { dia -> dia.itens.count { it.nova } })
    }

    @Test fun `os exemplos sao do aluno aberto, ja avisados e com chaves que nao se repetem`() {
        val agora = Instant.parse("2026-10-17T15:00:00Z")

        val exemplos = notificacoesDeTeste(alunoId = 7, agora, SAO_PAULO)
        val outraLeva = notificacoesDeTeste(alunoId = 7, agora + 1.seconds, SAO_PAULO)

        assertTrue(exemplos.all { it.alunoId == 7L && it.avisada })
        // Nem dentro da leva nem com a seguinte: o banco recusaria a chave repetida do mesmo aluno
        assertEquals(exemplos.size * 2, (exemplos + outraLeva).map { it.chave }.distinct().size)
    }

    private fun notificacao(id: Long, criadaEm: String, lida: Boolean = false) = NotificacaoDomain(
        id = id,
        alunoId = 1,
        tipo = TipoNotificacao.AVISO,
        chave = "teste:$id",
        titulo = "Notificação $id",
        texto = "",
        criadaEm = Instant.parse(criadaEm),
        lida = lida,
    )

    private companion object {
        val SAO_PAULO: ZoneId = ZoneId.of("America/Sao_Paulo")
        val HOJE: LocalDate = LocalDate.of(2026, 10, 17) // um sábado
    }
}
