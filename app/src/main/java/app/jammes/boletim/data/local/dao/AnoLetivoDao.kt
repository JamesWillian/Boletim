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

    @Query("SELECT id, descricao FROM ano_letivo ORDER BY descricao ASC")
    fun fetchAll(): Flow<List<AnoLetivoEntity>>

    @Query("SELECT id, descricao FROM ano_letivo WHERE id = :id")
    fun fetchById(id: String): Flow<AnoLetivoEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(anoLetivo: AnoLetivoEntity)

    @Update
    suspend fun update(anoLetivo: AnoLetivoEntity)

    @Delete
    suspend fun delete(anoLetivo: AnoLetivoEntity)
}