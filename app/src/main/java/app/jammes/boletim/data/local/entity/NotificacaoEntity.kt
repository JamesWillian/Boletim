package app.jammes.boletim.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Instant

/**
 * Um aviso do app, guardado para a tela de notificações. Não aponta para aluno nem disciplina: o
 * título e o texto já dizem do que se trata.
 *
 * Fica fora do backup em arquivo: é um aviso deste aparelho, e não um dado da escola.
 */
@Entity(tableName = "notificacao")
data class NotificacaoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "titulo") val titulo: String,
    @ColumnInfo(name = "texto") val texto: String,
    @ColumnInfo(name = "criada_em") val criadaEm: Instant,
    @ColumnInfo(name = "lida") val lida: Boolean = false,
)
