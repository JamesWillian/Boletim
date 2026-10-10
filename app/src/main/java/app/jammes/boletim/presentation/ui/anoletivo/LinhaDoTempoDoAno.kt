package app.jammes.boletim.presentation.ui.anoletivo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.presentation.ui.theme.Espacos
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A divisão do ano de relance: uma faixa de janeiro a dezembro com os períodos por cima. Os vãos
 * entre eles são as férias, e um período com as datas erradas (atropelando o vizinho) fica
 * vermelho, como a linha dele. Embaixo vai o resumo em texto, que é o que o leitor de tela lê.
 *
 * É um Canvas só, com os textos medidos pelo TextMeasurer: os vinte e poucos rótulos, um composable
 * para cada, pesariam na abertura da tela.
 */
@Composable
internal fun LinhaDoTempoDoAno(
    periodos: List<DatasPeriodo>,
    tipo: TipoPeriodo,
    ano: Int?,
    problemas: List<ProblemaDatas?>, // um por período, como o problemasDasDatas devolve
    modifier: Modifier = Modifier,
) {
    val referencia = ano ?: periodos.firstOrNull()?.inicio?.year ?: LocalDate.now().year
    val faixa = remember(periodos, referencia) { faixaDaLinhaDoTempo(periodos, referencia) }
    val meses = remember(faixa) { mesesDaFaixa(faixa) }
    val medidor = rememberTextMeasurer()

    val trilho = MaterialTheme.colorScheme.surfaceContainerHigh
    val corDoPeriodo = MaterialTheme.colorScheme.secondaryContainer
    val textoDoPeriodo = MaterialTheme.colorScheme.onSecondaryContainer
    val corDoErro = MaterialTheme.colorScheme.errorContainer
    val textoDoErro = MaterialTheme.colorScheme.onErrorContainer
    val corDoMes = MaterialTheme.colorScheme.onSurfaceVariant
    val estiloDoPeriodo = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
    val estiloDoMes = MaterialTheme.typography.labelSmall

    Column(modifier) {
        // O resumo embaixo já diz tudo para o leitor de tela; o desenho fica de fora
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(ALTURA_DA_FAIXA + ALTURA_DOS_MESES)
                .clearAndSetSemantics {}
        ) {
            val altura = ALTURA_DA_FAIXA.toPx()
            drawRoundRect(trilho, size = Size(size.width, altura), cornerRadius = CornerRadius(8.dp.toPx()))

            periodos.forEachIndexed { i, datas ->
                if (datas.fim < datas.inicio) return@forEachIndexed // não tem largura para desenhar
                val x0 = size.width * fracaoDaFaixa(datas.inicio, faixa)
                val x1 = size.width * fracaoDaFaixa(datas.fim.plusDays(1), faixa)
                val fio = 1.dp.toPx() // separa dois períodos colados
                val margem = 3.dp.toPx()
                val comProblema = problemas.getOrNull(i) != null
                drawRoundRect(
                    color = if (comProblema) corDoErro else corDoPeriodo,
                    topLeft = Offset(x0 + fio, margem),
                    size = Size((x1 - x0 - 2 * fio).coerceAtLeast(0f), altura - 2 * margem),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
                // O "1ª" só entra se couber no período
                val rotulo = medidor.measure(ordinalDoPeriodo(i + 1, tipo), estiloDoPeriodo)
                if (rotulo.size.width + 4.dp.toPx() < x1 - x0) {
                    drawText(
                        textLayoutResult = rotulo,
                        color = if (comProblema) textoDoErro else textoDoPeriodo,
                        topLeft = Offset((x0 + x1 - rotulo.size.width) / 2, (altura - rotulo.size.height) / 2),
                    )
                }
            }

            // A inicial de cada mês, embaixo do meio dele
            meses.forEach { mes ->
                val meio = size.width * (fracaoDaFaixa(mes.inicio, faixa) + fracaoDaFaixa(mes.fim.plusDays(1), faixa)) / 2
                val inicial = medidor.measure(mes.inicial, estiloDoMes)
                drawText(
                    textLayoutResult = inicial,
                    color = corDoMes,
                    topLeft = Offset(meio - inicial.size.width / 2, altura + 4.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.height(Espacos.xs))
        Text(
            text = resumoDaDivisao(periodos, tipo, ano),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val ALTURA_DA_FAIXA = 28.dp
private val ALTURA_DOS_MESES = 20.dp // o respiro e a linha das iniciais

/** Um mês embaixo da faixa: a inicial e os dias dele que caem nela. */
internal data class MesDaFaixa(val inicial: String, val inicio: LocalDate, val fim: LocalDate)

/** De 1º de janeiro a 31 de dezembro do [ano], esticada se algum período começa antes ou acaba depois. */
internal fun faixaDaLinhaDoTempo(periodos: List<DatasPeriodo>, ano: Int): ClosedRange<LocalDate> {
    val inicio = (periodos.map { it.inicio } + LocalDate.of(ano, 1, 1)).min()
    val fim = (periodos.map { it.fim } + LocalDate.of(ano, 12, 31)).max()
    return inicio..fim
}

/** Onde a [data] cai na faixa: 0 no primeiro dia, 1 no dia seguinte ao último. */
internal fun fracaoDaFaixa(data: LocalDate, faixa: ClosedRange<LocalDate>): Float {
    val dias = ChronoUnit.DAYS.between(faixa.start, faixa.endInclusive) + 1
    return ChronoUnit.DAYS.between(faixa.start, data).toFloat() / dias
}

/** Os meses da faixa em ordem, cada um com a inicial (J, F, M…) e só os dias que estão nela. */
internal fun mesesDaFaixa(faixa: ClosedRange<LocalDate>): List<MesDaFaixa> {
    val meses = mutableListOf<MesDaFaixa>()
    var primeiroDia = faixa.start.withDayOfMonth(1)
    while (primeiroDia <= faixa.endInclusive) {
        val ultimoDia = primeiroDia.plusMonths(1).minusDays(1)
        meses += MesDaFaixa(
            inicial = MESES[primeiroDia.monthValue - 1].first().uppercase(),
            inicio = maxOf(primeiroDia, faixa.start),
            fim = minOf(ultimoDia, faixa.endInclusive),
        )
        primeiroDia = primeiroDia.plusMonths(1)
    }
    return meses
}

/** "4 unidades · de 1 fev a 15 dez": quantos períodos, e de quando a quando vai o ano. */
internal fun resumoDaDivisao(periodos: List<DatasPeriodo>, tipo: TipoPeriodo, ano: Int?): String {
    val nome = tipo.displayName.lowercase() + if (periodos.size == 1) "" else "s"
    val inicio = periodos.minOf { it.inicio }
    val fim = periodos.maxOf { it.fim }
    return "${periodos.size} $nome · de ${dataCurta(inicio, ano)} a ${dataCurta(fim, ano)}"
}
