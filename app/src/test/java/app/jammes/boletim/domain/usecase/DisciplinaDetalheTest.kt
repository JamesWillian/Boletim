package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.StatusDisciplina
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoAvaliacao
import app.jammes.boletim.domain.model.TipoMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Instant

class DisciplinaDetalheTest {

    private val periodos = listOf(
        PeriodoDomain(id = 1, anoLetivoId = 1, periodo = 1, dataInicio = LocalDate.of(2026, 2, 1), dataFim = LocalDate.of(2026, 6, 30)),
        PeriodoDomain(id = 2, anoLetivoId = 1, periodo = 2, dataInicio = LocalDate.of(2026, 8, 1), dataFim = LocalDate.of(2026, 11, 30)),
    )
    private val avaliacoes = listOf(
        avaliacao(id = 1, periodoId = 1),
        avaliacao(id = 2, periodoId = 1, tipo = TipoAvaliacao.RECUPERACAO),
        avaliacao(id = 3, periodoId = 2),
        avaliacao(id = 4, periodoId = null, tipo = TipoAvaliacao.FINAL),
        avaliacao(id = 5, periodoId = 2, tipo = TipoAvaliacao.FINAL), // gravada antes, com período
    )

    @Test fun `no ano letivo a final tem o grupo dela, fora dos periodos`() {
        val (porPeriodo, finais) = agruparAvaliacoes(avaliacoes, periodos, periodoId = null)

        assertEquals(listOf(listOf(1L, 2L), listOf(3L)), porPeriodo.map { grupo -> grupo.avaliacoes.map { it.id } })
        assertEquals(listOf(4L, 5L), finais.map { it.id })
    }

    @Test fun `num periodo a final nao aparece, e a recuperacao fica no grupo dele`() {
        val (porPeriodo, finais) = agruparAvaliacoes(avaliacoes, periodos, periodoId = 1)

        assertEquals(listOf(listOf(1L, 2L)), porPeriodo.map { grupo -> grupo.avaliacoes.map { it.id } })
        assertTrue(finais.isEmpty())
    }

    @Test fun `periodo sem avaliacao fica de fora`() {
        val (porPeriodo, _) = agruparAvaliacoes(avaliacoes.filter { it.periodoId != 2L }, periodos, periodoId = null)

        assertEquals(listOf(1L), porPeriodo.map { it.periodo.id })
    }
    // Fechamento do ano --------------------------------------------------------------------

    private val regra = RegraAvaliacaoDomain(
        anoLetivoId = 1, disciplinaId = null,
        mediaMinima = 7.0, mediaRecuperacao = 5.0, frequenciaMinima = 75.0,
        tipoMedia = TipoMedia.SIMPLES, arredondamento = TipoArredondamento.NENHUM,
    )

    private fun fechar(avaliacoes: List<AvaliacaoDomain>, faltas: Int = 0, limiteFaltas: Int? = null) =
        fecharAno(avaliacoes, periodos, regra, faltas, limiteFaltas)

    @Test fun `sem nota em nenhum periodo nao ha o que finalizar`() {
        assertNull(fechar(emptyList()))
        assertNull(fechar(listOf(avaliacao(id = 1, periodoId = 1, nota = null)))) // só agendada
    }

    @Test fun `so a final, com os periodos zerados, tambem nao finaliza`() {
        assertNull(fechar(listOf(avaliacao(id = 1, periodoId = null, tipo = TipoAvaliacao.FINAL, nota = 9.0))))
    }

    @Test fun `periodo sem nota fica fora da media e entra no aviso`() {
        val fechamento = fechar(listOf(avaliacao(id = 1, periodoId = 1, nota = 8.0)))!!

        assertEquals(8.0, fechamento.mediaDoAno, 0.001) // e não 4,0, como seria contando o 2º como zero
        assertEquals(listOf(2L), fechamento.periodosSemNota.map { it.id })
        assertEquals(StatusDisciplina.APROVADO, fechamento.situacao)
    }

    @Test fun `so a recuperacao ja da nota ao periodo`() {
        val fechamento = fechar(
            listOf(
                avaliacao(id = 1, periodoId = 1, nota = null),
                avaliacao(id = 2, periodoId = 1, tipo = TipoAvaliacao.RECUPERACAO, nota = 7.5),
                avaliacao(id = 3, periodoId = 2, nota = 8.5),
            )
        )!!

        assertTrue(fechamento.periodosSemNota.isEmpty())
        assertEquals(8.0, fechamento.mediaDoAno, 0.001) // (7,5 + 8,5) / 2
    }

    @Test fun `media do ano abaixo da minima reprova, na minima aprova`() {
        val abaixo = fechar(listOf(avaliacao(id = 1, periodoId = 1, nota = 6.5), avaliacao(id = 2, periodoId = 2, nota = 7.0)))!!
        val naMinima = fechar(listOf(avaliacao(id = 1, periodoId = 1, nota = 7.0), avaliacao(id = 2, periodoId = 2, nota = 7.0)))!!

        assertTrue(abaixo.abaixoDaMinima)
        assertEquals(StatusDisciplina.REPROVADO, abaixo.situacao)
        assertEquals(StatusDisciplina.APROVADO, naMinima.situacao)
    }

    @Test fun `a final maior que a media do ano aprova`() {
        val fechamento = fechar(
            listOf(
                avaliacao(id = 1, periodoId = 1, nota = 5.0),
                avaliacao(id = 2, periodoId = 2, nota = 6.0),
                avaliacao(id = 3, periodoId = null, tipo = TipoAvaliacao.FINAL, nota = 8.0),
            )
        )!!

        assertEquals(8.0, fechamento.mediaDoAno, 0.001)
        assertEquals(StatusDisciplina.APROVADO, fechamento.situacao)
    }

    @Test fun `passar do limite de faltas reprova mesmo com a media boa`() {
        val notas = listOf(avaliacao(id = 1, periodoId = 1, nota = 9.0), avaliacao(id = 2, periodoId = 2, nota = 9.0))

        val passou = fechar(notas, faltas = 11, limiteFaltas = 10)!!
        val noLimite = fechar(notas, faltas = 10, limiteFaltas = 10)!!

        assertTrue(passou.passouDasFaltas)
        assertFalse(passou.abaixoDaMinima)
        assertEquals(StatusDisciplina.REPROVADO, passou.situacao)
        assertEquals(StatusDisciplina.APROVADO, noLimite.situacao) // 75% de frequência ainda passa
    }

    @Test fun `sem o total de aulas as faltas nao reprovam`() {
        val fechamento = fechar(listOf(avaliacao(id = 1, periodoId = 1, nota = 9.0)), faltas = 50, limiteFaltas = null)

        assertNotNull(fechamento)
        assertEquals(StatusDisciplina.APROVADO, fechamento!!.situacao)
    }
}

private fun avaliacao(
    id: Long,
    periodoId: Long?,
    tipo: TipoAvaliacao = TipoAvaliacao.NORMAL,
    nota: Double? = 7.0,
) = AvaliacaoDomain(
    id = id,
    disciplinaId = 1,
    periodoId = periodoId,
    nome = "Prova $id",
    nota = nota,
    notaMaxima = 10.0,
    peso = 1.0,
    tipo = tipo,
    data = null,
    criadoEm = Instant.fromEpochMilliseconds(0),
)
