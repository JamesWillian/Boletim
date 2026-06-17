package app.jammes.boletim.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "boletim",
    foreignKeys = [
        ForeignKey(
            entity = DisciplinaEntity::class,
            parentColumns = ["id"],
            childColumns = ["disciplina_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PeriodoEntity::class,
            parentColumns = ["id"],
            childColumns = ["periodo_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["disciplina_id","periodo_id"], unique = true)]
)
data class BoletimEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id : String,
    @ColumnInfo(name = "disciplina_id") val disciplinaId : String,
    @ColumnInfo(name = "periodo_id") val periodoId : String,
    @ColumnInfo(name = "nota") val nota : Double
)
