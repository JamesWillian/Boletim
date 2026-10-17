package app.jammes.boletim.presentation.ui.notificacoes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.jammes.boletim.presentation.ui.theme.Espacos
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * O relógio do Material para escolher um horário das notificações, sempre em 24 horas. Serve para um
 * horário novo ([horario] null), que abre numa sugestão que ainda não está na lista, e para mudar um
 * que já está. Um horário que já está nos [outros] não dá para salvar de novo: o aviso aparece
 * embaixo do relógio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HorarioBottomSheet(
    horario: LocalTime?,
    outros: List<LocalTime>, // os da lista, menos o que está mudando
    onSalvar: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val inicial = remember { horario ?: sugestaoDeHorario(outros) }
    // Salvo como o rememberSaveable: a escolha sobrevive a girar a tela
    val relogio = rememberTimePickerState(initialHour = inicial.hour, initialMinute = inicial.minute, is24Hour = true)
    val escolhido = LocalTime.of(relogio.hour, relogio.minute)
    val repetido = escolhido in outros

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Espacos.xs)) {
                Text(
                    text = if (horario == null) "Novo horário" else "Mudar horário",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "De preferência fora do horário das aulas, para não atrapalhar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TimePicker(
                state = relogio,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            AnimatedVisibility(visible = repetido) {
                Text(
                    text = "${textoDaHora(escolhido)} já está na lista.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    // O leitor de tela avisa assim que o relógio para num horário repetido
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

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
                        onSalvar(escolhido)
                        fechar()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !repetido,
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (horario == null) "Adicionar" else "Salvar") }
            }
        }
    }
}

/**
 * Onde o relógio abre para um horário novo: o primeiro destes que ainda não está na lista. Todos longe
 * das aulas mais comuns: o começo da noite, antes da aula da manhã, o almoço e mais tarde à noite.
 */
internal fun sugestaoDeHorario(existentes: List<LocalTime>): LocalTime =
    SUGESTOES.firstOrNull { it !in existentes } ?: LocalTime.NOON

private val SUGESTOES = listOf(
    LocalTime.of(19, 0),
    LocalTime.of(6, 30),
    LocalTime.of(12, 30),
    LocalTime.of(21, 0),
)
