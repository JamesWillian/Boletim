package app.jammes.boletim.avisos

import app.jammes.boletim.domain.model.AvisoDoAluno
import app.jammes.boletim.domain.model.NotificacaoDomain
import app.jammes.boletim.domain.model.TipoNotificacao
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

/** Os textos do aviso de cada aluno na barra do celular e do resumo do grupo. */
class TextosDosAvisosTest {

    // O aviso de um aluno --------------------------------------------------------------------

    @Test fun `com uma notificacao so, o aviso e ela`() {
        val aviso = aviso("Prova de Matemática amanhã")

        assertEquals("Prova de Matemática amanhã", tituloDoAviso(aviso))
        assertEquals("Texto de Prova de Matemática amanhã", textoDoAviso(aviso))
    }

    @Test fun `com varias, o titulo conta e o texto junta os titulos`() {
        val aviso = aviso("Prova de Matemática amanhã", "Média de Inglês abaixo da mínima", "Prova de Artes sem nota")

        assertEquals("3 avisos", tituloDoAviso(aviso))
        assertEquals(
            "Prova de Matemática amanhã · Média de Inglês abaixo da mínima · Prova de Artes sem nota",
            textoDoAviso(aviso),
        )
    }

    @Test fun `aberto, uma linha por notificacao enquanto cabem`() {
        val titulos = List(LINHAS_NO_AVISO) { "Aviso ${it + 1}" }

        assertEquals(titulos, linhasDoAviso(aviso(*titulos.toTypedArray())))
    }

    @Test fun `as que nao cabem viram e mais N na ultima linha`() {
        val linhas = linhasDoAviso(aviso("A", "B", "C", "D", "E", "F", "G"))

        assertEquals(listOf("A", "B", "C", "D", "e mais 3"), linhas)
    }

    // O resumo -------------------------------------------------------------------------------

    @Test fun `o resumo conta as notificacoes de todos os alunos`() {
        assertEquals("1 aviso", tituloDoResumo(listOf(aviso("A", nome = "James"))))
        assertEquals("3 avisos", tituloDoResumo(listOf(aviso("A", nome = "James"), aviso("B", "C", nome = "Maria"))))
    }

    @Test fun `cada aluno numa linha do resumo, com a mais nova e quantas mais`() {
        assertEquals("James: Prova de Matemática amanhã", linhaDoResumo(aviso("Prova de Matemática amanhã", nome = "James")))
        assertEquals(
            "Maria: Prova de Artes hoje e mais 2",
            linhaDoResumo(aviso("Prova de Artes hoje", "B", "C", nome = "Maria")),
        )
    }

    @Test fun `sem o nome do aluno, a linha vai sem ele`() {
        assertEquals("Prova de Artes hoje e mais 1", linhaDoResumo(aviso("Prova de Artes hoje", "B", nome = null)))
    }

    private fun aviso(vararg titulos: String, nome: String? = null) = AvisoDoAluno(
        alunoId = 1,
        nome = nome,
        notificacoes = titulos.mapIndexed { i, titulo ->
            NotificacaoDomain(
                id = i + 1L,
                alunoId = 1,
                tipo = TipoNotificacao.AVISO,
                chave = "teste:$i",
                titulo = titulo,
                texto = "Texto de $titulo",
                criadaEm = Instant.parse("2026-10-17T22:00:00Z"),
            )
        },
    )
}
