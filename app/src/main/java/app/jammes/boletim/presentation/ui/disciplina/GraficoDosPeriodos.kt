package app.jammes.boletim.presentation.ui.disciplina

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.MediaDoPeriodo
import app.jammes.boletim.domain.model.StatusDisciplina
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.presentation.ui.anoletivo.nomeDoPeriodo
import app.jammes.boletim.presentation.ui.anoletivo.ordinalDoPeriodo
import app.jammes.boletim.presentation.ui.boletim.corDoStatus
import app.jammes.boletim.presentation.ui.boletim.formatarMedia
import app.jammes.boletim.presentation.ui.components.ESPACO_DO_TRACEJADO
import app.jammes.boletim.presentation.ui.components.TRACO_DO_TRACEJADO
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerPadding
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.DashedShape
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import java.text.NumberFormat
import java.util.Locale

/**
 * A média de cada período da disciplina, em colunas na cor da situação, com a mínima tracejada.
 * Com um período escolhido no filtro, só a coluna dele fica colorida: as outras ficam cinza, de
 * contexto. O valor de cada período vai embaixo da coluna, junto do nome dele: em cima, ele cruzaria
 * a linha da mínima sempre que a média ficasse logo abaixo dela.
 */
@Composable
internal fun GraficoDosPeriodos(
    medias: List<MediaDoPeriodo>,
    mediaMinima: Double,
    periodoEscolhido: Long?, // null: o ano letivo inteiro, com todas as colunas coloridas
    tipoPeriodo: TipoPeriodo,
    modifier: Modifier = Modifier,
) {
    val produtor = remember { CartesianChartModelProducer() }
    val destaque = medias.indexOfFirst { it.periodo.id == periodoEscolhido }

    val corDoTexto = MaterialTheme.colorScheme.onSurface
    val corApagada = MaterialTheme.colorScheme.onSurfaceVariant
    val rotulos = medias.mapIndexed { i, media ->
        val emFoco = destaque < 0 || destaque == i
        val peso = if (destaque == i) FontWeight.SemiBold else FontWeight.Medium
        buildAnnotatedString {
            withStyle(SpanStyle(color = if (emFoco) corDoTexto else corApagada.copy(alpha = 0.7f), fontWeight = peso)) {
                append(formatarMedia(media.media))
            }
            append('\n')
            withStyle(
                SpanStyle(
                    color = if (destaque == i) corDoTexto else corApagada,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    fontWeight = peso,
                ),
            ) { append(ordinalDoPeriodo(media.periodo.periodo, tipoPeriodo)) }
        }
    }

    // Os dados entram pelo produtor: quando uma nota muda, a coluna cresce ou encolhe até a média nova
    LaunchedEffect(medias, destaque, rotulos) {
        produtor.runTransaction {
            // Período sem nota vira um toco, para não parecer zero
            columnModel { series(medias.map { it.media ?: ALTURA_DO_TOCO }) }
            extras { extra ->
                extra[CHAVE_DAS_SITUACOES] = medias.map { it.status }
                extra[CHAVE_DO_DESTAQUE] = destaque
                extra[CHAVE_DOS_ROTULOS] = rotulos
            }
        }
    }

    val descricao = descricaoDoGrafico(medias, mediaMinima, tipoPeriodo)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(ALTURA)
            .clearAndSetSemantics { contentDescription = descricao },
    ) {
        // Cada período tem uma vaga do mesmo tamanho, e a coluna fica no meio dela, sem passar de 20dp:
        // com poucos períodos (semestres) ela não engorda
        val vaga = maxWidth / medias.size.coerceAtLeast(1)
        val espessura = minOf(ESPESSURA_MAXIMA, vaga * 0.55f)
        val espaco = vaga - espessura

        val forma = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
        val colunas = StatusDisciplina.entries.associateWith { LineComponent(Fill(corDoStatus(it)), espessura, forma) }
        val apagada = LineComponent(Fill(corApagada.copy(alpha = 0.22f)), espessura, forma)
        val toco = LineComponent(Fill(MaterialTheme.colorScheme.surfaceContainerHighest), espessura, RoundedCornerShape(2.dp))
        val provedor = remember(colunas, apagada, toco) { ColunasPorSituacao(colunas, apagada, toco) }

        val corDaMinima = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        val linhaDaMinima = remember(mediaMinima, corDaMinima) {
            HorizontalLine(
                y = { mediaMinima },
                line = LineComponent(
                    fill = Fill(corDaMinima),
                    thickness = 1.5.dp,
                    shape = DashedShape(RectangleShape, TRACO_DO_TRACEJADO, ESPACO_DO_TRACEJADO),
                ),
            )
        }
        // De 0 a 10, como as notas; na soma, se alguma média passar de 10, a escala vai até ela
        val escala = remember(medias) {
            CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = maxOf(10.0, medias.maxOfOrNull { it.media ?: 0.0 } ?: 0.0))
        }

        ProvideVicoTheme(rememberM3VicoTheme()) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(
                        columnProvider = provedor,
                        columnCollectionSpacing = espaco,
                        rangeProvider = escala,
                    ),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        line = rememberAxisLineComponent(fill = Fill(corApagada.copy(alpha = 0.28f)), thickness = 1.dp),
                        label = rememberAxisLabelComponent(
                            style = MaterialTheme.typography.labelMedium.copy(color = corDoTexto, textAlign = TextAlign.Center),
                            lineCount = 2,
                            margins = Insets(top = 4.dp),
                            padding = Insets.Zero,
                        ),
                        valueFormatter = remember {
                            CartesianValueFormatter { context, x, _ ->
                                context.model.extraStore.getOrNull(CHAVE_DOS_ROTULOS)?.getOrNull(x.toInt()) ?: ""
                            }
                        },
                        tick = null,
                        guideline = null,
                    ),
                    decorations = listOf(linhaDaMinima),
                    // Meia vaga antes da primeira coluna e depois da última: as colunas ficam no meio das vagas
                    layerPadding = { CartesianLayerPadding(scalableStart = espaco / 2, scalableEnd = espaco / 2) },
                ),
                modelProducer = produtor,
                modifier = Modifier.fillMaxSize(),
                scrollState = rememberVicoScrollState(scrollEnabled = false),
                // As vagas já somam a largura toda; isso só garante que nada fique de fora por arredondamento
                zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
            )
        }
    }
}

/** A cor de cada coluna: a da situação, cinza fora do período escolhido, e o toco sem nota. */
private class ColunasPorSituacao(
    private val colunas: Map<StatusDisciplina, LineComponent>,
    private val apagada: LineComponent,
    private val toco: LineComponent,
) : ColumnCartesianLayer.ColumnProvider {
    override fun getColumn(entry: ColumnCartesianLayerModel.Entry, extraStore: ExtraStore): LineComponent {
        val i = entry.x.toInt()
        val situacao = extraStore.getOrNull(CHAVE_DAS_SITUACOES)?.getOrNull(i) ?: StatusDisciplina.SEM_NOTA
        val destaque = extraStore.getOrNull(CHAVE_DO_DESTAQUE) ?: -1
        return when {
            situacao == StatusDisciplina.SEM_NOTA -> toco
            destaque >= 0 && destaque != i -> apagada
            else -> colunas.getValue(situacao)
        }
    }

    override fun getWidestSeriesColumn(seriesKey: Any, seriesIndex: Int, extraStore: ExtraStore): LineComponent =
        apagada // todas têm a mesma espessura

    override fun equals(other: Any?): Boolean =
        other is ColunasPorSituacao && colunas == other.colunas && apagada == other.apagada && toco == other.toco

    override fun hashCode(): Int = 31 * (31 * colunas.hashCode() + apagada.hashCode()) + toco.hashCode()
}

/** O que o leitor de tela diz no lugar das colunas: a média de cada período e a mínima. */
private fun descricaoDoGrafico(medias: List<MediaDoPeriodo>, mediaMinima: Double, tipoPeriodo: TipoPeriodo): String {
    val periodos = medias.joinToString(", ") {
        val nome = nomeDoPeriodo(it.periodo.periodo, tipoPeriodo)
        if (it.media == null) "$nome sem notas" else "$nome ${formatarMedia(it.media)}"
    }
    return "Média por ${tipoPeriodo.displayName.lowercase()}: $periodos. Mínima ${formatarMedia(mediaMinima)}."
}

/**
 * Quanto falta para a [media] chegar na [minima]: "Falta 0,3 ponto", "Faltam 2,5 pontos". Abaixo de
 * 2 o português usa o singular ("1,5 ponto"). null sem média, ou na mínima e acima dela.
 */
internal fun quantoFalta(media: Double?, minima: Double): String? {
    if (media == null || media >= minima) return null
    // Pelo BigDecimal: 8,0 − 7,7 dá 0,3, e não 0,2999…
    val diferenca = minima.toBigDecimal().subtract(media.toBigDecimal())
    val pontos = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"))
        .apply { maximumFractionDigits = 2 }
        .format(diferenca)
    return if (diferenca < 2.toBigDecimal()) "Falta $pontos ponto" else "Faltam $pontos pontos"
}

private val CHAVE_DAS_SITUACOES = ExtraStore.Key<List<StatusDisciplina>>()
private val CHAVE_DO_DESTAQUE = ExtraStore.Key<Int>()
private val CHAVE_DOS_ROTULOS = ExtraStore.Key<List<CharSequence>>()

private val ALTURA = 132.dp
private val ESPESSURA_MAXIMA = 20.dp
private const val ALTURA_DO_TOCO = 0.35 // na escala de 0 a 10: uns 3dp
