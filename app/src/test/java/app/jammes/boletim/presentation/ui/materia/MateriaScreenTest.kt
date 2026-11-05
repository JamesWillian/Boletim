package app.jammes.boletim.presentation.ui.materia

import app.jammes.boletim.domain.model.FonteMateria
import app.jammes.boletim.domain.model.ImpedimentoDeExcluir
import app.jammes.boletim.domain.model.MateriaDoCadastro
import app.jammes.boletim.domain.model.MateriaDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A busca, o filtro e os grupos da lista, e os textos do sheet. */
class MateriaScreenTest {

    // Grupos -----------------------------------------------------------------------------------

    @Test fun `sem busca nem filtro, cada materia vai para o seu grupo, na ordem do cadastro`() {
        val grupos = gruposDeMaterias(CADASTRO, busca = "", FiltroDeMaterias.TODAS)

        assertEquals(listOf("Português", "Matemática", "Vinda de fora"), grupos.doApp.nomes())
        assertEquals(listOf("Espanhol", "Educação Musical"), grupos.doAluno.nomes())
    }

    @Test fun `o filtro deixa um grupo so`() {
        val doApp = gruposDeMaterias(CADASTRO, busca = "", FiltroDeMaterias.DO_APP)
        val doAluno = gruposDeMaterias(CADASTRO, busca = "", FiltroDeMaterias.DO_ALUNO)

        assertTrue(doApp.doAluno.isEmpty())
        assertEquals(3, doApp.doApp.size)
        assertTrue(doAluno.doApp.isEmpty())
        assertEquals(2, doAluno.doAluno.size)
    }

    @Test fun `a busca nao liga para maiusculas, acentos e espacos nas pontas`() {
        assertEquals(listOf("Matemática"), gruposDeMaterias(CADASTRO, " MATEMATICA ", FiltroDeMaterias.TODAS).doApp.nomes())
        assertEquals(listOf("Educação Musical"), gruposDeMaterias(CADASTRO, "educacao", FiltroDeMaterias.TODAS).doAluno.nomes())
    }

    @Test fun `a busca acha um pedaco do nome, em qualquer grupo`() {
        val grupos = gruposDeMaterias(CADASTRO, "ma", FiltroDeMaterias.TODAS)

        assertEquals(listOf("Matemática"), grupos.doApp.nomes())
    }

    @Test fun `a busca e o filtro valem juntos`() {
        assertTrue(gruposDeMaterias(CADASTRO, "português", FiltroDeMaterias.DO_ALUNO).vazios)
    }

    @Test fun `sem nada encontrado, os grupos ficam vazios`() {
        val grupos = gruposDeMaterias(CADASTRO, "xadrez", FiltroDeMaterias.TODAS)

        assertTrue(grupos.vazios)
        assertFalse(gruposDeMaterias(CADASTRO, "", FiltroDeMaterias.TODAS).vazios)
    }

    // Textos do sheet --------------------------------------------------------------------------

    @Test fun `o titulo diz o que da para fazer com a materia`() {
        assertEquals("Nova matéria", tituloDaMateria(materiaNova(coresUsadas = emptyList())))
        assertEquals("Editar matéria", tituloDaMateria(MateriaDomain(id = 14, fonte = FonteMateria.USUARIO)))
        assertEquals("Matéria do app", tituloDaMateria(MateriaDomain(id = 2, fonte = FonteMateria.APP)))
    }

    @Test fun `a materia do app avisa que nao muda nem sai`() {
        assertEquals(
            "As matérias do app não podem ser alteradas ou excluídas.",
            textoDoImpedimento(ImpedimentoDeExcluir.DoApp),
        )
    }

    @Test fun `em uso, avisa em quantos anos`() {
        assertEquals("Em uso em 1 ano letivo: não pode ser excluída.", textoDoImpedimento(ImpedimentoDeExcluir.EmUso(1)))
        assertEquals("Em uso em 2 anos letivos: não pode ser excluída.", textoDoImpedimento(ImpedimentoDeExcluir.EmUso(2)))
    }

    private fun List<MateriaDoCadastro>.nomes() = map { it.materia.nome }

    private companion object {
        val CADASTRO = listOf(
            materia(1, "Português", FonteMateria.APP),
            materia(2, "Matemática", FonteMateria.APP),
            materia(3, "Espanhol", FonteMateria.USUARIO),
            materia(4, "Vinda de fora", FonteMateria.REMOTA),
            materia(5, "Educação Musical", FonteMateria.USUARIO),
        )

        fun materia(id: Long, nome: String, fonte: FonteMateria) =
            MateriaDoCadastro(MateriaDomain(id = id, nome = nome, fonte = fonte))
    }
}
