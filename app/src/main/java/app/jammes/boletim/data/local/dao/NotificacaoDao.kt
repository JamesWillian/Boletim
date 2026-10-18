package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.jammes.boletim.data.local.entity.NotificacaoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacaoDao {

    // As apagadas pelo Limpar ficam no banco, pela chave, mas fora de tudo o que aparece

    // As mais novas primeiro; no mesmo instante, a gravada por último
    @Query("SELECT * FROM notificacao WHERE aluno_id = :alunoId AND apagada = 0 ORDER BY criada_em DESC, id DESC")
    fun observarDoAluno(alunoId: Long): Flow<List<NotificacaoEntity>>

    @Query("SELECT COUNT(*) FROM notificacao WHERE aluno_id = :alunoId AND lida = 0 AND apagada = 0")
    fun observarNaoLidas(alunoId: Long): Flow<Int>

    @Query("UPDATE notificacao SET lida = 1 WHERE id IN (:ids)")
    suspend fun marcarComoLidas(ids: List<Long>)

    @Query("SELECT * FROM notificacao WHERE aluno_id = :alunoId AND apagada = 0")
    suspend fun doAluno(alunoId: Long): List<NotificacaoEntity>

    @Query("UPDATE notificacao SET apagada = 1 WHERE aluno_id = :alunoId AND apagada = 0")
    suspend fun limparDoAluno(alunoId: Long)

    @Query("UPDATE notificacao SET apagada = 0 WHERE id IN (:ids)")
    suspend fun restaurar(ids: List<Long>)

    // Os pedaços da notificação do celular, de todos os alunos

    @Query("SELECT * FROM notificacao WHERE lida = 0 AND apagada = 0 ORDER BY criada_em DESC, id DESC")
    suspend fun naoLidas(): List<NotificacaoEntity>

    @Query("SELECT * FROM notificacao WHERE avisada = 0 AND apagada = 0")
    suspend fun naoAvisadas(): List<NotificacaoEntity>

    @Query("UPDATE notificacao SET avisada = 1 WHERE id IN (:ids)")
    suspend fun marcarComoAvisadas(ids: List<Long>)

    // IGNORE: o mesmo aviso gerado de novo para o aluno (a mesma chave) fica como está, até apagado
    // pelo Limpar. A que não entrou volta como -1
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(notificacoes: List<NotificacaoEntity>): List<Long>
}
