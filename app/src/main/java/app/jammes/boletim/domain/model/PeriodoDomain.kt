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

/** Os períodos de um filtro: só o escolhido, ou todos quando o periodoId é null (o ano letivo inteiro). */
fun List<PeriodoDomain>.noFiltro(periodoId: Long?): List<PeriodoDomain> =
    filter { periodoId == null || it.id == periodoId }

/** Do primeiro ao último dia dos períodos, com as férias entre eles. null quando não há nenhum. */
fun List<PeriodoDomain>.intervalo(): ClosedRange<LocalDate>? =
    if (isEmpty()) null else minOf { it.dataInicio }..maxOf { it.dataFim }
