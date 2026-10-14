package app.jammes.boletim.presentation.ui.boletim

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.Boletim
import app.jammes.boletim.domain.model.DisciplinaResumo
import app.jammes.boletim.domain.model.StatusDisciplina
import app.jammes.boletim.presentation.ui.components.ChaveTracejada
import app.jammes.boletim.presentation.ui.components.ESPACO_DO_TRACEJADO
import app.jammes.boletim.presentation.ui.components.Pilula
import app.jammes.boletim.presentation.ui.components.TRACO_DO_TRACEJADO
import app.jammes.boletim.presentation.ui.components.siglaDaDisciplina
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import app.jammes.boletim.presentation.ui.theme.Espacos

/**
 * O destaque do Boletim: a média de cada disciplina numa barra deitada, contra a mínima. Embaixo do
 * título, a legenda dos tracejados; embaixo do gráfico, quantas disciplinas estão em cada situação.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MediasPorDisciplina(
    boletim: Boletim,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Médias por disciplina",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(Espacos.xs),
            ) {
                ChaveTracejada(texto = "mínima ${formatarMedia(boletim.mediaMinima)}", cor = corDaMinimaGeral(), emPe = true)
                comMinimaPropria(boletim.disciplinas, boletim.mediaMinima).forEach {
                    ChaveTracejada(
                        texto = "${siglaDaDisciplina(it.abreviacao, it.nome)} ${formatarMedia(it.mediaMinima)}",
                        cor = CoresDisciplina.de(it.cor),
                        emPe = true,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            GraficoDasMedias(disciplinas = boletim.disciplinas, mediaMinima = boletim.mediaMinima)
            Spacer(Modifier.height(Espacos.l))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Espacos.s),
                verticalArrangement = Arrangement.spacedBy(Espacos.s),
            ) {
                contagemPorStatus(boletim.disciplinas).forEach { (texto, status) ->
                    Pilula(texto = texto, cor = corDoStatus(status))
                }
            }
        }
    }
}

/**
 * Uma linha por disciplina, então o gráfico cresce com o número delas e a sigla sempre cabe. A barra
 * sai da linha de base à esquerda, com a ponta quase reta, como as colunas do Vico (que não tem
 * barras deitadas: por isso o gráfico é desenhado aqui). A mínima do ano é o tracejado cinza; quem
 * tem regra própria ganha a mínima dela na sua linha, na cor da disciplina, e o cinza pula essa linha.
 */
@Composable
internal fun GraficoDasMedias(
    disciplinas: List<DisciplinaResumo>,
    mediaMinima: Double,
    modifier: Modifier = Modifier,
) {
    val medidor = rememberTextMeasurer()
    val estiloDaSigla = MaterialTheme.typography.labelMedium
    val estiloDoValor = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum", // os valores ficam numa coluna, alinhados
    )
    val estiloDaEscala = MaterialTheme.typography.labelSmall

    // Os textos são medidos uma vez, e não a cada quadro das barras animando
    val siglas = remember(disciplinas, estiloDaSigla) {
        disciplinas.map { medidor.measure(siglaDaDisciplina(it.abreviacao, it.nome), estiloDaSigla) }
    }
    val valores = remember(disciplinas, estiloDoValor) {
        disciplinas.map { medidor.measure(formatarMedia(it.media), estiloDoValor) }
    }
    val escala = remember(estiloDaEscala) { MARCAS_DA_ESCALA.map { medidor.measure(it.toString(), estiloDaEscala) } }
    // Com a fonte grande do aparelho, as linhas e a escala crescem junto com o texto
    val densidade = LocalDensity.current
    val alturaDaLinha = with(densidade) {
        maxOf(ALTURA_DA_LINHA, ((siglas + valores).maxOfOrNull { it.size.height } ?: 0).toDp() + 4.dp)
    }
    val alturaDaEscala = with(densidade) {
        maxOf(ALTURA_DA_ESCALA, (escala.maxOfOrNull { it.size.height } ?: 0).toDp() + 2.dp)
    }

    // Trocar o período estica ou encolhe cada barra até a média nova, e a cor acompanha a situação.
    // São lidas só no desenho: a animação redesenha o gráfico sem recompor a tela
    val fracoes = disciplinas.map { key(it.id) { animateFloatAsState(fracaoDaEscala(it.media), label = "barra") } }
    val cores = disciplinas.map { key(it.id) { animateColorAsState(corDoStatus(it.status), label = "corDaBarra") } }
    val trechosDaGeral = remember(disciplinas, mediaMinima) { trechosDaMinimaGeral(disciplinas, mediaMinima) }

    val corDoTexto = MaterialTheme.colorScheme.onSurface
    val corDoTextoApagado = MaterialTheme.colorScheme.onSurfaceVariant
    val corDaBase = MaterialTheme.colorScheme.outlineVariant
    val corDaGuia = MaterialTheme.colorScheme.surfaceContainerHigh
    val corDaGeral = corDaMinimaGeral()
    val descricao = descricaoDoGrafico(disciplinas, mediaMinima)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(alturaDaLinha * disciplinas.size + alturaDaEscala)
            .semantics { contentDescription = descricao },
    ) {
        val passo = alturaDaLinha.toPx()
        val corpo = passo * disciplinas.size
        val base = (siglas.maxOfOrNull { it.size.width } ?: 0) + VAO_DA_SIGLA.toPx()
        val fim = size.width - (valores.maxOfOrNull { it.size.width } ?: 0) - VAO_DO_VALOR.toPx()
        val x = { media: Double -> base + (fim - base) * fracaoDaEscala(media) }
        val tracejado = PathEffect.dashPathEffect(floatArrayOf(TRACO_DO_TRACEJADO.toPx(), ESPACO_DO_TRACEJADO.toPx()))

        // Guias leves no 5 e no 10, por trás das barras
        MARCAS_DA_ESCALA.drop(1).forEach { marca ->
            drawLine(corDaGuia, Offset(x(marca.toDouble()), 0f), Offset(x(marca.toDouble()), corpo), 1.dp.toPx())
        }

        disciplinas.forEachIndexed { i, disciplina ->
            val centro = passo * (i + 0.5f)
            val sigla = siglas[i]
            drawText(sigla, corDoTextoApagado, Offset(base - VAO_DA_SIGLA.toPx() - sigla.size.width, centro - sigla.size.height / 2f))
            if (disciplina.media != null) {
                barraDeitada(base, base + (fim - base) * fracoes[i].value, centro, cores[i].value)
            }
            val valor = valores[i]
            drawText(
                valor,
                if (disciplina.media != null) corDoTexto else corDoTextoApagado,
                Offset(size.width - valor.size.width, centro - valor.size.height / 2f),
            )
        }

        // A linha de base, à esquerda, e a escala embaixo
        drawLine(corDaBase, Offset(base, 0f), Offset(base, corpo), 1.2.dp.toPx())
        escala.forEachIndexed { j, marca ->
            val meio = x(MARCAS_DA_ESCALA[j].toDouble())
            drawText(marca, corDoTextoApagado, Offset(meio - marca.size.width / 2f, corpo + alturaDaEscala.toPx() - marca.size.height))
        }

        // Os tracejados vêm por cima das barras: é ali que se vê quem passou da mínima
        val folga = 1.dp.toPx()
        trechosDaGeral.forEach { trecho ->
            val topo = passo * trecho.first + if (trecho.first == 0) 0f else folga
            val baixo = passo * (trecho.last + 1) - if (trecho.last == disciplinas.lastIndex) 0f else folga
            drawLine(corDaGeral, Offset(x(mediaMinima), topo), Offset(x(mediaMinima), baixo), 1.5.dp.toPx(), pathEffect = tracejado)
        }
        disciplinas.forEachIndexed { i, disciplina ->
            if (temMinimaPropria(disciplina, mediaMinima)) {
                val xMinima = x(disciplina.mediaMinima)
                drawLine(
                    color = CoresDisciplina.de(disciplina.cor),
                    start = Offset(xMinima, passo * i + folga),
                    end = Offset(xMinima, passo * (i + 1) - folga),
                    strokeWidth = 2.5.dp.toPx(),
                    pathEffect = tracejado,
                )
            }
        }
    }
}

/** A barra de uma disciplina: base reta na linha de base, ponta com cantos pequenos. */
private fun DrawScope.barraDeitada(inicio: Float, fim: Float, centro: Float, cor: Color) {
    if (fim <= inicio) return
    val meia = ESPESSURA_DA_BARRA.toPx() / 2f
    val raio = CornerRadius(minOf(RAIO_DA_PONTA.toPx(), fim - inicio, meia))
    val forma = RoundRect(
        left = inicio,
        top = centro - meia,
        right = fim,
        bottom = centro + meia,
        topLeftCornerRadius = CornerRadius.Zero,
        topRightCornerRadius = raio,
        bottomRightCornerRadius = raio,
        bottomLeftCornerRadius = CornerRadius.Zero,
    )
    drawPath(Path().apply { addRoundRect(forma) }, cor)
}

@Composable
private fun corDaMinimaGeral(): Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)

/** O que o leitor de tela diz no lugar do desenho: cada disciplina com a média dela, e as mínimas. */
private fun descricaoDoGrafico(disciplinas: List<DisciplinaResumo>, mediaMinima: Double): String {
    val medias = disciplinas.joinToString(", ") {
        if (it.media == null) "${it.nome} sem notas" else "${it.nome} ${formatarMedia(it.media)}"
    }
    val proprias = comMinimaPropria(disciplinas, mediaMinima)
        .joinToString("") { ", ${it.nome} ${formatarMedia(it.mediaMinima)}" }
    return "Médias por disciplina: $medias. Mínima ${formatarMedia(mediaMinima)}$proprias."
}

/** A média na escala do gráfico, de 0 a 10. Sem média, zero. */
private fun fracaoDaEscala(media: Double?): Float = ((media ?: 0.0) / 10.0).coerceIn(0.0, 1.0).toFloat()

/** Se a disciplina tem regra própria com outra mínima: ela ganha o tracejado dela no gráfico. */
internal fun temMinimaPropria(disciplina: DisciplinaResumo, mediaMinima: Double): Boolean =
    disciplina.mediaMinima != mediaMinima

/** As disciplinas com mínima própria, na ordem do gráfico: entram na legenda depois da geral. */
internal fun comMinimaPropria(disciplinas: List<DisciplinaResumo>, mediaMinima: Double): List<DisciplinaResumo> =
    disciplinas.filter { temMinimaPropria(it, mediaMinima) }

/**
 * As faixas de linhas seguidas que usam a mínima do ano: o tracejado cinza passa por elas e pula as
 * de regra própria, para não parecer que a mínima do ano vale para essas também.
 */
internal fun trechosDaMinimaGeral(disciplinas: List<DisciplinaResumo>, mediaMinima: Double): List<IntRange> {
    val trechos = mutableListOf<IntRange>()
    var inicio: Int? = null
    disciplinas.forEachIndexed { i, disciplina ->
        if (!temMinimaPropria(disciplina, mediaMinima)) {
            if (inicio == null) inicio = i
        } else {
            inicio?.let { trechos += it until i }
            inicio = null
        }
    }
    inicio?.let { trechos += it until disciplinas.size }
    return trechos
}

/** "1 com folga", "4 no limite", "2 abaixo", "5 sem nota": só as situações que aparecem, nessa ordem. */
internal fun contagemPorStatus(disciplinas: List<DisciplinaResumo>): List<Pair<String, StatusDisciplina>> {
    val porStatus = disciplinas.groupingBy {
        // Reprovado entra junto com abaixo: no boletim do período os dois pedem a mesma atenção
        if (it.status == StatusDisciplina.REPROVADO) StatusDisciplina.ABAIXO else it.status
    }.eachCount()
    return listOf(
        StatusDisciplina.APROVADO to "com folga",
        StatusDisciplina.ATENCAO to "no limite",
        StatusDisciplina.ABAIXO to "abaixo",
        StatusDisciplina.SEM_NOTA to "sem nota",
    ).mapNotNull { (status, rotulo) ->
        porStatus[status]?.let { quantidade -> "$quantidade $rotulo" to status }
    }
}

private val MARCAS_DA_ESCALA = listOf(0, 5, 10)
private val ALTURA_DA_LINHA = 22.dp // no mínimo: com fonte grande, cresce com o texto
private val ALTURA_DA_ESCALA = 18.dp
private val ESPESSURA_DA_BARRA = 12.dp
private val RAIO_DA_PONTA = 3.dp
private val VAO_DA_SIGLA = 6.dp
private val VAO_DO_VALOR = 10.dp
