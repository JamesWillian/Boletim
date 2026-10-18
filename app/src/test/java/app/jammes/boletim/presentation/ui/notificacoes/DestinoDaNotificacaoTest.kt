package app.jammes.boletim.presentation.ui.notificacoes

import app.jammes.boletim.domain.model.NotificacaoDomain
import app.jammes.boletim.domain.model.TipoNotificacao
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

/** Para onde o toque numa notificação da lista leva, pelo tipo dela. */
class DestinoDaNotificacaoTest {

    @Test fun `avaliacao chegando e nota pendente abrem a avaliacao, no periodo dela`() {
        val esperado = DestinoDaNotificacao.Disciplina(anoLetivoId = 1, periodoId = 14, disciplinaId = 3, avaliacaoId = 40)

        assertEquals(esperado, destinoDa(notificacao(TipoNotificacao.AVALIACAO, periodoId = 14, disciplinaId = 3, avaliacaoId = 40)))
        assertEquals(esperado, destinoDa(notificacao(TipoNotificacao.NOTA, periodoId = 14, disciplinaId = 3, avaliacaoId = 40)))
    }

    @Test fun `a final abre no ano letivo inteiro`() {
        val destino = destinoDa(notificacao(TipoNotificacao.NOTA, periodoId = null, disciplinaId = 3, avaliacaoId = 40))

        assertEquals(DestinoDaNotificacao.Disciplina(anoLetivoId = 1, periodoId = null, disciplinaId = 3, avaliacaoId = 40), destino)
    }

    @Test fun `media abaixo abre a disciplina no periodo da media`() {
        val destino = destinoDa(notificacao(TipoNotificacao.MEDIA, periodoId = 14, disciplinaId = 3))

        assertEquals(DestinoDaNotificacao.Disciplina(anoLetivoId = 1, periodoId = 14, disciplinaId = 3), destino)
    }

    @Test fun `faltas abrem a aba das faltas, no ano letivo inteiro`() {
        val destino = destinoDa(notificacao(TipoNotificacao.FALTAS, periodoId = 14, disciplinaId = 3))

        assertEquals(DestinoDaNotificacao.Disciplina(anoLetivoId = 1, periodoId = null, disciplinaId = 3, abaDeFaltas = true), destino)
    }

    @Test fun `o fim do periodo abre o Boletim Geral nele`() {
        val destino = destinoDa(notificacao(TipoNotificacao.PERIODO, periodoId = 13))

        assertEquals(DestinoDaNotificacao.Boletim(anoLetivoId = 1, periodoId = 13), destino)
    }

    @Test fun `um aviso geral nao abre nada`() {
        assertEquals(null, destinoDa(notificacao(TipoNotificacao.AVISO)))
    }

    @Test fun `sem o que abrir, nao abre nada`() {
        assertEquals(null, destinoDa(notificacao(TipoNotificacao.MEDIA, anoLetivoId = null, periodoId = 14, disciplinaId = 3)))
        assertEquals(null, destinoDa(notificacao(TipoNotificacao.FALTAS, disciplinaId = null)))
        assertEquals(null, destinoDa(notificacao(TipoNotificacao.PERIODO, periodoId = null)))
    }

    private fun notificacao(
        tipo: TipoNotificacao,
        anoLetivoId: Long? = 1,
        periodoId: Long? = null,
        disciplinaId: Long? = null,
        avaliacaoId: Long? = null,
    ) = NotificacaoDomain(
        id = 1,
        alunoId = 1,
        tipo = tipo,
        chave = "teste",
        titulo = "Título",
        texto = "",
        criadaEm = Instant.parse("2026-10-17T22:00:00Z"),
        anoLetivoId = anoLetivoId,
        periodoId = periodoId,
        disciplinaId = disciplinaId,
        avaliacaoId = avaliacaoId,
    )
}
