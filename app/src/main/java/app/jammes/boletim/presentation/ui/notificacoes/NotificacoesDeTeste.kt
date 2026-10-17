package app.jammes.boletim.presentation.ui.notificacoes

import app.jammes.boletim.domain.model.NotificacaoDomain
import java.time.ZoneId
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Exemplos para ver a tela de notificações enquanto o app ainda não cria as de verdade. Entram pelo
 * botão que só o build de debug mostra, na lista vazia. Ficam espalhados por hoje, ontem, a semana e
 * o ano passado, para aparecer cada jeito de dar nome ao dia; as duas mais recentes chegam não lidas.
 */
internal fun notificacoesDeTeste(agora: Instant, zona: ZoneId): List<NotificacaoDomain> {
    val hoje = java.time.Instant.ofEpochMilli(agora.toEpochMilliseconds()).atZone(zona).toLocalDate()

    // Um horário redondo, alguns dias atrás
    fun diasAtras(dias: Long, hora: Int, minuto: Int = 0): Instant {
        val quando = hoje.minusDays(dias).atTime(hora, minuto).atZone(zona).toInstant()
        return Instant.fromEpochMilliseconds(quando.toEpochMilli())
    }

    return listOf(
        NotificacaoDomain(
            titulo = "Prova de Matemática amanhã",
            texto = "Capítulos 3 e 4. Separe a calculadora.",
            criadaEm = agora - 2.minutes,
        ),
        NotificacaoDomain(
            titulo = "Lance a nota do Trabalho de História",
            texto = "O trabalho foi entregue ontem. A nota já saiu?",
            criadaEm = agora - 3.hours,
        ),
        NotificacaoDomain(
            titulo = "Ed. Física perto do limite de faltas",
            texto = "Faltam 2 faltas para o limite do ano.",
            criadaEm = diasAtras(1, 19),
            lida = true,
        ),
        NotificacaoDomain(
            titulo = "Média de Português abaixo da mínima",
            texto = "A média do 3º Bimestre está em 5,5, e a mínima é 6,0.",
            criadaEm = diasAtras(3, 19),
            lida = true,
        ),
        NotificacaoDomain(
            titulo = "Hoje tem seminário de Geografia",
            texto = "Às 10h, com a turma toda.",
            criadaEm = diasAtras(3, 6, 30),
            lida = true,
        ),
        NotificacaoDomain(
            titulo = "O 3º Bimestre termina em 5 dias",
            texto = "Confira se todas as notas já foram lançadas.",
            criadaEm = diasAtras(12, 19),
            lida = true,
        ),
        NotificacaoDomain(
            titulo = "Boas-vindas ao Boletim",
            texto = "Os avisos do app aparecem aqui.",
            criadaEm = diasAtras(400, 19),
            lida = true,
        ),
    )
}
