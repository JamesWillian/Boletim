package app.jammes.boletim.presentation.ui.anoletivo

import app.jammes.boletim.domain.model.AjustesAnoLetivo
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.domain.model.Lancamentos
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.model.TipoPeriodo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Os textos e a lista de matérias que a tela de ajustes monta. */
class AnoLetivoScreenTest {

    // Datas dos períodos -----------------------------------------------------------------------

    @Test fun `resumo mostra o intervalo e quantas semanas ele tem`() {
        // prepara
        val datas = DatasPeriodo(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 4, 30)) // 90 dias

        // executa
        val resumo = resumoDasDatas(datas, ano = 2024)

        // confere
        assertEquals("1 fev – 30 abr · 13 semanas", resumo)
    }

    @Test fun `data fora do ano letivo mostra o ano`() {
        val datas = DatasPeriodo(LocalDate.of(2024, 12, 15), LocalDate.of(2025, 1, 31))

        assertEquals("15 dez – 31 jan 2025 · 7 semanas", resumoDasDatas(datas, ano = 2024))
    }

    @Test fun `periodo curto aparece em dias`() {
        val dia = LocalDate.of(2024, 3, 4)

        assertEquals("1 dia", duracao(DatasPeriodo(dia, dia)))
        assertEquals("10 dias", duracao(DatasPeriodo(dia, dia.plusDays(9))))
        assertEquals("2 semanas", duracao(DatasPeriodo(dia, dia.plusDays(13))))
    }

    @Test fun `fim antes do inicio fica sem duracao`() {
        val datas = DatasPeriodo(LocalDate.of(2024, 3, 10), LocalDate.of(2024, 3, 1))

        assertNull(duracao(datas))
        assertEquals("10 mar – 1 mar", resumoDasDatas(datas, ano = 2024))
    }

    // Avisos -----------------------------------------------------------------------------------

    @Test fun `bloqueio diz qual periodo nao sai e o que tem nele`() {
        val lancamentos = Lancamentos(avaliacoes = 11, faltas = 3)

        assertEquals(
            "Não dá para tirar a 4ª Unidade: ela tem 11 avaliações e 3 faltas.",
            motivoDoBloqueio(4, TipoPeriodo.UNIDADE, lancamentos),
        )
        assertEquals(
            "Não dá para tirar o 4º Bimestre: ele tem 11 avaliações e 3 faltas.",
            motivoDoBloqueio(4, TipoPeriodo.BIMESTRE, lancamentos),
        )
    }

    @Test fun `frequencia minima vira quanto pode faltar`() {
        assertEquals("Até 25% de faltas", faltasPermitidas(75.0))
        assertEquals("Até 27,5% de faltas", faltasPermitidas(72.5))
        assertEquals("Sem limite de faltas", faltasPermitidas(0.0))
        assertEquals("Nenhuma falta permitida", faltasPermitidas(100.0))
    }

    @Test fun `ordinal segue o genero do tipo`() {
        assertEquals("1ª", ordinalDoPeriodo(1, TipoPeriodo.UNIDADE))
        assertEquals("3º", ordinalDoPeriodo(3, TipoPeriodo.TRIMESTRE))
        assertEquals("2º Semestre", nomeDoPeriodo(2, TipoPeriodo.SEMESTRE))
    }

    // Regras -----------------------------------------------------------------------------------

    @Test fun `exemplo de cada arredondamento sai da conta de verdade`() {
        assertEquals("6,96 → 6,96", exemploDoArredondamento(TipoArredondamento.NENHUM))
        assertEquals("7,3 → 7,5", exemploDoArredondamento(TipoArredondamento.MEIO_PONTO))
        assertEquals("7,5 → 8,0", exemploDoArredondamento(TipoArredondamento.INTEIRO))
        assertEquals("6,62 → 6,7", exemploDoArredondamento(TipoArredondamento.CIMA))
        assertEquals("6,68 → 6,6", exemploDoArredondamento(TipoArredondamento.BAIXO))
    }

    @Test fun `linhas das regras mostram o valor e o jeito dele`() {
        assertEquals("Para cima · 6,62 → 6,7", resumoDoArredondamento(TipoArredondamento.CIMA))
        assertEquals("Ponderada · pelos pesos", resumoDoCalculo(TipoMedia.PONDERADA))
        assertEquals("Soma · sem dividir", resumoDoCalculo(TipoMedia.SOMA))
    }

    // Resumo das matérias ------------------------------------------------------------------------

    @Test fun `resumo das materias diz quantas estao no ano e quantas estao presas`() {
        assertEquals("10 de 13 no ano", tituloDasMaterias(noAno = 10, total = 13))
        assertEquals("5 com avaliações ou faltas", detalheDasMaterias(5))
        assertNull(detalheDasMaterias(0)) // sem lançamentos, a linha fica só com o título
    }

    @Test fun `sem materias cadastradas o resumo diz isso`() {
        assertEquals("Nenhuma matéria cadastrada", tituloDasMaterias(noAno = 0, total = 0))
    }

    // Matérias ---------------------------------------------------------------------------------

    @Test fun `materia que ja esta no ano aparece como a disciplina dela`() {
        // A disciplina de Português foi renomeada e trocou de cor nas abas
        val ajustes = ajustes(
            disciplinas = listOf(
                DisciplinaDomain(id = 10, nome = "Língua Portuguesa", cor = 3, icone = IconeMateria.REDACAO, materiaId = 1, anoLetivoId = 1)
            )
        )

        val portugues = linhasDasMaterias(ajustes).first()

        assertEquals("Língua Portuguesa", portugues.nome)
        assertEquals(3, portugues.cor)
        assertEquals(IconeMateria.REDACAO, portugues.icone)
    }

    @Test fun `materia fora do ano aparece como no cadastro`() {
        val ingles = linhasDasMaterias(ajustes(disciplinas = emptyList())).last()

        assertEquals("Inglês", ingles.nome)
        assertEquals(INGLES.cor, ingles.cor)
        assertNull(ingles.lancamentos)
    }

    @Test fun `so a materia com lancamentos fica presa no ano`() {
        val ajustes = ajustes(
            disciplinas = listOf(
                DisciplinaDomain(id = 10, nome = "Português", materiaId = 1, anoLetivoId = 1),
                DisciplinaDomain(id = 20, nome = "Inglês", materiaId = 2, anoLetivoId = 1),
            ),
            lancamentosPorDisciplina = mapOf(10L to Lancamentos(avaliacoes = 13, faltas = 6), 20L to Lancamentos()),
        )

        val linhas = linhasDasMaterias(ajustes)

        assertEquals(Lancamentos(avaliacoes = 13, faltas = 6), linhas[0].lancamentos)
        assertNull(linhas[1].lancamentos) // zerada também pode sair
    }
}

private val PORTUGUES = MateriaDomain(id = 1, nome = "Português", cor = 0, icone = IconeMateria.LIVRO)
private val INGLES = MateriaDomain(id = 2, nome = "Inglês", cor = 8, icone = IconeMateria.IDIOMA)

private fun ajustes(
    disciplinas: List<DisciplinaDomain>,
    lancamentosPorDisciplina: Map<Long, Lancamentos> = emptyMap(),
) = AjustesAnoLetivo(
    anoLetivo = AnoLetivoDomain(id = 1, ano = 2024, serie = "9º Ano", periodo = emptyList(), qtdPeriodos = 1, alunoId = 1),
    regra = RegraAvaliacaoDomain(anoLetivoId = 1, disciplinaId = null, mediaMinima = 7.0, mediaRecuperacao = null, frequenciaMinima = 75.0),
    materias = listOf(PORTUGUES, INGLES),
    disciplinas = disciplinas,
    lancamentosPorPeriodo = emptyMap(),
    lancamentosPorDisciplina = lancamentosPorDisciplina,
)
