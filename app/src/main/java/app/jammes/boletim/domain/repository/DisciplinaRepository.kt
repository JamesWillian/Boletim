package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.BoletimItem
import app.jammes.boletim.domain.model.DisciplinaDomain
import kotlinx.coroutines.flow.Flow

interface DisciplinaRepository {

    suspend fun upsert(disciplina: DisciplinaDomain): Long
    suspend fun delete(disciplina: DisciplinaDomain)
    fun observeByAnoLetivo(anoLetivoId: Long): Flow<List<DisciplinaDomain>>
    fun observarBoletim(anoLetivoId: Long, periodoId: Long): Flow<List<BoletimItem>>
}