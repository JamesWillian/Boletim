package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.jammes.boletim.data.local.entity.MateriaEntity
import app.jammes.boletim.data.local.entity.UsoDaMateria
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

    // Um ano letivo tem no máximo uma disciplina de cada matéria (índice único): contar as disciplinas é contar os anos
    @Query("SELECT materia_id, COUNT(*) AS anos FROM disciplina GROUP BY materia_id")
    fun observarAnosEmUso(): Flow<List<UsoDaMateria>>
}