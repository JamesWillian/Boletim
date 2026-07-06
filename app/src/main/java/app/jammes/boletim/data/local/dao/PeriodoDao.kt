package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.jammes.boletim.data.local.entity.AnoLetivoEntity
import app.jammes.boletim.data.local.entity.PeriodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodoDao {

    @Query("SELECT id, periodo, ano_letivo_id FROM periodo WHERE ano_letivo_id = :anoLetivoId ORDER BY periodo ASC")
    fun fetchByAnoLetivo(anoLetivoId: String): Flow<List<PeriodoEntity>>

    @Query("SELECT id, periodo, ano_letivo_id FROM periodo WHERE id = :id")
    fun fetchById(id: String): Flow<PeriodoEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(periodo: PeriodoEntity)

    @Update
    suspend fun update(periodo: PeriodoEntity)

    @Delete
    suspend fun delete(periodo: PeriodoEntity)
}