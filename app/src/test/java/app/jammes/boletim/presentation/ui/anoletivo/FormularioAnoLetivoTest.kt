package app.jammes.boletim.presentation.ui.anoletivo

import app.jammes.boletim.domain.model.AjustesAnoLetivo
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.Lancamentos
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.model.TipoPeriodo
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
        val periodos = FormularioAnoLetivo.de(ajustes()).periodos.maisUm()

        assertEquals(5, periodos.quantidade)
        assertEquals(periodoSeguinte(periodos.visiveis[3]), periodos.visiveis[4])
    }

    @Test fun `tirar e devolver o ultimo periodo traz as mesmas datas de volta`() {
        val gravado = FormularioAnoLetivo.de(ajustes())

        val idaEVolta = gravado.copy(periodos = gravado.periodos.menosUm().maisUm())

        assertEquals(gravado.periodos.visiveis[3], idaEVolta.periodos.visiveis[3])
        assertEquals(0, idaEVolta.alteracoes(gravado).total) // então a barra de salvar não aparece
    }

    @Test fun `a quantidade fica entre 1 e o maximo`() {
        var periodos = FormularioAnoLetivo.de(ajustes(periodos = PERIODOS.take(1))).periodos
        assertEquals(1, periodos.menosUm().quantidade)

        repeat(10) { periodos = periodos.maisUm() }
        assertEquals(FormularioAnoLetivo.MAX_PERIODOS, periodos.quantidade)
    }

    @Test fun `com menos periodos gravados que a quantidade, a tela completa os que faltam`() {
        val ajustes = ajustes(periodos = PERIODOS.take(2), qtdPeriodos = 3)

        val periodos = FormularioAnoLetivo.de(ajustes).periodos

        assertEquals(3, periodos.quantidade)
        assertEquals(periodoSeguinte(periodos.visiveis[1]), periodos.visiveis[2])
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

        assertEquals(Lancamentos(avaliacoes = 2), FormularioAnoLetivo.de(ajustes).periodos.bloqueioParaDiminuir(ajustes))
    }

    @Test fun `faltas no ultimo periodo tambem impedem de diminuir`() {
        val ajustes = ajustes(lancamentosPorPeriodo = mapOf(4L to Lancamentos(faltas = 1)))

        assertEquals(Lancamentos(faltas = 1), FormularioAnoLetivo.de(ajustes).periodos.bloqueioParaDiminuir(ajustes))
    }

    @Test fun `da para diminuir quando so os periodos de antes tem lancamentos`() {
        val ajustes = ajustes(
            lancamentosPorPeriodo = mapOf(1L to Lancamentos(avaliacoes = 12, faltas = 6), 4L to Lancamentos())
        )

        assertNull(FormularioAnoLetivo.de(ajustes).periodos.bloqueioParaDiminuir(ajustes))
    }

    @Test fun `periodo criado nesta edicao sempre pode sair`() {
        val ajustes = ajustes(lancamentosPorPeriodo = mapOf(4L to Lancamentos(avaliacoes = 3)))

        val periodos = FormularioAnoLetivo.de(ajustes).periodos.maisUm() // o 5º ainda não está no banco

        assertNull(periodos.bloqueioParaDiminuir(ajustes))
    }

    // Média e frequência -----------------------------------------------------------------------

    @Test fun `media minima anda de meio em meio ponto`() {
        val regras = FormularioAnoLetivo.de(ajustes()).regras // 7,0

        assertEquals(7.5, regras.comMediaMinima(1).mediaMinima, 0.0)
        assertEquals(6.5, regras.comMediaMinima(-1).mediaMinima, 0.0)
    }

    @Test fun `frequencia minima anda de 5 em 5`() {
        val regras = FormularioAnoLetivo.de(ajustes()).regras // 75%

        assertEquals(80.0, regras.comFrequenciaMinima(1).frequenciaMinima, 0.0)
        assertEquals(70.0, regras.comFrequenciaMinima(-1).frequenciaMinima, 0.0)
    }

    @Test fun `valor fora do passo vai para o vizinho da grade`() {
        // Uma média gravada antes, digitada à mão
        assertEquals(7.0, passo(6.75, PASSO_DA_MEDIA, 1, FAIXA_DA_MEDIA), 0.0)
        assertEquals(6.5, passo(6.75, PASSO_DA_MEDIA, -1, FAIXA_DA_MEDIA), 0.0)
        assertEquals(70.0, passo(72.3, PASSO_DA_FREQUENCIA, -1, FAIXA_DA_FREQUENCIA), 0.0)
    }

    @Test fun `ruido do Double nao pula um passo`() {
        assertEquals(7.5, passo(6.999999999999999, PASSO_DA_MEDIA, 1, FAIXA_DA_MEDIA), 0.0)
        assertEquals(6.5, passo(7.000000000000001, PASSO_DA_MEDIA, -1, FAIXA_DA_MEDIA), 0.0)
    }

    @Test fun `media e frequencia param nos limites`() {
        assertEquals(10.0, passo(10.0, PASSO_DA_MEDIA, 1, FAIXA_DA_MEDIA), 0.0)
        assertEquals(0.5, passo(0.5, PASSO_DA_MEDIA, -1, FAIXA_DA_MEDIA), 0.0)
        assertEquals(100.0, passo(100.0, PASSO_DA_FREQUENCIA, 1, FAIXA_DA_FREQUENCIA), 0.0)
        assertEquals(0.0, passo(0.0, PASSO_DA_FREQUENCIA, -1, FAIXA_DA_FREQUENCIA), 0.0)
    }

    // Alterações -------------------------------------------------------------------------------

    @Test fun `formulario recem aberto nao tem alteracoes`() {
        val gravado = FormularioAnoLetivo.de(ajustes())

        assertEquals(Alteracoes(ano = 0, periodos = 0, regras = 0, materias = 0), gravado.alteracoes(gravado))
    }

    @Test fun `cada campo e cada materia que entra ou sai contam uma alteracao`() {
        val gravado = FormularioAnoLetivo.de(ajustes(materiaIds = setOf(1, 2, 3, 4)))

        val form = gravado.copy(
            regras = gravado.regras.copy(arredondamento = TipoArredondamento.MEIO_PONTO),
            materias = setOf(1, 2, 5), // saem a 3 e a 4, entra a 5
        )

        assertEquals(Alteracoes(ano = 0, periodos = 0, regras = 1, materias = 3), form.alteracoes(gravado))
        assertEquals(4, form.alteracoes(gravado).total)
    }

    @Test fun `espaco sobrando no ano ou na serie nao conta`() {
        val gravado = FormularioAnoLetivo.de(ajustes())

        val form = gravado.copy(identificacao = FormularioAnoLetivo.Identificacao(ano = "2024 ", serie = " 9º Ano"))

        assertEquals(0, form.alteracoes(gravado).total)
    }

    @Test fun `quantidade e datas de cada periodo contam separado`() {
        val gravado = FormularioAnoLetivo.de(ajustes())

        val periodos = gravado.periodos
            .maisUm() // 1: a quantidade
            .comDatas(0, DatasPeriodo(dia(2, 5), dia(4, 30))) // 2: as datas da 1ª
            .copy(tipo = TipoPeriodo.BIMESTRE) // 3: o tipo

        assertEquals(3, gravado.copy(periodos = periodos).alteracoes(gravado).periodos)
    }

    // O que impede de salvar ------------------------------------------------------------------

    @Test fun `sem problema quando tudo esta certo`() {
        assertNull(FormularioAnoLetivo.de(ajustes()).problema())
    }

    @Test fun `ano invalido diz o que corrigir`() {
        val gravado = FormularioAnoLetivo.de(ajustes())

        val form = gravado.copy(identificacao = gravado.identificacao.copy(ano = "24"))

        assertEquals("O ano precisa ter 4 dígitos", form.problema())
    }

    @Test fun `datas com problema dizem qual periodo conferir`() {
        val gravado = FormularioAnoLetivo.de(ajustes())

        val invade = gravado.copy(periodos = gravado.periodos.comDatas(1, DatasPeriodo(dia(4, 1), dia(7, 15))))
        val bimestres = invade.copy(periodos = invade.periodos.copy(tipo = TipoPeriodo.BIMESTRE))

        assertEquals("Confira as datas da 2ª Unidade", invade.problema())
        assertEquals("Confira as datas do 2º Bimestre", bimestres.problema())
    }

    // O que vai para o banco ------------------------------------------------------------------

    @Test fun `periodo gravado mantem o id e periodo novo vai com id zero`() {
        val ajustes = ajustes()
        val form = FormularioAnoLetivo.de(ajustes)

        val gravacao = form.copy(periodos = form.periodos.maisUm()).paraGravar(ajustes)!!

        assertEquals(listOf(1L, 2L, 3L, 4L, 0L), gravacao.anoLetivo.periodo.map { it.id })
        assertEquals(listOf(1, 2, 3, 4, 5), gravacao.anoLetivo.periodo.map { it.periodo })
        assertEquals(5, gravacao.anoLetivo.qtdPeriodos)
    }

    @Test fun `periodo que saiu nao vai para o banco`() {
        val ajustes = ajustes()
        val form = FormularioAnoLetivo.de(ajustes)

        val gravacao = form.copy(periodos = form.periodos.menosUm()).paraGravar(ajustes)!!

        assertEquals(listOf(1L, 2L, 3L), gravacao.anoLetivo.periodo.map { it.id })
        assertEquals(3, gravacao.anoLetivo.qtdPeriodos)
    }

    @Test fun `serie em branco vai como null`() {
        val ajustes = ajustes()
        val form = FormularioAnoLetivo.de(ajustes)

        val gravacao = form.copy(identificacao = form.identificacao.copy(serie = "  ")).paraGravar(ajustes)!!

        assertNull(gravacao.anoLetivo.serie)
    }

    @Test fun `regras vao para o banco como estao na tela`() {
        val ajustes = ajustes()
        val form = FormularioAnoLetivo.de(ajustes)
        val regras = form.regras
            .comMediaMinima(-1)
            .comFrequenciaMinima(1)
            .copy(tipoMedia = TipoMedia.SOMA, arredondamento = TipoArredondamento.INTEIRO)

        val regra = form.copy(regras = regras).paraGravar(ajustes)!!.regra

        assertEquals(6.5, regra.mediaMinima, 0.0)
        assertEquals(80.0, regra.frequenciaMinima, 0.0)
        assertEquals(TipoMedia.SOMA, regra.tipoMedia)
        assertEquals(TipoArredondamento.INTEIRO, regra.arredondamento)
    }

    @Test fun `com problema nada vai para o banco`() {
        val ajustes = ajustes()
        val form = FormularioAnoLetivo.de(ajustes)

        assertNull(form.copy(identificacao = form.identificacao.copy(ano = "24")).paraGravar(ajustes))
        // invade o 1º
        assertNull(form.copy(periodos = form.periodos.comDatas(1, DatasPeriodo(dia(4, 1), dia(7, 15)))).paraGravar(ajustes))
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
    materiaIds: Set<Long> = emptySet(), // as matérias que já estão no ano
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
    disciplinas = materiaIds.map { id ->
        DisciplinaDomain(id = id * 10, nome = "Matéria $id", materiaId = id, anoLetivoId = 1)
    },
    lancamentosPorPeriodo = lancamentosPorPeriodo,
    lancamentosPorDisciplina = emptyMap(),
)
