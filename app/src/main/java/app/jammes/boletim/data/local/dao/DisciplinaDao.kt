package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import app.jammes.boletim.data.local.entity.ContagemLancamentos
import app.jammes.boletim.data.local.entity.DisciplinaComDados
import app.jammes.boletim.data.local.entity.DisciplinaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DisciplinaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(disciplina: DisciplinaEntity): Long

    @Update
    suspend fun update(disciplina: DisciplinaEntity)

    @Delete
    suspend fun delete(disciplina: DisciplinaEntity)

    @Query("SELECT * FROM disciplina WHERE id = :id")
    fun observarPorId(id: Long): Flow<DisciplinaEntity?>

    @Query("SELECT * FROM disciplina WHERE ano_letivo_id = :anoLetivoId ORDER BY ordem")
    fun observarPorAnoLetivo(anoLetivoId: Long): Flow<List<DisciplinaEntity>>

    @Transaction
    @Query("SELECT * FROM disciplina WHERE ano_letivo_id = :anoLetivoId AND ativa = 1 ORDER BY ordem, nome")
    fun observarBoletim(anoLetivoId: Long): Flow<List<DisciplinaComDados>>

    // Todas as disciplinas do ano, inclusive as inativas: elas também têm dados que não podem sumir
    @Query(
        """
        SELECT d.id AS id,
            (SELECT COUNT(*) FROM avaliacao a WHERE a.disciplina_id = d.id) AS avaliacoes,
            (SELECT COALESCE(SUM(f.qtd_aulas), 0) FROM falta f WHERE f.disciplina_id = d.id) AS faltas
        FROM disciplina d
        WHERE d.ano_letivo_id = :anoLetivoId
        """
    )
    suspend fun contarLancamentos(anoLetivoId: Long): List<ContagemLancamentos>
}