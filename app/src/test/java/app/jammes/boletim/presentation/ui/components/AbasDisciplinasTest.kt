package app.jammes.boletim.presentation.ui.components

import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.MateriaDomain
import org.junit.Assert.assertEquals
import org.junit.Test

/** As siglas que as abas mostram no estilo "Ícones + sigla". */
class AbasDisciplinasTest {

    @Test fun `disciplina com o nome da materia usa a sigla dela`() {
        val siglas = siglasDasDisciplinas(
            listOf(disciplina(1, "Educação Física", materiaId = 8)),
            listOf(materia(8, "Educação Física", "EDF")),
        )
        assertEquals(mapOf(1L to "EDF"), siglas)
    }

    @Test fun `disciplina renomeada tira a sigla do proprio nome`() {
        val siglas = siglasDasDisciplinas(
            listOf(disciplina(1, "Redação", materiaId = 1)),
            listOf(materia(1, "Português", "POR")),
        )
        assertEquals(mapOf(1L to "RED"), siglas)
    }

    @Test fun `materia sem sigla ou nao encontrada tira do nome`() {
        val siglas = siglasDasDisciplinas(
            listOf(disciplina(1, "Robótica", materiaId = 20), disciplina(2, "Xadrez", materiaId = 99)),
            listOf(materia(20, "Robótica", abreviacao = " ")),
        )
        assertEquals(mapOf(1L to "ROB", 2L to "XAD"), siglas)
    }

    @Test fun `sigla da materia vai em maiusculas e sem espacos`() {
        val siglas = siglasDasDisciplinas(
            listOf(disciplina(1, "Matemática", materiaId = 2)),
            listOf(materia(2, "Matemática", " mat ")),
        )
        assertEquals("MAT", siglas[1L])
    }

    @Test fun `sigla do nome pula o que nao e letra nem numero`() {
        assertEquals("EDF", siglaDoNome("Ed. Física"))
        assertEquals("CIÊ", siglaDoNome("Ciências"))
        assertEquals("AR", siglaDoNome("Ar"))
    }

    private fun disciplina(id: Long, nome: String, materiaId: Long) =
        DisciplinaDomain(id = id, nome = nome, materiaId = materiaId, anoLetivoId = 1)

    private fun materia(id: Long, nome: String, abreviacao: String?) =
        MateriaDomain(id = id, nome = nome, abreviacao = abreviacao)
}
