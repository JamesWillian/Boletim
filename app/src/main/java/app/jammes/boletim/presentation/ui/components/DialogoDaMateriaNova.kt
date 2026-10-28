package app.jammes.boletim.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.AlertDialog
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
import app.jammes.boletim.presentation.ui.theme.Espacos

/**
 * O nome e a sigla de uma matéria nova, com o teclado já aberto no nome. "Criar" só aceita um nome
 * que ainda não está nos [existentes]. É o mesmo no onboarding ("Outra matéria") e nas matérias do
 * ano ("Nova matéria"): os dois guardam o nome e a sigla, e a matéria entra no cadastro quando o resto
 * é gravado. Os campos são os da edição da disciplina: nome com até 20 caracteres e sigla com até 3,
 * que vazia sai do nome.
 */
@Composable
internal fun DialogoDaMateriaNova(
    titulo: String,
    existentes: List<String>,
    onCriar: (nome: String, abreviacao: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var nome by rememberSaveable { mutableStateOf("") }
    var abreviacao by rememberSaveable { mutableStateOf("") }
    val repetido = nomeRepetido(nome, existentes)
    val podeCriar = nome.isNotBlank() && !repetido
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }

    fun criar() {
        if (podeCriar) onCriar(nome.trim(), abreviacao)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacos.s)) {
                CampoDoNome(
                    valor = nome,
                    onMudar = { nome = it },
                    rotulo = "Nome da matéria",
                    erro = if (repetido) "Essa matéria já está na lista" else null,
                    modifier = Modifier.focusRequester(foco),
                )
                CampoDaSigla(
                    valor = abreviacao,
                    nome = nome,
                    onMudar = { abreviacao = it },
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { criar() }),
                )
            }
        },
        confirmButton = { TextButton(onClick = { criar() }, enabled = podeCriar) { Text("Criar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** O nome já está na lista (do cadastro ou criado aqui), sem ligar para maiúsculas e espaços. */
internal fun nomeRepetido(nome: String, existentes: List<String>): Boolean =
    existentes.any { it.trim().equals(nome.trim(), ignoreCase = true) }
