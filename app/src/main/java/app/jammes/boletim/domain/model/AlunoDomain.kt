package app.jammes.boletim.domain.model

import kotlin.time.Instant

data class AlunoDomain(
    val id : Long = 0L,
    val nome : String,
    val avatar: String? = null,
    val ativo: Int = 1,
    val criadoEm: Instant
)
