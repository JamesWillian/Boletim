package app.jammes.boletim.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "falta",
    foreignKeys = [
        ForeignKey(
            entity = DisciplinaEntity::class,
            parentColumns = ["id"],
            childColumns = ["disciplina_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PeriodoEntity::class,
            parentColumns = ["id"],
            childColumns = ["periodo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["disciplina_id","periodo_id"]),
        Index(value = ["data"])
    ]
)
data class FaltaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "disciplina_id") val disciplinaId: Long,
    @ColumnInfo(name = "periodo_id") val periodoId: Long,
    @ColumnInfo(name = "data") val data: LocalDate,
    @ColumnInfo(name = "qtd_aulas") val qtdAulas: Int = 1
)
