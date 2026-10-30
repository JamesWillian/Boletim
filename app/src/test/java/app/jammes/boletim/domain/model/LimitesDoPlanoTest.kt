package app.jammes.boletim.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** O que cada plano deixa criar e mudar. */
class LimitesDoPlanoTest {

    // Alunos ---------------------------------------------------------------------------------

    @Test fun `no gratis, o primeiro aluno pode`() {
        assertTrue(Plano.GRATIS.podeCriarAluno(alunos = 0))
    }

    @Test fun `no gratis, o segundo aluno e do Pro`() {
        assertFalse(Plano.GRATIS.podeCriarAluno(alunos = 1))
    }

    @Test fun `no gratis, quem ja tem dois nao cria o terceiro`() {
        // Vindos de um backup importado, ou de quando o Pro valia
        assertFalse(Plano.GRATIS.podeCriarAluno(alunos = 2))
    }

    @Test fun `no Pro, alunos sem limite`() {
        assertTrue(Plano.PRO.podeCriarAluno(alunos = 3))
    }

    // Matérias -------------------------------------------------------------------------------

    @Test fun `no gratis, ate tres materias proprias`() {
        assertTrue(Plano.GRATIS.podeCriarMateria(proprias = 0))
        assertTrue(Plano.GRATIS.podeCriarMateria(proprias = 2))
    }

    @Test fun `no gratis, as do formulario contam junto com as salvas`() {
        val salvas = 2
        val noFormulario = 1

        assertFalse(Plano.GRATIS.podeCriarMateria(proprias = salvas + noFormulario))
    }

    @Test fun `no Pro, materias sem limite`() {
        assertTrue(Plano.PRO.podeCriarMateria(proprias = 10))
    }

    // Anos letivos ---------------------------------------------------------------------------

    @Test fun `no gratis, ate o ano atual`() {
        assertEquals(CriacaoDoAno.LIBERADA, Plano.GRATIS.criacaoDoAno(2025, setOf(2026), NOVEMBRO))
        assertEquals(CriacaoDoAno.SO_NO_PRO, Plano.GRATIS.criacaoDoAno(2027, setOf(2026), NOVEMBRO))
    }

    @Test fun `no gratis, o seguinte libera em dezembro`() {
        assertEquals(CriacaoDoAno.LIBERADA, Plano.GRATIS.criacaoDoAno(2027, setOf(2026), LocalDate.of(2026, 12, 1)))
        assertEquals(CriacaoDoAno.LIBERADA, Plano.GRATIS.criacaoDoAno(2027, setOf(2026), LocalDate.of(2026, 12, 31)))
    }

    @Test fun `no gratis, dois anos para a frente continua no Pro, mesmo em dezembro`() {
        assertEquals(CriacaoDoAno.SO_NO_PRO, Plano.GRATIS.criacaoDoAno(2028, setOf(2026), LocalDate.of(2026, 12, 1)))
    }

    @Test fun `no gratis, o primeiro ano letivo do aluno pode ser o seguinte`() {
        // O do onboarding: o aluno ainda não tem nenhum
        val marco = LocalDate.of(2026, 3, 15)

        assertEquals(CriacaoDoAno.LIBERADA, Plano.GRATIS.criacaoDoAno(2027, emptySet(), marco))
        assertEquals(CriacaoDoAno.SO_NO_PRO, Plano.GRATIS.criacaoDoAno(2028, emptySet(), marco))
    }

    @Test fun `no gratis, um ano que o aluno ja tem nao repete`() {
        assertEquals(CriacaoDoAno.JA_EXISTE, Plano.GRATIS.criacaoDoAno(2026, setOf(2025, 2026), NOVEMBRO))
        assertEquals(CriacaoDoAno.JA_EXISTE, Plano.GRATIS.criacaoDoAno(2025, setOf(2025, 2026), NOVEMBRO))
    }

    @Test fun `no gratis, um ano futuro que o aluno ja tem tambem diz que ja existe`() {
        // De quando o Pro valia: o motivo na tela é o mesmo dos outros anos que ele tem
        assertEquals(CriacaoDoAno.JA_EXISTE, Plano.GRATIS.criacaoDoAno(2027, setOf(2026, 2027), NOVEMBRO))
    }

    @Test fun `no Pro, qualquer ano`() {
        assertEquals(CriacaoDoAno.LIBERADA, Plano.PRO.criacaoDoAno(2028, setOf(2026), NOVEMBRO))
        assertEquals(CriacaoDoAno.LIBERADA, Plano.PRO.criacaoDoAno(2020, setOf(2026), NOVEMBRO))
    }

    @Test fun `no Pro, um ano repetido pode, com aviso`() {
        assertEquals(CriacaoDoAno.REPETIDA, Plano.PRO.criacaoDoAno(2026, setOf(2026), NOVEMBRO))
    }

    @Test fun `so liberada e repetida deixam criar`() {
        assertEquals(
            listOf(CriacaoDoAno.LIBERADA, CriacaoDoAno.REPETIDA),
            CriacaoDoAno.entries.filter { it.permitida },
        )
    }

    // Personalização da disciplina -----------------------------------------------------------

    @Test fun `no gratis, a materia do app nao muda nome, sigla, cor nem icone`() {
        assertEquals(
            PersonalizacaoDaDisciplina(nomeESigla = false, corEIcone = false),
            Plano.GRATIS.personalizacaoDaDisciplina(FonteMateria.APP),
        )
    }

    @Test fun `no gratis, a materia propria muda nome e sigla, mas nao cor e icone`() {
        assertEquals(
            PersonalizacaoDaDisciplina(nomeESigla = true, corEIcone = false),
            Plano.GRATIS.personalizacaoDaDisciplina(FonteMateria.USUARIO),
        )
    }

    @Test fun `no gratis, a materia remota segue a do app`() {
        assertEquals(
            Plano.GRATIS.personalizacaoDaDisciplina(FonteMateria.APP),
            Plano.GRATIS.personalizacaoDaDisciplina(FonteMateria.REMOTA),
        )
    }

    @Test fun `no Pro, muda tudo em qualquer materia`() {
        val tudo = PersonalizacaoDaDisciplina(nomeESigla = true, corEIcone = true)

        FonteMateria.entries.forEach { fonte -> assertEquals(tudo, Plano.PRO.personalizacaoDaDisciplina(fonte)) }
    }

    private companion object {
        val NOVEMBRO: LocalDate = LocalDate.of(2026, 11, 30)
    }
}
