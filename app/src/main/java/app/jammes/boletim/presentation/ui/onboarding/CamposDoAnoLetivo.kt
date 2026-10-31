package app.jammes.boletim.presentation.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.jammes.boletim.domain.model.CriacaoDoAno
import app.jammes.boletim.domain.model.Plano
import app.jammes.boletim.domain.model.criacaoDoAno
import app.jammes.boletim.presentation.ui.components.Avatar
import app.jammes.boletim.presentation.ui.components.EncolherSemPartirPalavra
import app.jammes.boletim.presentation.ui.plano.EtiquetaPro
import app.jammes.boletim.presentation.ui.plano.FolhaDoPro
import app.jammes.boletim.presentation.ui.theme.Espacos
import java.time.LocalDate

// As peças do ano letivo que o onboarding e o "Criar novo ano letivo" do perfil mostram iguais: a
// prévia do topo, a série e o ano. A divisão do ano é um passo inteiro, o PassoDivisao.

/** Como o aluno vai aparecer no topo do app, já enquanto se digita. */
@Composable
internal fun PreviaDaIdentificacao(nome: String, descricao: String) {
    val cores = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(cores.surfaceContainerLow)
            .border(1.dp, cores.outlineVariant, MaterialTheme.shapes.large)
            .padding(horizontal = Espacos.l, vertical = Espacos.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(nome = nome.ifBlank { null })
        Column(Modifier.weight(1f).padding(start = Espacos.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = nome.ifBlank { "Nome do aluno" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (nome.isBlank()) cores.onSurfaceVariant else cores.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = cores.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Text(descricao, style = MaterialTheme.typography.bodyMedium, color = cores.onSurfaceVariant)
        }
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = cores.onSurfaceVariant)
    }
}

@Composable
internal fun EscolhaDaSerie(serie: Serie?, onEscolher: (Serie?) -> Unit) {
    val haptico = LocalHapticFeedback.current
    // Tocar de novo na escolhida tira a série: ela é opcional
    val escolher = { opcao: Serie ->
        haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
        onEscolher(opcao.takeUnless { it == serie })
    }

    TituloDaSecao("Série", detalhe = "Opcional")
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Os 9 anos do Fundamental cabem numa linha só; o Médio usa o mesmo tamanho de círculo
        val tamanho = (maxWidth - ESPACO_ENTRE_SERIES * 8) / 9
        Column {
            Text("Ensino Fundamental", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Espacos.s))
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(ESPACO_ENTRE_SERIES)) {
                (1..9).forEach { n ->
                    val opcao = Serie.Fundamental(n)
                    CirculoDaSerie("${n}º", selecionado = serie == opcao, tamanho = tamanho, onClick = { escolher(opcao) })
                }
            }
            Spacer(Modifier.height(Espacos.m))
            Text("Ensino Médio", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(ESPACO_ENTRE_SERIES),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                (1..3).forEach { n ->
                    val opcao = Serie.Medio(n)
                    CirculoDaSerie("${n}ª", selecionado = serie == opcao, tamanho = tamanho, onClick = { escolher(opcao) })
                }
                Spacer(Modifier.width(Espacos.s))
                FilterChip(
                    selected = serie is Serie.Outra,
                    onClick = {
                        haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onEscolher(if (serie is Serie.Outra) null else Serie.Outra())
                    },
                    label = { Text("Outra") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                    },
                )
            }
        }
    }
    AnimatedVisibility(visible = serie is Serie.Outra) {
        OutlinedTextField(
            value = (serie as? Serie.Outra)?.texto.orEmpty(),
            onValueChange = { onEscolher(Serie.Outra(it)) },
            label = { Text("Qual série?") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().padding(top = Espacos.s),
        )
    }
}

private val ESPACO_ENTRE_SERIES = 4.dp

@Composable
private fun CirculoDaSerie(texto: String, selecionado: Boolean, tamanho: Dp, onClick: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(tamanho)
            .clip(CircleShape)
            .background(if (selecionado) cores.primary else Color.Transparent)
            .border(1.dp, if (selecionado) cores.primary else cores.outlineVariant, CircleShape)
            .selectable(selected = selecionado, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selecionado) cores.onPrimary else cores.onSurfaceVariant,
        )
    }
}

/**
 * O ano letivo, nos [anosParaEscolher]. Os [existentes], que o aluno já tem, aparecem apagados e não
 * dá para escolher: o título da seção diz por quê. No onboarding o aluno ainda não tem nenhum.
 *
 * Os anos que o [plano] não libera levam a etiqueta do Pro, e o toque abre a folha dele em vez de
 * escolher. No Pro, os [existentes] também dão para escolher: ele repete o ano, e a tela avisa. Enquanto
 * o plano carrega (null), nenhum leva a etiqueta e os existentes ficam apagados: quem cria o ano confere
 * de novo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EscolhaDoAno(ano: Int, plano: Plano?, onEscolher: (Int) -> Unit, existentes: Set<Int> = emptySet()) {
    val hoje = remember { LocalDate.now() }
    val anos = remember(existentes, plano) { anosParaEscolher(existentes, hoje, plano ?: Plano.PRO) }
    var folhaDoPro by rememberSaveable { mutableStateOf(false) }

    TituloDaSecao("Ano letivo", detalhe = anosQueJaExistem(anos.filter { it in existentes }))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        anos.forEachIndexed { i, opcao ->
            val criacao = plano?.criacaoDoAno(opcao, existentes, hoje)
            val soNoPro = criacao == CriacaoDoAno.SO_NO_PRO
            SegmentedButton(
                selected = ano == opcao,
                onClick = { if (soNoPro) folhaDoPro = true else onEscolher(opcao) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = anos.size),
                enabled = opcao !in existentes || criacao == CriacaoDoAno.REPETIDA,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // A etiqueta é medida antes: com a fonte grande, o ano encolhe para ela caber
                    Text(
                        text = opcao.toString(),
                        maxLines = 1,
                        autoSize = EncolherSemPartirPalavra(minimo = 11.sp, maximo = MaterialTheme.typography.labelLarge.fontSize),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (soNoPro) {
                        Spacer(Modifier.width(Espacos.xs))
                        EtiquetaPro(compacta = true)
                    }
                }
            }
        }
    }

    // Com o Pro desbloqueado, ela fecha sozinha, e os anos perdem a etiqueta
    if (folhaDoPro) FolhaDoPro(onFechar = { folhaDoPro = false })
}
