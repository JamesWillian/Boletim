package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.jammes.boletim.data.local.entity.BoletimEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BoletimDao {

    @Query("""
        SELECT b.id, b.disciplina_id AS disciplinaId, d.nome AS disciplina, b.periodo_id AS periodoId, b.nota 
        FROM boletim b 
        INNER JOIN disciplina d ON d.id = b.disciplina_id
        WHERE b.periodo_id = :periodoId
        """)
    fun observeBoletimPeriodo(periodoId: String): Flow<List<BoletimPeriodo>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(boletim: BoletimEntity)

    @Update
    suspend fun update(boletim: BoletimEntity)

    @Delete
    suspend fun delete(boletim: BoletimEntity)
}

data class BoletimPeriodo(
    val id: String,
    val disciplinaId: String,
    val disciplina: String,
    val periodoId: String,
    val nota: Double
)