package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.jammes.boletim.data.local.entity.AlunoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlunoDao {

    @Query("SELECT * FROM aluno")
    fun fetchAll(): Flow<List<AlunoEntity>>

    @Query("SELECT * FROM aluno LIMIT 1")
    fun fetchFirst(): Flow<AlunoEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(aluno: AlunoEntity): Long

    @Update
    suspend fun update(aluno: AlunoEntity)

    @Delete
    suspend fun delete(aluno: AlunoEntity)
}