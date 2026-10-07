package app.jammes.boletim.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** As médias e a média mínima vão de 0 a 10: é a escala da barra e do anel. */
private const val ESCALA = 10.0

private fun fracao(valor: Double?): Float =
    ((valor ?: 0.0) / ESCALA).coerceIn(0.0, 1.0).toFloat()

/**
 * O ícone da disciplina num quadrado arredondado, tingido com a cor dela. É o que identifica a
 * disciplina nos cards e no detalhe, no lugar da faixa colorida.
 */
@Composable
fun SeloDisciplina(
    cor: Color,
    icone: ImageVector,
    modifier: Modifier = Modifier,
    tamanho: Dp = 36.dp,
) {
    Box(
        modifier = modifier
            .size(tamanho)
            .clip(RoundedCornerShape(tamanho * 0.3f))
            .background(cor.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null, // o nome da disciplina está sempre ao lado
            tint = cor,
            modifier = Modifier.size(tamanho * 0.55f),
        )
    }
}

/**
 * Barra fina da média, de 0 a 10, com um tique na média mínima: dá para ver de longe quanto falta
 * (ou sobra) sem ler o número. Sem média, fica só o trilho.
 */
@Composable
fun BarraDaMedia(
    media: Double?,
    mediaMinima: Double,
    cor: Color,
    modifier: Modifier = Modifier,
) {
    val progresso by animateFloatAsState(fracao(media), label = "progressoDaMedia")
    val trilho = MaterialTheme.colorScheme.surfaceContainerHighest
    val marca = MaterialTheme.colorScheme.onSurfaceVariant

    // O Canvas é mais alto que a barra para o tique passar um pouco acima e abaixo dela
    Canvas(modifier.fillMaxWidth().height(10.dp)) {
        val altura = 6.dp.toPx()
        val topo = (size.height - altura) / 2
        val raio = CornerRadius(altura / 2)
        drawRoundRect(trilho, topLeft = Offset(0f, topo), size = Size(size.width, altura), cornerRadius = raio)
        if (progresso > 0f) {
            drawRoundRect(
                color = cor,
                topLeft = Offset(0f, topo),
                size = Size(size.width * progresso, altura),
                cornerRadius = raio,
            )
        }
        val x = size.width * fracao(mediaMinima)
        drawLine(marca, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    }
}

/**
 * A média num anel de 0 a 10, aberto embaixo, com um tique na média mínima. O [conteudo] vai no
 * centro (o destaque do Boletim põe ali o ícone do status).
 */
@Composable
fun AnelDaMedia(
    media: Double?,
    mediaMinima: Double,
    cor: Color,
    modifier: Modifier = Modifier,
    espessura: Dp = 8.dp,
    conteudo: @Composable BoxScope.() -> Unit = {},
) {
    val progresso by animateFloatAsState(fracao(media), label = "progressoDoAnel")
    val trilho = MaterialTheme.colorScheme.surfaceContainerHighest
    val marca = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val traco = espessura.toPx()
            val canto = Offset(traco / 2, traco / 2)
            val area = Size(size.width - traco, size.height - traco)
            val estilo = Stroke(width = traco, cap = StrokeCap.Round)
            drawArc(trilho, INICIO, VARREDURA, useCenter = false, topLeft = canto, size = area, style = estilo)
            if (progresso > 0f) {
                drawArc(cor, INICIO, VARREDURA * progresso, useCenter = false, topLeft = canto, size = area, style = estilo)
            }
            // O tique atravessa o traço do anel, do lado de dentro até a borda
            val angulo = Math.toRadians((INICIO + VARREDURA * fracao(mediaMinima)).toDouble())
            val direcao = Offset(cos(angulo).toFloat(), sin(angulo).toFloat())
            val raioExterno = size.minDimension / 2
            drawLine(
                color = marca,
                start = center + direcao * (raioExterno - traco - 3.dp.toPx()),
                end = center + direcao * raioExterno,
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        conteudo()
    }
}

// O anel começa embaixo à esquerda e vai até embaixo à direita, deixando a boca para baixo
private const val INICIO = 135f
private const val VARREDURA = 270f

/**
 * Pílula de texto curto: com [cor], ganha uma bolinha e um fundo tingido (contagem de status);
 * sem ela, fica neutra (por exemplo, "Pendente").
 */
@Composable
fun Pilula(
    texto: String,
    modifier: Modifier = Modifier,
    cor: Color? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(cor?.copy(alpha = 0.14f) ?: MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cor != null) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(cor)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = if (cor != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
