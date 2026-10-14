package app.jammes.boletim.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.EstiloDasAbas
import app.jammes.boletim.domain.model.MateriaDomain
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
 * O [estilo], escolhido nas configurações, diz o que vai embaixo do ícone: nada, a sigla
 * ([siglas], pelo id da disciplina) ou o nome inteiro, na vertical.
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
    estilo: EstiloDasAbas = EstiloDasAbas.ICONES_E_DESCRICAO,
    siglas: Map<Long, String> = emptyMap(),
) {
    val corGeral = MaterialTheme.colorScheme.primary
    val corAberta by animateColorAsState(
        targetValue = disciplinas.find { it.id == disciplinaAbertaId }
            ?.let { CoresDisciplina.de(it.cor) }
            ?: corGeral,
        label = "corDaAbaAberta",
    )

    // A aba aberta sempre à vista: abrindo a disciplina pelo card do Boletim, a aba dela pode estar
    // lá embaixo, fora da tela. Se ela já aparece inteira, nada se mexe; se aparece cortada, rola
    // só o que falta; se nem aparece, rola até ela.
    val estado = rememberLazyListState()
    LaunchedEffect(disciplinaAbertaId, disciplinas) {
        val indice = if (disciplinaAbertaId == null) {
            0 // o Boletim Geral é o primeiro item
        } else {
            disciplinas.indexOfFirst { it.id == disciplinaAbertaId }.takeIf { it >= 0 }?.plus(1)
                ?: return@LaunchedEffect
        }
        val info = estado.layoutInfo
        val aba = info.visibleItemsInfo.find { it.index == indice }
        when {
            aba == null -> estado.animateScrollToItem(indice)
            aba.offset < info.viewportStartOffset -> estado.animateScrollBy((aba.offset - info.viewportStartOffset).toFloat())
            aba.offset + aba.size > info.viewportEndOffset ->
                estado.animateScrollBy((aba.offset + aba.size - info.viewportEndOffset).toFloat())
        }
    }

    LazyColumn(
        state = estado,
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
                sigla = SIGLA_DO_BOLETIM_GERAL,
                estilo = estilo,
                isSelected = disciplinaAbertaId == null,
                onClick = onAbrirBoletimGeral
            )
        }
        items(disciplinas, key = { it.id }) { disciplina ->
            AbaDisciplina(
                cor = CoresDisciplina.de(disciplina.cor),
                icone = IconesDisciplina.de(disciplina.icone),
                nome = disciplina.nome,
                sigla = siglas[disciplina.id] ?: siglaDoNome(disciplina.nome),
                estilo = estilo,
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
    onClick: () -> Unit,
    estilo: EstiloDasAbas = EstiloDasAbas.ICONES_E_DESCRICAO,
    sigla: String = siglaDoNome(nome),
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
        // Trocando o estilo, a aba cresce ou encolhe até a altura nova, sem pular
        Column(
            modifier = Modifier.animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val soIcone = estilo == EstiloDasAbas.APENAS_ICONES
            // O ícone fica em pé, só o nome gira
            Icon(
                imageVector = icone,
                // Com texto embaixo, ele já diz qual é a aba; sozinho, quem diz é o ícone
                contentDescription = if (soIcone) nome else null,
                tint = if (isSelected) conteudo else cor,
                modifier = Modifier
                    .padding(top = 12.dp, bottom = if (soIcone) 12.dp else 0.dp)
                    .size(20.dp),
            )
            when (estilo) {
                EstiloDasAbas.APENAS_ICONES -> Unit
                // A sigla é curta: cabe deitada, embaixo do ícone, sem precisar virar a cabeça
                EstiloDasAbas.ICONES_E_SIGLA -> Text(
                    sigla,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 10.dp)
                        // O leitor de tela fala o nome inteiro, e não "M A T"
                        .semantics { contentDescription = nome },
                )
                EstiloDasAbas.ICONES_E_DESCRICAO -> Text(
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
}

internal val LARGURA_ABERTA = 44.dp
private val LARGURA_FECHADA = 38.dp
internal val BORDA = 3.dp

// A aba do Boletim Geral no estilo "Ícones + sigla". É uma palavra, e não uma sigla: em
// maiúsculas, "GERAL" encostava nas bordas da aba fechada
internal const val SIGLA_DO_BOLETIM_GERAL = "Geral"

/**
 * A sigla de cada disciplina, pelo id, para as abas no estilo "Ícones + sigla". É a da matéria
 * ("MAT") enquanto a disciplina tem o nome dela; renomeada, ou com a matéria sem sigla, sai do nome.
 */
internal fun siglasDasDisciplinas(
    disciplinas: List<DisciplinaDomain>,
    materias: List<MateriaDomain>,
): Map<Long, String> {
    val porId = materias.associateBy { it.id }
    return disciplinas.associate { disciplina ->
        val materia = porId[disciplina.materiaId]
        val daMateria = materia?.abreviacao?.trim()?.takeIf { it.isNotEmpty() && materia.nome.trim() == disciplina.nome.trim() }
        disciplina.id to (daMateria?.uppercase() ?: siglaDoNome(disciplina.nome))
    }
}

/** As três primeiras letras do nome, em maiúsculas: "Redação" vira "RED". */
internal fun siglaDoNome(nome: String): String =
    nome.filter { it.isLetterOrDigit() }.take(3).uppercase()

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
