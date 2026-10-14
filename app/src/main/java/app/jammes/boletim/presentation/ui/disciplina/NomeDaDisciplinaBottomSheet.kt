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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.jammes.boletim.presentation.ui.components.siglaDoNome
import app.jammes.boletim.presentation.ui.theme.Espacos
import kotlinx.coroutines.launch

/**
 * O nome, a sigla e o professor da disciplina. Aplicar leva os três para o formulário da tela, e
 * quem grava é o Salvar dela. Sem nome não dá para aplicar, então o formulário nunca recebe um nome
 * vazio. A sigla, gravada como a abreviação da disciplina, é a das abas, com até 3 caracteres;
 * vazia, ela sai do nome, e o campo mostra qual seria.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NomeDaDisciplinaBottomSheet(
    nome: String,
    abreviacao: String,
    professor: String,
    onAplicar: (nome: String, abreviacao: String, professor: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // rememberSaveable: o que já foi digitado sobrevive a girar a tela
    var nomeDigitado by rememberSaveable { mutableStateOf(nome) }
    var abreviacaoDigitada by rememberSaveable { mutableStateOf(abreviacao) }
    var professorDigitado by rememberSaveable { mutableStateOf(professor) }
    val semNome = nomeDigitado.isBlank()

    // Esconde com a animação e só então avisa a tela, que tira o sheet da composição
    fun fechar() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    fun aplicar() {
        if (semNome) return
        onAplicar(nomeDigitado, abreviacaoDigitada, professorDigitado)
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
            Text(
                text = "Disciplina",
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = nomeDigitado,
                onValueChange = { nomeDigitado = it },
                label = { Text("Nome") },
                isError = semNome,
                supportingText = if (semNome) {
                    { Text("Dê um nome à disciplina") }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = abreviacaoDigitada,
                // Já em maiúsculas, como nas abas, e o que passar de 3 caracteres não entra
                onValueChange = { abreviacaoDigitada = lerAbreviacao(it) },
                label = { Text("Sigla") },
                // Vazia, as abas tiram a sigla do nome: ela aparece de exemplo
                placeholder = { Text(siglaDoNome(nomeDigitado)) },
                supportingText = {
                    Row(horizontalArrangement = Arrangement.spacedBy(Espacos.s)) {
                        Text("Aparece nas abas no estilo “Ícones + sigla”", Modifier.weight(1f))
                        Text("${abreviacaoDigitada.length}/$TAMANHO_DA_ABREVIACAO")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    // Uma sigla não é palavra: o corretor só atrapalharia
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = professorDigitado,
                onValueChange = { professorDigitado = it },
                label = { Text("Professor") },
                placeholder = { Text("Opcional") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
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
                    enabled = !semNome,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Aplicar") }
            }
        }
    }
}
