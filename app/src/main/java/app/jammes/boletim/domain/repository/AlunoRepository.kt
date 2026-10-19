package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.ResumoDoAluno
import kotlinx.coroutines.flow.Flow

interface AlunoRepository {

    fun observeAluno(): Flow<List<AlunoDomain>>
    suspend fun upsert(aluno: AlunoDomain): Long

    /**
     * Apaga o aluno e, pelas chaves estrangeiras, tudo o que é dele: os anos letivos, com os períodos,
     * as regras, as disciplinas, as avaliações e as faltas, e as notificações. As matérias ficam.
     */
    suspend fun delete(aluno: AlunoDomain)

    /** Quanto o aluno tem no aparelho: o que [delete] apagaria junto. */
    fun observeResumo(alunoId: Long): Flow<ResumoDoAluno>
}
