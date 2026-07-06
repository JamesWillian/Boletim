package app.jammes.boletim.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "aluno",
    foreignKeys = [
        ForeignKey(
            entity = AnoLetivoEntity::class,
            parentColumns = ["id"],
            childColumns = ["ano_letivo_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = PeriodoEntity::class,
            parentColumns = ["id"],
            childColumns = ["periodo_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ])
data class AlunoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id : String,
    @ColumnInfo(name = "nome") val nome : String,
    @ColumnInfo(name = "ano_letivo_id") val anoLetivoId: String? = null,
    @ColumnInfo(name = "periodo_id") val periodoId: String? = null
)
