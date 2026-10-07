package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.Lancamentos
import app.jammes.boletim.domain.model.PeriodoDomain
import kotlinx.coroutines.flow.Flow

interface AnoLetivoRepository {
    fun observeByAluno(alunoId: Long): Flow<List<AnoLetivoDomain>>
    /** O ano letivo com os períodos dele; null quando não existe. */
    fun observeById(id: Long): Flow<AnoLetivoDomain?>
    suspend fun upsert(anoLetivo: AnoLetivoDomain): Long
    suspend fun delete(anoLetivo: AnoLetivoDomain)

    suspend fun upsertPeriodo(periodo: PeriodoDomain): Long
    suspend fun deletePeriodo(periodo: PeriodoDomain)
    /** id do período → o que já foi lançado nele, para cada período do ano. */
    suspend fun countLancamentosByPeriodo(anoLetivoId: Long): Map<Long, Lancamentos>
}