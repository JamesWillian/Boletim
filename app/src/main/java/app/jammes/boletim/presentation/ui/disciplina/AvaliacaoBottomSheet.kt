package app.jammes.boletim.presentation.ui.disciplina

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.TipoAvaliacao
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Formulário de avaliação: cria quando o id é 0, senão edita e permite excluir.
 * Só monta a avaliação; quem grava e exclui é a tela, pelos callbacks.
 *
 * A nota pode ficar vazia: é a avaliação que ainda não aconteceu, e ela fica fora da média.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvaliacaoBottomSheet(
    avaliacao: AvaliacaoDomain,
    mostrarPeso: Boolean,
    onDismiss: () -> Unit,
    onSalvar: (AvaliacaoDomain) -> Unit,
    onExcluir: () -> Unit,
) {
    val nova = avaliacao.id == 0L
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // rememberSaveable: o que já foi digitado sobrevive a girar a tela
    var nome by rememberSaveable { mutableStateOf(avaliacao.nome) }
    var nota by rememberSaveable { mutableStateOf(avaliacao.nota?.let(::formatarNumero).orEmpty()) }
    var notaMaxima by rememberSaveable { mutableStateOf(formatarNumero(avaliacao.notaMaxima)) }
    var peso by rememberSaveable { mutableStateOf(formatarNumero(avaliacao.peso)) }
    var tipo by rememberSaveable { mutableStateOf(avaliacao.tipo) }
    var data by rememberSaveable { mutableStateOf(avaliacao.data) }
    var escolhendoData by rememberSaveable { mutableStateOf(false) }
    var confirmandoExclusao by rememberSaveable { mutableStateOf(false) }

    val notaMaximaValor = lerNumero(notaMaxima)?.takeIf { it > 0 }
    val notaValor = lerNumero(nota)?.takeIf { it >= 0 && (notaMaximaValor == null || it <= notaMaximaValor) }
    val notaInvalida = nota.isNotBlank() && notaValor == null
    // Escondido, o peso fica como estava: fora da média ponderada ele não muda a conta
    val pesoValor = if (mostrarPeso) lerNumero(peso)?.takeIf { it > 0 } else avaliacao.peso

    // A avaliação pronta para gravar, ou null enquanto algum campo estiver inválido
    val preenchida =
        if (nome.isBlank() || notaMaximaValor == null || notaInvalida || pesoValor == null) null
        else avaliacao.copy(
            nome = nome.trim(),
            nota = notaValor, // vazia fica null
            notaMaxima = notaMaximaValor,
            peso = pesoValor,
            tipo = tipo,
            data = data,
        )

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
                    text = if (nova) "Nova avaliação" else "Editar avaliação",
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

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = nota,
                onValueChange = { nota = it },
                label = { Text("Nota") },
                isError = notaInvalida,
                supportingText = {
                    Text(
                        if (notaInvalida) "Use um número entre 0 e a nota máxima"
                        else "Deixe vazia se a avaliação ainda não aconteceu"
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = notaMaxima,
                    onValueChange = { notaMaxima = it },
                    label = { Text("Nota máxima") },
                    isError = notaMaximaValor == null,
                    supportingText = if (notaMaximaValor == null) {
                        { Text("Maior que zero") }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                if (mostrarPeso) {
                    OutlinedTextField(
                        value = peso,
                        onValueChange = { peso = it },
                        label = { Text("Peso") },
                        isError = pesoValor == null,
                        supportingText = if (pesoValor == null) {
                            { Text("Maior que zero") }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Tipo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TipoAvaliacao.entries.forEach { opcao ->
                        FilterChip(
                            selected = tipo == opcao,
                            onClick = { tipo = opcao },
                            label = { Text(opcao.displayname) }
                        )
                    }
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
                value = data?.format(FORMATO_DATA_COMPLETA) ?: "Sem data",
                onValueChange = {},
                readOnly = true,
                label = { Text("Data") },
                trailingIcon = {
                    IconButton(onClick = { escolhendoData = true }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Escolher data")
                    }
                },
                interactionSource = toqueNaData,
                modifier = Modifier.fillMaxWidth()
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
        val calendario = rememberDatePickerState(
            initialSelectedDateMillis = (data ?: LocalDate.now()).paraMillisUtc()
        )
        DatePickerDialog(
            onDismissRequest = { escolhendoData = false },
            confirmButton = {
                TextButton(onClick = {
                    data = calendario.selectedDateMillis?.let(::millisUtcParaData)
                    escolhendoData = false
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        data = null
                        escolhendoData = false
                    }) { Text("Sem data") }
                    TextButton(onClick = { escolhendoData = false }) { Text("Cancelar") }
                }
            }
        ) {
            DatePicker(state = calendario)
        }
    }

    if (confirmandoExclusao) {
        AlertDialog(
            onDismissRequest = { confirmandoExclusao = false },
            title = { Text("Excluir avaliação?") },
            text = { Text("\"${avaliacao.nome}\" será apagada, e não dá para desfazer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoExclusao = false
                    // Exclui só depois de fechar: se a avaliação sumisse da lista antes,
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

internal val FORMATO_DATA_COMPLETA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// Aceita vírgula ou ponto. Vazio, ou texto que não é número, vira null.
internal fun lerNumero(texto: String): Double? =
    texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

// O DatePicker trabalha com a meia-noite UTC do dia escolhido, em milissegundos
internal fun LocalDate.paraMillisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun millisUtcParaData(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
