package app.jammes.boletim.presentation.ui.components

import app.jammes.boletim.domain.model.DisciplinaDomain
import org.junit.Assert.assertEquals
import org.junit.Test

/** As siglas que as abas mostram no estilo "Ícones + sigla". */
class AbasDisciplinasTest {

    @Test fun `cada disciplina usa a propria abreviacao`() {
        val siglas = siglasDasDisciplinas(
            listOf(disciplina(1, "Educação Física", abreviacao = "EDF"), disciplina(2, "Português", abreviacao = "POR")),
        )
        assertEquals(mapOf(1L to "EDF", 2L to "POR"), siglas)
    }

    @Test fun `disciplina renomeada continua com a abreviacao que tem`() {
        // A matéria era "Teste" (TES); a disciplina virou "Robótica", com a abreviação editada
        val siglas = siglasDasDisciplinas(listOf(disciplina(1, "Robótica", abreviacao = "RBT")))

        assertEquals(mapOf(1L to "RBT"), siglas)
    }

    @Test fun `sem abreviacao, ou so com espacos, a sigla sai do nome`() {
        val siglas = siglasDasDisciplinas(
            listOf(disciplina(1, "Robótica", abreviacao = null), disciplina(2, "Xadrez", abreviacao = " ")),
        )
        assertEquals(mapOf(1L to "ROB", 2L to "XAD"), siglas)
    }

    @Test fun `abreviacao vai em maiusculas e sem espacos nas pontas`() {
        assertEquals("MAT", siglaDaDisciplina(" mat ", "Matemática"))
    }

    @Test fun `sigla do nome pula o que nao e letra nem numero`() {
        assertEquals("EDF", siglaDoNome("Ed. Física"))
        assertEquals("CIÊ", siglaDoNome("Ciências"))
        assertEquals("AR", siglaDoNome("Ar"))
    }

    private fun disciplina(id: Long, nome: String, abreviacao: String?) =
        DisciplinaDomain(id = id, nome = nome, abreviacao = abreviacao, materiaId = 1, anoLetivoId = 1)
}
