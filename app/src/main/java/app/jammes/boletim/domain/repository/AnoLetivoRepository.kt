package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import kotlinx.coroutines.flow.Flow

interface AnoLetivoRepository {
    fun observeAll(): Flow<List<AnoLetivoDomain>>
    suspend fun findById(id: String): AnoLetivoDomain?
    suspend fun upsert(anoLetivo: AnoLetivoDomain): String
    suspend fun delete(anoLetivo: AnoLetivoDomain)

    fun observeAllPeriodos(): Flow<List<PeriodoDomain>>
    suspend fun findPeriodoById(id: String): PeriodoDomain?
    suspend fun upsertPeriodo(periodo: PeriodoDomain): String
    suspend fun deletePeriodo(periodo: PeriodoDomain)
}