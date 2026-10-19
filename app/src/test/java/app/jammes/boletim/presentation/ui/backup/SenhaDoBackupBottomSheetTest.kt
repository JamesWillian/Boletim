package app.jammes.boletim.presentation.ui.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A escolha da senha do exportar: quando dá para exportar, e com que senha. */
class SenhaDoBackupBottomSheetTest {

    @Test fun `sem nada digitado nao da para exportar`() {
        assertFalse(EscolhaDaSenha().pronta)
    }

    @Test fun `a senha confirmada exporta com ela`() {
        val escolha = EscolhaDaSenha(senha = "boletim2026", confirmacao = "boletim2026")

        assertTrue(escolha.pronta)
        assertEquals("boletim2026", escolha.escolhida)
    }

    @Test fun `a senha precisa de pelo menos 8 caracteres`() {
        val curta = EscolhaDaSenha(senha = "1234567", confirmacao = "1234567")
        val certa = EscolhaDaSenha(senha = "12345678", confirmacao = "12345678")

        assertTrue(curta.curta)
        assertFalse(curta.pronta)
        assertFalse(certa.curta)
        assertTrue(certa.pronta)
    }

    @Test fun `sem a confirmacao nao da para exportar`() {
        assertFalse(EscolhaDaSenha(senha = "boletim2026").pronta)
    }

    @Test fun `a confirmacao ainda sendo digitada nao e erro`() {
        val digitando = EscolhaDaSenha(senha = "boletim2026", confirmacao = "bole")

        assertFalse(digitando.confirmacaoErrada)
        assertFalse(digitando.pronta)
    }

    @Test fun `uma letra diferente na confirmacao e erro na hora`() {
        val errada = EscolhaDaSenha(senha = "boletim2026", confirmacao = "bola")

        assertTrue(errada.confirmacaoErrada)
        assertFalse(errada.pronta)
    }

    @Test fun `a confirmacao maior que a senha e erro`() {
        assertTrue(EscolhaDaSenha(senha = "boletim2026", confirmacao = "boletim20266").confirmacaoErrada)
    }

    @Test fun `maiuscula na confirmacao e outra senha`() {
        assertTrue(EscolhaDaSenha(senha = "boletim2026", confirmacao = "Boletim2026").confirmacaoErrada)
    }

    @Test fun `marcando sem senha, exporta sem ela, mesmo com algo digitado`() {
        val semSenha = EscolhaDaSenha(senha = "bol", confirmacao = "x", semSenha = true)

        assertTrue(semSenha.pronta)
        assertNull(semSenha.escolhida)
    }
}
