package app.jammes.boletim.presentation.ui.boletim

import app.jammes.boletim.domain.model.Substituta
import app.jammes.boletim.domain.model.TipoAvaliacao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OutraMediaTest {

    @Test fun `sem recuperacao nem final nao ha outra media`() {
        assertNull(outraMedia(regular = 6.0, substituta = null, periodoId = 4))
    }

    @Test fun `quando a substituta vale, a outra e a media do periodo ou do ano`() {
        val recuperacao = Substituta(TipoAvaliacao.RECUPERACAO, nota = 7.0, substituiu = true)
        val final = Substituta(TipoAvaliacao.FINAL, nota = 7.5, substituiu = true)

        assertEquals("Média do período" to 5.5, outraMedia(regular = 5.5, substituta = recuperacao, periodoId = 4))
        assertEquals("Média do ano" to 5.8, outraMedia(regular = 5.8, substituta = final, periodoId = null))
    }

    @Test fun `quando a substituta e menor, a outra e a nota dela`() {
        val recuperacao = Substituta(TipoAvaliacao.RECUPERACAO, nota = 5.0, substituiu = false)

        assertEquals("Recuperação" to 5.0, outraMedia(regular = 6.0, substituta = recuperacao, periodoId = 4))
    }

    @Test fun `so com a recuperacao, sem media regular, nao ha o que mostrar embaixo`() {
        val recuperacao = Substituta(TipoAvaliacao.RECUPERACAO, nota = 7.0, substituiu = true)

        assertNull(outraMedia(regular = null, substituta = recuperacao, periodoId = 4))
    }
}
