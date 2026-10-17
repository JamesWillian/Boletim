package app.jammes.boletim.presentation.ui.backup

import app.jammes.boletim.domain.model.ResumoDoBackup
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant

/** Os textos do backup e restauração. */
class BackupScreenTest {

    @Test fun `o arquivo ja vem com a data no nome`() {
        assertEquals("boletim-2026-10-15.json", nomeDoArquivo(LocalDate.of(2026, 10, 15)))
    }

    @Test fun `a confirmacao diz de quando e o backup e o que ele tem`() {
        val resumo = ResumoDoBackup(
            exportadoEm = Instant.parse("2026-10-14T18:30:00Z"),
            alunos = listOf("James Willian", "Maria Clara"),
            anosLetivos = 3,
            avaliacoes = 60,
        )

        assertEquals(
            "Backup de 14/10/2026 com James Willian e Maria Clara: 3 anos letivos e 60 avaliações.",
            descricaoDoBackup(resumo, SAO_PAULO),
        )
    }

    @Test fun `o dia e o do fuso do aparelho`() {
        // 1h da manhã em Greenwich ainda é a noite do dia anterior em São Paulo
        val resumo = ResumoDoBackup(Instant.parse("2026-10-15T01:00:00Z"), listOf("James"), anosLetivos = 1, avaliacoes = 1)

        assertEquals("Backup de 14/10/2026 com James: 1 ano letivo e 1 avaliação.", descricaoDoBackup(resumo, SAO_PAULO))
        assertEquals("Backup de 15/10/2026 com James: 1 ano letivo e 1 avaliação.", descricaoDoBackup(resumo, ZoneId.of("UTC")))
    }

    @Test fun `os nomes viram uma lista em portugues`() {
        assertEquals("Ana", listaDeNomes(listOf("Ana")))
        assertEquals("Ana e Bia", listaDeNomes(listOf("Ana", "Bia")))
        assertEquals("Ana, Bia e Caio", listaDeNomes(listOf("Ana", "Bia", "Caio")))
        assertEquals("Ana, Bia e mais 3 alunos", listaDeNomes(listOf("Ana", "Bia", "Caio", "Davi", "Eva")))
    }

    @Test fun `as avaliacoes no singular, no plural e nenhuma`() {
        assertEquals(listOf("nenhuma avaliação", "1 avaliação", "2 avaliações"), listOf(0, 1, 2).map(::quantasAvaliacoes))
    }

    private companion object {
        val SAO_PAULO: ZoneId = ZoneId.of("America/Sao_Paulo")
    }
}
