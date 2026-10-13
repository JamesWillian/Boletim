package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.DisciplinaDados
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.Lancamentos
import kotlinx.coroutines.flow.Flow

interface DisciplinaRepository {

    suspend fun upsert(disciplina: DisciplinaDomain): Long
    suspend fun delete(disciplina: DisciplinaDomain)
    fun observeById(id: Long): Flow<DisciplinaDomain?>
    fun observeByAnoLetivo(anoLetivoId: Long): Flow<List<DisciplinaDomain>>
    fun observeBoletim(anoLetivoId: Long): Flow<List<DisciplinaDados>>
    /** false finaliza a disciplina (ela fica no boletim, sem novos lançamentos); true reabre. */
    suspend fun setAtiva(disciplinaId: Long, ativa: Boolean)
    /** id da disciplina → o que já foi lançado nela, para cada disciplina do ano. */
    suspend fun countLancamentosByDisciplina(anoLetivoId: Long): Map<Long, Lancamentos>
}