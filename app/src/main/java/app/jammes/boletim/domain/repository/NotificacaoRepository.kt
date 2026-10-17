package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.NotificacaoDomain
import kotlinx.coroutines.flow.Flow

/** As notificações guardadas no aparelho, de todos os alunos. */
interface NotificacaoRepository {

    /** Todas, da mais nova para a mais antiga. */
    val notificacoes: Flow<List<NotificacaoDomain>>

    /** Quantas ainda não foram lidas: o que acende o ponto azul do sino. */
    val naoLidas: Flow<Int>

    suspend fun marcarComoLidas(ids: Collection<Long>)

    /** Apaga todas e devolve as que apagou, para o Desfazer pôr de volta. */
    suspend fun apagarTodas(): List<NotificacaoDomain>

    /**
     * Grava as [notificacoes]. Com id 0, cada uma ganha um id novo; com o id de antes (o Desfazer do
     * Limpar), ela volta igual, no mesmo lugar da lista.
     */
    suspend fun inserir(notificacoes: List<NotificacaoDomain>)
}
