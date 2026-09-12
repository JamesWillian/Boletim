package app.jammes.boletim.data.local.repository

import app.jammes.boletim.data.local.dao.RegraAvaliacaoDao
import app.jammes.boletim.data.mapper.toDomain
import app.jammes.boletim.data.mapper.toEntity
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.repository.RegraAvaliacaoRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

@Singleton
class RegraAvaliacaoRepositoryImpl @Inject constructor(
    private val regraAvaliacaoDao: RegraAvaliacaoDao
): RegraAvaliacaoRepository {
    override fun observeByAnoLetivo(anoLetivoId: Long): Flow<List<RegraAvaliacaoDomain>> {
        return regraAvaliacaoDao.observarPorAnoLetivo(anoLetivoId).map{
            it.map { regra -> regra.toDomain() }
        }
    }

    override fun observeByDisciplina(disciplinaId: Long): Flow<List<RegraAvaliacaoDomain>> {
        return regraAvaliacaoDao.observarPorDisciplina(disciplinaId).map{
            it.map { regra -> regra.toDomain() }
        }
    }

    override suspend fun upsert(regraAvaliacao: RegraAvaliacaoDomain): Long {
        val regraAvaliacaoEntity = regraAvaliacao.toEntity()

        return if (regraAvaliacao.id == 0L) {
            regraAvaliacaoDao.insert(regraAvaliacaoEntity)
        } else {
            regraAvaliacaoDao.update(regraAvaliacaoEntity)
            regraAvaliacaoEntity.id
        }
    }

    override suspend fun delete(regraAvaliacao: RegraAvaliacaoDomain) {
        regraAvaliacaoDao.delete(regraAvaliacao.toEntity())
    }
}