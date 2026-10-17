package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.jammes.boletim.data.local.entity.NotificacaoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacaoDao {

    // As mais novas primeiro; no mesmo instante, a gravada por último
    @Query("SELECT * FROM notificacao WHERE aluno_id = :alunoId ORDER BY criada_em DESC, id DESC")
    fun observarDoAluno(alunoId: Long): Flow<List<NotificacaoEntity>>

    @Query("SELECT COUNT(*) FROM notificacao WHERE aluno_id = :alunoId AND lida = 0")
    fun observarNaoLidas(alunoId: Long): Flow<Int>

    @Query("UPDATE notificacao SET lida = 1 WHERE id IN (:ids)")
    suspend fun marcarComoLidas(ids: List<Long>)

    @Query("SELECT * FROM notificacao WHERE aluno_id = :alunoId")
    suspend fun doAluno(alunoId: Long): List<NotificacaoEntity>

    @Query("DELETE FROM notificacao WHERE aluno_id = :alunoId")
    suspend fun apagarDoAluno(alunoId: Long)

    // IGNORE: uma que já esteja no banco fica como está, seja pelo id (o Desfazer chamado duas vezes),
    // seja pela chave (o mesmo aviso gerado de novo para o aluno)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(notificacoes: List<NotificacaoEntity>)
}
