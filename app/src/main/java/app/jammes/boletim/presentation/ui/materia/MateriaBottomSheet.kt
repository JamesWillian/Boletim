package app.jammes.boletim.presentation.ui.materia

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.domain.model.ImpedimentoDeExcluir
import app.jammes.boletim.domain.model.MateriaDoCadastro
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.presentation.ui.components.AvisoComCadeado
import app.jammes.boletim.presentation.ui.components.CampoDaSigla
import app.jammes.boletim.presentation.ui.components.CampoDoNome
import app.jammes.boletim.presentation.ui.components.NomeESiglaBloqueados
import app.jammes.boletim.presentation.ui.components.SecaoDaCor
import app.jammes.boletim.presentation.ui.components.SecaoDoIcone
import app.jammes.boletim.presentation.ui.components.SeloDisciplina
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import app.jammes.boletim.presentation.ui.theme.Espacos
import app.jammes.boletim.presentation.ui.theme.IconesDisciplina
import kotlinx.coroutines.launch

/**
 * Tudo de uma matéria numa página só, pronto para mudar: o nome, a sigla, a cor e o ícone, com o selo em
 * cima mostrando como ela fica. É o mesmo sheet na tela de matérias e nos botões de criar do onboarding e
 * das matérias do ano. Ele não grava: entrega a matéria em [onSalvar] e fecha, e quem abriu decide o que
 * fazer com ela.
 *
 * As matérias do app ficam como estão, em qualquer plano: tudo aparece apagado, e só dá para fechar. O
 * Excluir só existe numa matéria que já está no cadastro, e fica apagado numa do app ou em uso em
 * [anosEmUso] anos letivos, com o motivo e um cadeado embaixo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MateriaBottomSheet(
    materia: MateriaDomain, // id 0: uma nova (ver materiaNova)
    existentes: List<String>, // os nomes que ela não pode repetir, sem contar o dela
    onSalvar: (MateriaDomain) -> Unit,
    onDismiss: () -> Unit,
    anosEmUso: Int = 0,
    onExcluir: () -> Unit = {},
) {
    val nova = materia.id == 0L
    val bloqueada = !materia.propria
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptico = LocalHapticFeedback.current

    // rememberSaveable: o que já foi digitado sobrevive a girar a tela
    var form by rememberSaveable { mutableStateOf(FormularioMateria.de(materia)) }
    var confirmandoExclusao by rememberSaveable { mutableStateOf(false) }
    // O teclado abre sozinho só ao abrir o sheet: girar a tela depois de fechá-lo não traz ele de volta
    var focouNoNome by rememberSaveable { mutableStateOf(false) }
    // Salvou e está descendo: um toque duplo no botão não cria outra
    var salvou by remember { mutableStateOf(false) }
    // Os nomes de quando ele abriu: a matéria salva entra na lista enquanto o sheet desce, e não pode virar repetida
    val nomesOcupados = remember { existentes }
    val impedimento = MateriaDoCadastro(materia, anosEmUso).impedimentoDeExcluir
    val podeSalvar = form.podeSalvar(materia, nomesOcupados)
    // Com a tela deitada, o teclado quase não deixa altura: nada fica fixo, e rola tudo junto até os botões
    val tudoRola = LocalWindowInfo.current.containerDpSize.height < ALTURA_PARA_FIXAR
    val rolagem = rememberScrollState()

    // Esconde com a animação e só então avisa a tela, que tira o sheet da composição
    fun fechar(depois: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                depois()
                onDismiss()
            }
        }
    }

    fun salvar() {
        if (!podeSalvar || salvou) return
        salvou = true
        haptico.performHapticFeedback(HapticFeedbackType.Confirm)
        onSalvar(form.aplicadaEm(materia))
        fechar()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 2.dp
    ) {
        // O sheet abre em outra janela, com o foco dela: este é o de dentro dele
        val foco = LocalFocusManager.current
        val focoNoNome = remember { FocusRequester() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (tudoRola) Modifier.verticalScroll(rolagem) else Modifier)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(Espacos.m)
        ) {
            CabecalhoDaMateria(
                titulo = tituloDaMateria(materia),
                cor = form.cor,
                icone = form.icone,
                // Nova, ainda não há o que excluir
                excluir = if (nova) null else ({ confirmandoExclusao = true }),
                excluirHabilitado = impedimento == null,
            )

            if (impedimento != null) {
                Column(verticalArrangement = Arrangement.spacedBy(Espacos.xs)) {
                    AvisoComCadeado(textoDoImpedimento(impedimento))
                    // Cada disciplina tem a própria cópia: as que já existem não mudam
                    if (impedimento is ImpedimentoDeExcluir.EmUso) {
                        Text(
                            text = "O que mudar aqui só vale para os próximos anos letivos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Só os campos rolam: o cabeçalho e os botões ficam sempre à vista, mesmo com o teclado aberto
            Column(
                modifier = if (tudoRola) Modifier else Modifier.weight(1f, fill = false).verticalScroll(rolagem),
                verticalArrangement = Arrangement.spacedBy(Espacos.m),
            ) {
                if (bloqueada) {
                    NomeESiglaBloqueados(nome = form.nome, abreviacao = form.abreviacao)
                } else {
                    CampoDoNome(
                        valor = form.nome,
                        onMudar = { form = form.comNome(it) },
                        rotulo = "Nome",
                        erro = form.erroDoNome(materia, nomesOcupados),
                        modifier = Modifier.focusRequester(focoNoNome),
                    )
                    CampoDaSigla(
                        valor = form.abreviacao,
                        nome = form.nome,
                        onMudar = { form = form.copy(abreviacao = it) },
                        imeAction = ImeAction.Done,
                        // A cor e o ícone vêm depois da sigla: o teclado sai da frente deles
                        keyboardActions = KeyboardActions(onDone = { foco.clearFocus() }),
                    )
                }
                SecaoDaCor(
                    cor = form.cor,
                    onEscolher = { form = form.copy(cor = it) },
                    bloqueada = bloqueada,
                )
                SecaoDoIcone(
                    icone = form.icone,
                    cor = form.cor,
                    onEscolher = { form = form.comIcone(it) },
                    bloqueada = bloqueada,
                )
            }

            if (bloqueada) {
                OutlinedButton(
                    onClick = { fechar() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Fechar") }
            } else {
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
                        onClick = { salvar() },
                        modifier = Modifier.weight(1f),
                        enabled = podeSalvar,
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(if (nova) "Criar" else "Salvar") }
                }
            }
        }

        // Numa nova, o nome é o primeiro passo: o sheet já abre com o teclado nele
        if (nova && !focouNoNome) {
            LaunchedEffect(Unit) {
                focoNoNome.requestFocus()
                focouNoNome = true
            }
        }
    }

    if (confirmandoExclusao) {
        AlertDialog(
            onDismissRequest = { confirmandoExclusao = false },
            title = { Text("Excluir matéria?") },
            text = { Text("\"${materia.nome}\" será apagada, e não dá para desfazer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoExclusao = false
                    haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                    // Exclui só depois de fechar: se a matéria sumisse da lista antes,
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

/** O selo como a matéria está ficando, o título e, numa que já existe, o Excluir. */
@Composable
private fun CabecalhoDaMateria(
    titulo: String,
    cor: Int,
    icone: IconeMateria,
    excluir: (() -> Unit)?,
    excluirHabilitado: Boolean,
) {
    // O selo acompanha o que é escolhido embaixo, sem pular de uma cor ou ícone para outro
    val corDoSelo by animateColorAsState(CoresDisciplina.de(cor), label = "corDoSelo")

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crossfade(targetState = icone, label = "iconeDoSelo") { atual ->
            SeloDisciplina(cor = corDoSelo, icone = IconesDisciplina.de(atual), tamanho = 40.dp)
        }
        Spacer(Modifier.width(Espacos.m))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (excluir != null) {
            TextButton(
                onClick = excluir,
                enabled = excluirHabilitado,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Excluir") }
        }
    }
}

// Abaixo disso (um celular deitado), o cabeçalho e os botões fixos não deixariam lugar para os campos
private val ALTURA_PARA_FIXAR = 480.dp

// Textos ------------------------------------------------------------------------------------

/** "Nova matéria", "Editar matéria" ou, quando nada pode mudar, "Matéria do app". */
internal fun tituloDaMateria(materia: MateriaDomain): String = when {
    materia.id == 0L -> "Nova matéria"
    !materia.propria -> "Matéria do app"
    else -> "Editar matéria"
}

/** O que o cadeado embaixo do cabeçalho explica. */
internal fun textoDoImpedimento(impedimento: ImpedimentoDeExcluir): String = when (impedimento) {
    ImpedimentoDeExcluir.DoApp -> "As matérias do app não podem ser alteradas ou excluídas."
    is ImpedimentoDeExcluir.EmUso -> "Em uso em ${anosLetivos(impedimento.anos)}: não pode ser excluída."
}

private fun anosLetivos(quantidade: Int): String =
    if (quantidade == 1) "1 ano letivo" else "$quantidade anos letivos"
