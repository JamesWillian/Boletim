package app.jammes.boletim.presentation.ui.notificacoes

import app.jammes.boletim.domain.model.NotificacaoDomain
import app.jammes.boletim.domain.model.TipoNotificacao

/**
 * O que o toque numa notificação da lista abre. Antes, o app passa para o [anoLetivoId] e para o
 * filtro de [periodoId] (null: o ano letivo inteiro), porque a tela de destino mostra o que está nele.
 */
sealed interface DestinoDaNotificacao {
    val anoLetivoId: Long
    val periodoId: Long?

    /** O detalhe da disciplina, já com a avaliação aberta no formulário, ou na aba das faltas. */
    data class Disciplina(
        override val anoLetivoId: Long,
        override val periodoId: Long?,
        val disciplinaId: Long,
        val avaliacaoId: Long? = null,
        val abaDeFaltas: Boolean = false,
    ) : DestinoDaNotificacao

    /** O Boletim Geral, no período do aviso. */
    data class Boletim(
        override val anoLetivoId: Long,
        override val periodoId: Long?,
    ) : DestinoDaNotificacao
}

/**
 * Para onde a notificação leva, pelo tipo dela:
 * - a avaliação chegando e a nota pendente abrem a avaliação no formulário, no período dela;
 * - a média abaixo abre a disciplina no período da média;
 * - as faltas abrem a aba das faltas, no ano letivo inteiro, porque o limite é do ano;
 * - o fim do período abre o Boletim Geral nele.
 * Um aviso geral, ou um que não tem o que abrir, não leva a lugar nenhum: null.
 */
internal fun destinoDa(notificacao: NotificacaoDomain): DestinoDaNotificacao? {
    val ano = notificacao.anoLetivoId ?: return null
    return when (notificacao.tipo) {
        TipoNotificacao.AVALIACAO, TipoNotificacao.NOTA -> DestinoDaNotificacao.Disciplina(
            anoLetivoId = ano,
            periodoId = notificacao.periodoId,
            disciplinaId = notificacao.disciplinaId ?: return null,
            avaliacaoId = notificacao.avaliacaoId,
        )
        TipoNotificacao.MEDIA -> DestinoDaNotificacao.Disciplina(
            anoLetivoId = ano,
            periodoId = notificacao.periodoId,
            disciplinaId = notificacao.disciplinaId ?: return null,
        )
        TipoNotificacao.FALTAS -> DestinoDaNotificacao.Disciplina(
            anoLetivoId = ano,
            periodoId = null,
            disciplinaId = notificacao.disciplinaId ?: return null,
            abaDeFaltas = true,
        )
        TipoNotificacao.PERIODO -> DestinoDaNotificacao.Boletim(
            anoLetivoId = ano,
            periodoId = notificacao.periodoId ?: return null,
        )
        TipoNotificacao.AVISO -> null
    }
}
