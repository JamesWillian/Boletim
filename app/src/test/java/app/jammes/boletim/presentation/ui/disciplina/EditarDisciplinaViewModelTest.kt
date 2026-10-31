package app.jammes.boletim.presentation.ui.disciplina

import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.EdicaoDisciplina
import app.jammes.boletim.domain.model.FonteMateria
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.domain.model.Plano
import app.jammes.boletim.domain.usecase.REGRA_PADRAO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** O estado da edição da disciplina: a leitura do banco e, pelo plano, se ela se personaliza. */
class EditarDisciplinaViewModelTest {

    @Test fun `no gratis, a disciplina de uma materia do app nao se personaliza`() {
        val estado = comPlano(lida(FonteMateria.APP), Plano.GRATIS) as EdicaoDisciplinaUiState.Sucesso

        assertFalse(estado.personalizavel)
    }

    @Test fun `no gratis, a de uma materia criada pela pessoa se personaliza`() {
        val estado = comPlano(lida(FonteMateria.USUARIO), Plano.GRATIS) as EdicaoDisciplinaUiState.Sucesso

        assertTrue(estado.personalizavel)
    }

    @Test fun `no Pro, qualquer uma se personaliza`() {
        val estado = comPlano(lida(FonteMateria.APP), Plano.PRO) as EdicaoDisciplinaUiState.Sucesso

        assertTrue(estado.personalizavel)
    }

    @Test fun `sem a materia, ela se personaliza como uma criada pela pessoa`() {
        val estado = comPlano(lida(fonte = null), Plano.GRATIS) as EdicaoDisciplinaUiState.Sucesso

        assertTrue(estado.personalizavel)
    }

    @Test fun `carregando e nao encontrada passam como estao`() {
        assertEquals(EdicaoDisciplinaUiState.Carregando, comPlano(EdicaoDisciplinaUiState.Carregando, Plano.GRATIS))
        assertEquals(EdicaoDisciplinaUiState.NaoEncontrada, comPlano(EdicaoDisciplinaUiState.NaoEncontrada, Plano.GRATIS))
    }

    // A Matemática, vinda de uma matéria da [fonte]; null: a matéria não foi encontrada
    private fun lida(fonte: FonteMateria?) = EdicaoDisciplinaUiState.Sucesso(
        EdicaoDisciplina(
            disciplina = DisciplinaDomain(id = 2, nome = "Matemática", materiaId = 2, anoLetivoId = 1),
            materia = fonte?.let { MateriaDomain(id = 2, nome = "Matemática", fonte = it) },
            anoLetivo = null,
            regraDoAno = REGRA_PADRAO,
            regraPropria = null,
        )
    )
}
