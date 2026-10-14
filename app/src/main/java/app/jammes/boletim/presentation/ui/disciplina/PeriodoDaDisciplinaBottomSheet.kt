package app.jammes.boletim.presentation.ui.disciplina

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.jammes.boletim.presentation.ui.anoletivo.DatasPeriodo
import app.jammes.boletim.presentation.ui.theme.Espacos
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Os dias em que a disciplina tem aula, no mesmo calendário de intervalo das datas dos períodos nos
 * ajustes do ano: o primeiro toque marca o início e o segundo, o fim. Só os dias do ano letivo
 * ([doAno]) podem ser tocados. Aplicar leva o período para o formulário da tela, e quem grava é o
 * Salvar dela.
 *
 * Sem período próprio, a disciplina tem aula o ano letivo inteiro, e o calendário abre com ele todo
 * marcado. "Ano inteiro" volta a essa marcação.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeriodoDaDisciplinaBottomSheet(
    periodo: DatasPeriodo?, // null: o ano letivo inteiro
    doAno: DatasPeriodo?, // do primeiro ao último dia dos períodos do ano; null num ano sem períodos
    ano: Int?, // o ano letivo: uma data só mostra o ano quando foge dele
    onAplicar: (DatasPeriodo?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val inicial = periodo ?: doAno
    val calendario = rememberDateRangePickerState(
        initialSelectedStartDateMillis = inicial?.inicio?.paraMillisUtc(),
        initialSelectedEndDateMillis = inicial?.fim?.paraMillisUtc(),
        initialDisplayedMonthMillis = inicial?.inicio?.paraMillisUtc(),
        yearRange = anosDoCalendario(periodo, doAno, ano),
        selectableDates = remember(doAno) { DiasDoAnoLetivo(doAno) },
    )
    val inicio = calendario.selectedStartDateMillis?.let(::millisUtcParaData)
    val fim = calendario.selectedEndDateMillis?.let(::millisUtcParaData)
    val anoInteiro = doAno != null && inicio == doAno.inicio && fim == doAno.fim

    // Esconde com a animação e só então avisa a tela, que tira o sheet da composição
    fun fechar() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
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
                .padding(bottom = 32.dp),
        ) {
            // O botão fica na linha do título, que tem sempre a altura dele: ao aparecer ou sumir,
            // nada pula. O resumo vem embaixo, com a largura toda, e cabe numa linha
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .padding(start = 20.dp, end = Espacos.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Período",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                // Marca o ano todo de novo e volta o calendário para o começo dele
                if (doAno != null) {
                    AnimatedVisibility(visible = !anoInteiro, enter = fadeIn(), exit = fadeOut()) {
                        TextButton(
                            onClick = {
                                calendario.setSelection(doAno.inicio.paraMillisUtc(), doAno.fim.paraMillisUtc())
                                calendario.displayedMonthMillis = doAno.inicio.paraMillisUtc()
                            },
                        ) { Text("Ano inteiro") }
                    }
                }
            }
            Text(
                text = resumoDaEscolhaDoPeriodo(inicio, fim, doAno, ano),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    // O leitor de tela anuncia o intervalo a cada toque no calendário
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )

            DateRangePicker(
                state = calendario,
                // O título e as datas grandes do calendário repetiriam o que já está em cima
                title = null,
                headline = null,
                showModeToggle = false,
                // Transparente: o calendário fica com a cor do sheet, sem um retângulo de outro tom
                colors = DatePickerDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Espacos.s),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { fechar() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancelar") }

                Button(
                    onClick = {
                        if (inicio != null && fim != null) {
                            onAplicar(periodoEscolhido(inicio, fim, doAno))
                            fechar()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = inicio != null && fim != null,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Aplicar") }
            }
        }
    }
}

/** O calendário só deixa tocar nos dias do ano letivo; sem as datas dele, em qualquer dia. */
@OptIn(ExperimentalMaterial3Api::class)
private class DiasDoAnoLetivo(private val doAno: DatasPeriodo?) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        doAno == null || millisUtcParaData(utcTimeMillis) in doAno.inicio..doAno.fim

    override fun isSelectableYear(year: Int): Boolean =
        doAno == null || year in doAno.inicio.year..doAno.fim.year
}

/**
 * Os anos que o calendário mostra: os do ano letivo, esticados se o período gravado fugir deles (o
 * calendário não aceita uma data marcada fora dos anos dele). Sem as datas do ano, de um ano antes a
 * um depois do letivo, como nos ajustes.
 */
internal fun anosDoCalendario(periodo: DatasPeriodo?, doAno: DatasPeriodo?, ano: Int?): IntRange {
    val referencia = ano ?: periodo?.inicio?.year ?: LocalDate.now().year
    val primeiro = doAno?.inicio?.year ?: (referencia - 1)
    val ultimo = doAno?.fim?.year ?: (referencia + 1)
    return minOf(primeiro, periodo?.inicio?.year ?: primeiro)..maxOf(ultimo, periodo?.fim?.year ?: ultimo)
}
