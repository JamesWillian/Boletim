package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.jammes.boletim.data.local.entity.AnoLetivoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnoLetivoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(anoLetivo: AnoLetivoEntity): Long

    @Update
    suspend fun update(anoLetivo: AnoLetivoEntity)

    @Delete
    suspend fun delete(anoLetivo: AnoLetivoEntity)

    @Query("SELECT * FROM ano_letivo WHERE aluno_id = :alunoId ORDER BY ano DESC")
    fun observarPorAluno(alunoId: Long): Flow<List<AnoLetivoEntity>>
}