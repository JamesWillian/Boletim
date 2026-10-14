package app.jammes.boletim.presentation.ui.disciplina

import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.EdicaoDisciplina
import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.presentation.ui.anoletivo.DatasPeriodo
import app.jammes.boletim.presentation.ui.anoletivo.FormularioAnoLetivo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FormularioDisciplinaTest {

    // Da disciplina gravada ---------------------------------------------------------------------

    @Test fun `formulario comeca com a disciplina como esta gravada`() {
        // executa
        val form = FormularioDisciplina.de(PORTUGUES)

        // confere
        assertEquals(
            FormularioDisciplina(
                nome = "Português",
                abreviacao = "POR",
                professor = "Prof. Maria",
                cor = 3,
                icone = IconeMateria.LIVRO,
                totalAulas = 80,
                periodo = DatasPeriodo(dia(2, 1), dia(6, 30)),
            ),
            form,
        )
    }

    @Test fun `sem professor o campo fica vazio, e sem periodo vale o ano inteiro`() {
        val form = FormularioDisciplina.de(PORTUGUES.copy(professor = null, periodoInicio = null, periodoFim = null))

        assertEquals("", form.professor)
        assertNull(form.periodo)
    }

    @Test fun `sem abreviacao o campo fica vazio`() {
        assertEquals("", FormularioDisciplina.de(PORTUGUES.copy(abreviacao = null)).abreviacao)
    }

    @Test fun `uma data sozinha nao forma um periodo`() {
        val form = FormularioDisciplina.de(PORTUGUES.copy(periodoFim = null))

        assertNull(form.periodo)
    }

    // Alterações -------------------------------------------------------------------------------

    @Test fun `sem mexer em nada nao ha alteracao`() {
        val gravado = FormularioDisciplina.de(PORTUGUES)

        assertEquals(0, gravado.alteracoes(gravado, REGRAS_DO_ANO).total)
    }

    @Test fun `espaco sobrando nas pontas nao conta como alteracao`() {
        val gravado = FormularioDisciplina.de(PORTUGUES)

        val form = gravado.copy(nome = " Português ", abreviacao = "POR ", professor = "Prof. Maria  ")

        assertEquals(0, form.alteracoes(gravado, REGRAS_DO_ANO).total) // então a barra de salvar não aparece
    }

    @Test fun `cada campo mudado conta um, na secao dele`() {
        val gravado = FormularioDisciplina.de(PORTUGUES)

        val form = gravado.copy(
            nome = "Língua Portuguesa",
            abreviacao = "LPO",
            professor = "Prof. Ana",
            cor = 0,
            icone = IconeMateria.REDACAO,
            totalAulas = 160,
            periodo = null,
        )

        assertEquals(AlteracoesDaDisciplina(disciplina = 3, cor = 1, icone = 1, aulas = 2), form.alteracoes(gravado, REGRAS_DO_ANO))
        assertEquals(7, form.alteracoes(gravado, REGRAS_DO_ANO).total)
    }

    @Test fun `redefinir para o valor gravado desfaz a alteracao`() {
        val gravado = FormularioDisciplina.de(PORTUGUES)

        val idaEVolta = gravado.copy(cor = 7).copy(cor = gravado.cor)

        assertEquals(0, idaEVolta.alteracoes(gravado, REGRAS_DO_ANO).cor)
    }

    // O que vai para o banco ------------------------------------------------------------------

    @Test fun `grava o que foi editado e deixa o resto da disciplina como estava`() {
        // prepara: a disciplina finalizada, na posição 4 das abas
        val original = PORTUGUES.copy(ativa = false, ordem = 4)
        val form = FormularioDisciplina.de(original).copy(
            nome = "  Língua Portuguesa ",
            abreviacao = "LP",
            professor = " Prof. Ana ",
            cor = 0,
            icone = IconeMateria.REDACAO,
            totalAulas = 160,
            periodo = DatasPeriodo(dia(8, 1), dia(11, 30)),
        )

        // executa
        val gravada = form.paraGravar(original)

        // confere
        assertEquals(
            original.copy(
                nome = "Língua Portuguesa",
                abreviacao = "LP",
                professor = "Prof. Ana",
                cor = 0,
                icone = IconeMateria.REDACAO,
                totalAulas = 160,
                periodoInicio = dia(8, 1),
                periodoFim = dia(11, 30),
            ),
            gravada,
        )
    }

    @Test fun `professor e abreviacao em branco e periodo do ano inteiro gravam null`() {
        val form = FormularioDisciplina.de(PORTUGUES).copy(abreviacao = "", professor = "   ", periodo = null, totalAulas = null)

        val gravada = form.paraGravar(PORTUGUES)!!

        assertNull(gravada.abreviacao) // então as abas tiram a sigla do nome
        assertNull(gravada.professor)
        assertNull(gravada.periodoInicio)
        assertNull(gravada.periodoFim)
        assertNull(gravada.totalAulas)
    }

    @Test fun `sem nome nao grava`() {
        val form = FormularioDisciplina.de(PORTUGUES).copy(nome = "  ")

        assertEquals("A disciplina precisa de um nome", form.problema())
        assertNull(form.paraGravar(PORTUGUES))
    }

    // Abreviação digitada ----------------------------------------------------------------------

    @Test fun `abreviacao digitada fica em maiusculas, como nas abas`() {
        assertEquals("POR", lerAbreviacao("por"))
        assertEquals("CIÊ", lerAbreviacao("Ciê"))
    }

    @Test fun `o que passa de 3 caracteres nao entra`() {
        assertEquals("POR", lerAbreviacao("PORT"))
    }

    @Test fun `espaco nao entra na abreviacao`() {
        assertEquals("EF", lerAbreviacao(" E F "))
        assertEquals("", lerAbreviacao("   "))
    }

    // Período escolhido no calendário ---------------------------------------------------------

    @Test fun `escolher o ano letivo inteiro volta a ser o ano inteiro`() {
        assertNull(periodoEscolhido(ANO.inicio, ANO.fim, doAno = ANO))
    }

    @Test fun `um pedaco do ano fica com as datas dele`() {
        assertEquals(DatasPeriodo(dia(2, 1), dia(6, 30)), periodoEscolhido(dia(2, 1), dia(6, 30), doAno = ANO))
    }

    @Test fun `sem as datas do ano, o que for escolhido fica`() {
        assertEquals(DatasPeriodo(ANO.inicio, ANO.fim), periodoEscolhido(ANO.inicio, ANO.fim, doAno = null))
    }

    // Total de aulas digitado -----------------------------------------------------------------

    @Test fun `vazio e nao informado`() {
        assertEquals(TotalDigitado.Vazio, lerTotalDeAulas(""))
        assertEquals(TotalDigitado.Vazio, lerTotalDeAulas("  "))
    }

    @Test fun `numero a partir de 1 serve, mesmo com zero na frente`() {
        assertEquals(TotalDigitado.Valido(80), lerTotalDeAulas("80"))
        assertEquals(TotalDigitado.Valido(80), lerTotalDeAulas("080"))
        assertEquals(TotalDigitado.Valido(1), lerTotalDeAulas("1"))
    }

    @Test fun `zero nao serve`() {
        assertEquals(TotalDigitado.Invalido, lerTotalDeAulas("0"))
        assertEquals(TotalDigitado.Invalido, lerTotalDeAulas("000"))
    }

    // Regras próprias -------------------------------------------------------------------------

    @Test fun `sem regra propria a disciplina segue as do ano`() {
        val form = FormularioDisciplina.de(PORTUGUES, regraPropria = null)

        assertNull(form.regras)
        assertEquals(REGRA_DO_ANO, form.regraEfetiva(edicao()))
    }

    @Test fun `com regra propria o formulario comeca com ela`() {
        val form = FormularioDisciplina.de(PORTUGUES, regraPropria = REGRA_PROPRIA)

        assertEquals(FormularioAnoLetivo.Regras(6.0, 75.0, TipoMedia.PONDERADA, TipoArredondamento.NENHUM), form.regras)
    }

    @Test fun `cada parte da regra que muda conta uma alteracao`() {
        // prepara: segue as do ano, e passa a ter mínima e cálculo próprios
        val gravado = FormularioDisciplina.de(PORTUGUES)
        val form = gravado.copy(regras = REGRAS_DO_ANO.copy(mediaMinima = 6.0, tipoMedia = TipoMedia.PONDERADA))

        // executa
        val alteracoes = form.alteracoes(gravado, REGRAS_DO_ANO)

        // confere
        assertEquals(2, alteracoes.regras)
        assertEquals(2, alteracoes.total)
    }

    @Test fun `voltar a seguir as do ano conta o que muda na regra que vale`() {
        val gravado = FormularioDisciplina.de(PORTUGUES, regraPropria = REGRA_PROPRIA)

        val form = gravado.copy(regras = null)

        // mínima 6 → 8, frequência 75 → 70, ponderada → simples, nenhum → para cima
        assertEquals(4, form.alteracoes(gravado, REGRAS_DO_ANO).regras)
    }

    @Test fun `tirar uma propria igual as do ano ainda e uma alteracao`() {
        // Quando a do ano foi mudada depois e ficou igual à própria
        val gravado = FormularioDisciplina.de(PORTUGUES).copy(regras = REGRAS_DO_ANO)

        val form = gravado.copy(regras = null)

        assertEquals(1, form.alteracoes(gravado, REGRAS_DO_ANO).regras)
    }

    @Test fun `regra nova nasce da do ano so para a disciplina, sem o id dela`() {
        val form = FormularioDisciplina.de(PORTUGUES).copy(
            regras = FormularioAnoLetivo.Regras(6.5, 80.0, TipoMedia.SOMA, TipoArredondamento.INTEIRO),
        )

        val regra = form.regraParaGravar(edicao())

        assertEquals(
            REGRA_DO_ANO.copy(
                id = 0L, // id 0: um insert, e a do ano fica como está
                disciplinaId = 1L,
                mediaMinima = 6.5,
                frequenciaMinima = 80.0,
                tipoMedia = TipoMedia.SOMA,
                arredondamento = TipoArredondamento.INTEIRO,
            ),
            regra,
        )
    }

    @Test fun `regra propria que ja existe e regravada com o id dela`() {
        val form = FormularioDisciplina.de(PORTUGUES, REGRA_PROPRIA).copy(
            regras = FormularioAnoLetivo.Regras(5.0, 75.0, TipoMedia.PONDERADA, TipoArredondamento.NENHUM),
        )

        val regra = form.regraParaGravar(edicao(propria = REGRA_PROPRIA))

        assertEquals(REGRA_PROPRIA.copy(mediaMinima = 5.0), regra)
    }

    @Test fun `seguindo as do ano nao ha regra propria para gravar`() {
        val form = FormularioDisciplina.de(PORTUGUES, REGRA_PROPRIA).copy(regras = null)

        assertNull(form.regraParaGravar(edicao(propria = REGRA_PROPRIA)))
        assertEquals(REGRA_DO_ANO, form.regraEfetiva(edicao(propria = REGRA_PROPRIA)))
    }

    @Test fun `regras escolhidas iguais as do ano voltam a seguir o ano`() {
        assertNull(regrasEscolhidas(REGRAS_DO_ANO, doAno = REGRAS_DO_ANO))

        val proprias = REGRAS_DO_ANO.copy(frequenciaMinima = 75.0)
        assertEquals(proprias, regrasEscolhidas(proprias, doAno = REGRAS_DO_ANO))
    }
}

private fun dia(mes: Int, dia: Int): LocalDate = LocalDate.of(2026, mes, dia)

// As regras do emulador: mínima 8,0, frequência 70%, média simples, arredonda para cima
private val REGRA_DO_ANO = RegraAvaliacaoDomain(
    id = 1,
    anoLetivoId = 1,
    disciplinaId = null,
    mediaMinima = 8.0,
    mediaRecuperacao = 5.0,
    frequenciaMinima = 70.0,
    tipoMedia = TipoMedia.SIMPLES,
    arredondamento = TipoArredondamento.CIMA,
)
private val REGRAS_DO_ANO = regrasDe(REGRA_DO_ANO)

private val REGRA_PROPRIA = RegraAvaliacaoDomain(
    id = 7,
    anoLetivoId = 1,
    disciplinaId = 1,
    mediaMinima = 6.0,
    mediaRecuperacao = 5.0,
    frequenciaMinima = 75.0,
    tipoMedia = TipoMedia.PONDERADA,
    arredondamento = TipoArredondamento.NENHUM,
)

private fun edicao(propria: RegraAvaliacaoDomain? = null) = EdicaoDisciplina(
    disciplina = PORTUGUES,
    materia = null,
    anoLetivo = null,
    regraDoAno = REGRA_DO_ANO,
    regraPropria = propria,
)

// O ano letivo do emulador: 4 bimestres, de 1 fev a 30 nov
private val ANO = DatasPeriodo(dia(2, 1), dia(11, 30))

private val PORTUGUES = DisciplinaDomain(
    id = 1,
    nome = "Português",
    abreviacao = "POR",
    cor = 3,
    icone = IconeMateria.LIVRO,
    totalAulas = 80,
    professor = "Prof. Maria",
    periodoInicio = dia(2, 1),
    periodoFim = dia(6, 30),
    materiaId = 1,
    anoLetivoId = 1,
)
