package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.AlunoDomain
import kotlinx.coroutines.flow.Flow

interface AlunoRepository {

    fun fetchAll(): Flow<List<AlunoDomain>>

    suspend fun upsert(aluno: AlunoDomain): String

    suspend fun delete(aluno: AlunoDomain)
}