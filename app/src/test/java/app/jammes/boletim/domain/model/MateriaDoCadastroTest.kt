package app.jammes.boletim.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** O que segura uma matéria no cadastro. */
class MateriaDoCadastroTest {

    @Test fun `criada pela pessoa e fora dos anos letivos, pode sair`() {
        assertNull(MateriaDoCadastro(MateriaDomain(fonte = FonteMateria.USUARIO), anosEmUso = 0).impedimentoDeExcluir)
    }

    @Test fun `em uso, nao sai, e o impedimento diz em quantos anos`() {
        assertEquals(
            ImpedimentoDeExcluir.EmUso(2),
            MateriaDoCadastro(MateriaDomain(fonte = FonteMateria.USUARIO), anosEmUso = 2).impedimentoDeExcluir,
        )
    }

    @Test fun `as do app nunca saem, em uso ou nao`() {
        assertEquals(ImpedimentoDeExcluir.DoApp, MateriaDoCadastro(MateriaDomain(fonte = FonteMateria.APP)).impedimentoDeExcluir)
        assertEquals(
            ImpedimentoDeExcluir.DoApp,
            MateriaDoCadastro(MateriaDomain(fonte = FonteMateria.APP), anosEmUso = 3).impedimentoDeExcluir,
        )
    }

    @Test fun `as que vierem de fora contam como do app`() {
        assertEquals(ImpedimentoDeExcluir.DoApp, MateriaDoCadastro(MateriaDomain(fonte = FonteMateria.REMOTA)).impedimentoDeExcluir)
    }
}
