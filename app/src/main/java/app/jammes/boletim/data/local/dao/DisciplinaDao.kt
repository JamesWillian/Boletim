package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import app.jammes.boletim.data.local.entity.DisciplinaEntity

@Dao
interface DisciplinaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(disciplina: DisciplinaEntity)

    @Update
    suspend fun update(disciplina: DisciplinaEntity)

    @Delete
    suspend fun delete(disciplina: DisciplinaEntity)
}