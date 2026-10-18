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
 * O ano letivo, o período, a disciplina e a avaliação dizem o que o toque na notificação abre. São
 * opcionais (um aviso geral não aponta para nada), e a notificação sai junto com o que ela aponta:
 * uma avaliação apagada leva o aviso dela.
 *
 * O Limpar só marca como [apagada]: a notificação sai da lista, mas a chave continua no banco, e o
 * mesmo aviso não volta no horário seguinte, enquanto o que ele avisa continua valendo.
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
        ),
        ForeignKey(
            entity = AnoLetivoEntity::class,
            parentColumns = ["id"],
            childColumns = ["ano_letivo_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PeriodoEntity::class,
            parentColumns = ["id"],
            childColumns = ["periodo_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DisciplinaEntity::class,
            parentColumns = ["id"],
            childColumns = ["disciplina_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AvaliacaoEntity::class,
            parentColumns = ["id"],
            childColumns = ["avaliacao_id"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        // Uma só por aluno com a mesma chave: o mesmo aviso gerado de novo não é gravado outra vez
        Index(value = ["aluno_id", "chave"], unique = true),
        Index(value = ["ano_letivo_id"]),
        Index(value = ["periodo_id"]),
        Index(value = ["disciplina_id"]),
        Index(value = ["avaliacao_id"]),
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
    @ColumnInfo(name = "apagada") val apagada: Boolean = false,
    @ColumnInfo(name = "ano_letivo_id") val anoLetivoId: Long? = null,
    @ColumnInfo(name = "periodo_id") val periodoId: Long? = null,
    @ColumnInfo(name = "disciplina_id") val disciplinaId: Long? = null,
    @ColumnInfo(name = "avaliacao_id") val avaliacaoId: Long? = null,
)
