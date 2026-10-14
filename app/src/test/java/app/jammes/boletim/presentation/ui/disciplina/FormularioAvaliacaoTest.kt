package app.jammes.boletim.presentation.ui.disciplina

import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.Impedimento
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.StatusDisciplina
import app.jammes.boletim.domain.model.TipoAvaliacao
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.presentation.ui.disciplina.FormularioAvaliacao.Acao
import app.jammes.boletim.presentation.ui.disciplina.FormularioAvaliacao.Problema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Instant

class FormularioAvaliacaoTest {

    // Agendada ou com nota -----------------------------------------------------------------------

    @Test fun `avaliacao nova sem nota fica agendada e o botao agenda`() {
        val formulario = FormularioAvaliacao.de(NOVA)

        assertTrue(formulario.agendada)
        assertEquals(Acao.AGENDAR, formulario.acao(NOVA))
    }

    @Test fun `com nota digitada o botao lanca a nota`() {
        val formulario = FormularioAvaliacao.de(NOVA).copy(nota = "9,5")

        assertFalse(formulario.agendada)
        assertEquals(Acao.LANCAR, formulario.acao(NOVA))
    }

    @Test fun `a agendada que ganha nota tambem lanca, e as outras edicoes salvam`() {
        val agendada = NOVA.copy(id = 7, nome = "Trabalho")
        val comNota = NOVA.copy(id = 8, nome = "Prova 1", nota = 7.0)

        assertEquals(Acao.LANCAR, FormularioAvaliacao.de(agendada).copy(nota = "8").acao(agendada))
        assertEquals(Acao.SALVAR, FormularioAvaliacao.de(agendada).copy(nome = "Trabalho em grupo").acao(agendada))
        assertEquals(Acao.SALVAR, FormularioAvaliacao.de(comNota).copy(nota = "7,5").acao(comNota))
    }

    @Test fun `o formulario mostra os numeros como o app`() {
        val formulario = FormularioAvaliacao.de(NOVA.copy(nota = 7.5, peso = 1.5))

        assertEquals("7,5", formulario.nota)
        assertEquals("10", formulario.notaMaxima)
        assertEquals("1,5", formulario.peso)
        assertEquals("", FormularioAvaliacao.de(NOVA).nota)
    }

    // A nota ---------------------------------------------------------------------------------

    @Test fun `a nota aceita virgula ou ponto e vai de 0 ate a maxima`() {
        val formulario = FormularioAvaliacao.de(NOVA)

        assertEquals(9.5, formulario.copy(nota = "9,5").notaValor!!, 0.0)
        assertEquals(9.5, formulario.copy(nota = "9.5").notaValor!!, 0.0)
        assertEquals(10.0, formulario.copy(nota = "10").notaValor!!, 0.0)
        assertEquals(0.0, formulario.copy(nota = "0").notaValor!!, 0.0)
        assertNull(formulario.copy(nota = "10,5").notaValor)
        assertTrue(formulario.copy(nota = "10,5").notaInvalida)
        assertTrue(formulario.copy(nota = ",").notaInvalida)
    }

    @Test fun `com a maxima invalida a nota nao e julgada por ela, e o erro fica na maxima`() {
        val formulario = FormularioAvaliacao.de(NOVA).copy(nome = "Prova", nota = "15", notaMaxima = "")

        assertEquals(15.0, formulario.notaValor!!, 0.0)
        assertEquals(Problema.NOTA_MAXIMA, formulario.problema(comPeso = false, HOJE))
    }

    @Test fun `so algarismos e uma virgula ou ponto entram na nota`() {
        assertTrue(FormularioAvaliacao.aceitaComoNota(""))
        assertTrue(FormularioAvaliacao.aceitaComoNota("8,"))
        assertTrue(FormularioAvaliacao.aceitaComoNota("100,00"))
        assertFalse(FormularioAvaliacao.aceitaComoNota("8,5,"))
        assertFalse(FormularioAvaliacao.aceitaComoNota("8.5,"))
        assertFalse(FormularioAvaliacao.aceitaComoNota("-1"))
        assertFalse(FormularioAvaliacao.aceitaComoNota("8 5"))
        assertFalse(FormularioAvaliacao.aceitaComoNota("1000000"))
    }

    @Test fun `ao sair do campo a nota fica como o app mostra os numeros`() {
        val formulario = FormularioAvaliacao.de(NOVA)

        assertEquals("9,5", formulario.copy(nota = "9.5").notaArrumada().nota)
        assertEquals("9", formulario.copy(nota = "09").notaArrumada().nota)
        assertEquals("8", formulario.copy(nota = "8,").notaArrumada().nota)
        assertEquals("11", formulario.copy(nota = "11").notaArrumada().nota) // inválida: fica como está
        assertEquals("", formulario.notaArrumada().nota)
    }

    // A data -----------------------------------------------------------------------------------

    @Test fun `depois de hoje a nota vazia nao abre`() {
        val formulario = FormularioAvaliacao.de(NOVA)

        assertTrue(formulario.copy(data = HOJE.plusDays(1)).notaBloqueada(HOJE))
        assertFalse(formulario.copy(data = HOJE).notaBloqueada(HOJE))
        assertFalse(formulario.copy(data = HOJE.minusDays(1)).notaBloqueada(HOJE))
        assertFalse(formulario.copy(data = null).notaBloqueada(HOJE))
    }

    @Test fun `nota gravada com data futura continua aberta, para ser apagada, mas nao grava`() {
        val formulario = FormularioAvaliacao.de(NOVA).copy(nome = "Prova", nota = "8", data = HOJE.plusDays(3))

        assertFalse(formulario.notaBloqueada(HOJE))
        assertEquals(Problema.NOTA_NO_FUTURO, formulario.problema(comPeso = false, HOJE))
        assertNull(formulario.paraGravar(NOVA, comPeso = false, HOJE))
        // Apagando a nota, vira uma agendada e grava
        assertNull(formulario.copy(nota = "").problema(comPeso = false, HOJE))
    }

    // Gravar -----------------------------------------------------------------------------------

    @Test fun `sem nome nao grava, e o problema vem na ordem da tela`() {
        val formulario = FormularioAvaliacao.de(NOVA).copy(nome = "  ", nota = "11", notaMaxima = "0")

        assertEquals(Problema.SEM_NOME, formulario.problema(comPeso = false, HOJE))
        assertEquals(Problema.NOTA_FORA_DA_ESCALA, formulario.copy(nome = "Prova", notaMaxima = "10").problema(comPeso = false, HOJE))
        assertNull(formulario.paraGravar(NOVA, comPeso = false, HOJE))
    }

    @Test fun `gravando, o nome perde os espacos das pontas e os numeros viram numeros`() {
        val formulario = FormularioAvaliacao(
            nome = "  Prova 1 ",
            nota = "8,5",
            notaMaxima = "10",
            peso = "2",
            tipo = TipoAvaliacao.RECUPERACAO,
            periodoId = 3,
            data = HOJE.minusDays(2),
        )

        val gravada = formulario.paraGravar(NOVA, comPeso = true, HOJE)!!

        assertEquals("Prova 1", gravada.nome)
        assertEquals(8.5, gravada.nota!!, 0.0)
        assertEquals(10.0, gravada.notaMaxima, 0.0)
        assertEquals(2.0, gravada.peso, 0.0)
        assertEquals(TipoAvaliacao.RECUPERACAO, gravada.tipo)
        assertEquals(3L, gravada.periodoId)
        assertEquals(HOJE.minusDays(2), gravada.data)
    }

    @Test fun `sem nota grava como agendada, mesmo com a data no futuro`() {
        val formulario = FormularioAvaliacao.de(NOVA).copy(nome = "Trabalho", data = HOJE.plusDays(14))

        assertNull(formulario.paraGravar(NOVA, comPeso = false, HOJE)!!.nota)
    }

    @Test fun `fora da media ponderada o peso nao aparece e fica como estava`() {
        val formulario = FormularioAvaliacao.de(NOVA.copy(peso = 3.0)).copy(nome = "Prova", peso = "")

        assertEquals(3.0, formulario.paraGravar(NOVA.copy(peso = 3.0), comPeso = false, HOJE)!!.peso, 0.0)
        assertEquals(Problema.PESO, formulario.problema(comPeso = true, HOJE))
    }

    @Test fun `sem data nao grava, e o problema vem depois do nome e da nota`() {
        val formulario = FormularioAvaliacao.de(NOVA).copy(nome = "Prova", data = null)

        assertEquals(Problema.SEM_DATA, formulario.problema(comPeso = false, HOJE))
        assertEquals(Problema.NOTA_FORA_DA_ESCALA, formulario.copy(nota = "11").problema(comPeso = false, HOJE))
        assertNull(formulario.paraGravar(NOVA, comPeso = false, HOJE))
    }

    // Data e período ---------------------------------------------------------------------------

    @Test fun `no ano letivo inteiro o periodo acompanha a data`() {
        val formulario = FormularioAvaliacao.de(NOVA)

        val emSetembro = formulario.comData(LocalDate.of(2026, 9, 10), PERIODOS)

        assertEquals(LocalDate.of(2026, 9, 10), emSetembro.data)
        assertEquals(3L, emSetembro.periodoId)
    }

    @Test fun `num periodo so, trocar a data nao mexe no periodo`() {
        val formulario = FormularioAvaliacao.de(NOVA)

        assertEquals(4L, formulario.comData(LocalDate.of(2026, 10, 20), emptyList()).periodoId)
    }

    @Test fun `trocando o periodo, a data que ficou fora vai para o dia mais perto dentro dele`() {
        val formulario = FormularioAvaliacao.de(NOVA) // 12 de outubro, no 4º

        assertEquals(LocalDate.of(2026, 9, 30), formulario.comPeriodo(PERIODOS[2]).data) // fim do 3º
        assertEquals(LocalDate.of(2026, 2, 1), formulario.copy(data = LocalDate.of(2026, 1, 20)).comPeriodo(PERIODOS[0]).data)
        assertEquals(HOJE, formulario.comPeriodo(PERIODOS[3]).data) // já estava dentro
        assertEquals(3L, formulario.comPeriodo(PERIODOS[2]).periodoId)
    }

    @Test fun `o calendario mostra os dias do periodo e, com nota, so ate hoje`() {
        val formulario = FormularioAvaliacao.de(NOVA)
        val quarto = QUARTO_BIMESTRE.dataInicio..QUARTO_BIMESTRE.dataFim

        assertEquals(DiasDoCalendario(quarto.start, quarto.endInclusive), formulario.diasDoCalendario(quarto, HOJE))
        assertEquals(DiasDoCalendario(quarto.start, HOJE), formulario.copy(nota = "8").diasDoCalendario(quarto, HOJE))
        assertEquals(DiasDoCalendario(null, HOJE), formulario.copy(nota = "8").diasDoCalendario(null, HOJE))
        assertEquals(DiasDoCalendario(null, null), formulario.diasDoCalendario(null, HOJE))
    }

    @Test fun `com nota, um periodo que ainda nao comecou nao tem dia para escolher`() {
        val dezembro = LocalDate.of(2026, 12, 1)..LocalDate.of(2026, 12, 15)
        val dias = FormularioAvaliacao.de(NOVA).copy(nota = "8").diasDoCalendario(dezembro, HOJE)

        assertTrue(dias.vazio)
        assertFalse(LocalDate.of(2026, 12, 2) in dias)
        assertEquals(HOJE, dias.maisPerto(HOJE)) // sem dia que caiba, fica onde estava
    }

    @Test fun `o dia mais perto fica dentro dos dias do calendario`() {
        val dias = DiasDoCalendario(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30))

        assertEquals(LocalDate.of(2026, 10, 1), dias.maisPerto(LocalDate.of(2026, 9, 3)))
        assertEquals(LocalDate.of(2026, 11, 30), dias.maisPerto(LocalDate.of(2026, 12, 25)))
        assertEquals(HOJE, dias.maisPerto(HOJE))
        assertTrue(HOJE in dias)
        assertFalse(LocalDate.of(2026, 12, 1) in dias)
    }

    // Mais opções ------------------------------------------------------------------------------

    @Test fun `o peso anda de meio em meio, sem sair de meio a dez`() {
        val formulario = FormularioAvaliacao.de(NOVA)

        assertEquals("1,5", formulario.comPassoNoPeso(1).peso)
        assertEquals("0,5", formulario.comPassoNoPeso(-1).peso)
        assertEquals("0,5", formulario.copy(peso = "0,5").comPassoNoPeso(-1).peso)
        assertEquals("10", formulario.copy(peso = "10").comPassoNoPeso(1).peso)
        assertEquals("2", formulario.copy(peso = "1,7").comPassoNoPeso(1).peso) // fora da grade: cai no vizinho
        assertEquals("1,5", formulario.copy(peso = "").comPassoNoPeso(1).peso) // sem peso válido: parte do 1
    }

    @Test fun `10, 5 e 100 sao as notas maximas prontas`() {
        val formulario = FormularioAvaliacao.de(NOVA)

        assertTrue(formulario.notaMaximaComum)
        assertTrue(formulario.copy(notaMaxima = "100").notaMaximaComum)
        assertTrue(formulario.copy(notaMaxima = "5,0").notaMaximaComum)
        assertFalse(formulario.copy(notaMaxima = "2").notaMaximaComum)
        assertFalse(formulario.copy(notaMaxima = "").notaMaximaComum)
    }

    @Test fun `depois de gravar com Outra, o nome e a nota esvaziam e o resto fica`() {
        val gravada = FormularioAvaliacao(
            nome = "Prova 1", nota = "8", notaMaxima = "5", peso = "2",
            tipo = TipoAvaliacao.NORMAL, periodoId = 3, data = LocalDate.of(2026, 9, 10),
        )

        assertEquals(gravada.copy(nome = "", nota = ""), gravada.paraAProxima(PERIODOS))
    }

    @Test fun `depois de uma recuperacao ou de uma final, a proxima volta a ser normal`() {
        val recuperacao = FormularioAvaliacao(
            nome = "Recuperação", nota = "8", notaMaxima = "10", peso = "1",
            tipo = TipoAvaliacao.RECUPERACAO, periodoId = 3, data = LocalDate.of(2026, 9, 10),
        )
        val final = recuperacao.copy(tipo = TipoAvaliacao.FINAL, data = LocalDate.of(2026, 12, 10))

        assertEquals(recuperacao.copy(nome = "", nota = "", tipo = TipoAvaliacao.NORMAL), recuperacao.paraAProxima(PERIODOS))
        // A final de dezembro passa do 4º bimestre: a data volta para o último dia dele
        val depoisDaFinal = final.paraAProxima(PERIODOS)
        assertEquals(TipoAvaliacao.NORMAL, depoisDaFinal.tipo)
        assertEquals(LocalDate.of(2026, 11, 30), depoisDaFinal.data)
        assertEquals(4L, depoisDaFinal.periodoId)
    }


    @Test fun `o resumo de mais opcoes diz o tipo, quanto vale e o peso quando ele conta`() {
        val formulario = FormularioAvaliacao.de(NOVA.copy(peso = 2.0))

        assertEquals("Normal · vale 10", formulario.resumoDasOpcoes(comPeso = false))
        assertEquals("Normal · vale 10 · peso 2", formulario.resumoDasOpcoes(comPeso = true))
        assertEquals(
            "Final · vale ?",
            formulario.copy(tipo = TipoAvaliacao.FINAL, notaMaxima = " ").resumoDasOpcoes(comPeso = false),
        )
    }

    @Test fun `mais opcoes avisa o que esta errado nela`() {
        val formulario = FormularioAvaliacao.de(NOVA)

        assertNull(formulario.avisoDasOpcoes(comPeso = true))
        assertEquals("Confira a nota máxima", formulario.copy(notaMaxima = "0").avisoDasOpcoes(comPeso = true))
        assertEquals("Confira o peso", formulario.copy(peso = "0").avisoDasOpcoes(comPeso = true))
        assertNull(formulario.copy(peso = "0").avisoDasOpcoes(comPeso = false))
    }

    // Os textos da nota ------------------------------------------------------------------------

    @Test fun `a frase da nota diz o que acontece com ela na conta`() {
        val formulario = FormularioAvaliacao.de(NOVA).copy(nome = "Prova")
        val doQuarto = doPeriodo(QUARTO_BIMESTRE, TipoPeriodo.BIMESTRE)

        assertEquals("do 4º Bimestre", doQuarto)
        assertEquals(
            AvisoDaNota("Fica fora da média até você lançar a nota.", erro = false),
            avisoDaNota(formulario, HOJE, soma = false, doPeriodo = doQuarto),
        )
        assertEquals(
            AvisoDaNota("Dá para lançar a nota a partir de 26 out.", erro = false),
            avisoDaNota(formulario.copy(data = DIA_26), HOJE, soma = false, doPeriodo = doQuarto),
        )
        assertEquals(
            AvisoDaNota("Entra na média do 4º Bimestre.", erro = false),
            avisoDaNota(formulario.copy(nota = "9"), HOJE, soma = false, doPeriodo = doQuarto),
        )
        assertEquals(
            AvisoDaNota("Entra na soma.", erro = false),
            avisoDaNota(formulario.copy(nota = "9"), HOJE, soma = true, doPeriodo = null),
        )
        assertEquals(
            AvisoDaNota("Use um número de 0 a 10.", erro = true),
            avisoDaNota(formulario.copy(nota = "12"), HOJE, soma = false, doPeriodo = doQuarto),
        )
        assertEquals(
            AvisoDaNota("Essa data ainda não chegou: apague a nota ou mude a data.", erro = true),
            avisoDaNota(formulario.copy(nota = "9", data = DIA_26), HOJE, soma = false, doPeriodo = doQuarto),
        )
    }

    // Recuperação e final ---------------------------------------------------------------------

    @Test fun `a final grava sem periodo, e a recuperacao no dela`() {
        val formulario = FormularioAvaliacao.de(NOVA).copy(nome = "Prova final", nota = "6", tipo = TipoAvaliacao.FINAL)

        assertNull(formulario.paraGravar(NOVA, comPeso = false, HOJE)!!.periodoId)
        assertEquals(4L, formulario.copy(tipo = TipoAvaliacao.RECUPERACAO).paraGravar(NOVA, comPeso = false, HOJE)!!.periodoId)
    }

    @Test fun `com o tipo indisponivel nao grava, e o problema vem depois da data`() {
        val formulario = FormularioAvaliacao.de(NOVA).copy(nome = "Recuperação", tipo = TipoAvaliacao.RECUPERACAO)

        assertEquals(Problema.TIPO_INDISPONIVEL, formulario.problema(comPeso = false, HOJE, tipoIndisponivel = true))
        assertEquals(Problema.SEM_DATA, formulario.copy(data = null).problema(comPeso = false, HOJE, tipoIndisponivel = true))
        assertEquals(
            Problema.TIPO_INDISPONIVEL,
            formulario.copy(notaMaxima = "0").problema(comPeso = false, HOJE, tipoIndisponivel = true), // antes da máxima
        )
        assertNull(formulario.paraGravar(NOVA, comPeso = false, HOJE, tipoIndisponivel = true))
    }

    @Test fun `mais opcoes fechada avisa por que o tipo nao cabe`() {
        val formulario = FormularioAvaliacao.de(NOVA).copy(tipo = TipoAvaliacao.RECUPERACAO, notaMaxima = "0")
        val vaga = Impedimento.VagaOcupada(NOVA.copy(id = 9, tipo = TipoAvaliacao.RECUPERACAO))

        // O tipo vem antes da nota máxima, que também está errada aqui
        assertEquals(
            "Já tem uma recuperação no período",
            formulario.avisoDasOpcoes(comPeso = false, avisoDoTipo = resumoDoImpedimento(formulario.tipo, vaga)),
        )
        assertEquals("Já tem uma final no ano", resumoDoImpedimento(TipoAvaliacao.FINAL, vaga))
        assertEquals("O período ainda não tem média", resumoDoImpedimento(TipoAvaliacao.RECUPERACAO, Impedimento.SemMedia))
        assertEquals(
            "A média do ano já alcança a mínima",
            resumoDoImpedimento(TipoAvaliacao.FINAL, Impedimento.MediaSuficiente(media = 8.9, minima = 8.0)),
        )
    }

    @Test fun `o alerta do tipo diz o motivo, com a media e a minima`() {
        val noQuarto = noPeriodo(QUARTO_BIMESTRE, TipoPeriodo.BIMESTRE)
        val suficiente = Impedimento.MediaSuficiente(media = 8.8, minima = 8.0)

        assertEquals(
            "Recuperação só com média abaixo de 8,0. No 4º Bimestre, ela está em 8,8.",
            textoDoImpedimento(TipoAvaliacao.RECUPERACAO, suficiente, noQuarto),
        )
        assertEquals(
            "Final só com média do ano abaixo de 8,0. Ela está em 8,8.",
            textoDoImpedimento(TipoAvaliacao.FINAL, suficiente, noQuarto),
        )
        assertEquals(
            "Ainda não há média no 4º Bimestre: a recuperação vem depois das notas.",
            textoDoImpedimento(TipoAvaliacao.RECUPERACAO, Impedimento.SemMedia, noQuarto),
        )
        assertEquals(
            "O ano ainda não tem média: a final vem depois das notas.",
            textoDoImpedimento(TipoAvaliacao.FINAL, Impedimento.SemMedia, noQuarto),
        )
    }

    @Test fun `o aviso da vaga ocupada diz qual avaliacao esta la`() {
        val recuperacao = NOVA.copy(id = 9, nome = "Prova de recuperação", tipo = TipoAvaliacao.RECUPERACAO)
        val final = NOVA.copy(id = 10, nome = "Prova final", tipo = TipoAvaliacao.FINAL, periodoId = null)

        assertEquals(
            "Já tem uma recuperação no 4º Bimestre: “Prova de recuperação”.",
            textoDaVagaOcupada(recuperacao, noPeriodo(QUARTO_BIMESTRE, TipoPeriodo.BIMESTRE)),
        )
        assertEquals("Já tem uma final no ano: “Prova final”.", textoDaVagaOcupada(final, noPeriodo = null))
    }

    @Test fun `recuperacao e final substituem a media, se forem maiores`() {
        val doQuarto = doPeriodo(QUARTO_BIMESTRE, TipoPeriodo.BIMESTRE)
        val recuperacao = FormularioAvaliacao.de(NOVA).copy(nota = "7", tipo = TipoAvaliacao.RECUPERACAO)

        assertEquals(
            AvisoDaNota("Substitui a média do 4º Bimestre, se for maior.", erro = false),
            avisoDaNota(recuperacao, HOJE, soma = false, doPeriodo = doQuarto),
        )
        assertEquals(
            AvisoDaNota("Substitui a soma do ano letivo, se for maior.", erro = false),
            avisoDaNota(recuperacao.copy(tipo = TipoAvaliacao.FINAL), HOJE, soma = true, doPeriodo = doQuarto),
        )
        // Agendada, fica fora como qualquer outra
        assertEquals(
            AvisoDaNota("Fica fora da média até você lançar a nota.", erro = false),
            avisoDaNota(recuperacao.copy(nota = ""), HOJE, soma = false, doPeriodo = doQuarto),
        )
    }

    @Test fun `deixando de ser final, a data volta para dentro dos periodos e o periodo vai junto`() {
        val final = FormularioAvaliacao.de(NOVA).copy(tipo = TipoAvaliacao.FINAL, data = LocalDate.of(2026, 12, 10))

        val normal = final.comTipo(TipoAvaliacao.NORMAL, PERIODOS)

        assertEquals(LocalDate.of(2026, 11, 30), normal.data)
        assertEquals(4L, normal.periodoId)
        // Virando final, nada muda: a data já cabia
        assertEquals(normal.copy(tipo = TipoAvaliacao.FINAL), normal.comTipo(TipoAvaliacao.FINAL, PERIODOS))
    }

    @Test fun `a final pode ir ate o fim do ano, depois do ultimo periodo`() {
        assertEquals(LocalDate.of(2026, 2, 1)..LocalDate.of(2026, 12, 31), intervaloDaFinal(PERIODOS))
        assertNull(intervaloDaFinal(emptyList()))
    }

    @Test fun `na soma a recuperacao e a final tem status, a normal nao`() {
        val soma = RegraAvaliacaoDomain(
            anoLetivoId = 1, disciplinaId = null, mediaMinima = 7.0, mediaRecuperacao = null,
            frequenciaMinima = 75.0, tipoMedia = TipoMedia.SOMA,
        )

        assertNull(statusDaNota(4.0, 5.0, soma))
        assertEquals(StatusDisciplina.APROVADO, statusDaNota(4.0, 5.0, soma, TipoAvaliacao.RECUPERACAO)) // 8 de 10
        assertEquals(StatusDisciplina.ABAIXO, statusDaNota(3.0, 5.0, soma, TipoAvaliacao.FINAL)) // 6 de 10
    }

    @Test fun `no periodo feminino o artigo acompanha`() {
        assertEquals("da 1ª Unidade", doPeriodo(QUARTO_BIMESTRE.copy(periodo = 1), TipoPeriodo.UNIDADE))
    }

    @Test fun `a nota ganha o status como se fosse uma media na escala de 10`() {
        val regra = RegraAvaliacaoDomain(
            anoLetivoId = 1, disciplinaId = null, mediaMinima = 8.0, mediaRecuperacao = null,
            frequenciaMinima = 75.0, tipoMedia = TipoMedia.SIMPLES,
        )

        assertEquals(StatusDisciplina.APROVADO, statusDaNota(9.0, 10.0, regra))
        assertEquals(StatusDisciplina.ATENCAO, statusDaNota(8.5, 10.0, regra))
        assertEquals(StatusDisciplina.ABAIXO, statusDaNota(3.5, 5.0, regra)) // 7 na escala de 10
        assertEquals(StatusDisciplina.APROVADO, statusDaNota(4.5, 5.0, regra)) // 9
        assertNull(statusDaNota(9.0, 10.0, regra.copy(tipoMedia = TipoMedia.SOMA)))
        assertEquals("Perto da mínima", textoDoStatusDaNota(StatusDisciplina.ATENCAO))
    }

    // A linha da data --------------------------------------------------------------------------

    @Test fun `a linha da data diz o dia e quanto falta`() {
        assertEquals("Escolha a data", resumoDaData(null, HOJE))
        assertEquals("Hoje · seg, 12 out", resumoDaData(HOJE, HOJE))
        assertEquals("Amanhã · ter, 13 out", resumoDaData(HOJE.plusDays(1), HOJE))
        assertEquals("Ontem · dom, 11 out", resumoDaData(HOJE.minusDays(1), HOJE))
        assertEquals("seg, 26 out · daqui a 2 semanas", resumoDaData(DIA_26, HOJE))
        assertEquals("qui, 10 set", resumoDaData(LocalDate.of(2026, 9, 10), HOJE))
        assertEquals("seg, 1 fev 2027 · daqui a 3 meses", resumoDaData(LocalDate.of(2027, 2, 1), HOJE))
    }

    @Test fun `quanto falta vai de dias para semanas e depois meses`() {
        assertEquals("daqui a 2 dias", daquiA(HOJE.plusDays(2), HOJE))
        assertEquals("daqui a 6 dias", daquiA(HOJE.plusDays(6), HOJE))
        assertEquals("daqui a 1 semana", daquiA(HOJE.plusDays(7), HOJE))
        assertEquals("daqui a 4 semanas", daquiA(HOJE.plusDays(34), HOJE))
        assertEquals("daqui a 1 mês", daquiA(HOJE.plusDays(35), HOJE))
        assertEquals("daqui a 2 meses", daquiA(HOJE.plusDays(70), HOJE))
    }

    private companion object {
        val HOJE: LocalDate = LocalDate.of(2026, 10, 12) // uma segunda
        val DIA_26: LocalDate = LocalDate.of(2026, 10, 26)

        val NOVA = AvaliacaoDomain(
            disciplinaId = 1,
            periodoId = 4,
            nome = "",
            nota = null,
            notaMaxima = 10.0,
            peso = 1.0,
            data = HOJE,
            criadoEm = Instant.fromEpochMilliseconds(0),
        )

        val QUARTO_BIMESTRE = PeriodoDomain(
            id = 4, anoLetivoId = 1, periodo = 4,
            dataInicio = LocalDate.of(2026, 10, 1), dataFim = LocalDate.of(2026, 11, 30),
        )

        // Os bimestres do emulador, com as férias de julho entre o 2º e o 3º
        val PERIODOS = listOf(
            PeriodoDomain(id = 1, anoLetivoId = 1, periodo = 1, dataInicio = LocalDate.of(2026, 2, 1), dataFim = LocalDate.of(2026, 4, 30)),
            PeriodoDomain(id = 2, anoLetivoId = 1, periodo = 2, dataInicio = LocalDate.of(2026, 5, 1), dataFim = LocalDate.of(2026, 7, 15)),
            PeriodoDomain(id = 3, anoLetivoId = 1, periodo = 3, dataInicio = LocalDate.of(2026, 8, 1), dataFim = LocalDate.of(2026, 9, 30)),
            QUARTO_BIMESTRE,
        )
    }
}
