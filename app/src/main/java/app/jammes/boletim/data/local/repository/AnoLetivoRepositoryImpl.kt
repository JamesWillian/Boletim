package app.jammes.boletim.data.local.repository

import app.jammes.boletim.data.local.dao.AnoLetivoDao
import app.jammes.boletim.data.local.dao.PeriodoDao
import app.jammes.boletim.data.mapper.toDomain
import app.jammes.boletim.data.mapper.toEntity
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.repository.AnoLetivoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.emptyList

@Singleton
class AnoLetivoRepositoryImpl @Inject constructor(
    private val anoLetivoDao: AnoLetivoDao,
    private val periodoDao: PeriodoDao
): AnoLetivoRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<AnoLetivoDomain>> {
        return anoLetivoDao.fetchAll().flatMapLatest { list ->
            if (list.isEmpty()) flowOf(emptyList())
            else combine( list.map { l -> periodoDao.fetchByAnoLetivo(l.id) } ) { periodoRows ->
                list.mapIndexed { i, a ->
                    a.toDomain(periodoRows[i].map { it.toDomain() })
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeById(id: String): Flow<AnoLetivoDomain?> {
        return anoLetivoDao.fetchById(id).flatMapLatest { al ->
            if (al == null) flowOf(null)
            else periodoDao.fetchByAnoLetivo(id)
                .map { rows -> al.toDomain(rows.map { it.toDomain() }) }
        }
    }

    override suspend fun upsert(anoLetivo: AnoLetivoDomain): String {
        val anoLetivoEntity = anoLetivo.toEntity()

        if (anoLetivo.id.isEmpty())
            anoLetivoDao.insert(anoLetivoEntity)
        else
            anoLetivoDao.update(anoLetivoEntity)

        return anoLetivoEntity.id
    }
}