package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AjustesAnoLetivo
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AjustesAnoLetivoTest {

    @Test fun `materia que entra no ano vira disciplina com o nome, a cor e o icone dela`() {
        // prepara
        val original = ajustes(disciplinas = emptyList())

        // executa
        val novas = disciplinasNovas(original, materiaIds = setOf(INGLES.id))

        // confere
        val ingles = novas.single()
        assertEquals("Inglês", ingles.nome)
        assertEquals(INGLES.cor, ingles.cor)
        assertEquals(IconeMateria.IDIOMA, ingles.icone)
        assertEquals(INGLES.id, ingles.materiaId)
        assertEquals(ANO_LETIVO_ID, ingles.anoLetivoId)
        assertEquals(0L, ingles.id) // ainda não gravada
    }

    @Test fun `disciplinas novas vao para o fim das abas, na ordem das materias`() {
        val original = ajustes(disciplinas = listOf(disciplina(PORTUGUES, ordem = 1), disciplina(MATEMATICA, ordem = 5)))

        val novas = disciplinasNovas(original, materiaIds = setOf(ARTE.id, INGLES.id, PORTUGUES.id, MATEMATICA.id))

        assertEquals(listOf("Inglês", "Arte"), novas.map { it.nome }) // Inglês vem antes nas matérias
        assertEquals(listOf(6, 7), novas.map { it.ordem })
    }

    @Test fun `materia que ja esta no ano nao ganha outra disciplina`() {
        val original = ajustes(disciplinas = listOf(disciplina(PORTUGUES, ordem = 1)))

        assertTrue(disciplinasNovas(original, materiaIds = setOf(PORTUGUES.id)).isEmpty())
    }

    @Test fun `materia desmarcada nao vira disciplina`() {
        assertTrue(disciplinasNovas(ajustes(disciplinas = emptyList()), materiaIds = emptySet()).isEmpty())
    }
}

private const val ANO_LETIVO_ID = 7L

private val PORTUGUES = MateriaDomain(id = 1, nome = "Português", cor = 0)
private val MATEMATICA = MateriaDomain(id = 2, nome = "Matemática", cor = 1)
private val INGLES = MateriaDomain(id = 6, nome = "Inglês", cor = 8, icone = IconeMateria.IDIOMA)
private val ARTE = MateriaDomain(id = 7, nome = "Arte", cor = 6)

private fun disciplina(materia: MateriaDomain, ordem: Int) = DisciplinaDomain(
    id = materia.id * 10,
    nome = materia.nome,
    cor = materia.cor,
    ordem = ordem,
    materiaId = materia.id,
    anoLetivoId = ANO_LETIVO_ID,
)

// Preenche o que a regra não usa, pra cada teste mostrar só o que importa.
private fun ajustes(disciplinas: List<DisciplinaDomain>) = AjustesAnoLetivo(
    anoLetivo = AnoLetivoDomain(
        id = ANO_LETIVO_ID,
        ano = 2024,
        serie = null,
        periodo = emptyList(),
        qtdPeriodos = 4,
        alunoId = 1,
    ),
    regra = RegraAvaliacaoDomain(
        anoLetivoId = ANO_LETIVO_ID,
        disciplinaId = null,
        mediaMinima = 7.0,
        mediaRecuperacao = 5.0,
        frequenciaMinima = 75.0,
    ),
    materias = listOf(PORTUGUES, MATEMATICA, INGLES, ARTE),
    disciplinas = disciplinas,
    lancamentosPorPeriodo = emptyMap(),
    lancamentosPorDisciplina = emptyMap(),
)
