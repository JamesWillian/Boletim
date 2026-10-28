package app.jammes.boletim.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.jammes.boletim.presentation.ui.theme.Espacos

/**
 * O nome de uma matéria ou disciplina, com até [TAMANHO_DO_NOME] caracteres e o contador embaixo.
 * É o mesmo na edição da disciplina e na matéria nova. Com [erro], o campo fica vermelho e o texto
 * aparece antes do contador.
 */
@Composable
internal fun CampoDoNome(
    valor: String,
    onMudar: (String) -> Unit,
    rotulo: String,
    erro: String?, // null: nada de errado
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { onMudar(lerNome(it)) },
        label = { Text(rotulo) },
        isError = erro != null,
        supportingText = {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacos.s)) {
                Text(erro.orEmpty(), Modifier.weight(1f))
                Text("${valor.length}/$TAMANHO_DO_NOME")
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next,
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * A sigla das abas no estilo "Ícones + sigla", gravada como a abreviação. Vazia, as abas tiram a
 * sigla do [nome], e o campo mostra qual seria.
 */
@Composable
internal fun CampoDaSigla(
    valor: String,
    nome: String,
    onMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = valor,
        // Já em maiúsculas, como nas abas, e o que passar de 3 caracteres não entra
        onValueChange = { onMudar(lerAbreviacao(it)) },
        label = { Text("Sigla") },
        placeholder = { Text(siglaDoNome(nome)) },
        supportingText = {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacos.s)) {
                Text("Aparece nas abas no estilo “Ícones + sigla”", Modifier.weight(1f))
                Text("${valor.length}/$TAMANHO_DA_ABREVIACAO")
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            // Uma sigla não é palavra: o corretor só atrapalharia
            autoCorrectEnabled = false,
            imeAction = imeAction,
        ),
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth()
    )
}

/** O nome como fica no campo: o que passa de [TAMANHO_DO_NOME] caracteres não entra. */
internal fun lerNome(texto: String): String = texto.take(TAMANHO_DO_NOME)

/** O máximo de caracteres do nome de uma matéria ou disciplina. */
internal const val TAMANHO_DO_NOME = 20

/**
 * A abreviação como fica no campo: em maiúsculas, como as abas mostram, sem espaços e com até
 * [TAMANHO_DA_ABREVIACAO] caracteres.
 */
internal fun lerAbreviacao(texto: String): String =
    texto.filterNot(Char::isWhitespace).uppercase().take(TAMANHO_DA_ABREVIACAO)

/** O máximo de caracteres da abreviação, que vai embaixo do ícone, na aba fechada. */
internal const val TAMANHO_DA_ABREVIACAO = 3
