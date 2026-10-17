package app.jammes.boletim.domain.model

import java.time.LocalTime
import java.time.temporal.ChronoUnit

/*
 * Os horários do dia em que as notificações chegam. A lista fica sempre em ordem, sem repetir e com
 * pelo menos um: sem horário nenhum, nenhum aviso chegaria, e para isso já existe a chave de desligar.
 */

/** O padrão: um só, às 19h, quando quase ninguém está em aula. */
val HORARIOS_PADRAO: List<LocalTime> = listOf(LocalTime.of(19, 0))

/** Até 3 por dia, um para cada parte do dia: mais que isso vira barulho. */
const val MAXIMO_DE_HORARIOS = 3

/**
 * Os horários com [novo] no lugar de [antigo], ou com ele a mais quando [antigo] é null. Fica em
 * ordem e só com horas e minutos. Se o novo já está na lista, ou se ela já tem o máximo, nada muda.
 */
fun List<LocalTime>.comHorario(novo: LocalTime, antigo: LocalTime? = null): List<LocalTime> {
    val restantes = if (antigo == null) this else this - antigo
    val horario = novo.truncatedTo(ChronoUnit.MINUTES)
    return when {
        horario in restantes -> this
        restantes.size >= MAXIMO_DE_HORARIOS -> this
        else -> (restantes + horario).sorted()
    }
}

/** Os horários sem [horario]. O último não sai. */
fun List<LocalTime>.semHorario(horario: LocalTime): List<LocalTime> =
    if (size <= 1) this else this - horario
