package app.jammes.boletim.presentation.ui.plano

import org.junit.Assert.assertEquals
import org.junit.Test

/** Os textos que os limites do plano mostram nas telas. */
class TextosDoPlanoTest {

    @Test fun `as que restam falam no singular quando e uma so`() {
        assertEquals("Resta 1 grátis", textoDasQueRestam(1))
        assertEquals("Restam 3 grátis", textoDasQueRestam(3))
    }

    @Test fun `sem compra na conta, o aviso diz para trocar a conta da Play Store`() {
        assertEquals(
            "Nenhuma compra do Pro nesta conta da Google Play. Se comprou com outra, troque a conta na Play Store.",
            textoDoAviso(AvisoDaLoja.SEM_COMPRA),
        )
    }

    @Test fun `cada aviso tem o seu texto`() {
        val textos = AvisoDaLoja.entries.map { textoDoAviso(it) }

        assertEquals(textos.size, textos.toSet().size)
    }
}
