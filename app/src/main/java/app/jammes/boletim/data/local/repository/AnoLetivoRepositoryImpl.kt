package app.jammes.boletim.data.local.repository

import app.jammes.boletim.data.local.dao.AlunoDao
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.emptyList

@Singleton
class AnoLetivoRepositoryImpl @Inject constructor(
    private val anoLetivoDao: AnoLetivoDao,
    private val periodoDao: PeriodoDao,
    private val alunoDao: AlunoDao
): AnoLetivoRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<AnoLetivoDomain>> {
        val aluno = alunoDao.fetchFirst()
        return anoLetivoDao.fetchAll().flatMapLatest { list ->
            if (list.isEmpty()) flowOf(emptyList())
            else combine(
                list.map { l -> periodoDao.fetchByAnoLetivo(l.id) }
            ) { periodoRows ->
                list.mapIndexed { i, ano ->
                    ano.toDomain(
                        periodoRows[i].map { it.toDomain() },
                        aluno.firstOrNull()?.periodoType
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun findById(id: String): AnoLetivoDomain? {
        val ano = anoLetivoDao.fetchById(id)?.toDomain( emptyList() )
        return ano
//        flatMapLatest { al ->
//            if (al == null) flowOf(null)
//            else periodoDao.fetchByAnoLetivo(id)
//                .map { rows -> al.toDomain(rows.map { it.toDomain() }) }
//        }
    }

    override suspend fun upsert(anoLetivo: AnoLetivoDomain): String {
        val anoLetivoEntity = anoLetivo.toEntity()

        if (anoLetivo.id.isEmpty())
            anoLetivoDao.insert(anoLetivoEntity)
        else
            anoLetivoDao.update(anoLetivoEntity)

        return anoLetivoEntity.id
    }

    override suspend fun delete(anoLetivo: AnoLetivoDomain) =
        anoLetivoDao.delete(anoLetivo.toEntity())

    override fun observeAllPeriodos(): Flow<List<PeriodoDomain>> {
        return periodoDao.fetchAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun findPeriodoById(id: String): PeriodoDomain? {
        return periodoDao.fetchById(id)?.toDomain()
    }

    override suspend fun upsertPeriodo(periodo: PeriodoDomain): String {
        val periodoEntity = periodo.toEntity()

        if (periodo.id.isEmpty())
            periodoDao.insert(periodoEntity)
        else
            periodoDao.update(periodoEntity)

        return periodoEntity.id
    }

    override suspend fun deletePeriodo(periodo: PeriodoDomain) {
        periodoDao.delete(periodo.toEntity())
    }
}