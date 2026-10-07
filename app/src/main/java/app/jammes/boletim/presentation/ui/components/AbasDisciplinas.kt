package app.jammes.boletim.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import app.jammes.boletim.presentation.ui.theme.IconesDisciplina

/**
 * Abas da lateral direita: o Boletim Geral e uma aba por disciplina do ano letivo.
 * Não sabe nada de navegação, só avisa qual aba foi tocada.
 *
 * Funciona como um fichário: a aba aberta sai inteira, na cor cheia, e uma linha da mesma cor
 * desce pela borda da página, como se a folha da frente fosse dela. As outras ficam recolhidas
 * e tingidas, só com o ícone na cor da disciplina.
 *
 * @param disciplinaAbertaId disciplina aberta no centro; null quando é o Boletim Geral.
 */
@Composable
fun AbasDisciplinas(
    disciplinas: List<DisciplinaDomain>,
    disciplinaAbertaId: Long?,
    onAbrirBoletimGeral: () -> Unit,
    onAbrirDisciplina: (disciplinaId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val corGeral = MaterialTheme.colorScheme.primary
    val corAberta by animateColorAsState(
        targetValue = disciplinas.find { it.id == disciplinaAbertaId }
            ?.let { CoresDisciplina.de(it.cor) }
            ?: corGeral,
        label = "corDaAbaAberta",
    )

    LazyColumn(
        modifier = modifier
            .width(LARGURA_ABERTA + BORDA)
            .fillMaxHeight()
            .drawBehind { drawRect(corAberta, size = Size(BORDA.toPx(), size.height)) },
        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            AbaDisciplina(
                cor = corGeral,
                icone = Icons.Filled.GridView,
                nome = "Boletim Geral",
                isSelected = disciplinaAbertaId == null,
                onClick = onAbrirBoletimGeral
            )
        }
        items(disciplinas, key = { it.id }) { disciplina ->
            AbaDisciplina(
                cor = CoresDisciplina.de(disciplina.cor),
                icone = IconesDisciplina.de(disciplina.icone),
                nome = disciplina.nome,
                isSelected = disciplina.id == disciplinaAbertaId,
                onClick = { onAbrirDisciplina(disciplina.id) }
            )
        }
    }
}

@Composable
fun AbaDisciplina(
    modifier: Modifier = Modifier,
    cor: Color,
    icone: ImageVector,
    nome: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val largura by animateDpAsState(
        targetValue = if (isSelected) LARGURA_ABERTA else LARGURA_FECHADA,
        label = "larguraDaAba",
    )
    val fundo by animateColorAsState(
        targetValue = if (isSelected) {
            cor
        } else {
            cor.copy(alpha = 0.18f).compositeOver(MaterialTheme.colorScheme.surface)
        },
        label = "fundoDaAba",
    )
    val conteudo by animateColorAsState(
        targetValue = if (isSelected) conteudoSobre(cor) else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "conteudoDaAba",
    )
    val haptico = LocalHapticFeedback.current

    Surface(
        selected = isSelected,
        onClick = {
            // O mesmo "tique" do seletor de período: trocar de aba é trocar de folha
            if (!isSelected) haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onClick()
        },
        // Começa depois da linha da borda: a aba aberta, da mesma cor, emenda nela
        modifier = modifier.padding(start = BORDA).width(largura),
        shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
        color = fundo,
        contentColor = conteudo,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // O ícone fica em pé, só o nome gira
            Icon(
                imageVector = icone,
                contentDescription = null, // o nome logo abaixo já diz qual é a aba
                tint = if (isSelected) conteudo else cor,
                modifier = Modifier.padding(top = 12.dp).size(20.dp),
            )
            Text(
                nome,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.vertical().rotate(90f)
                    .padding(vertical = 8.dp, horizontal = 12.dp),
            )
        }
    }
}

private val LARGURA_ABERTA = 44.dp
private val LARGURA_FECHADA = 38.dp
private val BORDA = 3.dp

// Texto escuro ou branco, o que tiver mais contraste com a cor cheia da aba (o limite de 0,179 é
// o ponto em que preto e branco empatam no contraste do WCAG)
private fun conteudoSobre(cor: Color): Color =
    if (cor.luminance() > 0.179f) Color(0xFF191C21) else Color.White

fun Modifier.vertical() = layout { measurable, constraints ->
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minHeight,
            maxWidth = constraints.maxHeight,
            minHeight = constraints.minWidth,
            maxHeight = constraints.maxWidth
        )
    )
    layout(placeable.height, placeable.width) {
        placeable.place(
            x = -(placeable.width / 2 - placeable.height / 2),
            y = -(placeable.height / 2 - placeable.width / 2)
        )
    }
}
