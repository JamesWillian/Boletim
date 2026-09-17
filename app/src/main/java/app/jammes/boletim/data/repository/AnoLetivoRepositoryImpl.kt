package app.jammes.boletim.data.repository

import app.jammes.boletim.data.local.dao.AnoLetivoDao
import app.jammes.boletim.data.local.dao.PeriodoDao
import app.jammes.boletim.data.mapper.toDomain
import app.jammes.boletim.data.mapper.toEntity
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.repository.AnoLetivoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.emptyList

@Singleton
class AnoLetivoRepositoryImpl @Inject constructor(
    private val anoLetivoDao: AnoLetivoDao,
    private val periodoDao: PeriodoDao
): AnoLetivoRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeByAluno(alunoId: Long): Flow<List<AnoLetivoDomain>> {

        return anoLetivoDao.observarPorAluno(alunoId).flatMapLatest { list ->
            if (list.isEmpty()) flowOf(emptyList())
            else combine(
                list.map { ano -> periodoDao.observarPorAnoLetivo(ano.id) }
            ) { periodoRows ->
                list.mapIndexed { i, ano ->
                    ano.toDomain(
                        periodoRows[i].map { it.toDomain() }
                    )
                }
            }
        }
    }

    override suspend fun upsert(anoLetivo: AnoLetivoDomain): Long {
        val anoLetivoEntity = anoLetivo.toEntity()

        return if (anoLetivo.id == 0L) {
            anoLetivoDao.insert(anoLetivoEntity)
        } else {
            anoLetivoDao.update(anoLetivoEntity)
            anoLetivoEntity.id
        }
    }

    override suspend fun delete(anoLetivo: AnoLetivoDomain) =
        anoLetivoDao.delete(anoLetivo.toEntity())

    override suspend fun upsertPeriodo(periodo: PeriodoDomain): Long {
        val periodoEntity = periodo.toEntity()

        return if (periodo.id == 0L) {
            periodoDao.insert(periodoEntity)
        } else {
            periodoDao.update(periodoEntity)
            periodoEntity.id
        }
    }

    override suspend fun deletePeriodo(periodo: PeriodoDomain) {
        periodoDao.delete(periodo.toEntity())
    }
}