package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.PeriodoType
import kotlinx.coroutines.flow.Flow

interface AlunoRepository {

    fun observeAluno(): Flow<AlunoDomain?>
    suspend fun upsert(aluno: AlunoDomain): String
    suspend fun delete(aluno: AlunoDomain)
    suspend fun setAnoLetivoPadrao(anoLetivoId: String)
    suspend fun setPeriodoPadrao(periodoId: String)
    suspend fun setPeriodoType(periodoType: PeriodoType)
}