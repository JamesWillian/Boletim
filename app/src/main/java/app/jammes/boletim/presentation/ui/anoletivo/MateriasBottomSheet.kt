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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.jammes.boletim.presentation.ui.components.Pilula
import app.jammes.boletim.presentation.ui.components.SeloDisciplina
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import app.jammes.boletim.presentation.ui.theme.Espacos
import app.jammes.boletim.presentation.ui.theme.IconesDisciplina
import kotlinx.coroutines.launch

/**
 * Quais matérias o aluno tem no ano: um switch em cada. As que já têm avaliações ou faltas ficam
 * presas com um cadeado, porque tirá-las apagaria esses lançamentos.
 *
 * Cada toque já vai para o formulário da tela, e quem grava é o Salvar dela; "Pronto" só fecha.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MateriasBottomSheet(
    linhas: List<LinhaMateria>,
    marcadas: Set<Long>, // as que entram no ano
    onMudar: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptico = LocalHapticFeedback.current

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
                if (linhas.isNotEmpty()) {
                    Pilula(texto = "${linhas.count { it.materiaId in marcadas }} de ${linhas.size}")
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
                        text = "Com avaliações ou faltas, a matéria fica no ano",
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
                if (linhas.isEmpty()) {
                    Text(
                        text = "Nenhuma matéria cadastrada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Cartao {
                        linhas.forEachIndexed { i, linha ->
                            if (i > 0) Divisoria()
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
                        }
                    }
                }
            }

            Button(
                onClick = { fechar() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Pronto") }
        }
    }
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
                contentDescription = "Fica no ano",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Switch(checked = marcada, onCheckedChange = null)
        }
    }
}
