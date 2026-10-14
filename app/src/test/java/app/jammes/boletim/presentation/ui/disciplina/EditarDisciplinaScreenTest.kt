package app.jammes.boletim.presentation.ui.disciplina

import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.presentation.ui.anoletivo.DatasPeriodo
import app.jammes.boletim.presentation.ui.anoletivo.FormularioAnoLetivo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Os textos da edição da disciplina e dos sheets dela. */
class EditarDisciplinaScreenTest {

    // Total de aulas ---------------------------------------------------------------------------

    @Test fun `total mostra as aulas e quantas faltas ele permite`() {
        // prepara: frequência mínima de 70%, como no emulador
        val regra = regra(frequenciaMinima = 70.0)

        // executa
        val resumo = resumoDoTotalDeAulas(80, regra)

        // confere: 30% de 80
        assertEquals("80 aulas · até 24 faltas", resumo)
    }

    @Test fun `sem o total, as faltas ficam sem limite`() {
        assertEquals("Não informado · faltas sem limite", resumoDoTotalDeAulas(null, regra(70.0)))
    }

    @Test fun `uma aula e uma falta ficam no singular`() {
        assertEquals("1 aula · nenhuma falta", resumoDoTotalDeAulas(1, regra(75.0)))
        assertEquals("4 aulas · até 1 falta", resumoDoTotalDeAulas(4, regra(75.0)))
    }

    @Test fun `com frequencia de 100 por cento nenhuma falta e permitida`() {
        assertEquals("200 aulas · nenhuma falta", resumoDoTotalDeAulas(200, regra(100.0)))
    }

    @Test fun `dica do sheet acompanha o que esta digitado`() {
        val regra = regra(frequenciaMinima = 70.0)

        assertEquals("Até 24 faltas, com a frequência mínima de 70%", dicaDoTotalDeAulas(TotalDigitado.Valido(80), regra))
        assertEquals("Sem o total, as faltas ficam sem limite", dicaDoTotalDeAulas(TotalDigitado.Vazio, regra))
        assertEquals("Pelo menos 1 aula", dicaDoTotalDeAulas(TotalDigitado.Invalido, regra))
    }

    @Test fun `frequencia quebrada aparece com virgula`() {
        assertEquals(
            "Até 20 faltas, com a frequência mínima de 87,5%",
            dicaDoTotalDeAulas(TotalDigitado.Valido(160), regra(87.5)),
        )
    }

    // Período ----------------------------------------------------------------------------------

    @Test fun `sem periodo proprio mostra o ano letivo inteiro e as datas dele`() {
        assertEquals("Ano letivo inteiro · 1 fev – 30 nov", resumoDoPeriodo(null, ANO, ano = 2026))
    }

    @Test fun `com periodo proprio mostra as datas e as semanas`() {
        val periodo = DatasPeriodo(dia(2, 1), dia(4, 30))

        assertEquals("1 fev – 30 abr · 13 semanas", resumoDoPeriodo(periodo, ANO, ano = 2026))
    }

    @Test fun `num ano sem periodos fica so o ano inteiro`() {
        assertEquals("Ano letivo inteiro", resumoDoPeriodo(null, doAno = null, ano = 2026))
    }

    @Test fun `marcar o ano todo no calendario aparece como ano letivo inteiro`() {
        assertEquals("Ano letivo inteiro · 1 fev – 30 nov", resumoDaEscolhaDoPeriodo(ANO.inicio, ANO.fim, ANO, ano = 2026))
    }

    @Test fun `o resto da escolha usa os textos do calendario dos ajustes`() {
        assertEquals("1 fev – 30 abr · 13 semanas", resumoDaEscolhaDoPeriodo(dia(2, 1), dia(4, 30), ANO, ano = 2026))
        assertEquals("Começa em 1 fev; agora toque no último dia", resumoDaEscolhaDoPeriodo(dia(2, 1), null, ANO, ano = 2026))
        assertEquals("Toque no primeiro e no último dia", resumoDaEscolhaDoPeriodo(null, null, ANO, ano = 2026))
    }

    @Test fun `periodo dentro do ano nao tem aviso`() {
        assertNull(avisoDoPeriodo(DatasPeriodo(dia(2, 1), dia(11, 30)), ANO, ano = 2026))
        assertNull(avisoDoPeriodo(null, ANO, ano = 2026))
    }

    @Test fun `periodo que passa do ano letivo avisa as datas dele`() {
        // O ano letivo encolheu depois que o período foi escolhido
        val periodo = DatasPeriodo(dia(8, 1), dia(12, 15))

        assertEquals("Fora do ano letivo, que vai de 1 fev a 30 nov", avisoDoPeriodo(periodo, ANO, ano = 2026))
    }

    // Anos do calendário -----------------------------------------------------------------------

    @Test fun `calendario mostra os anos do ano letivo`() {
        assertEquals(2026..2026, anosDoCalendario(periodo = null, doAno = ANO, ano = 2026))
    }

    @Test fun `periodo gravado fora do ano estica os anos do calendario`() {
        val periodo = DatasPeriodo(LocalDate.of(2025, 12, 1), dia(3, 1))

        assertEquals(2025..2026, anosDoCalendario(periodo, doAno = ANO, ano = 2026))
    }

    @Test fun `sem as datas do ano, de um ano antes a um depois`() {
        assertEquals(2025..2027, anosDoCalendario(periodo = null, doAno = null, ano = 2026))
    }

    // Regras de média --------------------------------------------------------------------------

    @Test fun `resumo das regras mostra os numeros e, embaixo, o calculo e o arredondamento`() {
        val regras = FormularioAnoLetivo.Regras(8.0, 70.0, TipoMedia.SIMPLES, TipoArredondamento.CIMA)

        assertEquals("Mínima 8,0 · frequência 70%\nMédia simples · arredonda para cima", resumoDasRegras(regras))
    }

    @Test fun `resumo das regras com numeros quebrados e sem arredondar`() {
        val regras = FormularioAnoLetivo.Regras(6.5, 87.5, TipoMedia.SOMA, TipoArredondamento.NENHUM)

        assertEquals("Mínima 6,5 · frequência 87,5%\nSoma das notas · sem arredondar", resumoDasRegras(regras))
    }

    @Test fun `titulo diz de onde vem a regra`() {
        assertEquals("Geral do ano letivo", tituloDasRegras(proprias = false))
        assertEquals("Próprias desta disciplina", tituloDasRegras(proprias = true))
    }

    @Test fun `sheet explica para quem as regras valem`() {
        assertEquals(
            "Iguais às do ano letivo. O que mudar aqui vale só para Português.",
            situacaoDasRegras(iguaisAsDoAno = true, disciplina = "Português"),
        )
        assertEquals(
            "Só para Português. As outras disciplinas seguem as do ano letivo.",
            situacaoDasRegras(iguaisAsDoAno = false, disciplina = "Português"),
        )
    }

    @Test fun `com o total de aulas a frequencia diz quantas faltas cabem`() {
        assertEquals("Até 24 faltas", dicaDaFrequencia(70.0, totalAulas = 80, base = regra(75.0)))
        assertEquals("Nenhuma falta", dicaDaFrequencia(100.0, totalAulas = 80, base = regra(75.0)))
    }

    @Test fun `sem o total de aulas a frequencia fica em porcentagem, como nos ajustes`() {
        assertEquals("Até 30% de faltas", dicaDaFrequencia(70.0, totalAulas = null, base = regra(75.0)))
    }
}

private fun dia(mes: Int, dia: Int): LocalDate = LocalDate.of(2026, mes, dia)

// O ano letivo do emulador: 4 bimestres, de 1 fev a 30 nov
private val ANO = DatasPeriodo(dia(2, 1), dia(11, 30))

private fun regra(frequenciaMinima: Double) = RegraAvaliacaoDomain(
    anoLetivoId = 1,
    disciplinaId = null,
    mediaMinima = 7.0,
    mediaRecuperacao = null,
    frequenciaMinima = frequenciaMinima,
)
