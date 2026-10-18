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

    /**
     * O Limpar: tira da lista as do aluno e devolve as que tirou, para o Desfazer pôr de volta. As dos
     * outros alunos ficam. Elas continuam guardadas, sem aparecer em lugar nenhum: o mesmo aviso, gerado
     * de novo no horário seguinte, não volta.
     */
    suspend fun limparDoAluno(alunoId: Long): List<NotificacaoDomain>

    /** O Desfazer do Limpar: as [ids] voltam para a lista, iguais, no mesmo lugar. */
    suspend fun restaurar(ids: Collection<Long>)

    /**
     * Grava as [notificacoes], cada uma com um id novo, e diz quantas entraram. Uma com a chave de
     * outra do mesmo aluno não entra, mesmo que a outra tenha saído da lista pelo Limpar: esse aviso
     * já foi dado.
     */
    suspend fun inserir(notificacoes: List<NotificacaoDomain>): Int

    /** As que ainda não foram lidas, de todos os alunos, da mais nova para a mais antiga. */
    suspend fun todasNaoLidas(): List<NotificacaoDomain>

    /** As que ainda não entraram na notificação do celular, de todos os alunos. */
    suspend fun naoAvisadas(): List<NotificacaoDomain>

    suspend fun marcarComoAvisadas(ids: Collection<Long>)
}
