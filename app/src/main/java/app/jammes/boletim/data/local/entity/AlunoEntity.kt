package app.jammes.boletim.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aluno")
data class AlunoEntity(
    @PrimaryKey
    val id : String,
    val nome : String
)
