package app.jammes.boletim.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina

/**
 * Abas da lateral direita: o Boletim Geral e uma aba por disciplina do ano letivo.
 * Não sabe nada de navegação, só avisa qual aba foi tocada.
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
    LazyColumn(
        modifier = modifier
            .width(46.dp)
            .fillMaxHeight()
            .background(color = MaterialTheme.colorScheme.secondary),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            AbaDisciplina(
                cor = Color.LightGray,
                nome = "Boletim Geral",
                isSelected = disciplinaAbertaId == null,
                onClick = onAbrirBoletimGeral
            )
        }
        items(disciplinas, key = { it.id }) { disciplina ->
            AbaDisciplina(
                cor = CoresDisciplina.de(disciplina.cor),
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
    nome: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cornerRadius = 8.dp

    Surface(
        selected = isSelected,
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(topEnd = cornerRadius, bottomEnd = cornerRadius),
        color = cor,
        border = if (isSelected) BorderStroke(1.dp, Color.Yellow) else null
    ) {
        Text(
            nome,
            modifier = Modifier.vertical().rotate(90f)
                .padding(vertical = 8.dp, horizontal = 12.dp),
        )
    }
}

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
