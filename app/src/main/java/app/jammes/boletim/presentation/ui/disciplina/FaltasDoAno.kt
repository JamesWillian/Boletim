package app.jammes.boletim.presentation.ui.disciplina

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.DisciplinaDetalhe
import app.jammes.boletim.domain.usecase.CalcularFrequencia
import app.jammes.boletim.presentation.ui.theme.Espacos

/**
 * O topo da aba de faltas: quantas no ano, contra quantas a frequência mínima permite, num medidor, e
 * a frequência que isso dá. É sempre o ano inteiro, em qualquer filtro: o limite de faltas é anual.
 * Sem o total de aulas não há limite nem frequência, e o resumo diz onde informar.
 */
@Composable
internal fun ResumoDasFaltas(
    detalhe: DisciplinaDetalhe,
    modifier: Modifier = Modifier,
) {
    val limite = detalhe.limiteFaltas
    val frequencia = CalcularFrequencia.percentual(detalhe.disciplina.totalAulas, detalhe.faltas)
    val corDoLimite = if (detalhe.emRiscoPorFalta) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(Espacos.l)) {
            Text(
                text = "Faltas no ano",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = detalhe.faltas.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (detalhe.emRiscoPorFalta) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alignByBaseline(),
                )
                if (limite != null) {
                    Spacer(Modifier.width(Espacos.s))
                    Text(
                        text = "de $limite permitidas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
            if (limite != null) {
                Spacer(Modifier.height(10.dp))
                MedidorDeFaltas(faltas = detalhe.faltas, limite = limite, cor = corDoLimite)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (frequencia != null) "Frequência de $frequencia%"
                else "Informe o total de aulas em Editar para ver o limite de faltas e a frequência.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Quanto do limite as faltas já usaram, de 0 a cheio. Passou do limite, fica cheio. */
@Composable
private fun MedidorDeFaltas(faltas: Int, limite: Int, cor: Color, modifier: Modifier = Modifier) {
    val alvo = if (limite > 0) (faltas.toFloat() / limite).coerceIn(0f, 1f) else if (faltas > 0) 1f else 0f
    val progresso by animateFloatAsState(alvo, label = "medidorDeFaltas")
    val trilho = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier.fillMaxWidth().height(8.dp)) {
        val raio = CornerRadius(size.height / 2)
        drawRoundRect(trilho, size = size, cornerRadius = raio)
        if (progresso > 0f) {
            // Nunca mais curta que a própria altura, para a ponta redonda não virar um risco
            val largura = maxOf(size.width * progresso, size.height)
            drawRoundRect(cor, topLeft = Offset.Zero, size = Size(largura, size.height), cornerRadius = raio)
        }
    }
}

/**
 * O número de faltas do ano na própria aba: dá para ver sem abrir a lista. Vermelho quando passou do
 * limite; azul com a aba aberta; neutro com ela fechada.
 */
@Composable
internal fun ContadorDeFaltas(faltas: Int, emRisco: Boolean, selecionada: Boolean, modifier: Modifier = Modifier) {
    val (fundo, texto) = when {
        emRisco -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        selecionada -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) to MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(fundo)
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = faltas.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = texto,
        )
    }
}
