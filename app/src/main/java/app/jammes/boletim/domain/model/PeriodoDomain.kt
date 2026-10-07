package app.jammes.boletim.domain.model

import java.time.LocalDate

data class PeriodoDomain(
    val id : Long = 0L,
    val anoLetivoId : Long,
    val periodo : Int,
    val dataInicio: LocalDate,
    val dataFim: LocalDate
)

/**
 * O período em que a data cai. Fora de todos (férias, recesso), o último que já começou;
 * antes do ano começar, o primeiro. A lista vem em ordem, como o Room entrega.
 */
fun List<PeriodoDomain>.periodoDe(data: LocalDate): PeriodoDomain? =
    firstOrNull { data in it.dataInicio..it.dataFim }
        ?: lastOrNull { it.dataInicio <= data }
        ?: firstOrNull()
