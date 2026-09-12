package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.jammes.boletim.data.local.entity.AvaliacaoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AvaliacaoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(avaliacao: AvaliacaoEntity): Long

    @Update
    suspend fun update(avaliacao: AvaliacaoEntity)

    @Delete
    suspend fun delete(avaliacao: AvaliacaoEntity)

    @Query("SELECT * FROM avaliacao WHERE disciplina_id = :disciplinaId ORDER BY data, id")
    fun observarPorDisciplina(disciplinaId: Long): Flow<List<AvaliacaoEntity>>

    @Query("SELECT * FROM avaliacao WHERE periodo_id = :periodoId ORDER BY data, id")
    fun observarPorPeriodo(periodoId: Long): Flow<List<AvaliacaoEntity>>
}