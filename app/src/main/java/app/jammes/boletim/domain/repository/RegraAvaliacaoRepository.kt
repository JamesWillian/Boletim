package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import kotlinx.coroutines.flow.Flow

interface RegraAvaliacaoRepository {

    fun observeByAnoLetivo(anoLetivoId: Long): Flow<List<RegraAvaliacaoDomain>>
    fun observeByDisciplina(disciplinaId: Long): Flow<List<RegraAvaliacaoDomain>>
    suspend fun upsert(regraAvaliacao: RegraAvaliacaoDomain): Long
    suspend fun delete(regraAvaliacao: RegraAvaliacaoDomain)
}