package app.jammes.boletim.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "periodo",
    foreignKeys = [
        ForeignKey(
            entity = AnoLetivoEntity::class,
            parentColumns = ["id"],
            childColumns = ["ano_letivo_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class PeriodoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id : String,
    @ColumnInfo(name = "periodo") val periodo : Int,
    @ColumnInfo(name = "ano_letivo_id") val anoLetivoId : String
)
