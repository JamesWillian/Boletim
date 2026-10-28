package app.jammes.boletim.presentation.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** O nome e a sigla como ficam nos campos, na edição da disciplina e na matéria nova. */
class CamposDaMateriaTest {

    // Nome digitado ----------------------------------------------------------------------------

    @Test fun `o que passa de 20 caracteres nao entra no nome`() {
        assertEquals("Educação Tecnológica", lerNome("Educação Tecnológica e Digital"))
        assertEquals(20, lerNome("a".repeat(30)).length)
    }

    @Test fun `nome curto fica como foi digitado`() {
        assertEquals("Robótica ", lerNome("Robótica "))
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
}
