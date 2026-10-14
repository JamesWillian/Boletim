package app.jammes.boletim.domain.model

import java.time.LocalDate
import kotlin.time.Instant

data class AvaliacaoDomain(
    val id: Long = 0L,
    val disciplinaId: Long,
    val periodoId: Long?,
    val nome: String,
    val nota: Double?, // null: avaliação que ainda não aconteceu
    val notaMaxima: Double,
    val peso: Double,
    val tipo: TipoAvaliacao = TipoAvaliacao.NORMAL,
    val data: LocalDate?,
    val criadoEm: Instant
)

/**
 * A outra avaliação que já ocupa a vaga de uma do [tipo]: em cada disciplina cabe uma recuperação
 * por período e uma final no ano. A de id [alemDe], a que está sendo editada, não conta; numa nova,
 * o id é 0 e nenhuma gravada tem esse. Normal não tem limite: sempre null.
 */
fun List<AvaliacaoDomain>.ocupandoAVaga(tipo: TipoAvaliacao, periodoId: Long?, alemDe: Long): AvaliacaoDomain? =
    when (tipo) {
        TipoAvaliacao.NORMAL -> null
        TipoAvaliacao.RECUPERACAO -> firstOrNull { it.id != alemDe && it.tipo == tipo && it.periodoId == periodoId }
        TipoAvaliacao.FINAL -> firstOrNull { it.id != alemDe && it.tipo == tipo }
    }

/**
 * A final ganhou nota ao ser gravada assim: nova, ou estava agendada, ou acabou de virar final. [antes]
 * é como ela estava (a nova: id 0, sem nota). Mudar a nota de uma final que já tinha nota não conta.
 */
fun AvaliacaoDomain.lancouAFinal(antes: AvaliacaoDomain): Boolean =
    tipo == TipoAvaliacao.FINAL && nota != null && !(antes.tipo == TipoAvaliacao.FINAL && antes.nota != null)

/**
 * Por que uma recuperação ou uma final não cabe agora. Elas existem para quem ficou abaixo da
 * mínima: a recuperação, no período; a final, no ano. Também só cabe uma de cada.
 */
sealed interface Impedimento {
    /** Já tem a recuperação do período, ou a final do ano: a [ocupante]. */
    data class VagaOcupada(val ocupante: AvaliacaoDomain) : Impedimento

    /** O período (ou o ano) ainda não tem média: não dá para saber se ficou abaixo. */
    data object SemMedia : Impedimento

    /** A [media] do período (ou do ano) já alcança a [minima]: não há o que recuperar. */
    data class MediaSuficiente(val media: Double, val minima: Double) : Impedimento
}
