package app.jammes.boletim.presentation.ui.disciplina

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.presentation.ui.theme.Espacos
import kotlinx.coroutines.launch

/**
 * Quantas aulas a disciplina tem no ano. Com o total, a frequência mínima da [regra] vira um limite
 * de faltas, e o campo já mostra esse limite enquanto o número é digitado. Vazio volta a "não
 * informado"; zero não dá para aplicar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TotalDeAulasBottomSheet(
    totalAulas: Int?,
    regra: RegraAvaliacaoDomain,
    onAplicar: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // rememberSaveable: o que já foi digitado sobrevive a girar a tela
    var texto by rememberSaveable { mutableStateOf(totalAulas?.toString().orEmpty()) }
    val digitado = lerTotalDeAulas(texto)

    // Esconde com a animação e só então avisa a tela, que tira o sheet da composição
    fun fechar() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    fun aplicar() {
        when (digitado) {
            TotalDigitado.Vazio -> onAplicar(null)
            is TotalDigitado.Valido -> onAplicar(digitado.aulas)
            TotalDigitado.Invalido -> return
        }
        fechar()
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
                    text = "Total de aulas",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Quantas aulas a disciplina tem no ano letivo inteiro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = texto,
                // Só algarismos: assim o que estiver no campo é sempre um número inteiro
                onValueChange = { novo -> texto = novo.filter(Char::isDigit).take(DIGITOS_DO_TOTAL) },
                label = { Text("Aulas no ano") },
                isError = digitado == TotalDigitado.Invalido,
                supportingText = {
                    Text(
                        text = dicaDoTotalDeAulas(digitado, regra),
                        // O leitor de tela anuncia o limite novo enquanto o número é digitado
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { aplicar() }),
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
                    onClick = { aplicar() },
                    modifier = Modifier.weight(1f),
                    enabled = digitado != TotalDigitado.Invalido,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Aplicar") }
            }
        }
    }
}
