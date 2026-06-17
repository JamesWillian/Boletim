package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import app.jammes.boletim.data.local.entity.AlunoEntity

@Dao
interface AlunoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(aluno: AlunoEntity): String

    @Update
    suspend fun update(aluno: AlunoEntity)

    @Delete
    suspend fun delete(aluno: AlunoEntity)
}