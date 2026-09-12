package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.jammes.boletim.data.local.entity.RegraAvaliacaoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegraAvaliacaoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(regraAvaliacao: RegraAvaliacaoEntity): Long

    @Update
    suspend fun update(regraAvaliacao: RegraAvaliacaoEntity)

    @Delete
    suspend fun delete(regraAvaliacao: RegraAvaliacaoEntity)

    @Query("SELECT * FROM regra_avaliacao WHERE ano_letivo_id = :anoLetivoId")
    fun observarPorAnoLetivo(anoLetivoId: Long): Flow<List<RegraAvaliacaoEntity>>

    @Query("SELECT * FROM regra_avaliacao WHERE disciplina_id = :disciplinaId")
    fun observarPorDisciplina(disciplinaId: Long): Flow<List<RegraAvaliacaoEntity>>
}