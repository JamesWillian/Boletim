package app.jammes.boletim.domain.model

import kotlin.time.Instant

/**
 * Um aviso do app para a pessoa, como os que a tela de notificações lista. [lida] passa a true
 * quando ele aparece nessa tela; até lá, o sino do topo fica com o ponto azul.
 */
data class NotificacaoDomain(
    val id: Long = 0L,
    val titulo: String,
    val texto: String,
    val criadaEm: Instant,
    val lida: Boolean = false,
)
