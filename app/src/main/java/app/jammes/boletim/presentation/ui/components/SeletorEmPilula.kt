package app.jammes.boletim.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.presentation.ui.anoletivo.nomeDoPeriodo
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Uma opção do [SeletorEmPilula]: o nome inteiro e o curto, para quando os inteiros não cabem. */
data class OpcaoDoSeletor(val completo: String, val curto: String = completo)

/** "1º Bimestre", que encurta para "1º". */
internal fun opcaoDoPeriodo(numero: Int, tipo: TipoPeriodo): OpcaoDoSeletor {
    val nome = nomeDoPeriodo(numero, tipo)
    return OpcaoDoSeletor(completo = nome, curto = nome.substringBefore(' '))
}

/**
 * Trilho em pílula com o indicador que desliza até a opção escolhida. É o seletor de período do
 * topo do app, e o formulário de avaliação usa o mesmo para o tipo e o período.
 *
 * Se os nomes inteiros ("1ª Unidade") não cabem todos na largura, só o escolhido fica inteiro e
 * os outros encurtam ("1ª"); se nem assim couber, todos encurtam. As opções dividem a largura na
 * proporção do texto. [selecionada] -1 deixa nenhuma marcada. [trilho] é a cor do fundo: a padrão
 * combina com o fundo da tela.
 */
@Composable
internal fun SeletorEmPilula(
    opcoes: List<OpcaoDoSeletor>,
    selecionada: Int,
    onSelecionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trilho: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    papel: Role = Role.Tab, // num formulário, RadioButton: o leitor de tela diz que é uma escolha
) {
    val estilo = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    val medidor = rememberTextMeasurer()
    val densidade = LocalDensity.current
    val haptico = LocalHapticFeedback.current

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val folga = with(densidade) { (PADDING_OPCAO * 2).roundToPx() }
        val bordas = with(densidade) { (PADDING_TRILHO * 2).roundToPx() }
        fun larguras(textos: List<String>) = textos.map { medidor.measure(it, estilo).size.width + folga }
        fun cabe(textos: List<String>) = larguras(textos).sum() + bordas <= constraints.maxWidth

        val textos = listOf(
            opcoes.map { it.completo },
            opcoes.mapIndexed { i, opcao -> if (i == selecionada) opcao.completo else opcao.curto },
        ).firstOrNull(::cabe) ?: opcoes.map { it.curto }
        val pesos = larguras(textos)

        // Onde cada opção ficou (x e largura, em px), medido no layout; o indicador vai atrás
        val limites = remember(opcoes.size) { mutableStateListOf<Pair<Int, Int>>().apply { repeat(opcoes.size) { add(0 to 0) } } }
        val alvo = limites.getOrNull(selecionada) ?: (0 to 0)
        val x = remember { Animatable(0f) }
        val largura = remember { Animatable(0f) }
        LaunchedEffect(alvo) {
            // Na primeira medida o indicador já nasce no lugar, sem crescer do zero
            if (largura.value == 0f) {
                x.snapTo(alvo.first.toFloat())
                largura.snapTo(alvo.second.toFloat())
            } else {
                launch { x.animateTo(alvo.first.toFloat()) }
                launch { largura.animateTo(alvo.second.toFloat()) }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(trilho)
                .padding(PADDING_TRILHO)
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .wrapContentWidth(Alignment.Start)
                    .offset { IntOffset(x.value.roundToInt(), 0) }
                    .width(with(densidade) { largura.value.toDp() })
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.Start) {
                textos.forEachIndexed { i, texto ->
                    val escolhida = i == selecionada
                    val cor by animateColorAsState(
                        targetValue = if (escolhida) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "corDaOpcao",
                    )
                    Box(
                        modifier = Modifier
                            .weight(pesos[i].toFloat())
                            .onPlaced { limites[i] = it.positionInParent().x.roundToInt() to it.size.width }
                            .clip(CircleShape)
                            .selectable(
                                selected = escolhida,
                                role = papel,
                                onClick = {
                                    // Um "tique" a cada troca, como um seletor de verdade
                                    if (!escolhida) haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    onSelecionar(i)
                                },
                            )
                            .padding(horizontal = PADDING_OPCAO, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = texto,
                            style = estilo,
                            fontWeight = if (escolhida) FontWeight.SemiBold else FontWeight.Medium,
                            color = cor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            // O leitor de tela fala o nome inteiro mesmo quando aparece só "1ª"
                            modifier = Modifier.semantics { contentDescription = opcoes[i].completo },
                        )
                    }
                }
            }
        }
    }
}

private val PADDING_OPCAO = 12.dp
private val PADDING_TRILHO = 4.dp
