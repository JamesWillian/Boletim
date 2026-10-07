package app.jammes.boletim.presentation.ui.anoletivo

import app.jammes.boletim.domain.model.AjustesAnoLetivo
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.Lancamentos
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FormularioAnoLetivoTest {

    // Períodos ------------------------------------------------------------------------------

    @Test fun `periodo novo comeca no dia seguinte ao anterior e dura os mesmos dias`() {
        // prepara
        val anterior = DatasPeriodo(dia(3, 1), dia(3, 10))

        // executa
        val seguinte = periodoSeguinte(anterior)

        // confere
        assertEquals(DatasPeriodo(dia(3, 11), dia(3, 20)), seguinte)
    }

    @Test fun `aumentar alem do que estava gravado cria um periodo depois do ultimo`() {
        val form = FormularioAnoLetivo.de(ajustes()).maisUmPeriodo()

        assertEquals(5, form.quantidade)
        assertEquals(periodoSeguinte(form.visiveis[3]), form.visiveis[4])
    }

    @Test fun `tirar e devolver o ultimo periodo traz as mesmas datas de volta`() {
        val gravado = FormularioAnoLetivo.de(ajustes())

        val idaEVolta = gravado.menosUmPeriodo().maisUmPeriodo()

        assertEquals(gravado.visiveis[3], idaEVolta.visiveis[3])
        assertEquals(gravado, idaEVolta.semGuardados()) // então a tela não acusa alteração
    }

    @Test fun `a quantidade fica entre 1 e o maximo`() {
        var form = FormularioAnoLetivo.de(ajustes(periodos = PERIODOS.take(1)))
        assertEquals(1, form.menosUmPeriodo().quantidade)

        repeat(10) { form = form.maisUmPeriodo() }
        assertEquals(FormularioAnoLetivo.MAX_PERIODOS, form.quantidade)
    }

    @Test fun `com menos periodos gravados que a quantidade, a tela completa os que faltam`() {
        val ajustes = ajustes(periodos = PERIODOS.take(2), qtdPeriodos = 3)

        val form = FormularioAnoLetivo.de(ajustes)

        assertEquals(3, form.quantidade)
        assertEquals(periodoSeguinte(form.visiveis[1]), form.visiveis[2])
    }

    // Datas ---------------------------------------------------------------------------------

    @Test fun `fim antes do inicio nao serve`() {
        val problemas = problemasDasDatas(listOf(DatasPeriodo(dia(3, 10), dia(3, 1))))

        assertEquals(listOf(ProblemaDatas.FIM_ANTES_DO_INICIO), problemas)
    }

    @Test fun `periodo que comeca no dia em que o anterior acaba nao serve`() {
        val problemas = problemasDasDatas(
            listOf(DatasPeriodo(dia(2, 1), dia(4, 30)), DatasPeriodo(dia(4, 30), dia(7, 15)))
        )

        assertEquals(listOf(null, ProblemaDatas.COMECA_ANTES_DO_FIM_DO_ANTERIOR), problemas)
    }

    @Test fun `folga entre um periodo e outro pode, sao as ferias`() {
        val problemas = problemasDasDatas(
            listOf(DatasPeriodo(dia(5, 1), dia(7, 15)), DatasPeriodo(dia(8, 1), dia(9, 30)))
        )

        assertEquals(listOf(null, null), problemas)
    }

    // O que impede de diminuir -----------------------------------------------------------------

    @Test fun `nao da para diminuir quando o ultimo periodo tem avaliacoes`() {
        val ajustes = ajustes(lancamentosPorPeriodo = mapOf(4L to Lancamentos(avaliacoes = 2)))

        assertEquals(Lancamentos(avaliacoes = 2), FormularioAnoLetivo.de(ajustes).bloqueioParaDiminuir(ajustes))
    }

    @Test fun `faltas no ultimo periodo tambem impedem de diminuir`() {
        val ajustes = ajustes(lancamentosPorPeriodo = mapOf(4L to Lancamentos(faltas = 1)))

        assertEquals(Lancamentos(faltas = 1), FormularioAnoLetivo.de(ajustes).bloqueioParaDiminuir(ajustes))
    }

    @Test fun `da para diminuir quando so os periodos de antes tem lancamentos`() {
        val ajustes = ajustes(
            lancamentosPorPeriodo = mapOf(1L to Lancamentos(avaliacoes = 12, faltas = 6), 4L to Lancamentos())
        )

        assertNull(FormularioAnoLetivo.de(ajustes).bloqueioParaDiminuir(ajustes))
    }

    @Test fun `periodo criado nesta edicao sempre pode sair`() {
        val ajustes = ajustes(lancamentosPorPeriodo = mapOf(4L to Lancamentos(avaliacoes = 3)))

        val form = FormularioAnoLetivo.de(ajustes).maisUmPeriodo() // o 5º ainda não está no banco

        assertNull(form.bloqueioParaDiminuir(ajustes))
    }

    // O que vai para o banco ------------------------------------------------------------------

    @Test fun `periodo gravado mantem o id e periodo novo vai com id zero`() {
        val ajustes = ajustes()

        val gravacao = FormularioAnoLetivo.de(ajustes).maisUmPeriodo().paraGravar(ajustes)!!

        assertEquals(listOf(1L, 2L, 3L, 4L, 0L), gravacao.anoLetivo.periodo.map { it.id })
        assertEquals(listOf(1, 2, 3, 4, 5), gravacao.anoLetivo.periodo.map { it.periodo })
        assertEquals(5, gravacao.anoLetivo.qtdPeriodos)
    }

    @Test fun `periodo que saiu nao vai para o banco`() {
        val ajustes = ajustes()

        val gravacao = FormularioAnoLetivo.de(ajustes).menosUmPeriodo().paraGravar(ajustes)!!

        assertEquals(listOf(1L, 2L, 3L), gravacao.anoLetivo.periodo.map { it.id })
        assertEquals(3, gravacao.anoLetivo.qtdPeriodos)
    }

    @Test fun `serie em branco vai como null`() {
        val ajustes = ajustes()

        val gravacao = FormularioAnoLetivo.de(ajustes).copy(serie = "  ").paraGravar(ajustes)!!

        assertNull(gravacao.anoLetivo.serie)
    }

    @Test fun `media e frequencia aceitam virgula`() {
        val ajustes = ajustes()
        val form = FormularioAnoLetivo.de(ajustes).copy(mediaMinima = "6,5", frequenciaMinima = "75,5")

        val regra = form.paraGravar(ajustes)!!.regra

        assertEquals(6.5, regra.mediaMinima, 0.0)
        assertEquals(75.5, regra.frequenciaMinima, 0.0)
    }

    @Test fun `campo invalido nao deixa gravar`() {
        val ajustes = ajustes()
        val form = FormularioAnoLetivo.de(ajustes)

        assertNull(form.copy(ano = "24").paraGravar(ajustes))
        assertNull(form.copy(mediaMinima = "0").paraGravar(ajustes))
        assertNull(form.copy(frequenciaMinima = "120").paraGravar(ajustes))
        assertNull(form.comDatas(1, DatasPeriodo(dia(4, 1), dia(7, 15))).paraGravar(ajustes)) // invade o 1º
    }
}

private fun dia(mes: Int, dia: Int): LocalDate = LocalDate.of(2024, mes, dia)

private fun periodo(id: Long, inicio: LocalDate, fim: LocalDate) = PeriodoDomain(
    id = id,
    anoLetivoId = 1,
    periodo = id.toInt(),
    dataInicio = inicio,
    dataFim = fim,
)

// As quatro unidades do seed
private val PERIODOS = listOf(
    periodo(1, dia(2, 1), dia(4, 30)),
    periodo(2, dia(5, 1), dia(7, 15)),
    periodo(3, dia(8, 1), dia(9, 30)),
    periodo(4, dia(10, 1), dia(12, 15)),
)

// Preenche o que o formulário não usa, pra cada teste mostrar só o que importa.
private fun ajustes(
    periodos: List<PeriodoDomain> = PERIODOS,
    qtdPeriodos: Int = periodos.size,
    lancamentosPorPeriodo: Map<Long, Lancamentos> = emptyMap(),
) = AjustesAnoLetivo(
    anoLetivo = AnoLetivoDomain(
        id = 1,
        ano = 2024,
        serie = "9º Ano",
        periodo = periodos,
        qtdPeriodos = qtdPeriodos,
        alunoId = 1,
    ),
    regra = RegraAvaliacaoDomain(
        anoLetivoId = 1,
        disciplinaId = null,
        mediaMinima = 7.0,
        mediaRecuperacao = 5.0,
        frequenciaMinima = 75.0,
    ),
    materias = emptyList(),
    disciplinas = emptyList(),
    lancamentosPorPeriodo = lancamentosPorPeriodo,
    lancamentosPorDisciplina = emptyMap(),
)
