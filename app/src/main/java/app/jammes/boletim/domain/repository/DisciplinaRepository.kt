package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.DisciplinaDomain
import kotlinx.coroutines.flow.Flow

interface DisciplinaRepository {

    fun observeByAnoLetivo(anoLetivoId: Long): Flow<List<DisciplinaDomain>>
    suspend fun upsert(disciplina: DisciplinaDomain): Long
    suspend fun delete(disciplina: DisciplinaDomain)
}