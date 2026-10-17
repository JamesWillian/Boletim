package app.jammes.boletim.domain.model

import kotlin.time.Instant

/**
 * Um aviso do app sobre um aluno, como os que a tela de notificações lista.
 *
 * - [chave] diz qual é o aviso (por exemplo "avaliacao:42:2026-10-18"): gerado de novo para o mesmo
 *   aluno, ele não é gravado outra vez.
 * - [lida] passa a true quando ele aparece na tela de notificações; até lá, o sino do topo fica com o
 *   ponto azul.
 * - [avisada] passa a true quando ele entra no resumo da notificação do celular, para não ser avisado
 *   de novo no horário seguinte.
 */
data class NotificacaoDomain(
    val id: Long = 0L,
    val alunoId: Long,
    val tipo: TipoNotificacao,
    val chave: String,
    val titulo: String,
    val texto: String,
    val criadaEm: Instant,
    val lida: Boolean = false,
    val avisada: Boolean = false,
)
