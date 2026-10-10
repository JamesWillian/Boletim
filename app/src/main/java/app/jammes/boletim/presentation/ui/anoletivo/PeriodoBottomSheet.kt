package app.jammes.boletim.presentation.ui.anoletivo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.presentation.ui.disciplina.millisUtcParaData
import app.jammes.boletim.presentation.ui.disciplina.paraMillisUtc
import app.jammes.boletim.presentation.ui.theme.Espacos
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * As datas de um período num calendário de intervalo: o primeiro toque marca o início e o segundo,
 * o fim, então o fim nunca fica antes do início. Aplicar leva as datas para o formulário da tela, e
 * quem grava é o Salvar dela.
 *
 * Quando a escolha atropela um vizinho, o sheet avisa, mas deixa aplicar: às vezes é preciso mexer
 * num período antes do outro. O painel continua mostrando o problema, e o Salvar espera ele sumir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeriodoBottomSheet(
    numero: Int,
    tipo: TipoPeriodo,
    datas: DatasPeriodo,
    anterior: DatasPeriodo?, // o período de antes; null no primeiro
    seguinte: DatasPeriodo?, // o de depois; null no último
    ano: Int?, // o ano letivo: uma data só mostra o ano quando foge dele
    onAplicar: (DatasPeriodo) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // De um ano antes a um depois do letivo, esticado se as datas de agora fugirem disso
    val referencia = ano ?: datas.inicio.year
    val calendario = rememberDateRangePickerState(
        initialSelectedStartDateMillis = datas.inicio.paraMillisUtc(),
        initialSelectedEndDateMillis = datas.fim.paraMillisUtc(),
        initialDisplayedMonthMillis = datas.inicio.paraMillisUtc(),
        yearRange = minOf(referencia - 1, datas.inicio.year)..maxOf(referencia + 1, datas.fim.year),
    )
    val inicio = calendario.selectedStartDateMillis?.let(::millisUtcParaData)
    val fim = calendario.selectedEndDateMillis?.let(::millisUtcParaData)
    val escolhidas = if (inicio != null && fim != null) DatasPeriodo(inicio, fim) else null
    val aviso = escolhidas?.let { conflitoComVizinhos(it, numero, tipo, anterior, seguinte, ano) }

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
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = nomeDoPeriodo(numero, tipo),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = resumoDaEscolha(inicio, fim, ano),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // O leitor de tela anuncia o intervalo a cada toque no calendário
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                if (aviso != null) {
                    Text(
                        text = aviso,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = Espacos.xs),
                    )
                }
            }

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
                        escolhidas?.let {
                            onAplicar(it)
                            fechar()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = escolhidas != null,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Aplicar") }
            }
        }
    }
}

/** O que está marcado no calendário enquanto a pessoa escolhe: "1 fev – 30 abr · 13 semanas". */
internal fun resumoDaEscolha(inicio: LocalDate?, fim: LocalDate?, ano: Int?): String = when {
    inicio != null && fim != null -> resumoDasDatas(DatasPeriodo(inicio, fim), ano)
    inicio != null -> "Começa em ${dataCurta(inicio, ano)}; agora toque no último dia"
    else -> "Toque no primeiro e no último dia"
}

/**
 * O que fazer quando as [datas] atropelam um vizinho; null quando elas cabem entre o [anterior] e o
 * [seguinte]. A regra é a do problemasDasDatas: um período só começa depois que o anterior acaba.
 */
internal fun conflitoComVizinhos(
    datas: DatasPeriodo,
    numero: Int,
    tipo: TipoPeriodo,
    anterior: DatasPeriodo?,
    seguinte: DatasPeriodo?,
    ano: Int?,
): String? {
    val artigo = if (tipo.feminino) "A" else "O"
    return when {
        anterior != null && datas.inicio <= anterior.fim ->
            "$artigo ${nomeDoPeriodo(numero - 1, tipo)} vai até ${dataCurta(anterior.fim, ano)}: comece depois disso."
        seguinte != null && datas.fim >= seguinte.inicio ->
            "$artigo ${nomeDoPeriodo(numero + 1, tipo)} começa em ${dataCurta(seguinte.inicio, ano)}: termine antes disso."
        else -> null
    }
}
