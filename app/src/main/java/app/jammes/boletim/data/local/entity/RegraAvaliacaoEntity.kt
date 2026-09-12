package app.jammes.boletim.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "regra_avaliacao",
    foreignKeys = [
        ForeignKey(
            entity = PeriodoEntity::class,
            parentColumns = ["id"],
            childColumns = ["ano_letivo_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DisciplinaEntity::class,
            parentColumns = ["id"],
            childColumns = ["disciplina_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ano_letivo_id"]),
        Index(value = ["disciplina_id"]),
    ]
)
data class RegraAvaliacaoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "ano_letivo_id") val anoLetivoId: Long,
    @ColumnInfo(name = "disciplina_id") val disciplinaId: Long?,
    @ColumnInfo(name = "media_minima") val mediaMinima: Double = 7.0,
    @ColumnInfo(name = "media_recuperacao") val mediaRecuperacao: Double? = 5.0,
    @ColumnInfo(name = "frequencia_minima") val frequenciaMinima: Double = 75.0,
    @ColumnInfo(name = "tipo_media") val tipoMedia: String = "PONDERADA",
    @ColumnInfo(name = "arredondamento") val arredondamento: String = "NENHUM",
    @ColumnInfo(name = "descartar_menor_nota") val descartarMenorNota: Int = 0
)
