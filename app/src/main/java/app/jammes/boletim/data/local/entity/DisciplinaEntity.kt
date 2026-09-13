package app.jammes.boletim.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "disciplina",
    foreignKeys = [
        ForeignKey(
            entity = MateriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["materia_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AnoLetivoEntity::class,
            parentColumns = ["id"],
            childColumns = ["ano_letivo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["materia_id","ano_letivo_id"], unique = true)]
)
data class DisciplinaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id : Long = 0L,
    @ColumnInfo(name = "materia_id") val materiaId: Long,
    @ColumnInfo(name = "ano_letivo_id") val anoLetivoId: Long,
    @ColumnInfo(name = "nome") val nome: String,
    @ColumnInfo(name = "cor") val cor: Int = 0,
    @ColumnInfo(name = "total_aulas") val totalAulas: Int?,
    @ColumnInfo(name = "professor") val professor: String?,
    @ColumnInfo(name = "periodo_inicio") val periodoInicio: Long?,
    @ColumnInfo(name = "periodo_fim") val periodoFim: Long?,
    @ColumnInfo(name = "ativa") val ativa: Int = 1,
    @ColumnInfo(name = "ordem") val ordem: Int = 0
)

data class DisciplinaComDados(
    @Embedded val disciplina: DisciplinaEntity,

    @Relation(parentColumn = "id", entityColumn = "disciplina_id")
    val avaliacoes: List<AvaliacaoEntity>,

    @Relation(parentColumn = "id", entityColumn = "disciplina_id")
    val faltas: List<FaltaEntity>
)
