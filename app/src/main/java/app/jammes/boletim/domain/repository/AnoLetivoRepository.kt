package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.AnoLetivoDomain
import kotlinx.coroutines.flow.Flow

interface AnoLetivoRepository {
    fun observeAll(): Flow<List<AnoLetivoDomain>>
    fun observeById(id: String): Flow<AnoLetivoDomain?>
    suspend fun upsert(anoLetivo: AnoLetivoDomain): String
}