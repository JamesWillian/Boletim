package app.jammes.boletim.presentation.ui.boletim

import app.jammes.boletim.domain.model.DisciplinaResumo
import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.domain.model.StatusDisciplina
import org.junit.Assert.assertEquals
import org.junit.Test

/** O gráfico de barras do Boletim: os tracejados das mínimas e a contagem por situação. */
class GraficoDasMediasTest {

    @Test fun `sem regra propria a minima do ano atravessa todas as linhas`() {
        val disciplinas = listOf(resumo(1), resumo(2), resumo(3))

        assertEquals(listOf(0..2), trechosDaMinimaGeral(disciplinas, mediaMinima = 8.0))
    }

    @Test fun `a minima do ano pula a linha de quem tem a sua`() {
        // Matemática, a segunda, tem mínima 7,0: o tracejado cinza para antes dela e volta depois
        val disciplinas = listOf(resumo(1), resumo(2, minima = 7.0), resumo(3), resumo(4))

        assertEquals(listOf(0..0, 2..3), trechosDaMinimaGeral(disciplinas, mediaMinima = 8.0))
    }

    @Test fun `regra propria na primeira ou na ultima linha nao deixa trecho vazio`() {
        val disciplinas = listOf(resumo(1, minima = 6.0), resumo(2), resumo(3, minima = 7.0))

        assertEquals(listOf(1..1), trechosDaMinimaGeral(disciplinas, mediaMinima = 8.0))
    }

    @Test fun `todas com regra propria, nao sobra trecho da minima do ano`() {
        val disciplinas = listOf(resumo(1, minima = 6.0), resumo(2, minima = 7.0))

        assertEquals(emptyList<IntRange>(), trechosDaMinimaGeral(disciplinas, mediaMinima = 8.0))
    }

    @Test fun `a legenda so lista quem tem mínima diferente da do ano`() {
        // A terceira tem regra própria, mas com a mesma mínima: o tracejado dela seria o cinza
        val disciplinas = listOf(resumo(1), resumo(2, minima = 7.0), resumo(3, minima = 8.0), resumo(4, minima = 9.0))

        assertEquals(listOf(2L, 4L), comMinimaPropria(disciplinas, mediaMinima = 8.0).map { it.id })
    }

    @Test fun `contagem junta reprovado com abaixo e pula a situacao sem disciplina`() {
        val disciplinas = listOf(
            resumo(1, status = StatusDisciplina.APROVADO),
            resumo(2, status = StatusDisciplina.ABAIXO),
            resumo(3, status = StatusDisciplina.REPROVADO),
            resumo(4, status = StatusDisciplina.SEM_NOTA),
            resumo(5, status = StatusDisciplina.APROVADO),
        )

        assertEquals(
            listOf(
                "2 com folga" to StatusDisciplina.APROVADO,
                "2 abaixo" to StatusDisciplina.ABAIXO,
                "1 sem nota" to StatusDisciplina.SEM_NOTA,
            ),
            contagemPorStatus(disciplinas),
        )
    }
}

private fun resumo(
    id: Long,
    minima: Double = 8.0,
    status: StatusDisciplina = StatusDisciplina.APROVADO,
) = DisciplinaResumo(
    id = id,
    nome = "Disciplina $id",
    abreviacao = null,
    cor = 0,
    icone = IconeMateria.LIVRO,
    media = 9.0,
    mediaMinima = minima,
    status = status,
    faltas = 0,
    limiteFaltas = null,
    situacao = null,
)
