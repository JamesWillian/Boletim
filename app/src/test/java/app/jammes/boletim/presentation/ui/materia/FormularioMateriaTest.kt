package app.jammes.boletim.presentation.ui.materia

import app.jammes.boletim.domain.model.FonteMateria
import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** O que o sheet da matéria faz com o que é digitado e escolhido. */
class FormularioMateriaTest {

    // Matéria nova -----------------------------------------------------------------------------

    @Test fun `a nova abre com a primeira cor livre e criada pela pessoa`() {
        // As padrão usam 0–6, 8, 10, 11, 14, 16 e 17
        val nova = materiaNova(coresUsadas = listOf(0, 1, 16, 10, 2, 8, 6, 3, 5, 4, 11, 17, 14))

        assertEquals(7, nova.cor)
        assertEquals(0L, nova.id) // ainda não gravada
        assertEquals(FonteMateria.USUARIO, nova.fonte)
    }

    @Test fun `com todas as cores usadas, a nova fica com a primeira`() {
        assertEquals(0, materiaNova(coresUsadas = CoresDisciplina.paleta.indices.toList()).cor)
    }

    // Ícone ------------------------------------------------------------------------------------

    @Test fun `numa nova, o icone acompanha o nome`() {
        val form = FormularioMateria.de(NOVA).comNome("Robótica")

        assertEquals(IconeMateria.ROBO, form.icone)
        assertEquals(IconeMateria.TEATRO, form.comNome("Teatro").icone)
    }

    @Test fun `o icone escolhido nao muda mais com o nome`() {
        val form = FormularioMateria.de(NOVA).comIcone(IconeMateria.PALETA).comNome("Robótica")

        assertEquals(IconeMateria.PALETA, form.icone)
    }

    @Test fun `numa que ja existe, o icone nao muda sozinho`() {
        assertEquals(IconeMateria.IDIOMA, FormularioMateria.de(ESPANHOL).comNome("Robótica").icone)
    }

    // Nome -------------------------------------------------------------------------------------

    @Test fun `nome vazio numa nova ainda nao e erro, mas nao deixa criar`() {
        val form = FormularioMateria.de(NOVA)

        assertNull(form.erroDoNome(NOVA, existentes = emptyList()))
        assertFalse(form.podeSalvar(NOVA, existentes = emptyList()))
    }

    @Test fun `apagar o nome de uma que ja existe e erro`() {
        val form = FormularioMateria.de(ESPANHOL).comNome("  ")

        assertEquals("Dê um nome à matéria", form.erroDoNome(ESPANHOL, existentes = emptyList()))
        assertFalse(form.podeSalvar(ESPANHOL, existentes = emptyList()))
    }

    @Test fun `nome repetido e erro, sem ligar para maiusculas e espacos`() {
        val form = FormularioMateria.de(NOVA).comNome(" português ")

        assertEquals("Essa matéria já está na lista", form.erroDoNome(NOVA, existentes = listOf("Português")))
        assertFalse(form.podeSalvar(NOVA, existentes = listOf("Português")))
    }

    // Salvar -----------------------------------------------------------------------------------

    @Test fun `a nova so precisa do nome para ser criada`() {
        assertTrue(FormularioMateria.de(NOVA).comNome("Robótica").podeSalvar(NOVA, existentes = emptyList()))
    }

    @Test fun `uma que ja existe, sem nenhuma mudanca, nao tem o que salvar`() {
        assertFalse(FormularioMateria.de(ESPANHOL).podeSalvar(ESPANHOL, existentes = emptyList()))
    }

    @Test fun `espaco sobrando no nome nao conta como mudanca`() {
        assertFalse(FormularioMateria.de(ESPANHOL).comNome("Espanhol ").podeSalvar(ESPANHOL, existentes = emptyList()))
    }

    @Test fun `apagar a sigla que ja saia do nome nao conta como mudanca`() {
        val semSigla = ESPANHOL.copy(abreviacao = null)

        assertFalse(FormularioMateria.de(semSigla).podeSalvar(semSigla, existentes = emptyList()))
    }

    @Test fun `mudar a cor, o icone, o nome ou a sigla da o que salvar`() {
        val form = FormularioMateria.de(ESPANHOL)

        assertTrue(form.copy(cor = 1).podeSalvar(ESPANHOL, existentes = emptyList()))
        assertTrue(form.comIcone(IconeMateria.LIVRO).podeSalvar(ESPANHOL, existentes = emptyList()))
        assertTrue(form.comNome("Espanhol II").podeSalvar(ESPANHOL, existentes = emptyList()))
        assertTrue(form.copy(abreviacao = "ES").podeSalvar(ESPANHOL, existentes = emptyList()))
    }

    @Test fun `vai para o banco em cima da original, sem espaco sobrando no nome`() {
        val gravada = FormularioMateria.de(ESPANHOL).comNome(" Espanhol II ").copy(cor = 9).aplicadaEm(ESPANHOL)

        assertEquals("Espanhol II", gravada.nome)
        assertEquals(9, gravada.cor)
        assertEquals(ESPANHOL.id, gravada.id)
    }

    @Test fun `a sigla digitada vale, e vazia sai das letras do nome`() {
        val form = FormularioMateria.de(NOVA)

        assertEquals("RBT", form.comNome("Robótica").copy(abreviacao = "rbt").aplicadaEm(NOVA).abreviacao)
        assertEquals("EDF", form.comNome("Ed. Física").copy(abreviacao = " ").aplicadaEm(NOVA).abreviacao)
        assertEquals("TEA", form.comNome("Teatro").aplicadaEm(NOVA).abreviacao)
    }

    private companion object {
        val NOVA = materiaNova(coresUsadas = emptyList())
        val ESPANHOL = MateriaDomain(id = 14, nome = "Espanhol", abreviacao = "ESP", cor = 7, icone = IconeMateria.IDIOMA)
    }
}
