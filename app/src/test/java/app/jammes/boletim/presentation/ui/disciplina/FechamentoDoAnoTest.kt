package app.jammes.boletim.presentation.ui.disciplina

import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.StatusDisciplina
import app.jammes.boletim.domain.model.TipoPeriodo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FechamentoDoAnoTest {

    @Test fun `todos os periodos com nota nao tem aviso`() {
        assertNull(avisoDosPeriodosSemNota(emptyList(), TipoPeriodo.BIMESTRE))
    }

    @Test fun `um periodo sem nota, no singular`() {
        assertEquals(
            "O 4º Bimestre está sem nota e fica fora da média do ano.",
            avisoDosPeriodosSemNota(listOf(periodo(4)), TipoPeriodo.BIMESTRE),
        )
    }

    @Test fun `varios periodos sem nota, com o artigo de cada um`() {
        assertEquals(
            "A 3ª e a 4ª Unidade estão sem nota e ficam fora da média do ano.",
            avisoDosPeriodosSemNota(listOf(periodo(3), periodo(4)), TipoPeriodo.UNIDADE),
        )
        assertEquals(
            "O 2º, o 3º e o 4º Bimestre estão sem nota e ficam fora da média do ano.",
            avisoDosPeriodosSemNota(listOf(periodo(2), periodo(3), periodo(4)), TipoPeriodo.BIMESTRE),
        )
    }

    @Test fun `sem nota nenhuma, no genero do tipo de periodo`() {
        assertEquals(
            "Nenhuma unidade tem nota ainda. A situação final sai da média do ano: lance as notas antes de finalizar.",
            textoSemNota(TipoPeriodo.UNIDADE),
        )
        assertEquals(
            "Nenhum trimestre tem nota ainda. A situação final sai da média do ano: lance as notas antes de finalizar.",
            textoSemNota(TipoPeriodo.TRIMESTRE),
        )
    }

    @Test fun `reabrir diz qual situacao sai`() {
        assertEquals(
            "Ela volta a aceitar notas e faltas, e deixa de aparecer como Reprovado até ser finalizada de novo.",
            textoDaReabertura(StatusDisciplina.REPROVADO),
        )
        assertEquals("Ela volta a aceitar notas e faltas.", textoDaReabertura(null))
    }
}

private fun periodo(numero: Int) = PeriodoDomain(
    id = numero.toLong(),
    anoLetivoId = 1,
    periodo = numero,
    dataInicio = LocalDate.of(2026, 2, 1),
    dataFim = LocalDate.of(2026, 3, 31),
)
