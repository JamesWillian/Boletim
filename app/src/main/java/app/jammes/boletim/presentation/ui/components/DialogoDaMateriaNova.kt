package app.jammes.boletim.presentation.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization

/**
 * O nome de uma matéria nova, com o teclado já aberto. "Criar" só aceita um nome que ainda não está
 * nos [existentes]. É o mesmo no onboarding ("Outra matéria") e nas matérias do ano ("Nova matéria"):
 * os dois guardam só o nome, e a matéria entra no cadastro quando o resto é gravado.
 */
@Composable
internal fun DialogoDaMateriaNova(
    titulo: String,
    existentes: List<String>,
    onCriar: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var nome by rememberSaveable { mutableStateOf("") }
    val repetido = nomeRepetido(nome, existentes)
    val podeCriar = nome.isNotBlank() && !repetido
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome da matéria") },
                singleLine = true,
                isError = repetido,
                supportingText = if (repetido) {
                    { Text("Essa matéria já está na lista") }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (podeCriar) onCriar(nome.trim()) }),
                modifier = Modifier.focusRequester(foco),
            )
        },
        confirmButton = { TextButton(onClick = { onCriar(nome.trim()) }, enabled = podeCriar) { Text("Criar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** O nome já está na lista (do cadastro ou criado aqui), sem ligar para maiúsculas e espaços. */
internal fun nomeRepetido(nome: String, existentes: List<String>): Boolean =
    existentes.any { it.trim().equals(nome.trim(), ignoreCase = true) }
