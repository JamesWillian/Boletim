package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.jammes.boletim.data.local.entity.MateriaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MateriaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(materia: MateriaEntity): Long

    @Update
    suspend fun update(materia: MateriaEntity)

    @Delete
    suspend fun delete(materia: MateriaEntity)

    @Query("SELECT * FROM materia ORDER BY id")
    fun observar(): Flow<List<MateriaEntity>>
}