package app.jammes.boletim.presentation.ui.anoletivo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.jammes.boletim.presentation.ui.components.Pilula
import app.jammes.boletim.presentation.ui.theme.Espacos
import kotlinx.coroutines.launch

/**
 * Escolha de uma opção entre poucas, cada uma com o que ela faz e, se houver, um [exemplo] à
 * direita. Tocar numa opção já escolhe e fecha: não tem botão de confirmar.
 *
 * Como o resto da tela de ajustes, a escolha vai para o formulário, e quem grava é o Salvar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> EscolhaBottomSheet(
    titulo: String,
    descricao: String,
    opcoes: List<T>,
    selecionada: T,
    nome: (T) -> String,
    detalhe: (T) -> String,
    onEscolher: (T) -> Unit,
    onDismiss: () -> Unit,
    exemplo: ((T) -> String)? = null,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = descricao,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Espacos.xs, bottom = Espacos.m),
            )

            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(Espacos.xs),
            ) {
                opcoes.forEach { opcao ->
                    val escolhida = opcao == selecionada
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (escolhida) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                            )
                            .selectable(
                                selected = escolhida,
                                role = Role.RadioButton,
                                onClick = {
                                    if (!escolhida) haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    // Já marca a nova antes de descer: dá para ver o que foi escolhido
                                    onEscolher(opcao)
                                    fechar()
                                },
                            )
                            .padding(horizontal = Espacos.s, vertical = Espacos.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // O clique é da linha inteira; o rádio só mostra
                        RadioButton(selected = escolhida, onClick = null)
                        Spacer(Modifier.width(Espacos.m))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = nome(opcao),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (escolhida) FontWeight.SemiBold else null,
                            )
                            Text(
                                text = detalhe(opcao),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (exemplo != null) {
                            Spacer(Modifier.width(Espacos.s))
                            Pilula(texto = exemplo(opcao))
                        }
                    }
                }
            }
        }
    }
}
