package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import kotlinx.coroutines.flow.Flow

interface AnoLetivoRepository {
    fun observeAll(): Flow<List<AnoLetivoDomain>>
    fun observeById(id: String): Flow<AnoLetivoDomain?>
    suspend fun upsert(anoLetivo: AnoLetivoDomain): String
    suspend fun delete(anoLetivo: AnoLetivoDomain)

    suspend fun upsertPeriodo(periodo: PeriodoDomain): String
    suspend fun deletePeriodo(periodo: PeriodoDomain)
}