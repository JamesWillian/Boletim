package app.jammes.boletim.domain.repository

/**
 * Agrupa gravações de vários repositórios: ou o bloco inteiro fica gravado, ou nada fica.
 * Se ele lançar exceção no meio, o que já tinha sido escrito é desfeito.
 */
interface Transacao {
    suspend operator fun <T> invoke(bloco: suspend () -> T): T
}
