package app.jammes.boletim.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "disciplina")
data class DisciplinaEntity(
    @PrimaryKey
    val id : String,
    val nome : String
)
