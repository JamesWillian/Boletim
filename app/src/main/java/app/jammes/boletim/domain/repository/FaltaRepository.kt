package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.FaltaDomain
import kotlinx.coroutines.flow.Flow

interface FaltaRepository {

    fun observeByDisciplina(disciplinaId: Long): Flow<List<FaltaDomain>>
    suspend fun upsert(falta: FaltaDomain): Long
    suspend fun delete(falta: FaltaDomain)
}