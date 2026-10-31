package app.jammes.boletim.presentation.ui.aluno

import app.jammes.boletim.domain.model.AlunoComAno
import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PerfilAluno
import app.jammes.boletim.domain.model.Plano
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Instant

/** O estado do perfil: o aluno aberto e, pelo plano, se o Novo aluno e o Criar novo ano letivo abrem a folha do Pro. */
class AlunoViewModelTest {

    @Test fun `no gratis, com um aluno, o novo aluno e do Pro`() {
        val estado = estadoDoPerfil(perfilCom(alunos = 1), Plano.GRATIS, NOVEMBRO) as AlunoUiState.Sucesso

        assertTrue(estado.novoAlunoSoNoPro)
    }

    @Test fun `no gratis, com dois alunos de um backup, o terceiro tambem e do Pro`() {
        val estado = estadoDoPerfil(perfilCom(alunos = 2), Plano.GRATIS, NOVEMBRO) as AlunoUiState.Sucesso

        assertTrue(estado.novoAlunoSoNoPro)
    }

    @Test fun `no Pro, o novo aluno abre o cadastro`() {
        val estado = estadoDoPerfil(perfilCom(alunos = 2), Plano.PRO, NOVEMBRO) as AlunoUiState.Sucesso

        assertFalse(estado.novoAlunoSoNoPro)
    }

    @Test fun `no gratis, com o ano atual criado, o ano novo e do Pro`() {
        val estado = estadoDoPerfil(perfilCom(alunos = 1, anos = listOf(2026)), Plano.GRATIS, NOVEMBRO) as AlunoUiState.Sucesso

        assertTrue(estado.novoAnoSoNoPro)
    }

    @Test fun `no gratis, sem o ano atual, o ano novo abre os passos`() {
        val estado = estadoDoPerfil(perfilCom(alunos = 1, anos = listOf(2025)), Plano.GRATIS, NOVEMBRO) as AlunoUiState.Sucesso

        assertFalse(estado.novoAnoSoNoPro)
    }

    @Test fun `no Pro, o ano novo abre os passos`() {
        val estado = estadoDoPerfil(perfilCom(alunos = 1, anos = listOf(2026)), Plano.PRO, NOVEMBRO) as AlunoUiState.Sucesso

        assertFalse(estado.novoAnoSoNoPro)
    }

    @Test fun `sem o aluno no banco, o perfil nao encontra`() {
        assertEquals(AlunoUiState.NaoEncontrado, estadoDoPerfil(null, Plano.GRATIS, NOVEMBRO))
    }

    // O aberto é o primeiro, com os [anos]
    private fun perfilCom(alunos: Int, anos: List<Int> = emptyList()): PerfilAluno {
        val todos = (1..alunos).map { AlunoDomain(id = it.toLong(), nome = "Aluno $it", criadoEm = CRIADO_EM) }
        return PerfilAluno(
            aluno = todos.first(),
            anoLetivo = null,
            anosLetivos = anos.mapIndexed { i, ano ->
                AnoLetivoDomain(id = i + 1L, ano = ano, serie = null, periodo = emptyList(), qtdPeriodos = 0, alunoId = 1)
            },
            alunos = todos.map { AlunoComAno(aluno = it, anoLetivo = null) },
        )
    }

    private companion object {
        val CRIADO_EM: Instant = Instant.parse("2026-02-01T10:00:00Z")
        val NOVEMBRO: LocalDate = LocalDate.of(2026, 11, 30)
    }
}
