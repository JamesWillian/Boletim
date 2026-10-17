package app.jammes.boletim.presentation.ui.notificacoes

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.Configuracoes
import app.jammes.boletim.domain.model.MAXIMO_DE_HORARIOS
import app.jammes.boletim.presentation.ui.aluno.Selo
import app.jammes.boletim.presentation.ui.anoletivo.Cartao
import app.jammes.boletim.presentation.ui.anoletivo.Divisoria
import app.jammes.boletim.presentation.ui.anoletivo.MolduraAjustes
import app.jammes.boletim.presentation.ui.anoletivo.TituloDaSecao
import app.jammes.boletim.presentation.ui.backup.LinhaComChave
import app.jammes.boletim.presentation.ui.configuracoes.ConfiguracoesViewModel
import app.jammes.boletim.presentation.ui.theme.Espacos
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * As configurações das notificações, abertas pela engrenagem da lista, no mesmo painel das
 * configurações do app: a chave que liga e desliga os avisos e os horários do dia em que eles chegam,
 * com o lembrete de escolher horários fora das aulas. Cada escolha vale na hora, sem Salvar.
 * Desligadas, os horários continuam guardados, apagados na tela, para quando elas voltarem.
 *
 * Usa o ViewModel das configurações preso à Activity, que já está carregado: a chave abre na posição
 * certa, sem pular.
 */
@Composable
fun ConfiguracoesDasNotificacoesScreen(
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConfiguracoesViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    val configuracoes by viewModel.configuracoes.collectAsStateWithLifecycle()
    // Antes de carregar vale o padrão, como nas configurações do app
    val atuais = configuracoes ?: Configuracoes()
    val horarios = atuais.horariosDasNotificacoes
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptico = LocalHapticFeedback.current

    // O sheet do relógio, aberto para um horário novo ([mudando] null) ou para mudar um da lista
    var escolhendo by rememberSaveable { mutableStateOf(false) }
    var mudando by rememberSaveable { mutableStateOf<LocalTime?>(null) }

    // Remove na hora, e o aviso deixa desfazer por alguns segundos
    fun remover(horario: LocalTime) {
        viewModel.removerHorario(horario)
        // Um aviso de cada vez: o novo toma o lugar do que estiver na tela
        snackbarHostState.currentSnackbarData?.dismiss()
        scope.launch {
            val resultado = snackbarHostState.showSnackbar(
                message = "Horário das ${textoDaHora(horario)} removido",
                actionLabel = "Desfazer",
                duration = SnackbarDuration.Long,
            )
            if (resultado == SnackbarResult.ActionPerformed) viewModel.salvarHorario(horario, antigo = null)
        }
    }

    MolduraAjustes(
        onVoltar = onVoltar,
        modifier = modifier,
        titulo = "Configurar notificações",
        snackbarHostState = snackbarHostState,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = Espacos.l, end = Espacos.l, top = Espacos.xs, bottom = Espacos.xl),
            verticalArrangement = Arrangement.spacedBy(Espacos.l),
        ) {
            SecaoNotificacoes(
                ativas = atuais.notificacoesAtivas,
                onMudar = { ativas ->
                    haptico.performHapticFeedback(if (ativas) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    viewModel.ativarNotificacoes(ativas)
                },
            )
            SecaoHorarios(
                horarios = horarios,
                habilitada = atuais.notificacoesAtivas,
                onAdicionar = {
                    mudando = null
                    escolhendo = true
                },
                onMudar = { horario ->
                    mudando = horario
                    escolhendo = true
                },
                onRemover = ::remover,
            )
        }
    }

    if (escolhendo) {
        HorarioBottomSheet(
            horario = mudando,
            outros = horarios.filter { it != mudando },
            onSalvar = { novo ->
                haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                viewModel.salvarHorario(novo, antigo = mudando)
            },
            onDismiss = { escolhendo = false },
        )
    }
}

// Seções ------------------------------------------------------------------------------------

@Composable
private fun SecaoNotificacoes(
    ativas: Boolean,
    onMudar: (Boolean) -> Unit,
) {
    Column {
        TituloDaSecao("Notificações", alterada = false)
        Cartao {
            LinhaComChave(
                titulo = "Receber notificações",
                // Uma linha nos dois casos: o card não muda de altura quando a chave vira
                resumo = if (ativas) "Nos horários abaixo" else "Nenhum aviso vai chegar",
                ligada = ativas,
                onMudar = onMudar,
            ) { Selo(if (ativas) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff) }
        }
    }
}

/**
 * O lembrete das aulas, os horários e o Adicionar. Com as notificações desligadas, fica tudo apagado e
 * sem responder ao toque, mas à vista: os horários continuam ali para quando elas voltarem.
 */
@Composable
private fun SecaoHorarios(
    horarios: List<LocalTime>,
    habilitada: Boolean,
    onAdicionar: () -> Unit,
    onMudar: (LocalTime) -> Unit,
    onRemover: (LocalTime) -> Unit,
) {
    Column(Modifier.alpha(if (habilitada) 1f else ALFA_APAGADA)) {
        TituloDaSecao("Horários", alterada = false)
        AvisoDasAulas()
        Spacer(Modifier.height(Espacos.m))
        Cartao {
            horarios.forEachIndexed { i, horario ->
                if (i > 0) Divisoria()
                LinhaDoHorario(
                    horario = horario,
                    habilitada = habilitada,
                    // O último não sai: para não receber nada, existe a chave
                    podeRemover = horarios.size > 1,
                    onMudar = { onMudar(horario) },
                    onRemover = { onRemover(horario) },
                )
            }
            if (horarios.size < MAXIMO_DE_HORARIOS) {
                Divisoria()
                LinhaDeAdicionar(habilitada = habilitada, onClick = onAdicionar)
            }
        }
        if (horarios.size >= MAXIMO_DE_HORARIOS) {
            Text(
                text = "Até $MAXIMO_DE_HORARIOS horários por dia.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Espacos.xs, top = Espacos.s),
            )
        }
    }
}

// Peças -------------------------------------------------------------------------------------

/** O lembrete de fugir do horário das aulas, num quadro tingido acima dos horários. */
@Composable
private fun AvisoDasAulas() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(Espacos.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.School,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Spacer(Modifier.width(Espacos.m))
        Text(
            text = AVISO_DAS_AULAS,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/** Um horário: tocar na linha abre o relógio para mudar; a lixeira tira ele da lista. */
@Composable
private fun LinhaDoHorario(
    horario: LocalTime,
    habilitada: Boolean,
    podeRemover: Boolean,
    onMudar: () -> Unit,
    onRemover: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // A mesma altura com e sem a lixeira: a linha não pula quando o segundo horário entra
            .heightIn(min = 64.dp)
            .clickable(enabled = habilitada, onClickLabel = "Mudar o horário", onClick = onMudar)
            .padding(start = Espacos.l, end = Espacos.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Selo(Icons.Outlined.Schedule)
        Spacer(Modifier.width(Espacos.m))
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = Espacos.s),
        ) {
            Text(
                text = textoDaHora(horario),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = parteDoDia(horario),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (podeRemover) {
            IconButton(onClick = onRemover, enabled = habilitada) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Remover o horário das ${textoDaHora(horario)}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LinhaDeAdicionar(
    habilitada: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = habilitada, role = Role.Button, onClick = onClick)
            .padding(horizontal = Espacos.l, vertical = Espacos.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Selo(Icons.Filled.Add)
        Spacer(Modifier.width(Espacos.m))
        Text(
            text = "Adicionar horário",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// A transparência do Material para o que está desabilitado
private const val ALFA_APAGADA = 0.38f

// Textos ------------------------------------------------------------------------------------

internal const val AVISO_DAS_AULAS = "Escolha horários fora das aulas: assim as notificações não atrapalham."

/** Em que parte do dia o horário cai, para conferir de relance que 07:00 não era 19:00. */
internal fun parteDoDia(horario: LocalTime): String = when (horario.hour) {
    in 0..4 -> "De madrugada"
    in 5..11 -> "De manhã"
    in 12..17 -> "À tarde"
    else -> "À noite"
}
