package app.jammes.boletim.presentation.ui.notificacoes

import app.jammes.boletim.domain.model.NotificacaoDomain
import app.jammes.boletim.domain.model.TipoNotificacao
import java.time.ZoneId
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Exemplos para ver a tela de notificações enquanto o app ainda não cria as de verdade. Entram pelo
 * botão que só o build de debug mostra, na lista vazia, para o aluno [alunoId]. Ficam espalhados por
 * hoje, ontem, a semana e o ano passado, para aparecer cada jeito de dar nome ao dia; as duas mais
 * recentes chegam não lidas.
 *
 * Todas já vêm avisadas, para nunca irem parar na notificação do celular. A chave leva o instante, para
 * cada leva de exemplos entrar inteira, sem esbarrar nas chaves da anterior.
 */
internal fun notificacoesDeTeste(alunoId: Long, agora: Instant, zona: ZoneId): List<NotificacaoDomain> {
    val hoje = java.time.Instant.ofEpochMilli(agora.toEpochMilliseconds()).atZone(zona).toLocalDate()

    // Um horário redondo, alguns dias atrás
    fun diasAtras(dias: Long, hora: Int, minuto: Int = 0): Instant {
        val quando = hoje.minusDays(dias).atTime(hora, minuto).atZone(zona).toInstant()
        return Instant.fromEpochMilliseconds(quando.toEpochMilli())
    }

    data class Exemplo(
        val tipo: TipoNotificacao,
        val titulo: String,
        val texto: String,
        val criadaEm: Instant,
        val lida: Boolean = true,
    )

    val exemplos = listOf(
        Exemplo(
            tipo = TipoNotificacao.AVALIACAO,
            titulo = "Prova de Matemática amanhã",
            texto = "Capítulos 3 e 4. Separe a calculadora.",
            criadaEm = agora - 2.minutes,
            lida = false,
        ),
        Exemplo(
            tipo = TipoNotificacao.NOTA,
            titulo = "Lance a nota do Trabalho de História",
            texto = "O trabalho foi entregue ontem. A nota já saiu?",
            criadaEm = agora - 3.hours,
            lida = false,
        ),
        Exemplo(
            tipo = TipoNotificacao.FALTAS,
            titulo = "Ed. Física perto do limite de faltas",
            texto = "Faltam 2 faltas para o limite do ano.",
            criadaEm = diasAtras(1, 19),
        ),
        Exemplo(
            tipo = TipoNotificacao.MEDIA,
            titulo = "Média de Português abaixo da mínima",
            texto = "A média do 3º Bimestre está em 5,5, e a mínima é 6,0.",
            criadaEm = diasAtras(3, 19),
        ),
        Exemplo(
            tipo = TipoNotificacao.AVALIACAO,
            titulo = "Hoje tem seminário de Geografia",
            texto = "Às 10h, com a turma toda.",
            criadaEm = diasAtras(3, 6, 30),
        ),
        Exemplo(
            tipo = TipoNotificacao.PERIODO,
            titulo = "O 3º Bimestre termina em 5 dias",
            texto = "Confira se todas as notas já foram lançadas.",
            criadaEm = diasAtras(12, 19),
        ),
        Exemplo(
            tipo = TipoNotificacao.AVISO,
            titulo = "Boas-vindas ao Boletim Escolar",
            texto = "Os avisos do app aparecem aqui.",
            criadaEm = diasAtras(400, 19),
        ),
    )

    return exemplos.mapIndexed { i, exemplo ->
        NotificacaoDomain(
            alunoId = alunoId,
            tipo = exemplo.tipo,
            chave = "teste:${agora.toEpochMilliseconds()}:$i",
            titulo = exemplo.titulo,
            texto = exemplo.texto,
            criadaEm = exemplo.criadaEm,
            lida = exemplo.lida,
            avisada = true,
        )
    }
}
