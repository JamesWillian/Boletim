package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.FonteMateria
import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.domain.model.LimiteDoGratisException
import app.jammes.boletim.domain.model.MateriaDoCadastro
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.domain.model.Plano
import app.jammes.boletim.domain.repository.MateriaRepository
import app.jammes.boletim.domain.repository.PlanoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** O que a tela de matérias lê e grava, e o que os casos de uso conferem antes. */
class MateriasUseCaseTest {

    // Regras puras -----------------------------------------------------------------------------

    @Test fun `o icone da materia nova sai do nome, e sem palavra conhecida fica o padrao`() {
        assertEquals(IconeMateria.ROBO, iconeSugerido("Robótica"))
        assertEquals(IconeMateria.NOTA_MUSICAL, iconeSugerido("Educação Musical"))
        assertEquals(IconeMateria.CODIGO, iconeSugerido("Programação"))
        assertEquals(IconeMateria.PADRAO, iconeSugerido("Ensino Religioso"))
    }

    @Test fun `sem acentos e em minusculas, dois jeitos de escrever dao no mesmo`() {
        assertEquals("matematica", "Matemática".semAcentos())
        assertEquals("educacao fisica", "EDUCAÇÃO FÍSICA".semAcentos())
    }

    // Observar ---------------------------------------------------------------------------------

    @Test fun `cada materia vem com os anos em que esta em uso, na ordem do cadastro`() = runBlocking {
        val materias = MateriasFalsas(listOf(ESPANHOL, ROBOTICA), anosEmUso = mapOf(ESPANHOL.id to 2))

        val lidas = ObservarMateriasDoCadastro(materias)().first()

        assertEquals(listOf(MateriaDoCadastro(ESPANHOL, 2), MateriaDoCadastro(ROBOTICA, 0)), lidas)
    }

    // Salvar -----------------------------------------------------------------------------------

    @Test fun `no gratis, a matéria nova entra enquanto cabe no limite`() = runBlocking {
        val materias = MateriasFalsas(proprias(2))

        SalvarMateria(materias, PlanoFalso(Plano.GRATIS))(MateriaDomain(nome = "Robótica"))

        assertEquals("Robótica", materias.gravadas.single().nome)
    }

    @Test fun `no gratis, a quarta matéria e do Pro, e nada e gravado`() {
        val materias = MateriasFalsas(proprias(3))

        assertThrows(LimiteDoGratisException::class.java) {
            runBlocking { SalvarMateria(materias, PlanoFalso(Plano.GRATIS))(MateriaDomain(nome = "Robótica")) }
        }
        assertTrue(materias.gravadas.isEmpty())
    }

    @Test fun `no Pro, a matéria nova nao tem limite`() = runBlocking {
        val materias = MateriasFalsas(proprias(3))

        SalvarMateria(materias, PlanoFalso(Plano.PRO))(MateriaDomain(nome = "Robótica"))

        assertEquals(1, materias.gravadas.size)
    }

    @Test fun `no gratis, a matéria propria se edita mesmo acima do limite`() = runBlocking {
        val cadastro = proprias(5)
        val materias = MateriasFalsas(cadastro)

        SalvarMateria(materias, PlanoFalso(Plano.GRATIS))(cadastro.first().copy(cor = 7))

        assertEquals(7, materias.gravadas.single().cor)
    }

    @Test fun `a matéria do app nao se edita em nenhum plano, e nada e gravado`() {
        Plano.entries.forEach { plano ->
            val materias = MateriasFalsas(listOf(MATEMATICA))

            assertThrows(MateriaProtegidaException::class.java) {
                runBlocking { SalvarMateria(materias, PlanoFalso(plano))(MATEMATICA.copy(cor = 7)) }
            }
            assertTrue(materias.gravadas.isEmpty())
        }
    }

    @Test fun `vale a origem que esta no banco, e nao a que veio da tela`() {
        val materias = MateriasFalsas(listOf(MATEMATICA))

        assertThrows(MateriaProtegidaException::class.java) {
            runBlocking {
                SalvarMateria(materias, PlanoFalso(Plano.PRO))(MATEMATICA.copy(fonte = FonteMateria.USUARIO))
            }
        }
    }

    // Excluir ----------------------------------------------------------------------------------

    @Test fun `a matéria propria fora dos anos letivos sai do cadastro`() = runBlocking {
        val materias = MateriasFalsas(listOf(ROBOTICA))

        ExcluirMateria(materias)(ROBOTICA)

        assertEquals(listOf(ROBOTICA), materias.excluidas)
    }

    @Test fun `em uso num ano letivo, a matéria nao sai, e nada e apagado`() {
        val materias = MateriasFalsas(listOf(ESPANHOL), anosEmUso = mapOf(ESPANHOL.id to 1))

        assertThrows(MateriaProtegidaException::class.java) {
            runBlocking { ExcluirMateria(materias)(ESPANHOL) }
        }
        assertTrue(materias.excluidas.isEmpty())
    }

    @Test fun `a matéria do app nao sai, mesmo fora dos anos letivos`() {
        val materias = MateriasFalsas(listOf(MATEMATICA))

        assertThrows(MateriaProtegidaException::class.java) {
            runBlocking { ExcluirMateria(materias)(MATEMATICA) }
        }
        assertTrue(materias.excluidas.isEmpty())
    }

    private fun proprias(quantas: Int) = (1..quantas).map { MateriaDomain(id = 100L + it, nome = "Própria $it") }

    private class MateriasFalsas(
        private val cadastro: List<MateriaDomain>,
        private val anosEmUso: Map<Long, Int> = emptyMap(),
    ) : MateriaRepository {
        val gravadas = mutableListOf<MateriaDomain>()
        val excluidas = mutableListOf<MateriaDomain>()

        override fun observeMaterias() = flowOf(cadastro)
        override fun observeAnosEmUso() = flowOf(anosEmUso)

        override suspend fun upsert(materia: MateriaDomain): Long {
            gravadas += materia
            return materia.id
        }

        override suspend fun delete(materia: MateriaDomain) {
            excluidas += materia
        }
    }

    private class PlanoFalso(plano: Plano) : PlanoRepository {
        override val plano = flowOf(plano)
        override suspend fun salvarProDeTeste(ligado: Boolean) = Unit
    }

    private companion object {
        val MATEMATICA = MateriaDomain(id = 2, nome = "Matemática", fonte = FonteMateria.APP)
        val ESPANHOL = MateriaDomain(id = 14, nome = "Espanhol", fonte = FonteMateria.USUARIO)
        val ROBOTICA = MateriaDomain(id = 15, nome = "Robótica", fonte = FonteMateria.USUARIO)
    }
}
