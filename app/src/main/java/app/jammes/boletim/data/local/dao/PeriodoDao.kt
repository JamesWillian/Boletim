package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import app.jammes.boletim.data.local.entity.PeriodoEntity

@Dao
interface PeriodoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(periodo: PeriodoEntity)

    @Update
    suspend fun update(periodo: PeriodoEntity)

    @Delete
    suspend fun delete(periodo: PeriodoEntity)
}