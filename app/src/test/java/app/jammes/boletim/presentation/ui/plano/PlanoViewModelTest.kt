package app.jammes.boletim.presentation.ui.plano

import app.jammes.boletim.domain.model.ResultadoDaCompra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** O que a folha do Pro avisa depois de uma compra ou de um "Restaurar compra". */
class PlanoViewModelTest {

    @Test fun `comprado e cancelado nao avisam nada`() {
        // Comprado: o Pro chega pelo plano, e a folha fecha sozinha
        assertNull(avisoDaCompra(ResultadoDaCompra.Comprado))
        assertNull(avisoDaCompra(ResultadoDaCompra.Cancelado))
    }

    @Test fun `pendente e indisponivel avisam`() {
        assertEquals(AvisoDaLoja.PENDENTE, avisoDaCompra(ResultadoDaCompra.Pendente))
        assertEquals(AvisoDaLoja.INDISPONIVEL, avisoDaCompra(ResultadoDaCompra.Indisponivel))
    }

    @Test fun `restaurar que acha o Pro nao avisa nada`() {
        assertNull(avisoDaRestauracao(achou = true))
    }

    @Test fun `restaurar sem compra na conta diz para trocar de conta`() {
        assertEquals(AvisoDaLoja.SEM_COMPRA, avisoDaRestauracao(achou = false))
    }

    @Test fun `restaurar sem resposta da loja diz para tentar de novo`() {
        assertEquals(AvisoDaLoja.SEM_RESPOSTA, avisoDaRestauracao(achou = null))
    }
}
