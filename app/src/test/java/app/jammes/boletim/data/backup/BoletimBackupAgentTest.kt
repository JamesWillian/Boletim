package app.jammes.boletim.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Quando a chave "Backup automático no Google" deixa os arquivos do app entrarem no backup. */
class BoletimBackupAgentTest {

    @Test fun `com a chave ligada os dados vao para a nuvem`() {
        assertTrue(entraNoBackup(backupAutomatico = true, deAparelhoParaAparelho = false))
    }

    @Test fun `com a chave desligada a nuvem recebe o backup vazio`() {
        assertFalse(entraNoBackup(backupAutomatico = false, deAparelhoParaAparelho = false))
    }

    @Test fun `a copia de um aparelho para outro nao depende da chave`() {
        assertTrue(entraNoBackup(backupAutomatico = false, deAparelhoParaAparelho = true))
    }
}
