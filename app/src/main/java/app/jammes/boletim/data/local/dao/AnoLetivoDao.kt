package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import app.jammes.boletim.data.local.entity.AnoLetivoEntity

@Dao
interface AnoLetivoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(anoLetivo: AnoLetivoEntity): String

    @Update
    suspend fun update(anoLetivo: AnoLetivoEntity)

    @Delete
    suspend fun delete(anoLetivo: AnoLetivoEntity)
}