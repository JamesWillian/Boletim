package app.jammes.boletim.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ano_letivo",
    foreignKeys = [
        ForeignKey(
            entity = AlunoEntity::class,
            parentColumns = ["id"],
            childColumns = ["aluno_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["aluno_id"])]
)
data class AnoLetivoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "aluno_id") val alunoId: Long,
    @ColumnInfo(name = "ano") val ano: Int,
    @ColumnInfo(name = "serie") val serie: String?,
    @ColumnInfo(name = "tipo_periodo") val tipoPeriodo: String = "UNIDADE",
    @ColumnInfo(name = "qtd_periodos") val qtdPeriodos: Int,
    @ColumnInfo(name = "ativo") val ativo: Int = 0
)
