package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.MateriaDomain
import kotlinx.coroutines.flow.Flow

interface MateriaRepository {

    fun observeMaterias(): Flow<List<MateriaDomain>>
    /** id da matéria → em quantos anos letivos ela está. A que não está em nenhum fica de fora. */
    fun observeAnosEmUso(): Flow<Map<Long, Int>>
    suspend fun upsert(materia: MateriaDomain): Long
    suspend fun delete(materia: MateriaDomain)
}
