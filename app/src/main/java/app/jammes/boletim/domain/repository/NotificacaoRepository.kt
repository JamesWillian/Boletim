package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.NotificacaoDomain
import kotlinx.coroutines.flow.Flow

/** As notificações guardadas no aparelho. Cada uma é de um aluno, e o app mostra só as do aluno aberto. */
interface NotificacaoRepository {

    /** As do aluno, da mais nova para a mais antiga. */
    fun notificacoes(alunoId: Long): Flow<List<NotificacaoDomain>>

    /** Quantas do aluno ainda não foram lidas: o que acende o ponto azul do sino. */
    fun naoLidas(alunoId: Long): Flow<Int>

    suspend fun marcarComoLidas(ids: Collection<Long>)

    /** Apaga as do aluno e devolve as que apagou, para o Desfazer pôr de volta. As dos outros ficam. */
    suspend fun apagarDoAluno(alunoId: Long): List<NotificacaoDomain>

    /**
     * Grava as [notificacoes]. Com id 0, cada uma ganha um id novo; com o id de antes (o Desfazer do
     * Limpar), ela volta igual, no mesmo lugar da lista. Uma com a chave de outra do mesmo aluno não
     * entra: esse aviso já existe.
     */
    suspend fun inserir(notificacoes: List<NotificacaoDomain>)
}
