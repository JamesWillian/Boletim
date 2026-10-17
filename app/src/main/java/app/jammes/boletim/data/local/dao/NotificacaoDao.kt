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
    @Query("SELECT * FROM notificacao ORDER BY criada_em DESC, id DESC")
    fun observarTodas(): Flow<List<NotificacaoEntity>>

    @Query("SELECT COUNT(*) FROM notificacao WHERE lida = 0")
    fun observarNaoLidas(): Flow<Int>

    @Query("UPDATE notificacao SET lida = 1 WHERE id IN (:ids)")
    suspend fun marcarComoLidas(ids: List<Long>)

    @Query("SELECT * FROM notificacao")
    suspend fun todas(): List<NotificacaoEntity>

    @Query("DELETE FROM notificacao")
    suspend fun apagarTodas()

    // IGNORE: uma que já esteja no banco (o Desfazer chamado duas vezes) fica como está
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(notificacoes: List<NotificacaoEntity>)
}
