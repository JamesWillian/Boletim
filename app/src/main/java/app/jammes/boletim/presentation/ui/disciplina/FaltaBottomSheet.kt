package app.jammes.boletim.presentation.ui.disciplina

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.FaltaDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.domain.model.intervalo
import app.jammes.boletim.domain.model.periodoDe
import app.jammes.boletim.presentation.ui.anoletivo.feminino
import app.jammes.boletim.presentation.ui.anoletivo.nomeDoPeriodo
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Formulário de falta: o dia e quantas faltas teve nele. Cria quando o id é 0, senão edita e
 * permite excluir. Como o [AvaliacaoBottomSheet], só monta a falta; quem grava e exclui é a tela.
 *
 * O período não é perguntado: sai da data, entre os [periodos] (os que a tela mostra). O
 * calendário só deixa escolher dias dentro deles, então a falta não vai parar fora da lista aberta.
 *
 * Cada dia tem um lançamento só: um dia que já está em [faltasLancadas] não pode ser lançado de
 * novo. Para mudar a quantidade dele, edita-se o lançamento que já existe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaltaBottomSheet(
    falta: FaltaDomain,
    periodos: List<PeriodoDomain>,
    tipoPeriodo: TipoPeriodo,
    faltasLancadas: List<FaltaDomain>,
    onDismiss: () -> Unit,
    onSalvar: (FaltaDomain) -> Unit,
    onExcluir: () -> Unit,
) {
    val nova = falta.id == 0L
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // rememberSaveable: o que já foi escolhido sobrevive a girar a tela
    var data by rememberSaveable { mutableStateOf(falta.data) }
    var quantidade by rememberSaveable { mutableIntStateOf(falta.qtdAulas) }
    var escolhendoData by rememberSaveable { mutableStateOf(false) }
    var confirmandoExclusao by rememberSaveable { mutableStateOf(false) }

    val periodo = periodos.periodoDe(data)
    // Outro lançamento no mesmo dia; o próprio, quando editando, não conta
    val diaOcupado = faltasLancadas.any { it.id != falta.id && it.data == data }

    // A falta pronta para gravar, ou null enquanto ela não puder ser gravada
    val preenchida =
        if (periodo == null || diaOcupado) null
        else falta.copy(periodoId = periodo.id, data = data, qtdAulas = quantidade)

    // Esconde com a animação e só então avisa a tela, que tira o sheet da composição
    fun fechar(depois: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                depois()
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (nova) "Nova falta" else "Editar falta",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (!nova) {
                    TextButton(
                        onClick = { confirmandoExclusao = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Excluir") }
                }
            }

            // O campo não aceita digitação: tocar nele (ou no ícone) abre o calendário
            val toqueNaData = remember { MutableInteractionSource() }
            LaunchedEffect(toqueNaData) {
                toqueNaData.interactions.collect {
                    if (it is PressInteraction.Release) escolhendoData = true
                }
            }
            OutlinedTextField(
                value = data.format(FORMATO_DATA_COMPLETA),
                onValueChange = {},
                readOnly = true,
                label = { Text("Data") },
                isError = diaOcupado,
                supportingText = when {
                    diaOcupado -> {
                        { Text("Esse dia já tem falta lançada. Para mudar a quantidade, edite o dia na lista.") }
                    }
                    // No ano letivo inteiro é a data que decide o período: o aviso mostra qual
                    periodo != null && periodos.size > 1 -> {
                        {
                            val artigo = if (tipoPeriodo.feminino) "na" else "no"
                            Text("Conta $artigo ${nomeDoPeriodo(periodo.periodo, tipoPeriodo)}")
                        }
                    }
                    else -> null
                },
                trailingIcon = {
                    IconButton(onClick = { escolhendoData = true }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Escolher data")
                    }
                },
                interactionSource = toqueNaData,
                modifier = Modifier.fillMaxWidth()
            )

            SeletorDeQuantidade(
                quantidade = quantidade,
                onMudar = { quantidade = it },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { fechar() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancelar") }

                Button(
                    onClick = {
                        preenchida?.let {
                            onSalvar(it)
                            fechar()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = preenchida != null,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Salvar") }
            }
        }
    }

    if (escolhendoData) {
        val intervalo = periodos.intervalo()
        val calendario = rememberDatePickerState(
            initialSelectedDateMillis = data.paraMillisUtc(),
            // Só os dias dos períodos da tela: fora deles a falta não teria onde contar
            selectableDates = remember(intervalo) { DiasDoIntervalo(intervalo) },
        )
        DatePickerDialog(
            onDismissRequest = { escolhendoData = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        calendario.selectedDateMillis?.let { data = millisUtcParaData(it) }
                        escolhendoData = false
                    },
                    // Digitando, uma data fora dos períodos deixa o calendário sem dia escolhido
                    enabled = calendario.selectedDateMillis != null,
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { escolhendoData = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = calendario)
        }
    }

    if (confirmandoExclusao) {
        AlertDialog(
            onDismissRequest = { confirmandoExclusao = false },
            title = { Text("Excluir falta?") },
            text = {
                val dia = falta.data.format(FORMATO_DATA_COMPLETA)
                Text(
                    if (falta.qtdAulas == 1) "A falta de $dia será apagada, e não dá para desfazer."
                    else "As ${falta.qtdAulas} faltas de $dia serão apagadas, e não dá para desfazer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoExclusao = false
                    // Exclui só depois de fechar: se a falta sumisse da lista antes,
                    // o sheet sairia da tela sem a animação
                    fechar(depois = onExcluir)
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoExclusao = false }) { Text("Cancelar") }
            }
        )
    }
}

/**
 * O "– 2 +" do formulário: o número grande no meio e um botão de cada lado. Vai de 1 (para zerar,
 * exclui-se o dia) até [MAX_FALTAS_NO_DIA].
 */
@Composable
private fun SeletorDeQuantidade(
    quantidade: Int,
    onMudar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptico = LocalHapticFeedback.current
    // Um "tique" a cada toque, como num seletor de verdade
    fun mudar(nova: Int) {
        haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
        onMudar(nova)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = { mudar(quantidade - 1) },
            enabled = quantidade > 1,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "Menos uma falta")
        }
        Column(
            modifier = Modifier
                .width(112.dp)
                // Lidos juntos ("2 faltas"), e o leitor de tela anuncia o número novo a cada toque
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = quantidade,
                transitionSpec = {
                    // Como no MediaAnimada: subindo, o número novo entra por baixo; descendo, por cima
                    val sentido = if (targetState > initialState) 1 else -1
                    (slideInVertically { altura -> sentido * altura } + fadeIn()) togetherWith
                        (slideOutVertically { altura -> -sentido * altura } + fadeOut()) using
                        SizeTransform(clip = true)
                },
                label = "quantidadeDeFaltas",
            ) { valor ->
                Text(
                    text = valor.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = if (quantidade == 1) "falta" else "faltas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalIconButton(
            onClick = { mudar(quantidade + 1) },
            enabled = quantidade < MAX_FALTAS_NO_DIA,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Mais uma falta")
        }
    }
}

// Nenhum dia tem tantas aulas de uma disciplina: o limite só evita um número sem sentido
private const val MAX_FALTAS_NO_DIA = 10

/** O calendário só deixa tocar nos dias de [intervalo] (sem ele, em qualquer um). */
private class DiasDoIntervalo(private val intervalo: ClosedRange<LocalDate>?) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        intervalo == null || millisUtcParaData(utcTimeMillis) in intervalo

    override fun isSelectableYear(year: Int): Boolean =
        intervalo == null || year in intervalo.start.year..intervalo.endInclusive.year
}
