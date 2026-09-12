package app.jammes.boletim.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Instant

@Entity(tableName = "aluno")
data class AlunoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id : Long = 0L,
    @ColumnInfo(name = "nome") val nome : String,
    @ColumnInfo(name = "avatar") val avatar : String?,
    @ColumnInfo(name = "ativo") val ativo: Int = 1,
    @ColumnInfo(name = "created_at") val criadoEm: Instant
)
