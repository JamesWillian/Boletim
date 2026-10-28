package app.jammes.boletim.presentation.ui.onboarding

import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.domain.usecase.MateriaNova
import app.jammes.boletim.presentation.ui.anoletivo.DatasPeriodo
import app.jammes.boletim.presentation.ui.components.nomeRepetido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** O que só o onboarding tem; a série, o ano e a divisão estão no FormularioNovoAnoLetivoTest. */
class FormularioOnboardingTest {

    @Test fun `so da para concluir com o nome preenchido`() {
        assertFalse(formulario(nome = "   ").podeConcluir)
        assertTrue(formulario(nome = "Ana").podeConcluir)
    }

    @Test fun `as materias seguem a sugestao da serie ate a pessoa mexer`() {
        val formulario = formulario()
        assertEquals(setOf(1L, 2L), formulario.materiasMarcadas(sugeridas = setOf(1L, 2L)))

        val mexido = formulario.alternarMateria(2L, sugeridas = setOf(1L, 2L))

        // Depois de mexer, uma sugestão nova (outra série) não muda mais nada
        assertEquals(setOf(1L), mexido.materiasMarcadas(sugeridas = setOf(1L, 2L, 3L)))
    }

    @Test fun `nao grava sem nome, sem materia ou com datas sobrepostas`() {
        val certo = formulario(nome = "Ana")
        val sobreposto = certo.copy(
            anoLetivo = certo.anoLetivo.comDatas(1, DatasPeriodo(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 7, 15))),
        )

        assertNotNull(certo.paraGravar(setOf(1L)))
        assertNull(certo.copy(nome = "  ").paraGravar(setOf(1L)))
        assertNull(certo.paraGravar(emptySet()))
        assertNull(sobreposto.paraGravar(setOf(1L)))
    }

    @Test fun `o que vai para o banco leva a serie, os periodos e a regra escolhidos`() {
        val formulario = FormularioOnboarding(
            nome = " Ana ",
            anoLetivo = FormularioNovoAnoLetivo(serie = Serie.Medio(2), ano = 2026).comTipo(TipoPeriodo.SEMESTRE),
        )
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

    @Test fun `materia criada aqui conta, e sozinha ja basta para gravar`() {
        val formulario = formulario(nome = "Ana").copy(materiaIds = emptySet()).comMateriaNova(" Robótica ")

        val primeiro = formulario.paraGravar(sugeridas = setOf(1L))!!

        assertEquals(1, formulario.totalDeMaterias(setOf(1L)))
        assertEquals(emptySet<Long>(), primeiro.materiaIds)
        assertEquals(listOf(MateriaNova("Robótica")), primeiro.materiasNovas)
        assertNull(formulario.semMateriaNova("Robótica").paraGravar(setOf(1L)))
    }

    @Test fun `nome de materia repetido nao liga para maiusculas nem espacos`() {
        assertTrue(nomeRepetido("  português ", listOf("Português", "Matemática")))
        assertFalse(nomeRepetido("Robótica", listOf("Português", "Matemática")))
    }
}

// O onboarding de 2026 com o resto no padrão, pra cada teste mostrar só o que importa.
private fun formulario(nome: String = "") = FormularioOnboarding(nome = nome, anoLetivo = FormularioNovoAnoLetivo(ano = 2026))
