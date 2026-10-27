package app.jammes.boletim.presentation.ui.onboarding

import app.jammes.boletim.domain.model.Etapa
import app.jammes.boletim.domain.model.TipoPeriodo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** A série, o ano e a divisão: o que o onboarding e o ano novo do perfil escolhem do mesmo jeito. */
class FormularioNovoAnoLetivoTest {

    // Série ------------------------------------------------------------------------------------

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

    // Divisão ----------------------------------------------------------------------------------

    @Test fun `trocar o tipo refaz as datas com a quantidade dele, e bimestre e o padrao`() {
        val formulario = FormularioNovoAnoLetivo(ano = 2026)

        assertEquals(TipoPeriodo.BIMESTRE, formulario.tipoPeriodo)
        assertEquals(4, formulario.datas.size)
        assertEquals(3, formulario.comTipo(TipoPeriodo.TRIMESTRE).datas.size)
        assertEquals(2, formulario.comTipo(TipoPeriodo.SEMESTRE).datas.size)
    }

    @Test fun `trocar o ano leva as datas para o ano novo`() {
        val formulario = FormularioNovoAnoLetivo(ano = 2026).comAno(2027)

        assertEquals(LocalDate.of(2027, 2, 1), formulario.datas.first().inicio)
        assertEquals(LocalDate.of(2027, 12, 15), formulario.datas.last().fim)
    }

    @Test fun `o mais poe um periodo depois do ultimo, com a mesma duracao, e o menos tira o ultimo`() {
        val formulario = FormularioNovoAnoLetivo(ano = 2026) // 4 bimestres, o último de 1 out a 15 dez

        val com5 = formulario.comQuantidade(5)

        assertEquals(5, com5.datas.size)
        assertEquals(LocalDate.of(2026, 12, 16), com5.datas.last().inicio)
        assertEquals(LocalDate.of(2027, 3, 1), com5.datas.last().fim)
        assertEquals(formulario.datas, com5.comQuantidade(4).datas)
    }

    @Test fun `a quantidade fica entre 1 e 6`() {
        val formulario = FormularioNovoAnoLetivo(ano = 2026)

        assertEquals(1, formulario.comQuantidade(0).datas.size)
        assertEquals(6, formulario.comQuantidade(9).datas.size)
    }

    @Test fun `datas sobrepostas nao estao certas`() {
        val formulario = FormularioNovoAnoLetivo(ano = 2026)
        val sobreposto = formulario.comDatas(1, formulario.datas[1].copy(inicio = LocalDate.of(2026, 4, 1)))

        assertTrue(formulario.datasCertas)
        assertFalse(sobreposto.datasCertas)
    }

    @Test fun `os periodos vao numerados na ordem, com as datas escolhidas e ainda sem ano letivo`() {
        val formulario = FormularioNovoAnoLetivo(ano = 2026).comTipo(TipoPeriodo.SEMESTRE)

        val periodos = formulario.periodos()

        assertEquals(listOf(1, 2), periodos.map { it.periodo })
        assertEquals(listOf(0L, 0L), periodos.map { it.anoLetivoId })
        assertEquals(LocalDate.of(2026, 8, 1), periodos[1].dataInicio)
        assertEquals(LocalDate.of(2026, 12, 15), periodos[1].dataFim)
    }

    // Ano --------------------------------------------------------------------------------------

    @Test fun `ano sugerido e o do calendario quando o aluno so tem anos passados`() {
        assertEquals(2026, anoSugerido(existentes = setOf(2024), atual = 2026))
    }

    @Test fun `ano sugerido e o seguinte ao mais recente quando o do calendario ja existe`() {
        assertEquals(2027, anoSugerido(existentes = setOf(2024, 2026), atual = 2026))
    }

    @Test fun `sem nenhum ano a sugestao e o do calendario`() {
        assertEquals(2026, anoSugerido(existentes = emptySet(), atual = 2026))
    }

    @Test fun `os anos para escolher sao o passado, o atual e o proximo`() {
        assertEquals(listOf(2025, 2026, 2027), anosParaEscolher(existentes = emptySet(), atual = 2026))
        assertEquals(listOf(2025, 2026, 2027), anosParaEscolher(existentes = setOf(2026), atual = 2026))
    }

    @Test fun `quando o aluno ja tem o proximo, o sugerido entra no fim`() {
        assertEquals(listOf(2025, 2026, 2027, 2028), anosParaEscolher(existentes = setOf(2027), atual = 2026))
    }

    @Test fun `os anos que ja existem dizem por que nao da para escolher`() {
        assertNull(anosQueJaExistem(emptyList()))
        assertEquals("2026 já existe", anosQueJaExistem(listOf(2026)))
        assertEquals("2025 e 2026 já existem", anosQueJaExistem(listOf(2025, 2026)))
        assertEquals("2025, 2026 e 2027 já existem", anosQueJaExistem(listOf(2025, 2026, 2027)))
    }
}
