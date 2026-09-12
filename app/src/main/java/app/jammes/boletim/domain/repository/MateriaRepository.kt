package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.MateriaDomain
import kotlinx.coroutines.flow.Flow

interface MateriaRepository {

    fun observeMaterias(): Flow<List<MateriaDomain>>
    suspend fun upsert(materia: MateriaDomain): Long
    suspend fun delete(materia: MateriaDomain)
}