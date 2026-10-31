package app.jammes.boletim.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test fun `no gratis, restam as tres menos as proprias do cadastro e as do formulario`() {
        val cadastro = listOf(materia(FonteMateria.APP), materia(FonteMateria.USUARIO), materia(FonteMateria.REMOTA))

        assertEquals(2, Plano.GRATIS.materiasQueRestam(cadastro))
        assertEquals(1, Plano.GRATIS.materiasQueRestam(cadastro, noFormulario = 1))
        assertEquals(0, Plano.GRATIS.materiasQueRestam(cadastro, noFormulario = 2))
    }

    @Test fun `no gratis, quem passou do limite fica sem nenhuma, e nao com negativo`() {
        // Vindas de um backup importado, ou de quando o Pro valia
        assertEquals(0, Plano.GRATIS.materiasQueRestam(proprias(5)))
    }

    @Test fun `no Pro, nao ha conta das que restam`() {
        assertNull(Plano.PRO.materiasQueRestam(proprias(5), noFormulario = 2))
    }

    @Test fun `no gratis, grava as novas que cabem no limite`() {
        assertTrue(Plano.GRATIS.podeGravarMateriasNovas(proprias(1), novas = 2))
        assertFalse(Plano.GRATIS.podeGravarMateriasNovas(proprias(1), novas = 3))
    }

    @Test fun `no gratis, sem nenhuma nova, grava mesmo passado do limite`() {
        assertTrue(Plano.GRATIS.podeGravarMateriasNovas(proprias(5), novas = 0))
    }

    @Test fun `no Pro, grava quantas novas vierem`() {
        assertTrue(Plano.PRO.podeGravarMateriasNovas(proprias(5), novas = 4))
    }

    // Anos letivos ---------------------------------------------------------------------------

    @Test fun `no gratis, o ano atual pode`() {
        assertEquals(CriacaoDoAno.LIBERADA, Plano.GRATIS.criacaoDoAno(2026, setOf(2025), NOVEMBRO))
    }

    @Test fun `no gratis, ano passado e do Pro`() {
        assertEquals(CriacaoDoAno.SO_NO_PRO, Plano.GRATIS.criacaoDoAno(2025, setOf(2026), NOVEMBRO))
        assertEquals(CriacaoDoAno.SO_NO_PRO, Plano.GRATIS.criacaoDoAno(2025, setOf(2026), LocalDate.of(2026, 12, 1)))
        // Nem no onboarding: o primeiro ano letivo é o atual ou o seguinte
        assertEquals(CriacaoDoAno.SO_NO_PRO, Plano.GRATIS.criacaoDoAno(2025, emptySet(), NOVEMBRO))
    }

    @Test fun `no gratis, o seguinte so libera em dezembro`() {
        assertEquals(CriacaoDoAno.SO_NO_PRO, Plano.GRATIS.criacaoDoAno(2027, setOf(2026), NOVEMBRO))
        assertEquals(CriacaoDoAno.LIBERADA, Plano.GRATIS.criacaoDoAno(2027, setOf(2026), LocalDate.of(2026, 12, 1)))
        assertEquals(CriacaoDoAno.LIBERADA, Plano.GRATIS.criacaoDoAno(2027, setOf(2026), LocalDate.of(2026, 12, 31)))
    }

    @Test fun `no gratis, dois anos para a frente continua no Pro, mesmo em dezembro`() {
        assertEquals(CriacaoDoAno.SO_NO_PRO, Plano.GRATIS.criacaoDoAno(2028, setOf(2026), LocalDate.of(2026, 12, 1)))
    }

    @Test fun `no gratis, o primeiro ano letivo do aluno pode ser o atual ou o seguinte`() {
        // O do onboarding: o aluno ainda não tem nenhum
        val marco = LocalDate.of(2026, 3, 15)

        assertEquals(CriacaoDoAno.LIBERADA, Plano.GRATIS.criacaoDoAno(2026, emptySet(), marco))
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

    @Test fun `no gratis, com o ano atual criado, nao sobra ano ate dezembro`() {
        assertFalse(Plano.GRATIS.podeCriarAlgumAno(setOf(2026), NOVEMBRO))
        assertTrue(Plano.GRATIS.podeCriarAlgumAno(setOf(2026), LocalDate.of(2026, 12, 1)))
    }

    @Test fun `no gratis, em dezembro, com o atual e o seguinte criados, nao sobra ano`() {
        assertFalse(Plano.GRATIS.podeCriarAlgumAno(setOf(2026, 2027), LocalDate.of(2026, 12, 1)))
    }

    @Test fun `no gratis, sobra o ano atual quando o aluno so tem outros`() {
        // Anos passados e futuros, de um backup ou de quando o Pro valia, não ocupam o atual
        assertTrue(Plano.GRATIS.podeCriarAlgumAno(setOf(2024, 2027), NOVEMBRO))
    }

    @Test fun `no Pro, sempre sobra um ano`() {
        assertTrue(Plano.PRO.podeCriarAlgumAno(setOf(2025, 2026, 2027), NOVEMBRO))
    }

    @Test fun `so liberada e repetida deixam criar`() {
        assertEquals(
            listOf(CriacaoDoAno.LIBERADA, CriacaoDoAno.REPETIDA),
            CriacaoDoAno.entries.filter { it.permitida },
        )
    }

    // Personalização da disciplina -----------------------------------------------------------

    @Test fun `no gratis, a materia do app nao se personaliza`() {
        assertFalse(Plano.GRATIS.podePersonalizarDisciplina(FonteMateria.APP))
    }

    @Test fun `no gratis, a materia propria se personaliza`() {
        assertTrue(Plano.GRATIS.podePersonalizarDisciplina(FonteMateria.USUARIO))
    }

    @Test fun `no gratis, a materia remota segue a do app`() {
        assertFalse(Plano.GRATIS.podePersonalizarDisciplina(FonteMateria.REMOTA))
    }

    @Test fun `no Pro, qualquer materia se personaliza`() {
        FonteMateria.entries.forEach { fonte -> assertTrue(Plano.PRO.podePersonalizarDisciplina(fonte)) }
    }

    private fun materia(fonte: FonteMateria) = MateriaDomain(nome = "Matéria", fonte = fonte)

    // Um cadastro com [quantas] matérias criadas pela pessoa
    private fun proprias(quantas: Int) = List(quantas) { materia(FonteMateria.USUARIO) }

    private companion object {
        val NOVEMBRO: LocalDate = LocalDate.of(2026, 11, 30)
    }
}
