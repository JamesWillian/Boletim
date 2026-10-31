package app.jammes.boletim.presentation.ui.plano

import org.junit.Assert.assertEquals
import org.junit.Test

/** Os textos que os limites do plano mostram nas telas. */
class TextosDoPlanoTest {

    @Test fun `as que restam falam no singular quando e uma so`() {
        assertEquals("Resta 1 grátis", textoDasQueRestam(1))
        assertEquals("Restam 3 grátis", textoDasQueRestam(3))
    }
}
