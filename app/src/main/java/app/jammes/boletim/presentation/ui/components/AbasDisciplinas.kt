package app.jammes.boletim.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.EstiloDasAbas
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
    // só o que falta; se nem aparece, rola até ela. "Inteira" é fora das folgas do topo e da base,
    // que esmaecem: encostada na ponta, ela ficaria meio apagada.
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
        // O offset das abas conta a partir do fim da folga do topo
        val fimSemFolga = info.viewportEndOffset - info.afterContentPadding
        when {
            aba == null -> estado.animateScrollToItem(indice)
            aba.offset < 0 -> estado.animateScrollBy(aba.offset.toFloat())
            aba.offset + aba.size > fimSemFolga ->
                estado.animateScrollBy((aba.offset + aba.size - fimSemFolga).toFloat())
        }
    }

    LazyColumn(
        state = estado,
        modifier = modifier
            .width(LARGURA_ABERTA + BORDA)
            .fillMaxHeight()
            // Em pixels inteiros, como o padding da aba: com 3dp quebrados (7,875px a 420dpi), a
            // última coluna da linha saía meio apagada, uma costura entre ela e a aba aberta
            .drawBehind { drawRect(corAberta, size = Size(BORDA.roundToPx().toFloat(), size.height)) }
            // Rolando, a aba some aos poucos na folga do topo e da base, em vez de sair cortada
            .esmaecerPontas(FOLGA_VERTICAL),
        contentPadding = PaddingValues(vertical = FOLGA_VERTICAL),
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

    // O Surface clicável reserva 48dp de toque em volta da aba e a centraliza nesse espaço. Mais
    // estreita que isso, a aba aberta saía 1px para a direita no arredondamento e descolava da linha
    // da borda. Sem essa reserva, os 48dp de altura ficam aqui, por fora, e as abas "Apenas ícones"
    // continuam com o mesmo espaço entre elas
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Surface(
            selected = isSelected,
            onClick = {
                // O mesmo "tique" do seletor de período: trocar de aba é trocar de folha
                if (!isSelected) haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            },
            // Começa depois da linha da borda: a aba aberta, da mesma cor, emenda nela
            modifier = modifier
                .padding(start = BORDA)
                .heightIn(min = ALTURA_MINIMA)
                .wrapContentHeight()
                .width(largura),
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
}

internal val LARGURA_ABERTA = 44.dp
private val LARGURA_FECHADA = 38.dp
// A altura mínima de toque do Material, que o Surface deixou de reservar
private val ALTURA_MINIMA = 48.dp
internal val BORDA = 3.dp
// O respiro acima da primeira aba e abaixo da última, onde elas esmaecem ao rolar
private val FOLGA_VERTICAL = 8.dp

// A aba do Boletim Geral no estilo "Ícones + sigla". É uma palavra, e não uma sigla: em
// maiúsculas, "GERAL" encostava nas bordas da aba fechada
internal const val SIGLA_DO_BOLETIM_GERAL = "Geral"

/** A sigla de cada disciplina, pelo id, para as abas no estilo "Ícones + sigla". */
internal fun siglasDasDisciplinas(disciplinas: List<DisciplinaDomain>): Map<Long, String> =
    disciplinas.associate { it.id to siglaDaDisciplina(it.abreviacao, it.nome) }

/**
 * A sigla que as abas mostram: sempre a abreviação da própria disciplina ("MAT"), em maiúsculas.
 * A da matéria só conta quando a disciplina nasce, que é quando ela é copiada. Sem abreviação, a
 * sigla sai do [nome].
 */
internal fun siglaDaDisciplina(abreviacao: String?, nome: String): String =
    abreviacao?.trim()?.takeIf { it.isNotEmpty() }?.uppercase() ?: siglaDoNome(nome)

/** As três primeiras letras do nome, em maiúsculas: "Redação" vira "RED". */
internal fun siglaDoNome(nome: String): String =
    nome.filter { it.isLetterOrDigit() }.take(3).uppercase()

// Texto escuro ou branco, o que tiver mais contraste com a cor cheia da aba (o limite de 0,179 é
// o ponto em que preto e branco empatam no contraste do WCAG)
private fun conteudoSobre(cor: Color): Color =
    if (cor.luminance() > 0.179f) Color(0xFF191C21) else Color.White

/**
 * Apaga aos poucos o que passa pelas faixas de [altura] no topo e na base, de transparente a
 * opaco. A máscara fica numa camada à parte e só pega o que é desenhado depois dela: o que vem
 * antes na cadeia, como a linha da borda, continua inteiro.
 */
private fun Modifier.esmaecerPontas(altura: Dp) = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val ponta = (altura.toPx() / size.height).coerceAtMost(0.5f)
        drawRect(
            Brush.verticalGradient(
                0f to Color.Transparent,
                ponta to Color.Black,
                1f - ponta to Color.Black,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
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
