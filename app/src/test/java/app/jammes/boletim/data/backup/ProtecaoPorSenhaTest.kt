package app.jammes.boletim.data.backup

import app.jammes.boletim.domain.model.BackupInvalidoException
import app.jammes.boletim.domain.model.SenhaIncorretaException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom
import java.util.Base64
import java.util.HexFormat
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** O backup com senha: só a senha certa abre, e o que dá para conferir sem ela é conferido antes. */
class ProtecaoPorSenhaTest {

    // Ida e volta --------------------------------------------------------------------------

    @Test fun `a senha certa devolve o mesmo texto`() {
        val protegido = proteger(BACKUP, "senha boa")

        assertEquals(BACKUP, abrirComSenha(protegido, "senha boa"))
    }

    @Test fun `com o padrao do app tambem volta igual`() {
        // As iterações de verdade, como no aparelho: mais lento, por isso só neste teste
        val protegido = protegerComSenha(BACKUP, "senha boa")

        assertTrue(protegido.contains("\"iteracoes\": $ITERACOES"))
        assertEquals(BACKUP, abrirComSenha(protegido, "senha boa"))
    }

    @Test fun `o arquivo com senha nao mostra os dados`() {
        val protegido = proteger(BACKUP, "senha boa")

        assertFalse(protegido.contains("James Willian"))
        assertFalse(protegido.contains("Matemática"))
    }

    @Test fun `o arquivo diz de que app e de que versao ele e, e como foi protegido`() {
        val protegido = proteger(BACKUP, "senha boa")

        assertTrue(protegido.contains("\"app\": \"app.jammes.boletim\""))
        assertTrue(protegido.contains("\"versao\": $VERSAO_DO_ARQUIVO"))
        assertTrue(protegido.contains("\"algoritmo\": \"$ALGORITMO\""))
    }

    @Test fun `a mesma senha nao repete o sal, o iv nem os dados`() {
        val primeiro = proteger(BACKUP, "senha boa")
        val segundo = proteger(BACKUP, "senha boa")

        assertNotEquals(campo(primeiro, "sal"), campo(segundo, "sal"))
        assertNotEquals(campo(primeiro, "iv"), campo(segundo, "iv"))
        assertNotEquals(campo(primeiro, "dados"), campo(segundo, "dados"))
    }

    @Test fun `o acento vale do mesmo jeito, venha ele junto da letra ou separado`() {
        // "é" numa letra só (U+00E9) e "e" mais o acento (U+0301): na tela, é a mesma senha
        val protegido = proteger(BACKUP, "café da manhã")

        assertEquals(BACKUP, abrirComSenha(protegido, "café da manhã"))
    }

    @Test fun `um arquivo salvo por esta versao continua abrindo`() {
        // Gerado uma vez, com sal e IV fixos. Se este teste falhar, os arquivos que as pessoas já
        // salvaram também não abrem mais: o formato mudou sem querer
        assertEquals(BACKUP_CURTO, abrirComSenha(ARQUIVO_SALVO, "boletim2026"))
        assertEquals(ARQUIVO_SALVO, protegerComSenha(BACKUP_CURTO, "boletim2026", iteracoes = 1_000, aleatorio = SemSorte()))
    }

    @Test fun `um arquivo salvo pela versao 1 continua abrindo`() {
        // A versão vai na conferência do GCM: o arquivo abre com a que está escrita nele, não com a atual
        assertEquals(BACKUP_CURTO, abrirComSenha(ARQUIVO_SALVO_NA_VERSAO_1, "boletim2026"))
    }

    // A chave da senha ----------------------------------------------------------------------

    @Test fun `a chave e a do PBKDF2 com HMAC-SHA256 dos exemplos da RFC 7914`() {
        // Os 32 primeiros bytes dos resultados da seção 11 da RFC, que é o primeiro bloco
        assertEquals(
            "55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc",
            hex(chaveDaSenha("passwd", "salt".toByteArray(), iteracoes = 1)),
        )
        assertEquals(
            "4ddcd8f60b98be21830cee5ef22701f9641a4418d04c0414aeff08876b34ab56",
            hex(chaveDaSenha("Password", "NaCl".toByteArray(), iteracoes = 80_000)),
        )
    }

    @Test fun `a chave e a mesma do PBKDF2 do Java`() {
        // O SecretKeyFactory faz a mesma conta, só que mais devagar no Android
        val sal = ByteArray(16) { (it * 7).toByte() }
        listOf("boletim2026", "café da manhã", "senha com espaços e 😀", "x".repeat(100)).forEach { senha ->
            val doJava = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(PBEKeySpec(senha.toCharArray(), sal, 1_000, 256))
                .encoded

            assertEquals(senha, hex(doJava), hex(chaveDaSenha(senha, sal, iteracoes = 1_000)))
        }
    }

    // Senha errada -------------------------------------------------------------------------

    @Test fun `a senha errada nao abre`() {
        val protegido = proteger(BACKUP, "senha boa")

        assertThrows(SenhaIncorretaException::class.java) { abrirComSenha(protegido, "senha ruim") }
    }

    @Test fun `maiuscula e minuscula sao senhas diferentes`() {
        val protegido = proteger(BACKUP, "Senha Boa")

        assertThrows(SenhaIncorretaException::class.java) { abrirComSenha(protegido, "senha boa") }
    }

    @Test fun `a senha vazia nao abre`() {
        val protegido = proteger(BACKUP, "senha boa")

        assertThrows(SenhaIncorretaException::class.java) { abrirComSenha(protegido, "") }
    }

    @Test fun `dados alterados nao abrem nem com a senha certa`() {
        val protegido = proteger(BACKUP, "senha boa")
        val dados = Base64.getDecoder().decode(campo(protegido, "dados"))
        dados[0] = (dados[0].toInt() xor 1).toByte()
        val alterado = protegido.replace(campo(protegido, "dados"), Base64.getEncoder().encodeToString(dados))

        assertThrows(SenhaIncorretaException::class.java) { abrirComSenha(alterado, "senha boa") }
    }

    @Test fun `a versao aberta alterada nao abre nem com a senha certa`() {
        // A versão vai aberta, mas presa aos dados: trocar ela no arquivo é como trocar os dados
        val protegido = proteger(BACKUP, "senha boa").replace("\"versao\": $VERSAO_DO_ARQUIVO", "\"versao\": 0")

        assertThrows(SenhaIncorretaException::class.java) { abrirComSenha(protegido, "senha boa") }
    }

    // O que é conferido antes da senha -----------------------------------------------------

    @Test fun `o backup com senha pede senha`() {
        assertTrue(temSenha(proteger(BACKUP, "senha boa")))
    }

    @Test fun `o backup sem senha nao pede senha`() {
        assertFalse(temSenha(BACKUP))
    }

    @Test fun `o que nao e backup nao pede senha`() {
        // Quem diz que não é backup é o lerBackup
        assertFalse(temSenha("Prova de matemática: 8,5"))
        assertFalse(temSenha("[1, 2, 3]"))
        assertFalse(temSenha("""{"app": "com.outro.app", "protecao": {}}"""))
    }

    @Test fun `backup com senha de uma versao mais nova pede para atualizar, antes da senha`() {
        val protegido = proteger(BACKUP, "senha boa")
            .replace("\"versao\": $VERSAO_DO_ARQUIVO", "\"versao\": ${VERSAO_DO_ARQUIVO + 1}")

        assertEquals(VERSAO_MAIS_NOVA, motivo { temSenha(protegido) })
    }

    @Test fun `outro jeito de proteger e de uma versao mais nova`() {
        val protegido = proteger(BACKUP, "senha boa").replace(ALGORITMO, "Argon2id/AES-256-GCM")

        assertEquals(VERSAO_MAIS_NOVA, motivo { temSenha(protegido) })
    }

    @Test fun `protecao sem um campo foi alterada`() {
        val protegido = proteger(BACKUP, "senha boa").replace(Regex(""""sal": "[^"]*",\s*"""), "")

        assertEquals(BACKUP_ALTERADO, motivo { temSenha(protegido) })
    }

    @Test fun `dados que nao sao Base64 foram alterados`() {
        val protegido = proteger(BACKUP, "senha boa").let { it.replace(campo(it, "dados"), "não é base64!") }

        assertEquals(BACKUP_ALTERADO, motivo { temSenha(protegido) })
    }

    @Test fun `iteracoes demais travariam o app, entao o arquivo foi alterado`() {
        val protegido = proteger(BACKUP, "senha boa").replace("\"iteracoes\": 1000", "\"iteracoes\": 2000000000")

        assertEquals(BACKUP_ALTERADO, motivo { temSenha(protegido) })
        assertEquals(BACKUP_ALTERADO, motivo { abrirComSenha(protegido, "senha boa") })
    }

    @Test fun `sal de outro tamanho foi alterado`() {
        val protegido = proteger(BACKUP, "senha boa").let { it.replace(campo(it, "sal"), "AAAA") }

        assertEquals(BACKUP_ALTERADO, motivo { temSenha(protegido) })
    }

    // Ajudantes ----------------------------------------------------------------------------

    // Poucas iterações, para os testes não demorarem
    private fun proteger(texto: String, senha: String) = protegerComSenha(texto, senha, iteracoes = 1_000)

    private fun motivo(executar: () -> Unit): String? =
        assertThrows(BackupInvalidoException::class.java) { executar() }.message

    /** O valor de um campo de texto do arquivo, como ele está escrito. */
    private fun campo(texto: String, nome: String): String =
        Regex(""""$nome": "([^"]*)"""").find(texto)!!.groupValues[1]

    private fun hex(bytes: ByteArray): String = HexFormat.of().formatHex(bytes)

    /** Sempre os mesmos bytes, para o arquivo sair igual toda vez. Só para o teste do formato. */
    private class SemSorte : SecureRandom() {
        override fun nextBytes(bytes: ByteArray) {
            bytes.indices.forEach { bytes[it] = it.toByte() }
        }
    }

    private companion object {
        // Só o começo de um backup: o que vai dentro não importa aqui, quem confere é o lerBackup
        val BACKUP = """
            {
                "app": "app.jammes.boletim",
                "versao": 1,
                "exportadoEm": "2026-10-19T13:45:00Z",
                "materias": [{"id": 1, "nome": "Matemática"}],
                "alunos": [{"id": 1, "nome": "James Willian"}]
            }
        """.trimIndent()

        const val BACKUP_CURTO = """{"app": "app.jammes.boletim", "versao": 1}"""

        val ARQUIVO_SALVO = """
            {
                "app": "app.jammes.boletim",
                "versao": 2,
                "protecao": {
                    "algoritmo": "PBKDF2WithHmacSHA256/AES-256-GCM",
                    "iteracoes": 1000,
                    "sal": "AAECAwQFBgcICQoLDA0ODw==",
                    "iv": "AAECAwQFBgcICQoL"
                },
                "dados": "7AL0gDsEDndPogCEROgNguO4OozLR4+va0kXvH4SerW0+ueBgJ04YujfHKkXt+QAQgDPfyVK7rSzUg=="
            }
        """.trimIndent()

        val ARQUIVO_SALVO_NA_VERSAO_1 = """
            {
                "app": "app.jammes.boletim",
                "versao": 1,
                "protecao": {
                    "algoritmo": "PBKDF2WithHmacSHA256/AES-256-GCM",
                    "iteracoes": 1000,
                    "sal": "AAECAwQFBgcICQoLDA0ODw==",
                    "iv": "AAECAwQFBgcICQoL"
                },
                "dados": "7AL0gDsEDndPogCEROgNguO4OozLR4+va0kXvH4SerW0+ueBgJ04YujfHRjtTJNHvoKcbdltc+QT3Q=="
            }
        """.trimIndent()
    }
}
