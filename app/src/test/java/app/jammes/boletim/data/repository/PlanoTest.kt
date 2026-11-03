package app.jammes.boletim.data.repository

import app.jammes.boletim.domain.model.Plano
import org.junit.Assert.assertEquals
import org.junit.Test

/** Qual plano vale, pelas chaves do build, pela chave "Pro de teste" e pela compra na Play. */
class PlanoTest {

    @Test fun `com os limites desligados, todo mundo e Pro`() {
        assertEquals(Plano.PRO, planoDe(RELEASE_SEM_LIMITES, proDeTeste = false, comprado = false))
        assertEquals(Plano.PRO, planoDe(DEBUG.copy(limitesLigados = false), proDeTeste = false, comprado = false))
    }

    @Test fun `no debug, a chave de teste da o Pro`() {
        assertEquals(Plano.PRO, planoDe(DEBUG, proDeTeste = true, comprado = false))
    }

    @Test fun `no debug, sem a chave de teste nem a compra, vale o gratis`() {
        assertEquals(Plano.GRATIS, planoDe(DEBUG, proDeTeste = false, comprado = false))
    }

    @Test fun `fora do debug, a chave de teste nao conta`() {
        // Um backup do debug pode trazer ela de volta num aparelho com o release
        assertEquals(Plano.GRATIS, planoDe(RELEASE_COM_LIMITES, proDeTeste = true, comprado = false))
    }

    @Test fun `com a compra, e Pro mesmo com os limites ligados`() {
        assertEquals(Plano.PRO, planoDe(RELEASE_COM_LIMITES, proDeTeste = false, comprado = true))
        assertEquals(Plano.PRO, planoDe(DEBUG, proDeTeste = false, comprado = true))
    }

    private companion object {
        val DEBUG = ChavesDoPlano(limitesLigados = true, proDeTestePermitido = true)
        val RELEASE_SEM_LIMITES = ChavesDoPlano(limitesLigados = false, proDeTestePermitido = false)
        val RELEASE_COM_LIMITES = ChavesDoPlano(limitesLigados = true, proDeTestePermitido = false)
    }
}
