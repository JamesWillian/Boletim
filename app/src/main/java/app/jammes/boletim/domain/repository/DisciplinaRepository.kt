package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.DisciplinaDados
import app.jammes.boletim.domain.model.DisciplinaDomain
import kotlinx.coroutines.flow.Flow

interface DisciplinaRepository {

    suspend fun upsert(disciplina: DisciplinaDomain): Long
    suspend fun delete(disciplina: DisciplinaDomain)
    fun observeById(id: Long): Flow<DisciplinaDomain?>
    fun observeByAnoLetivo(anoLetivoId: Long): Flow<List<DisciplinaDomain>>
    fun observeBoletim(anoLetivoId: Long): Flow<List<DisciplinaDados>>
}