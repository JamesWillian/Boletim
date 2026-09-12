package app.jammes.boletim.data.local.repository

import app.jammes.boletim.data.local.dao.MateriaDao
import app.jammes.boletim.data.mapper.toDomain
import app.jammes.boletim.data.mapper.toEntity
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.domain.repository.MateriaRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class MateriaRepositoryImpl @Inject constructor(
    private val materiaDao: MateriaDao
): MateriaRepository {

    override fun observeMaterias(): Flow<List<MateriaDomain>> {
        return materiaDao.observar().map { it.map { materia -> materia.toDomain() } }
    }

    override suspend fun upsert(materia: MateriaDomain): Long {
        val materiaEntity = materia.toEntity()

        return if (materia.id == 0L) {
            materiaDao.insert(materiaEntity)
        } else {
            materiaDao.update(materiaEntity)
            materiaEntity.id
        }
    }

    override suspend fun delete(materia: MateriaDomain) {
        materiaDao.delete(materia.toEntity())
    }
}