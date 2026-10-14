package app.jammes.boletim.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.presentation.ui.theme.BoletimColors
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import app.jammes.boletim.presentation.ui.theme.IconesDisciplina
import kotlinx.coroutines.flow.first

/**
 * As cores da paleta das disciplinas, em bolinhas numa fileira que rola. A escolhida ganha um aro e
 * um ponto no meio. É o seletor do cadastro de matéria e o da edição de disciplina.
 */
@Composable
fun SeletorDeCor(
    selecionada: Int, // o índice na paleta
    onSelecionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    FileiraDeOpcoes(
        quantidade = CoresDisciplina.paleta.size,
        selecionada = selecionada,
        modifier = modifier,
        contentPadding = contentPadding,
    ) { indice ->
        val escolhida = indice == selecionada
        OpcaoRedonda(
            escolhida = escolhida,
            fundo = CoresDisciplina.de(indice),
            descricao = CoresDisciplina.nomeDe(indice),
            onEscolher = { onSelecionar(indice) },
        ) {
            AnimatedVisibility(
                visible = escolhida,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(BoletimColors.OnPrimary)
                )
            }
        }
    }
}

/**
 * Os ícones das matérias numa fileira que rola, como o [SeletorDeCor]. Cada um já vem na [cor] da
 * disciplina, do jeito que vai aparecer no selo, e o escolhido ganha o mesmo aro. Trocar a cor
 * pinta a fileira inteira de novo.
 */
@Composable
fun SeletorDeIcone(
    selecionado: IconeMateria,
    cor: Color,
    onSelecionar: (IconeMateria) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val icones = IconeMateria.entries
    val corAnimada by animateColorAsState(cor, label = "corDosIcones")

    FileiraDeOpcoes(
        quantidade = icones.size,
        selecionada = icones.indexOf(selecionado),
        modifier = modifier,
        contentPadding = contentPadding,
    ) { indice ->
        val icone = icones[indice]
        val escolhido = icone == selecionado
        // O fundo tingido do selo; no escolhido, um pouco mais forte
        val forca by animateFloatAsState(if (escolhido) 0.28f else 0.12f, label = "fundoDoIcone")
        OpcaoRedonda(
            escolhida = escolhido,
            fundo = corAnimada.copy(alpha = forca),
            descricao = IconesDisciplina.nomeDe(icone),
            onEscolher = { onSelecionar(icone) },
        ) {
            Icon(
                imageVector = IconesDisciplina.de(icone),
                contentDescription = null, // quem diz o nome é a opção inteira
                tint = corAnimada,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * A fileira dos dois seletores. Ela mostra a escolhida sempre que ela fica fora da vista: ao abrir,
 * já no lugar; e quando a escolha muda por fora, como no "Redefinir" da edição, rolando até ela.
 */
@Composable
private fun FileiraDeOpcoes(
    quantidade: Int,
    selecionada: Int, // -1: nenhuma
    modifier: Modifier,
    contentPadding: PaddingValues,
    opcao: @Composable (indice: Int) -> Unit,
) {
    val lista = rememberLazyListState()
    var aberta by remember { mutableStateOf(false) }

    LaunchedEffect(selecionada) {
        if (selecionada < 0) return@LaunchedEffect
        // Espera a fileira ter tamanho: só então dá para saber o que está à vista
        val vista = snapshotFlow { lista.layoutInfo }.first { it.visibleItemsInfo.isNotEmpty() }
        if (!vista.mostraInteiro(selecionada)) {
            // Uma antes dela, para a escolhida não ficar colada na borda
            val alvo = (selecionada - 1).coerceAtLeast(0)
            if (aberta) lista.animateScrollToItem(alvo) else lista.scrollToItem(alvo)
        }
        aberta = true
    }

    LazyRow(
        state = lista,
        modifier = modifier.selectableGroup(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(quantidade) { indice -> opcao(indice) }
    }
}

private fun LazyListLayoutInfo.mostraInteiro(indice: Int): Boolean {
    val item = visibleItemsInfo.firstOrNull { it.index == indice } ?: return false
    return item.offset >= viewportStartOffset && item.offset + item.size <= viewportEndOffset
}

/**
 * Uma opção dos seletores: um círculo com o [fundo] e o [conteudo] no meio. A escolhida ganha um
 * aro, que entra e sai suave. O leitor de tela diz a [descricao] e se ela está escolhida.
 */
@Composable
private fun OpcaoRedonda(
    escolhida: Boolean,
    fundo: Color,
    descricao: String,
    onEscolher: () -> Unit,
    conteudo: @Composable BoxScope.() -> Unit,
) {
    val haptico = LocalHapticFeedback.current
    val corDoAro = MaterialTheme.colorScheme.onSurface
    // Lido só no desenho: a animação do aro não recompõe a opção a cada quadro
    val aro by animateFloatAsState(if (escolhida) 1f else 0f, label = "aro")

    Box(
        modifier = Modifier
            .size(TAMANHO_DA_OPCAO)
            .clip(CircleShape)
            .background(fundo)
            .drawWithContent {
                drawContent()
                if (aro > 0f) {
                    val espessura = ESPESSURA_DO_ARO.toPx() * aro
                    drawCircle(
                        color = corDoAro,
                        radius = (size.minDimension - espessura) / 2,
                        style = Stroke(espessura),
                    )
                }
            }
            .selectable(
                selected = escolhida,
                role = Role.RadioButton,
                onClick = {
                    // Um "tique" a cada troca, como nos outros seletores do app
                    if (!escolhida) haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onEscolher()
                },
            )
            .semantics { contentDescription = descricao },
        contentAlignment = Alignment.Center,
        content = conteudo,
    )
}

private val TAMANHO_DA_OPCAO = 36.dp
private val ESPESSURA_DO_ARO = 3.dp
