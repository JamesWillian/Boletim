package app.jammes.boletim.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import kotlin.time.Instant

@Entity(
    tableName = "avaliacao",
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
        Index(value = ["periodo_id"])
    ]
)
data class AvaliacaoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "disciplina_id") val disciplinaId: Long,
    @ColumnInfo(name = "periodo_id") val periodoId: Long?,
    @ColumnInfo(name = "nome") val nome: String,
    @ColumnInfo(name = "nota") val nota: Double,
    @ColumnInfo(name = "nota_maxima") val notaMaxima: Double,
    @ColumnInfo(name = "peso") val peso: Double,
    @ColumnInfo(name = "tipo") val tipo: String = "NORMAL",
    @ColumnInfo(name = "data") val data: LocalDate?,
    @ColumnInfo(name = "created_at") val criadoEm: Instant
)
