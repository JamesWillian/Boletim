package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.AlunoDomain
import kotlinx.coroutines.flow.Flow

interface AlunoRepository {

    fun observeAluno(): Flow<List<AlunoDomain>>
    suspend fun upsert(aluno: AlunoDomain): Long
    suspend fun delete(aluno: AlunoDomain)
}