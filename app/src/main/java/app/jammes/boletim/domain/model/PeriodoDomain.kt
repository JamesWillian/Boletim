package app.jammes.boletim.domain.model

import java.time.LocalDate

data class PeriodoDomain(
    val id : Long = 0L,
    val anoLetivoId : Long,
    val periodo : Int,
    val dataInicio: LocalDate,
    val dataFim: LocalDate
)
