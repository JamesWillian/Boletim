package app.jammes.boletim.data.repository

import app.jammes.boletim.data.local.dao.FaltaDao
import app.jammes.boletim.data.mapper.toDomain
import app.jammes.boletim.data.mapper.toEntity
import app.jammes.boletim.domain.model.FaltaDomain
import app.jammes.boletim.domain.repository.FaltaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class FaltaRepositoryImpl @Inject constructor(
    private val faltaDao: FaltaDao
): FaltaRepository {

    override fun observeByDisciplina(disciplinaId: Long): Flow<List<FaltaDomain>> {
        return faltaDao.observarPorDisciplina(disciplinaId).map {
            it.map { falta -> falta.toDomain() }
        }
    }

    override suspend fun upsert(falta: FaltaDomain): Long {
        val faltaEntity = falta.toEntity()

        return if (falta.id == 0L) {
            faltaDao.insert(faltaEntity)
        } else {
            faltaDao.update(faltaEntity)
            faltaEntity.id
        }
    }

    override suspend fun delete(falta: FaltaDomain) {
        faltaDao.delete(falta.toEntity())
    }
}