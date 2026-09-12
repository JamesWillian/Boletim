package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.jammes.boletim.data.local.entity.FaltaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FaltaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(falta: FaltaEntity): Long

    @Update
    suspend fun update(falta: FaltaEntity)

    @Delete
    suspend fun delete(falta: FaltaEntity)

    @Query("SELECT * FROM falta WHERE disciplina_id = :disciplinaId ORDER BY data, id")
    fun observarPorDisciplina(disciplinaId: Long): Flow<List<FaltaEntity>>
}