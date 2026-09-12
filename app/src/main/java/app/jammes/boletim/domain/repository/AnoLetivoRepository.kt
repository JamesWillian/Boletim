package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import kotlinx.coroutines.flow.Flow

interface AnoLetivoRepository {
    fun observeByAluno(alunoId: Long): Flow<List<AnoLetivoDomain>>
    suspend fun upsert(anoLetivo: AnoLetivoDomain): Long
    suspend fun delete(anoLetivo: AnoLetivoDomain)

    suspend fun upsertPeriodo(periodo: PeriodoDomain): Long
    suspend fun deletePeriodo(periodo: PeriodoDomain)
}