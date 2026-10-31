package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.IconeMateria
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** O que a gravação da edição da disciplina confere antes de gravar. */
class EdicaoDisciplinaTest {

    @Test fun `mudar o professor, as aulas e o periodo nao e personalizar`() {
        val depois = MATEMATICA.copy(
            professor = "Prof. Ana",
            totalAulas = 80,
            periodoInicio = LocalDate.of(2026, 2, 1),
            periodoFim = LocalDate.of(2026, 6, 30),
        )

        assertFalse(mudaPersonalizacao(MATEMATICA, depois))
    }

    @Test fun `mudar o nome, a sigla, a cor ou o icone e personalizar`() {
        assertTrue(mudaPersonalizacao(MATEMATICA, MATEMATICA.copy(nome = "Matemática I")))
        assertTrue(mudaPersonalizacao(MATEMATICA, MATEMATICA.copy(abreviacao = "MT")))
        assertTrue(mudaPersonalizacao(MATEMATICA, MATEMATICA.copy(cor = 5)))
        assertTrue(mudaPersonalizacao(MATEMATICA, MATEMATICA.copy(icone = IconeMateria.LIVRO)))
    }

    @Test fun `uma personalizacao de antes nao conta, porque a comparacao e com a disciplina salva`() {
        // Personalizada antes do limite existir: a cor já não é a da matéria
        val personalizada = MATEMATICA.copy(cor = 7)

        assertFalse(mudaPersonalizacao(personalizada, personalizada.copy(professor = "Prof. Ana")))
    }

    @Test fun `espacos nas pontas e sigla vazia nao contam como mudanca`() {
        assertFalse(mudaPersonalizacao(MATEMATICA.copy(nome = "Matemática "), MATEMATICA))
        assertFalse(mudaPersonalizacao(MATEMATICA.copy(abreviacao = ""), MATEMATICA.copy(abreviacao = null)))
    }

    private companion object {
        val MATEMATICA = DisciplinaDomain(
            id = 2,
            nome = "Matemática",
            abreviacao = "MAT",
            cor = 1,
            icone = IconeMateria.CALCULADORA,
            materiaId = 2,
            anoLetivoId = 1,
        )
    }
}
