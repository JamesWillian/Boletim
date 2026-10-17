package app.jammes.boletim.data.repository

import androidx.room.withTransaction
import app.jammes.boletim.data.local.AppDatabase
import app.jammes.boletim.data.local.dao.NotificacaoDao
import app.jammes.boletim.data.mapper.toDomain
import app.jammes.boletim.data.mapper.toEntity
import app.jammes.boletim.domain.model.NotificacaoDomain
import app.jammes.boletim.domain.repository.NotificacaoRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificacaoRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val dao: NotificacaoDao,
) : NotificacaoRepository {

    override fun notificacoes(alunoId: Long): Flow<List<NotificacaoDomain>> =
        dao.observarDoAluno(alunoId).map { lista -> lista.map { it.toDomain() } }

    override fun naoLidas(alunoId: Long): Flow<Int> = dao.observarNaoLidas(alunoId)

    override suspend fun marcarComoLidas(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        // Aos pedaços, porque o SQLite dos Androids mais antigos aceita no máximo 999 valores num IN.
        // Numa transação só, a lista e o sino mudam uma vez
        db.withTransaction {
            ids.chunked(TAMANHO_DO_PEDACO).forEach { dao.marcarComoLidas(it) }
        }
    }

    override suspend fun apagarDoAluno(alunoId: Long): List<NotificacaoDomain> = db.withTransaction {
        val apagadas = dao.doAluno(alunoId)
        dao.apagarDoAluno(alunoId)
        apagadas.map { it.toDomain() }
    }

    override suspend fun inserir(notificacoes: List<NotificacaoDomain>) {
        dao.inserir(notificacoes.map { it.toEntity() })
    }
}

private const val TAMANHO_DO_PEDACO = 500
