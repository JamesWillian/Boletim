package app.jammes.boletim.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ano_letivo")
data class AnoLetivoEntity(
    @PrimaryKey
    val id : String,
    val descricao : String
)
