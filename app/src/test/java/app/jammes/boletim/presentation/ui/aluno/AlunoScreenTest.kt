package app.jammes.boletim.presentation.ui.aluno

import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.ResumoDoAluno
import app.jammes.boletim.domain.model.TipoPeriodo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Os textos e as validações que o perfil do aluno mostra. */
class AlunoScreenTest {

    // Textos -----------------------------------------------------------------------------------

    @Test fun `resumo dos alunos fala no singular quando ha um so`() {
        assertEquals("1 aluno neste aparelho", resumoDosAlunos(1))
        assertEquals("3 alunos neste aparelho", resumoDosAlunos(3))
    }

    @Test fun `resumo dos anos fala no singular quando ha um so`() {
        assertEquals("1 ano letivo", resumoDosAnos(1))
        assertEquals("2 anos letivos", resumoDosAnos(2))
    }

    @Test fun `quantidade de periodos vai no plural do tipo`() {
        assertEquals("4 unidades", quantosPeriodos(4, TipoPeriodo.UNIDADE))
        assertEquals("1 bimestre", quantosPeriodos(1, TipoPeriodo.BIMESTRE))
        assertEquals("2 semestres", quantosPeriodos(2, TipoPeriodo.SEMESTRE))
        assertNull(quantosPeriodos(0, TipoPeriodo.TRIMESTRE))
    }

    @Test fun `resumo do ano tem a serie e como ele se divide`() {
        assertEquals("9º Ano · 4 unidades", resumoDoAnoLetivo(ano(2024, serie = "9º Ano", periodos = 4)))
    }

    @Test fun `ano sem serie diz isso`() {
        assertEquals("Sem série · 4 unidades", resumoDoAnoLetivo(ano(2024, serie = " ", periodos = 4)))
    }

    // Ano novo ---------------------------------------------------------------------------------

    @Test fun `ano sugerido e o do calendario quando o aluno so tem anos passados`() {
        assertEquals(2026, anoSugerido(listOf(ano(2024)), hoje = LocalDate.of(2026, 10, 10)))
    }

    @Test fun `ano sugerido e o seguinte quando o do calendario ja existe`() {
        assertEquals(2027, anoSugerido(listOf(ano(2024), ano(2026)), hoje = LocalDate.of(2026, 10, 10)))
    }

    @Test fun `sem nenhum ano a sugestao e o do calendario`() {
        assertEquals(2026, anoSugerido(emptyList(), hoje = LocalDate.of(2026, 10, 10)))
    }

    @Test fun `ano sem 4 digitos nao pode ser criado`() {
        assertEquals("4 dígitos", problemaDoAnoNovo(ano = null, anosLetivos = emptyList()))
    }

    @Test fun `ano que o aluno ja tem nao pode ser criado de novo`() {
        assertEquals("Já existe", problemaDoAnoNovo(ano = 2024, anosLetivos = listOf(ano(2024))))
    }

    @Test fun `ano novo e valido nao tem problema`() {
        assertNull(problemaDoAnoNovo(ano = 2026, anosLetivos = listOf(ano(2024))))
    }

    // Editar e excluir o aluno -----------------------------------------------------------------

    @Test fun `nome em branco nao pode ser salvo`() {
        assertEquals("Escreva o nome", problemaDoNome(""))
        assertEquals("Escreva o nome", problemaDoNome("   "))
        assertNull(problemaDoNome(" Ana "))
    }

    @Test fun `o que e do aluno vem em lista, com o e antes do ultimo`() {
        assertEquals(
            "2 anos letivos, 10 disciplinas, 53 avaliações e 8 faltas",
            oQueEDoAluno(ResumoDoAluno(anosLetivos = 2, disciplinas = 10, avaliacoes = 53, faltas = 8)),
        )
    }

    @Test fun `o que e do aluno fala no singular quando ha um so`() {
        assertEquals(
            "1 ano letivo, 1 disciplina, 1 avaliação e 1 falta",
            oQueEDoAluno(ResumoDoAluno(anosLetivos = 1, disciplinas = 1, avaliacoes = 1, faltas = 1)),
        )
    }

    @Test fun `o que esta zerado fica de fora`() {
        assertEquals("1 ano letivo e 5 disciplinas", oQueEDoAluno(ResumoDoAluno(anosLetivos = 1, disciplinas = 5)))
        assertEquals("1 ano letivo", oQueEDoAluno(ResumoDoAluno(anosLetivos = 1)))
        assertNull(oQueEDoAluno(ResumoDoAluno()))
    }

    @Test fun `aviso da exclusao diz o que sai e que nao da para desfazer`() {
        assertEquals(
            "ATENÇÃO! Serão excluídos: 1 ano letivo e 5 disciplinas. Não dá para desfazer.",
            avisoDaExclusao(ResumoDoAluno(anosLetivos = 1, disciplinas = 5), unico = false),
        )
    }

    @Test fun `aviso da exclusao sem os numeros ainda e do unico aluno`() {
        assertEquals(
            "ATENÇÃO! Serão excluídos todos os dados do aluno. Não dá para desfazer. " +
                "Como é o único aluno, o app volta ao começo, para um cadastro novo.",
            avisoDaExclusao(resumo = null, unico = true),
        )
    }
}

// Preenche o que os textos não usam, pra cada teste mostrar só o que importa.
private fun ano(ano: Int, serie: String? = null, periodos: Int = 0) = AnoLetivoDomain(
    id = ano.toLong(),
    ano = ano,
    serie = serie,
    periodo = List(periodos) { i ->
        PeriodoDomain(
            anoLetivoId = ano.toLong(),
            periodo = i + 1,
            dataInicio = LocalDate.of(ano, i + 1, 1),
            dataFim = LocalDate.of(ano, i + 1, 28),
        )
    },
    tipoPeriodo = TipoPeriodo.UNIDADE,
    qtdPeriodos = periodos,
    alunoId = 1,
)
