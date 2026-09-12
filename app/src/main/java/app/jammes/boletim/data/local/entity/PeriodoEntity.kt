package app.jammes.boletim.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "periodo",
    foreignKeys = [
        ForeignKey(
            entity = AnoLetivoEntity::class,
            parentColumns = ["id"],
            childColumns = ["ano_letivo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ano_letivo_id","periodo"], unique = true)]
)
data class PeriodoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "ano_letivo_id") val anoLetivoId: Long,
    @ColumnInfo(name = "periodo") val periodo: Int,
    @ColumnInfo(name = "data_inicio") val dataInicio: LocalDate,
    @ColumnInfo(name = "data_fim") val dataFim: LocalDate
)
