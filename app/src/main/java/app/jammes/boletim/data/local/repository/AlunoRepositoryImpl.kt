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

    override fun observeAluno(): Flow<List<AlunoDomain>> {
        return alunoDao.observar().map { list -> list.map{ it.toDomain() } }
    }

    override suspend fun upsert(aluno: AlunoDomain): Long {
        val alunoEntity = aluno.toEntity()

        return if (aluno.id == 0L) {
            alunoDao.insert(alunoEntity)
        } else {
            alunoDao.update(alunoEntity)
            alunoEntity.id
        }
    }

    override suspend fun delete(aluno: AlunoDomain) = alunoDao.delete(aluno.toEntity())
}