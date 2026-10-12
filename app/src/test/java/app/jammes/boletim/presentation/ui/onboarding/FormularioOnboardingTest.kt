package app.jammes.boletim.presentation.ui.onboarding

import app.jammes.boletim.domain.model.Etapa
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.presentation.ui.anoletivo.DatasPeriodo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FormularioOnboardingTest {

    @Test fun `no fundamental a serie vira o ano, e no medio vira a serie`() {
        assertEquals("9º Ano", Serie.Fundamental(9).texto())
        assertEquals("1ª Série", Serie.Medio(1).texto())
    }

    @Test fun `serie escrita a mao vai sem os espacos, e em branco fica sem serie`() {
        assertEquals("Pré-vestibular", Serie.Outra("  Pré-vestibular ").texto())
        assertNull(Serie.Outra("   ").texto())
        assertNull((null as Serie?).texto())
    }

    @Test fun `a etapa sai dos chips, e serie escrita a mao nao tem etapa`() {
        assertEquals(Etapa.FUNDAMENTAL, Serie.Fundamental(6).etapa)
        assertEquals(Etapa.MEDIO, Serie.Medio(3).etapa)
        assertNull(Serie.Outra("Técnico").etapa)
        assertNull((null as Serie?).etapa)
    }

    @Test fun `so da para concluir com o nome preenchido`() {
        assertFalse(FormularioOnboarding(nome = "   ", ano = 2026).podeConcluir)
        assertTrue(FormularioOnboarding(nome = "Ana", ano = 2026).podeConcluir)
    }

    @Test fun `trocar o tipo refaz as datas com a quantidade dele, e bimestre e o padrao`() {
        val formulario = FormularioOnboarding(ano = 2026)

        assertEquals(TipoPeriodo.BIMESTRE, formulario.tipoPeriodo)
        assertEquals(4, formulario.datas.size)
        assertEquals(3, formulario.comTipo(TipoPeriodo.TRIMESTRE).datas.size)
        assertEquals(2, formulario.comTipo(TipoPeriodo.SEMESTRE).datas.size)
    }

    @Test fun `trocar o ano leva as datas para o ano novo`() {
        val formulario = FormularioOnboarding(ano = 2026).comAno(2027)

        assertEquals(LocalDate.of(2027, 2, 1), formulario.datas.first().inicio)
        assertEquals(LocalDate.of(2027, 12, 15), formulario.datas.last().fim)
    }

    @Test fun `as materias seguem a sugestao da serie ate a pessoa mexer`() {
        val formulario = FormularioOnboarding(ano = 2026)
        assertEquals(setOf(1L, 2L), formulario.materiasMarcadas(sugeridas = setOf(1L, 2L)))

        val mexido = formulario.alternarMateria(2L, sugeridas = setOf(1L, 2L))

        // Depois de mexer, uma sugestão nova (outra série) não muda mais nada
        assertEquals(setOf(1L), mexido.materiasMarcadas(sugeridas = setOf(1L, 2L, 3L)))
    }

    @Test fun `nao grava sem nome, sem materia ou com datas sobrepostas`() {
        val certo = FormularioOnboarding(nome = "Ana", ano = 2026)
        val sobreposto = certo.comDatas(1, DatasPeriodo(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 7, 15)))

        assertNotNull(certo.paraGravar(setOf(1L)))
        assertNull(certo.copy(nome = "  ").paraGravar(setOf(1L)))
        assertNull(certo.paraGravar(emptySet()))
        assertNull(sobreposto.paraGravar(setOf(1L)))
    }

    @Test fun `o que vai para o banco leva a serie, os periodos e a regra escolhidos`() {
        val formulario = FormularioOnboarding(nome = " Ana ", serie = Serie.Medio(2), ano = 2026)
            .comTipo(TipoPeriodo.SEMESTRE)
            .mudarMedia(-1)
            .copy(tipoMedia = TipoMedia.PONDERADA)

        val primeiro = formulario.paraGravar(setOf(5L))!!

        assertEquals("Ana", primeiro.nome)
        assertEquals("2ª Série", primeiro.serie)
        assertEquals(TipoPeriodo.SEMESTRE, primeiro.tipoPeriodo)
        assertEquals(listOf(1, 2), primeiro.periodos.map { it.periodo })
        assertEquals(6.5, primeiro.regra.mediaMinima, 0.0)
        assertEquals(TipoMedia.PONDERADA, primeiro.regra.tipoMedia)
        assertEquals(setOf(5L), primeiro.materiaIds)
    }

    @Test fun `o mais poe um periodo depois do ultimo, com a mesma duracao, e o menos tira o ultimo`() {
        val formulario = FormularioOnboarding(ano = 2026) // 4 bimestres, o último de 1 out a 15 dez

        val com5 = formulario.comQuantidade(5)

        assertEquals(5, com5.datas.size)
        assertEquals(LocalDate.of(2026, 12, 16), com5.datas.last().inicio)
        assertEquals(LocalDate.of(2027, 3, 1), com5.datas.last().fim)
        assertEquals(formulario.datas, com5.comQuantidade(4).datas)
    }

    @Test fun `a quantidade fica entre 1 e 6`() {
        val formulario = FormularioOnboarding(ano = 2026)

        assertEquals(1, formulario.comQuantidade(0).datas.size)
        assertEquals(6, formulario.comQuantidade(9).datas.size)
    }

    @Test fun `materia criada aqui conta, e sozinha ja basta para gravar`() {
        val formulario = FormularioOnboarding(nome = "Ana", ano = 2026, materiaIds = emptySet()).comMateriaNova(" Robótica ")

        val primeiro = formulario.paraGravar(sugeridas = setOf(1L))!!

        assertEquals(1, formulario.totalDeMaterias(setOf(1L)))
        assertEquals(emptySet<Long>(), primeiro.materiaIds)
        assertEquals(listOf("Robótica"), primeiro.materiasNovas)
        assertNull(formulario.semMateriaNova("Robótica").paraGravar(setOf(1L)))
    }

    @Test fun `nome de materia repetido nao liga para maiusculas nem espacos`() {
        assertTrue(nomeRepetido("  português ", listOf("Português", "Matemática")))
        assertFalse(nomeRepetido("Robótica", listOf("Português", "Matemática")))
    }
}
