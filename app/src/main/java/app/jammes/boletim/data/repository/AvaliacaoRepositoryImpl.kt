package app.jammes.boletim.data.repository

import app.jammes.boletim.data.local.dao.AvaliacaoDao
import app.jammes.boletim.data.mapper.toDomain
import app.jammes.boletim.data.mapper.toEntity
import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.repository.AvaliacaoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class AvaliacaoRepositoryImpl @Inject constructor(
   private val avaliacaoDao: AvaliacaoDao
): AvaliacaoRepository {

    override fun observeByDisciplina(disciplinaId: Long): Flow<List<AvaliacaoDomain>> {
        return avaliacaoDao.observarPorDisciplina(disciplinaId).map {
            it.map { avaliacao -> avaliacao.toDomain() }
        }
    }

    override fun observeByPeriodo(periodoId: Long): Flow<List<AvaliacaoDomain>> {
        return avaliacaoDao.observarPorPeriodo(periodoId).map {
            it.map { avaliacao -> avaliacao.toDomain() }
        }
    }

    override suspend fun upsert(avaliacao: AvaliacaoDomain): Long {
        val avaliacaoEntity = avaliacao.toEntity()

        return if (avaliacao.id == 0L) {
            avaliacaoDao.insert(avaliacaoEntity)
        } else {
            avaliacaoDao.update(avaliacaoEntity)
            avaliacaoEntity.id
        }
    }

    override suspend fun delete(avaliacao: AvaliacaoDomain) {
        avaliacaoDao.delete(avaliacao.toEntity())
    }
}