package app.jammes.boletim.presentation.ui.anoletivo

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.presentation.ui.components.DialogoDaMateriaNova
import app.jammes.boletim.presentation.ui.components.Pilula
import app.jammes.boletim.presentation.ui.components.SeloDisciplina
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import app.jammes.boletim.presentation.ui.theme.Espacos
import app.jammes.boletim.presentation.ui.theme.IconesDisciplina
import kotlinx.coroutines.launch

/**
 * Quais matérias o aluno tem no ano: um switch em cada. As que já têm avaliações ou faltas ficam
 * presas com um cadeado, porque tirá-las apagaria esses lançamentos. No fim da lista, "Nova matéria"
 * abre o mesmo diálogo do onboarding: a criada entra ligada, e no cadastro só no Salvar.
 *
 * Cada toque já vai para o formulário da tela, e quem grava é o Salvar dela; "Pronto" só fecha. Aberto
 * sem a tela (o [MateriasDoAnoSheet]), o botão é que grava: ele vira [textoDoBotao] e chama [onBotao].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MateriasBottomSheet(
    linhas: List<LinhaMateria>,
    marcadas: Set<Long>, // as que entram no ano
    novas: List<LinhaMateria>, // criadas aqui: entram no ano junto com as marcadas
    existentes: List<String>, // os nomes que uma nova não pode repetir
    onMudar: (Set<Long>) -> Unit,
    onCriar: (nome: String) -> Unit,
    onDesistir: (nome: String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    textoDoBotao: String = "Pronto",
    onBotao: (() -> Unit)? = null, // null: só fecha
) {
    val scope = rememberCoroutineScope()
    val haptico = LocalHapticFeedback.current
    var criando by rememberSaveable { mutableStateOf(false) }

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
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(Espacos.m)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Matérias do ano",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                val total = linhas.size + novas.size
                if (total > 0) {
                    Pilula(texto = "${linhas.count { it.materiaId in marcadas } + novas.size} de $total")
                }
            }

            // Uma frase só explica todos os cadeados
            if (linhas.any { it.lancamentos != null }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(Espacos.s))
                    Text(
                        text = "Matérias com avaliações ou faltas não podem ser removidas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Só a lista rola: a contagem em cima e o Pronto embaixo ficam sempre à vista
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                // As do cadastro, as criadas aqui e, por último, a linha de criar outra
                Cartao {
                    linhas.forEach { linha ->
                        LinhaDaMateria(
                            linha = linha,
                            marcada = linha.materiaId in marcadas,
                            onMarcar = { marcar ->
                                haptico.performHapticFeedback(
                                    if (marcar) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
                                )
                                onMudar(if (marcar) marcadas + linha.materiaId else marcadas - linha.materiaId)
                            },
                            // Presa, o toque só responde "não" no dedo: o porquê já está escrito na linha
                            onBloqueada = { haptico.performHapticFeedback(HapticFeedbackType.Reject) },
                        )
                        Divisoria()
                    }
                    // Desligar uma criada aqui é desistir dela: ainda não existe no cadastro
                    novas.forEach { nova ->
                        LinhaDaMateria(
                            linha = nova,
                            marcada = true,
                            onMarcar = {
                                haptico.performHapticFeedback(HapticFeedbackType.ToggleOff)
                                onDesistir(nova.nome)
                            },
                            onBloqueada = {},
                        )
                        Divisoria()
                    }
                    LinhaNovaMateria(onClick = { criando = true })
                }
            }

            Button(
                onClick = { onBotao?.invoke() ?: fechar() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text(textoDoBotao) }
        }
    }

    if (criando) {
        DialogoDaMateriaNova(
            titulo = "Nova matéria",
            existentes = existentes,
            onCriar = { nome ->
                onCriar(nome)
                criando = false
            },
            onDismiss = { criando = false },
        )
    }
}

/**
 * As matérias do ano num sheet por cima de qualquer tela, sem passar pelos ajustes. O mesmo sheet e as
 * mesmas regras deles; a diferença é que aqui o "Salvar" do sheet grava, e fechar sem ele descarta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MateriasDoAnoSheet(
    onFechar: () -> Unit,
    viewModel: MateriasDoAnoViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.abrir() }
    val ajustes by viewModel.ajustes.collectAsStateWithLifecycle()
    val salvamento by viewModel.salvamento.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Gravou: desce com a animação e só então sai da tela
    LaunchedEffect(salvamento) {
        if (salvamento == Salvamento.Salvo) {
            sheetState.hide()
            onFechar()
        }
    }

    // Enquanto lê o banco, nada: é rápido, e um sheet vazio piscaria
    val lido = ajustes ?: return
    val linhas = remember(lido) { linhasDasMaterias(lido) }
    // O mesmo formulário dos ajustes, só com as matérias mexidas: o resto vai como estava gravado
    var form by rememberSaveable(lido) { mutableStateOf(FormularioAnoLetivo.de(lido)) }
    val novas = remember(form.materiasNovas, lido) { linhasDasNovas(form.materiasNovas, lido) }
    val nomesUsados = remember(lido, linhas) { (lido.materias.map { it.nome } + linhas.map { it.nome }).distinct() }

    MateriasBottomSheet(
        linhas = linhas,
        marcadas = form.materias,
        novas = novas,
        existentes = nomesUsados + form.materiasNovas,
        onMudar = { form = form.copy(materias = it) },
        onCriar = { form = form.comMateriaNova(it) },
        onDesistir = { form = form.semMateriaNova(it) },
        onDismiss = onFechar,
        sheetState = sheetState,
        textoDoBotao = "Salvar",
        onBotao = { viewModel.salvar(form) },
    )
}

@Composable
private fun LinhaDaMateria(
    linha: LinhaMateria,
    marcada: Boolean,
    onMarcar: (Boolean) -> Unit,
    onBloqueada: () -> Unit,
) {
    val bloqueada = linha.lancamentos != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (bloqueada) {
                    Modifier.clickable(onClick = onBloqueada)
                } else {
                    Modifier.toggleable(value = marcada, role = Role.Switch, onValueChange = onMarcar)
                }
            )
            .padding(horizontal = Espacos.l, vertical = Espacos.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeloDisciplina(
            cor = CoresDisciplina.de(linha.cor),
            icone = IconesDisciplina.de(linha.icone),
            tamanho = 32.dp,
        )
        Spacer(Modifier.width(Espacos.m))
        Column(Modifier.weight(1f)) {
            Text(
                text = linha.nome,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (linha.lancamentos != null) {
                Text(
                    text = descrever(linha.lancamentos),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(Espacos.s))
        if (bloqueada) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "Não pode ser removida",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Switch(checked = marcada, onCheckedChange = null)
        }
    }
}

/** A última linha da lista, com o mesmo tamanho das outras e a cor de ação. */
@Composable
private fun LinhaNovaMateria(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClickLabel = "Criar uma matéria", role = Role.Button, onClick = onClick)
            .padding(horizontal = Espacos.l, vertical = Espacos.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeloDisciplina(
            cor = MaterialTheme.colorScheme.primary,
            icone = Icons.Filled.Add,
            tamanho = 32.dp,
        )
        Spacer(Modifier.width(Espacos.m))
        Text(
            text = "Nova matéria",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
