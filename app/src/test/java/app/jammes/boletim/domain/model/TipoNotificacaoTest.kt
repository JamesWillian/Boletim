package app.jammes.boletim.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** O tipo da notificação, gravado pelo nome. */
class TipoNotificacaoTest {

    @Test fun `cada tipo volta do nome gravado`() {
        TipoNotificacao.entries.forEach { tipo ->
            assertEquals(tipo, TipoNotificacao.fromString(TipoNotificacao.toString(tipo)))
        }
    }

    @Test fun `um nome que nao existe mais vira aviso`() {
        assertEquals(TipoNotificacao.AVISO, TipoNotificacao.fromString("PROVA_SURPRESA"))
        assertEquals(TipoNotificacao.AVISO, TipoNotificacao.fromString(null))
    }
}
