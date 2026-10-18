package app.jammes.boletim.domain.repository

import java.time.ZonedDateTime

/** O despertador do Android que acorda o app no próximo horário das notificações. Só um fica marcado. */
interface AgendaDosAvisos {

    /** Marca o próximo para [quando]; o que estava marcado sai. */
    fun marcar(quando: ZonedDateTime)

    /** Desmarca: as notificações foram desligadas. */
    fun desmarcar()
}
