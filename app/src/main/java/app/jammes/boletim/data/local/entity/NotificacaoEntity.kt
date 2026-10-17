package app.jammes.boletim.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Instant

/**
 * Um aviso do app sobre um aluno, guardado para a tela de notificações. Sai junto com o aluno, e
 * por isso também quando um backup é importado, porque a importação apaga os alunos de antes.
 *
 * Fica fora do backup em arquivo: é um aviso deste aparelho, e não um dado da escola.
 */
@Entity(
    tableName = "notificacao",
    foreignKeys = [
        ForeignKey(
            entity = AlunoEntity::class,
            parentColumns = ["id"],
            childColumns = ["aluno_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        // Uma só por aluno com a mesma chave: o mesmo aviso gerado de novo não é gravado outra vez
        Index(value = ["aluno_id", "chave"], unique = true)
    ]
)
data class NotificacaoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "aluno_id") val alunoId: Long,
    @ColumnInfo(name = "tipo") val tipo: String,
    @ColumnInfo(name = "chave") val chave: String,
    @ColumnInfo(name = "titulo") val titulo: String,
    @ColumnInfo(name = "texto") val texto: String,
    @ColumnInfo(name = "criada_em") val criadaEm: Instant,
    @ColumnInfo(name = "lida") val lida: Boolean = false,
    @ColumnInfo(name = "avisada") val avisada: Boolean = false,
)
