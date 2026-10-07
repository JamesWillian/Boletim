package app.jammes.boletim.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.Contexto
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.presentation.ui.anoletivo.nomeDoPeriodo
import app.jammes.boletim.presentation.ui.theme.Espacos
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Topo do app: aluno, ano letivo, o botão dos ajustes do ano e o seletor de período.
 * A opção "Ano letivo" chama [onSelectPeriodo] com null: o boletim passa a ser o do ano inteiro.
 */
@Composable
fun IdentificacaoCard(
    modifier: Modifier = Modifier,
    contexto: Contexto,
    aluno: AlunoDomain?,
    anosLetivos: List<AnoLetivoDomain> = emptyList(),
    onSelectPeriodo: (PeriodoDomain?) -> Unit = {},
    onAbrirAjustesAnoLetivo: () -> Unit = {}
) {
    val ano = anosLetivos.find { ano -> ano.id == contexto.anoLetivoId }
    val periodos = ano?.periodo.orEmpty()

    Surface(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = Espacos.m)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Espacos.l, end = Espacos.xs, top = Espacos.m, bottom = Espacos.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(nome = aluno?.nome)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = Espacos.m)
                ) {
                    Text(
                        text = aluno?.nome.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (ano != null) {
                        Text(
                            text = descricaoDoAno(ano),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onAbrirAjustesAnoLetivo) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = "Ajustes do ano letivo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (ano != null && periodos.isNotEmpty()) {
                val opcoes = listOf(OpcaoPeriodo(completo = "Ano letivo", curto = "Ano")) +
                    periodos.map { periodo ->
                        val nome = nomeDoPeriodo(periodo.periodo, ano.tipoPeriodo)
                        OpcaoPeriodo(completo = nome, curto = nome.substringBefore(' '))
                    }
                SeletorPeriodo(
                    opcoes = opcoes,
                    // -1 quando o período salvo não existe mais: nenhuma opção fica marcada
                    selecionada = if (contexto.periodoId == null) {
                        0
                    } else {
                        periodos.indexOfFirst { it.id == contexto.periodoId }.let { if (it < 0) -1 else it + 1 }
                    },
                    onSelecionar = { i -> onSelectPeriodo(if (i == 0) null else periodos[i - 1]) },
                    modifier = Modifier.padding(horizontal = Espacos.l),
                )
            }
        }
    }
}

/** "9º Ano · 2024"; sem série, só o ano. */
private fun descricaoDoAno(ano: AnoLetivoDomain): String =
    listOfNotNull(ano.serie?.takeIf { it.isNotBlank() }, ano.ano.toString()).joinToString(" · ")

@Composable
private fun Avatar(nome: String?, modifier: Modifier = Modifier) {
    val iniciais = nome?.let(::iniciais).orEmpty()
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        if (iniciais.isNotEmpty()) {
            Text(
                text = iniciais,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

/** "James Willian" → "JW"; um nome só → a primeira letra dele. */
internal fun iniciais(nome: String): String {
    val partes = nome.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when (partes.size) {
        0 -> ""
        1 -> partes[0].take(1)
        else -> partes.first().take(1) + partes.last().take(1)
    }.uppercase()
}

private data class OpcaoPeriodo(val completo: String, val curto: String)

/**
 * Trilho em pílula com o indicador que desliza até a opção escolhida. Se os nomes inteiros
 * ("1ª Unidade") não cabem todos na largura, só o escolhido fica inteiro e os outros encurtam
 * ("1ª"); se nem assim couber, todos encurtam. As opções dividem a largura na proporção do texto.
 */
@Composable
private fun SeletorPeriodo(
    opcoes: List<OpcaoPeriodo>,
    selecionada: Int,
    onSelecionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val estilo = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    val medidor = rememberTextMeasurer()
    val densidade = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val folga = with(densidade) { (PADDING_OPCAO * 2).roundToPx() }
        val trilho = with(densidade) { (PADDING_TRILHO * 2).roundToPx() }
        fun larguras(textos: List<String>) = textos.map { medidor.measure(it, estilo).size.width + folga }
        fun cabe(textos: List<String>) = larguras(textos).sum() + trilho <= constraints.maxWidth

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
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                            .selectable(selected = escolhida, role = Role.Tab, onClick = { onSelecionar(i) })
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
