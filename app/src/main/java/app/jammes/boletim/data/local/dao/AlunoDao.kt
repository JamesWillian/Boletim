package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.jammes.boletim.data.local.entity.AlunoEntity
import app.jammes.boletim.data.local.entity.ContagemDoAluno
import kotlinx.coroutines.flow.Flow

@Dao
interface AlunoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(aluno: AlunoEntity): Long

    @Update
    suspend fun update(aluno: AlunoEntity)

    @Delete
    suspend fun delete(aluno: AlunoEntity)

    @Query("SELECT * FROM aluno")
    fun observar(): Flow<List<AlunoEntity>>

    @Query("SELECT * FROM aluno LIMIT 1")
    fun fetchFirst(): Flow<AlunoEntity?>

    // O que as chaves estrangeiras CASCADE apagam junto com o aluno, para o perfil avisar antes
    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM ano_letivo a WHERE a.aluno_id = :alunoId) AS anos_letivos,
            (SELECT COUNT(*) FROM disciplina d
                JOIN ano_letivo a ON a.id = d.ano_letivo_id
                WHERE a.aluno_id = :alunoId) AS disciplinas,
            (SELECT COUNT(*) FROM avaliacao av
                JOIN disciplina d ON d.id = av.disciplina_id
                JOIN ano_letivo a ON a.id = d.ano_letivo_id
                WHERE a.aluno_id = :alunoId) AS avaliacoes,
            (SELECT COALESCE(SUM(f.qtd_aulas), 0) FROM falta f
                JOIN disciplina d ON d.id = f.disciplina_id
                JOIN ano_letivo a ON a.id = d.ano_letivo_id
                WHERE a.aluno_id = :alunoId) AS faltas
        """
    )
    fun observarContagem(alunoId: Long): Flow<ContagemDoAluno>
}