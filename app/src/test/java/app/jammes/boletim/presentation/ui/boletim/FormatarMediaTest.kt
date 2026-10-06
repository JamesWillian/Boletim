package app.jammes.boletim.presentation.ui.boletim

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatarMediaTest {

    @Test fun `media inteira mostra uma casa`() {
        assertEquals("7,0", formatarMedia(7.0)) // antes saía "7,"
        assertEquals("10,0", formatarMedia(10.0))
    }

    @Test fun `media com uma casa fica como esta`() {
        assertEquals("7,5", formatarMedia(7.5))
    }

    @Test fun `media com duas casas mostra as duas`() {
        // 6,96 é ABAIXO de 7,0: antes aparecia "7," em vermelho
        assertEquals("6,96", formatarMedia(6.96))
    }

    @Test fun `ruido do Double nao aparece na tela`() {
        assertEquals("7,0", formatarMedia(6.999999999999999))
    }

    @Test fun `media geral com mais casas arredonda para duas e o 5 sobe`() {
        assertEquals("7,25", formatarMedia(7.245))
    }

    @Test fun `sem media mostra traco`() {
        assertEquals("—", formatarMedia(null))
    }
}
