package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.AvaliacaoDomain
import kotlinx.coroutines.flow.Flow

interface AvaliacaoRepository {

    fun observeByDisciplina(disciplinaId: Long): Flow<List<AvaliacaoDomain>>
    fun observeByPeriodo(periodoId: Long): Flow<List<AvaliacaoDomain>>
    suspend fun upsert(avaliacao: AvaliacaoDomain): Long
    suspend fun delete(avaliacao: AvaliacaoDomain)
}