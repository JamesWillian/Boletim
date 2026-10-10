package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.Contexto
import app.jammes.boletim.domain.model.TipoPeriodo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Instant

class PerfilAlunoTest {

    // Montar o perfil ------------------------------------------------------------------------

    @Test fun `perfil tem o aluno e o ano do contexto`() {
        // prepara
        val anos = listOf(ano(id = 10, alunoId = JAMES.id, ano = 2024), ano(id = 11, alunoId = JAMES.id, ano = 2025))

        // executa
        val perfil = montarPerfil(Contexto(JAMES.id, anoLetivoId = 10, periodoId = null), listOf(JAMES, MARIA), anos)!!

        // confere
        assertEquals(JAMES, perfil.aluno)
        assertEquals(10L, perfil.anoLetivo?.id)
    }

    @Test fun `os anos do aluno vem do mais recente ao mais antigo e sem os de outro aluno`() {
        val anos = listOf(
            ano(id = 10, alunoId = JAMES.id, ano = 2024),
            ano(id = 20, alunoId = MARIA.id, ano = 2026),
            ano(id = 11, alunoId = JAMES.id, ano = 2025),
        )

        val perfil = montarPerfil(Contexto(JAMES.id, 10, null), listOf(JAMES, MARIA), anos)!!

        assertEquals(listOf(2025, 2024), perfil.anosLetivos.map { it.ano })
    }

    @Test fun `outro aluno abre no ano mais recente dele`() {
        val anos = listOf(
            ano(id = 20, alunoId = MARIA.id, ano = 2023),
            ano(id = 21, alunoId = MARIA.id, ano = 2025),
        )

        val perfil = montarPerfil(Contexto(JAMES.id, 10, null), listOf(JAMES, MARIA), anos)!!

        assertEquals(21L, perfil.alunos.single { it.aluno == MARIA }.anoLetivo?.id)
    }

    @Test fun `o aluno aberto fica no ano aberto, mesmo com um mais recente`() {
        val anos = listOf(ano(id = 10, alunoId = JAMES.id, ano = 2024), ano(id = 11, alunoId = JAMES.id, ano = 2026))

        val perfil = montarPerfil(Contexto(JAMES.id, anoLetivoId = 10, periodoId = null), listOf(JAMES), anos)!!

        assertEquals(10L, perfil.alunos.single().anoLetivo?.id)
    }

    @Test fun `aluno sem ano letivo aparece na lista sem ano para abrir`() {
        val perfil = montarPerfil(
            Contexto(JAMES.id, 10, null),
            listOf(JAMES, MARIA),
            listOf(ano(id = 10, alunoId = JAMES.id, ano = 2024)),
        )!!

        assertEquals(listOf(JAMES, MARIA), perfil.alunos.map { it.aluno })
        assertNull(perfil.alunos.single { it.aluno == MARIA }.anoLetivo)
    }

    @Test fun `alunos vem na ordem do cadastro`() {
        val perfil = montarPerfil(Contexto(MARIA.id, 20, null), listOf(MARIA, JAMES), emptyList())!!

        assertEquals(listOf(JAMES, MARIA), perfil.alunos.map { it.aluno })
    }

    @Test fun `aluno do contexto que nao existe mais nao tem perfil`() {
        assertNull(montarPerfil(Contexto(alunoId = 99, anoLetivoId = 10, periodoId = null), listOf(JAMES), emptyList()))
    }

    @Test fun `ano do contexto que nao existe mais deixa o perfil sem ano aberto`() {
        val perfil = montarPerfil(
            Contexto(JAMES.id, anoLetivoId = 99, periodoId = null),
            listOf(JAMES),
            listOf(ano(id = 10, alunoId = JAMES.id, ano = 2024)),
        )!!

        assertNull(perfil.anoLetivo)
        assertEquals(1, perfil.anosLetivos.size)
    }

    // Ano novo -------------------------------------------------------------------------------

    @Test fun `ano novo comeca com 4 unidades de fevereiro a dezembro`() {
        val novo = anoLetivoNovo(alunoId = JAMES.id, ano = 2026, serie = "1º Ano EM")

        assertEquals(TipoPeriodo.UNIDADE, novo.tipoPeriodo)
        assertEquals(4, novo.qtdPeriodos)
        assertEquals(listOf(1, 2, 3, 4), novo.periodo.map { it.periodo })
        assertEquals(LocalDate.of(2026, 2, 1), novo.periodo.first().dataInicio)
        assertEquals(LocalDate.of(2026, 12, 15), novo.periodo.last().dataFim)
        assertEquals(JAMES.id, novo.alunoId)
        assertEquals(0L, novo.id) // ainda não gravado
    }

    @Test fun `periodos do ano novo nao se sobrepoem e cada um termina depois de comecar`() {
        val periodos = periodosPadrao(2026)

        assertTrue(periodos.all { it.dataFim > it.dataInicio })
        assertTrue(periodos.zipWithNext().all { (antes, depois) -> depois.dataInicio > antes.dataFim })
    }

    @Test fun `serie do ano novo vai sem os espacos, e em branco fica sem serie`() {
        assertEquals("1º Ano EM", anoLetivoNovo(JAMES.id, 2026, serie = "  1º Ano EM ").serie)
        assertNull(anoLetivoNovo(JAMES.id, 2026, serie = "   ").serie)
        assertNull(anoLetivoNovo(JAMES.id, 2026, serie = null).serie)
    }
}

private val JAMES = aluno(id = 1, nome = "James Willian")
private val MARIA = aluno(id = 2, nome = "Maria Clara")

private fun aluno(id: Long, nome: String) = AlunoDomain(id = id, nome = nome, criadoEm = Instant.fromEpochSeconds(0))

// Preenche o que o perfil não usa, pra cada teste mostrar só o que importa.
private fun ano(id: Long, alunoId: Long, ano: Int) = AnoLetivoDomain(
    id = id,
    ano = ano,
    serie = null,
    periodo = emptyList(),
    qtdPeriodos = 4,
    alunoId = alunoId,
)
