package app.jammes.boletim.presentation.ui.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.jammes.boletim.presentation.ui.theme.Espacos
import kotlinx.coroutines.launch

/*
 * As senhas digitadas ficam em remember, e não em rememberSaveable: assim elas não vão para o estado
 * que o Android salva da tela. Girar o celular apaga o que foi digitado, e é de propósito.
 */

/**
 * O que está no sheet da senha do exportar: se já dá para exportar, e com que senha. A senha vai
 * duas vezes porque não dá para recuperá-la: uma letra trocada deixaria o arquivo trancado.
 */
internal data class EscolhaDaSenha(
    val senha: String = "",
    val confirmacao: String = "",
    val semSenha: Boolean = false,
) {
    val curta: Boolean get() = senha.length < TAMANHO_MINIMO_DA_SENHA

    /**
     * A confirmação já não bate com a senha: uma letra diferente, ou mais comprida que ela. Enquanto
     * é o começo da senha, ainda está sendo digitada, e não é erro.
     */
    val confirmacaoErrada: Boolean get() = !senha.startsWith(confirmacao)

    val pronta: Boolean get() = semSenha || (!curta && confirmacao == senha)

    /** A senha que vai para o arquivo; null é sem senha. Só vale com a escolha [pronta]. */
    val escolhida: String? get() = if (semSenha) null else senha
}

internal const val TAMANHO_MINIMO_DA_SENHA = 8

internal const val AVISO_SEM_SENHA =
    "Sem senha, quem tiver o arquivo consegue abrir e ver os dados dos alunos, como notas e faltas."

/**
 * A senha do arquivo que vai ser exportado, antes do seletor de onde salvar. Marcando "Exportar sem
 * senha", os campos somem e o [AVISO_SEM_SENHA] aparece. Exportar fecha o sheet e passa a escolha
 * para a tela, que abre o seletor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SenhaParaExportarBottomSheet(
    onExportar: (senha: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var senha by remember { mutableStateOf("") }
    var confirmacao by remember { mutableStateOf("") }
    var semSenha by rememberSaveable { mutableStateOf(false) }
    var mostrando by rememberSaveable { mutableStateOf(false) }
    val escolha = EscolhaDaSenha(senha, confirmacao, semSenha)

    // Esconde com a animação e só então avisa a tela, que tira o sheet da composição
    fun fechar(depois: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) depois()
        }
    }

    fun exportar() {
        if (!escolha.pronta) return
        val escolhida = escolha.escolhida
        fechar { onExportar(escolhida) }
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
        ) {
            Text(
                text = "Exportar dados",
                style = MaterialTheme.typography.titleLarge,
            )

            // O que some leva o próprio espaço junto: a altura do sheet muda sem pulo
            AnimatedVisibility(visible = !semSenha) {
                Column(
                    modifier = Modifier.padding(top = Espacos.m),
                    verticalArrangement = Arrangement.spacedBy(Espacos.l),
                ) {
                    Text(
                        text = "Crie uma senha para proteger o arquivo. Ela vai ser pedida na hora de " +
                            "importar, e não dá para recuperá-la se você esquecer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CampoDeSenha(
                        valor = senha,
                        onMudar = { senha = it },
                        rotulo = "Senha",
                        mostrando = mostrando,
                        onMostrar = { mostrando = !mostrando },
                        acao = ImeAction.Next,
                        dica = "Pelo menos $TAMANHO_MINIMO_DA_SENHA caracteres",
                    )
                    CampoDeSenha(
                        valor = confirmacao,
                        onMudar = { confirmacao = it },
                        rotulo = "Confirmar senha",
                        mostrando = mostrando,
                        onMostrar = { mostrando = !mostrando },
                        acao = ImeAction.Done,
                        onConcluir = { exportar() },
                        erro = if (escolha.confirmacaoErrada) "As senhas não são iguais" else null,
                    )
                }
            }

            MarcarSemSenha(
                marcado = semSenha,
                onMudar = { semSenha = it },
                modifier = Modifier.padding(top = Espacos.s),
            )
            AnimatedVisibility(visible = semSenha) {
                AvisoSemSenha(Modifier.padding(top = Espacos.xs))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Espacos.xl),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { fechar(onDismiss) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancelar") }

                Button(
                    onClick = { exportar() },
                    modifier = Modifier.weight(1f),
                    enabled = escolha.pronta,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Exportar") }
            }
        }
    }
}

/**
 * A senha de um arquivo protegido, antes da confirmação da importação. Continuar manda a senha
 * para o ViewModel conferir; enquanto ele confere ([abrindo]), a roda gira no botão. Se ela abrir,
 * o ViewModel passa para a confirmação e o sheet sai; se não, o campo diz que está incorreta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SenhaParaImportarBottomSheet(
    abrindo: Boolean,
    senhaIncorreta: Boolean,
    onAbrir: (senha: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val foco = remember { FocusRequester() }

    var senha by remember { mutableStateOf("") }
    var conferida by remember { mutableStateOf<String?>(null) } // a última que foi para o ViewModel
    var mostrando by rememberSaveable { mutableStateOf(false) }
    // Mexendo na senha, o erro da anterior some
    val erro = senhaIncorreta && senha == conferida

    // Esconde com a animação e só então avisa a tela, que tira o sheet da composição
    fun fechar() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    fun abrir() {
        if (senha.isEmpty() || abrindo) return
        conferida = senha
        onAbrir(senha)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 2.dp
    ) {
        // Só há um campo: ele já abre pronto para digitar
        LaunchedEffect(Unit) { foco.requestFocus() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Espacos.s)) {
                Text(
                    text = "Backup com senha",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Digite a senha que foi criada ao exportar este arquivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            CampoDeSenha(
                valor = senha,
                onMudar = { senha = it },
                rotulo = "Senha",
                mostrando = mostrando,
                onMostrar = { mostrando = !mostrando },
                acao = ImeAction.Done,
                onConcluir = { abrir() },
                erro = if (erro) "Senha incorreta" else null,
                // O campo continua aberto enquanto confere: só leitura, o teclado fecharia e
                // voltaria. Mexer na senha no meio não atrapalha, só apaga o erro da anterior
                modifier = Modifier.focusRequester(foco),
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
                    onClick = { abrir() },
                    modifier = Modifier.weight(1f),
                    enabled = senha.isNotEmpty() && !abrindo,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (abrindo) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                    } else {
                        Text("Continuar")
                    }
                }
            }
        }
    }
}

// Peças ---------------------------------------------------------------------------------------

/** Um campo de senha com o olho para mostrar o que foi digitado. */
@Composable
private fun CampoDeSenha(
    valor: String,
    onMudar: (String) -> Unit,
    rotulo: String,
    mostrando: Boolean,
    onMostrar: () -> Unit,
    acao: ImeAction,
    modifier: Modifier = Modifier,
    onConcluir: () -> Unit = {},
    dica: String? = null,
    erro: String? = null, // no lugar da dica
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onMudar,
        label = { Text(rotulo) },
        isError = erro != null,
        supportingText = (erro ?: dica)?.let { texto -> { Text(texto) } },
        trailingIcon = {
            IconButton(onClick = onMostrar) {
                Icon(
                    imageVector = if (mostrando) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (mostrando) "Esconder a senha" else "Mostrar a senha",
                )
            }
        },
        visualTransformation = if (mostrando) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            // O teclado de senha não aprende nem sugere o que é digitado
            keyboardType = KeyboardType.Password,
            autoCorrectEnabled = false,
            imeAction = acao,
        ),
        keyboardActions = KeyboardActions(onDone = { onConcluir() }),
        modifier = modifier.fillMaxWidth()
    )
}

/** O "Exportar sem senha": a linha toda responde ao toque, e a caixa só mostra o estado. */
@Composable
private fun MarcarSemSenha(
    marcado: Boolean,
    onMudar: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = marcado, role = Role.Checkbox, onValueChange = onMudar)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = marcado, onCheckedChange = null)
        Spacer(Modifier.width(Espacos.m))
        Text(
            text = "Exportar sem senha",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** O que muda sem a senha, sem alarde: um fato para a pessoa decidir, nas cores neutras da tela. */
@Composable
private fun AvisoSemSenha(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(Espacos.m),
        horizontalArrangement = Arrangement.spacedBy(Espacos.m),
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = AVISO_SEM_SENHA,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
