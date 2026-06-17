package app.jammes.boletim.data.local.repository

import app.jammes.boletim.data.local.dao.AlunoDao
import app.jammes.boletim.data.mapper.toDomain
import app.jammes.boletim.data.mapper.toEntity
import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.repository.AlunoRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class AlunoRepositoryImpl @Inject constructor(
    private val alunoDao: AlunoDao
): AlunoRepository {

    override fun fetchAll(): Flow<List<AlunoDomain>> {
        return alunoDao.fetchAll().map { lista ->
            lista.map { aluno ->
                aluno.toDomain()
            }
        }
    }

    override suspend fun upsert(aluno: AlunoDomain): String {
        val alunoEntity = aluno.toEntity()

        if (aluno.id.isEmpty())
            alunoDao.insert(alunoEntity)
        else
            alunoDao.update(alunoEntity)

        return alunoEntity.id
    }

    override suspend fun delete(aluno: AlunoDomain) = alunoDao.delete(aluno.toEntity())
}