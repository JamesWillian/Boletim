package app.jammes.boletim.data.repository

import app.jammes.boletim.data.local.dao.DisciplinaDao
import app.jammes.boletim.data.mapper.toDomain
import app.jammes.boletim.data.mapper.toEntity
import app.jammes.boletim.domain.model.BoletimItem
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.repository.DisciplinaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisciplinaRepositoryImpl @Inject constructor(
    private val disciplinaDao: DisciplinaDao
): DisciplinaRepository {

    override suspend fun upsert(disciplina: DisciplinaDomain): Long {
        val disciplinaEntity = disciplina.toEntity()

        return if (disciplina.id == 0L) {
            disciplinaDao.insert(disciplinaEntity)
        } else {
            disciplinaDao.update(disciplinaEntity)
            disciplinaEntity.id
        }
    }

    override suspend fun delete(disciplina: DisciplinaDomain) {
        disciplinaDao.delete(disciplina.toEntity())
    }

    override fun observeByAnoLetivo(anoLetivoId: Long): Flow<List<DisciplinaDomain>> {
        return disciplinaDao.observarPorAnoLetivo(anoLetivoId).map {
            it.map { disciplina -> disciplina.toDomain() }
        }
    }

    override fun observarBoletim(anoLetivoId: Long, periodoId: Long): Flow<List<BoletimItem>> =
        disciplinaDao.observarBoletim(anoLetivoId).map { lista ->
            lista.map { it.toDomain(periodoId) }
        }
}