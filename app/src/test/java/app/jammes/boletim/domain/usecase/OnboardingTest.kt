package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.Etapa
import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.domain.model.MateriaDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTest {

    @Test fun `fundamental traz as 8 de costume, na ordem do cadastro`() {
        val nomes = materiasDoPrimeiroAno(Etapa.FUNDAMENTAL, PADRAO).map { it.nome }

        assertEquals(
            listOf("Português", "Matemática", "História", "Geografia", "Ciências", "Inglês", "Arte", "Educação Física"),
            nomes,
        )
    }

    @Test fun `medio troca ciencias por fisica, quimica e biologia, e ganha filosofia e sociologia`() {
        val nomes = materiasDoPrimeiroAno(Etapa.MEDIO, PADRAO).map { it.nome }

        assertEquals(12, nomes.size)
        assertFalse("Ciências" in nomes)
        assertTrue(nomes.containsAll(listOf("Física", "Química", "Biologia", "Filosofia", "Sociologia")))
    }

    @Test fun `sem etapa vem as 5 primeiras do cadastro, como num ano novo`() {
        assertEquals(PADRAO.take(MATERIAS_DO_ANO_NOVO), materiasDoPrimeiroAno(null, PADRAO))
    }

    @Test fun `cadastro sem nenhuma das de costume tambem fica com as 5 primeiras`() {
        val proprias = (1..7).map { MateriaDomain(id = it.toLong(), nome = "Matéria $it") }

        assertEquals(proprias.take(MATERIAS_DO_ANO_NOVO), materiasDoPrimeiroAno(Etapa.MEDIO, proprias))
    }

    @Test fun `materia nova pega as cores que o cadastro ainda nao usa, em ordem`() {
        // As padrão usam 0–6, 8, 10, 11, 14, 16 e 17
        val padrao = PADRAO.zip(listOf(0, 1, 16, 10, 2, 8, 6, 3, 5, 4, 11, 17, 14)) { materia, cor -> materia.copy(cor = cor) }

        val novas = materiasNovas(listOf("Robótica", "Música", "Teatro"), padrao)

        assertEquals(listOf(7, 9, 12), novas.map { it.cor })
        assertEquals(listOf(0L, 0L, 0L), novas.map { it.id }) // ainda não gravadas
        assertEquals("ROB", novas.first().abreviacao)
    }

    @Test fun `o icone da materia nova sai do nome, e sem palavra conhecida fica o padrao`() {
        assertEquals(IconeMateria.ROBO, iconeSugerido("Robótica"))
        assertEquals(IconeMateria.NOTA_MUSICAL, iconeSugerido("Educação Musical"))
        assertEquals(IconeMateria.CODIGO, iconeSugerido("Programação"))
        assertEquals(IconeMateria.PADRAO, iconeSugerido("Ensino Religioso"))
    }
}

// As do MateriasPadraoCallback, na mesma ordem
private val PADRAO = listOf(
    "Português", "Matemática", "História", "Geografia", "Ciências", "Inglês", "Arte", "Educação Física",
    "Física", "Química", "Biologia", "Filosofia", "Sociologia",
).mapIndexed { i, nome -> MateriaDomain(id = i + 1L, nome = nome) }
