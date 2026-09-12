package app.jammes.boletim.domain.model

import java.time.LocalDate

data class FaltaDomain(
    val id: Long = 0L,
    val disciplinaId: Long,
    val periodoId: Long,
    val data: LocalDate,
    val qtdAulas: Int = 1
)
