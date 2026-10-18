package app.jammes.boletim.presentation.ui.notificacoes

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.BuildConfig
import app.jammes.boletim.domain.model.Configuracoes
import app.jammes.boletim.domain.model.NotificacaoDomain
import app.jammes.boletim.domain.model.TipoNotificacao
import app.jammes.boletim.presentation.ui.anoletivo.Cartao
import app.jammes.boletim.presentation.ui.anoletivo.Divisoria
import app.jammes.boletim.presentation.ui.anoletivo.MolduraAjustes
import app.jammes.boletim.presentation.ui.anoletivo.TituloDaSecao
import app.jammes.boletim.presentation.ui.components.CarregandoDiscreto
import app.jammes.boletim.presentation.ui.components.EstadoVazio
import app.jammes.boletim.presentation.ui.components.SeloDisciplina
import app.jammes.boletim.presentation.ui.configuracoes.ConfiguracoesViewModel
import app.jammes.boletim.presentation.ui.theme.Espacos
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * As notificações do aluno aberto, abertas pelo sino do topo, no mesmo painel das configurações:
 * agrupadas por dia, das mais novas para as mais antigas. As que chegaram desde a última visita vêm
 * com o selo azul e um ponto, e aparecer aqui já conta como lida: o sino do topo apaga. O Limpar, no
 * topo da lista, apaga as do aluno, com Desfazer; a engrenagem leva às configurações delas, que valem
 * para o aparelho todo.
 *
 * A chave das notificações vem do ViewModel das configurações preso à Activity: desligadas, a lista
 * vazia diz isso, em vez de esperar avisos que não vão chegar.
 */
@Composable
fun NotificacoesScreen(
    onVoltar: () -> Unit,
    onConfigurar: () -> Unit,
    onAbrirDisciplina: (disciplinaId: Long, avaliacaoId: Long?, abaDeFaltas: Boolean) -> Unit,
    onAbrirBoletimGeral: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificacoesViewModel = hiltViewModel(),
    configuracoesViewModel: ConfiguracoesViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val configuracoes by configuracoesViewModel.configuracoes.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptico = LocalHapticFeedback.current
    val jaPediu by configuracoesViewModel.permissaoDeNotificacoesPedida.collectAsStateWithLifecycle()
    val permissao = rememberPermissaoDeNotificacoes(jaPediu, configuracoesViewModel::marcarPermissaoDeNotificacoesPedida)
    // Ligadas no app, mas bloqueadas no Android: a faixa no topo diz isso e libera
    val bloqueadas = (configuracoes ?: Configuracoes()).notificacoesAtivas && !permissao.liberada

    // O toque numa notificação: o app já está no ano e no período dela
    LaunchedEffect(viewModel) {
        viewModel.abrir.collect { destino ->
            when (destino) {
                is DestinoDaNotificacao.Disciplina ->
                    onAbrirDisciplina(destino.disciplinaId, destino.avaliacaoId, destino.abaDeFaltas)
                is DestinoDaNotificacao.Boletim -> onAbrirBoletimGeral()
            }
        }
    }

    // O que aparece na tela conta como lido. Só enquanto ela está aberta: com as configurações por
    // cima, esta tela sai da composição, e o que chegar nesse meio-tempo fica para quando ela voltar
    val naoLidas = (state as? NotificacoesUiState.Carregadas)?.naoLidas.orEmpty()
    LaunchedEffect(naoLidas) {
        if (naoLidas.isNotEmpty()) viewModel.marcarComoVistas(naoLidas)
    }

    // O Limpar apaga na hora, e o aviso deixa desfazer por alguns segundos
    LaunchedEffect(viewModel) {
        viewModel.limpou.collect { apagadas ->
            // Um aviso de cada vez: o novo toma o lugar do que estiver na tela
            snackbarHostState.currentSnackbarData?.dismiss()
            scope.launch {
                val resultado = snackbarHostState.showSnackbar(
                    message = textoDaLimpeza(apagadas.size),
                    actionLabel = "Desfazer",
                    duration = SnackbarDuration.Long,
                )
                if (resultado == SnackbarResult.ActionPerformed) viewModel.desfazerLimpeza(apagadas)
            }
        }
    }

    MolduraAjustes(
        onVoltar = onVoltar,
        modifier = modifier,
        titulo = "Notificações",
        snackbarHostState = snackbarHostState,
        acoes = {
            IconButton(onClick = onConfigurar) {
                Icon(Icons.Outlined.Settings, contentDescription = "Configurar notificações")
            }
        },
    ) { padding ->
        when (val s = state) {
            NotificacoesUiState.Carregando -> CarregandoDiscreto(Modifier.fillMaxSize().padding(padding))

            is NotificacoesUiState.Carregadas -> if (s.dias.isEmpty()) {
                SemNotificacoes(
                    titulo = tituloSemNotificacoes(s.nomeDoAluno),
                    onPermitir = if (bloqueadas) permissao::pedir else null,
                    // Antes de carregar vale o padrão, como nas configurações
                    ativas = (configuracoes ?: Configuracoes()).notificacoesAtivas,
                    onAtivar = {
                        haptico.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        configuracoesViewModel.ativarNotificacoes(true)
                        permissao.pedirAoLigar()
                    },
                    onCriarDeTeste = if (BuildConfig.DEBUG) viewModel::criarNotificacoesDeTeste else null,
                    modifier = Modifier.padding(padding),
                )
            } else {
                ListaDeNotificacoes(
                    estado = s,
                    onPermitir = if (bloqueadas) permissao::pedir else null,
                    onAbrir = viewModel::abrir,
                    onLimpar = {
                        haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                        viewModel.limpar()
                    },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

/** O resumo com o Limpar em cima e, embaixo, um card por dia, com o nome do dia por cima. */
@Composable
private fun ListaDeNotificacoes(
    estado: NotificacoesUiState.Carregadas,
    onPermitir: (() -> Unit)?, // só com os avisos bloqueados no Android
    onAbrir: (NotificacaoDomain) -> Unit,
    onLimpar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hoje = LocalDate.now()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Espacos.l, end = Espacos.l, top = Espacos.xs, bottom = Espacos.xl),
        verticalArrangement = Arrangement.spacedBy(Espacos.l),
    ) {
        if (onPermitir != null) {
            item(key = "bloqueio") { AvisoDeBloqueio(onPermitir = onPermitir, modifier = Modifier.animateItem()) }
        }
        item(key = "topo") {
            TopoDaLista(
                resumo = resumoDasNotificacoes(estado.total, estado.novas, estado.nomeDoAluno),
                onLimpar = onLimpar,
                modifier = Modifier.animateItem(),
            )
        }
        items(estado.dias, key = { it.dia.toEpochDay() }) { dia ->
            Column(Modifier.animateItem()) {
                TituloDaSecao(tituloDoDia(dia.dia, hoje), alterada = false)
                Cartao {
                    dia.itens.forEachIndexed { i, item ->
                        if (i > 0) Divisoria()
                        LinhaDaNotificacao(
                            item = item,
                            // Só a que aponta para algum lugar abre
                            onClick = if (destinoDa(item.notificacao) != null) ({ onAbrir(item.notificacao) }) else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopoDaLista(
    resumo: String,
    onLimpar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = resumo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(start = Espacos.xs),
        )
        TextButton(onClick = onLimpar) {
            Icon(Icons.Outlined.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Espacos.s))
            Text("Limpar")
        }
    }
}

/** O selo, o título com a hora ao lado e o texto embaixo. A nova tem o título mais forte. */
@Composable
private fun LinhaDaNotificacao(item: ItemDeNotificacao, onClick: (() -> Unit)?) {
    val notificacao = item.notificacao

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClickLabel = "Abrir", onClick = onClick) else Modifier)
            // Uma frase só para o leitor de tela, que avisa antes quando ela é nova
            .semantics(mergeDescendants = true) {
                if (item.nova) stateDescription = "Nova"
            }
            .padding(horizontal = Espacos.l, vertical = Espacos.m),
    ) {
        SeloDaNotificacao(tipo = notificacao.tipo, nova = item.nova)
        Spacer(Modifier.width(Espacos.m))
        Column(Modifier.weight(1f)) {
            Row {
                Text(
                    text = notificacao.titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.nova) FontWeight.SemiBold else null,
                    modifier = Modifier
                        .weight(1f)
                        .alignByBaseline(),
                )
                Spacer(Modifier.width(Espacos.s))
                Text(
                    text = textoDaHora(item.hora),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            if (notificacao.texto.isNotBlank()) {
                Text(
                    text = notificacao.texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * O ícone do tipo num quadrado: azul e com o ponto, como o sino do topo, quando a notificação é nova;
 * cinza depois de vista.
 */
@Composable
private fun SeloDaNotificacao(tipo: TipoNotificacao, nova: Boolean) {
    Box {
        SeloDisciplina(
            cor = if (nova) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            icone = iconeDoTipo(tipo),
        )
        if (nova) {
            // Um anel da cor do card em volta separa o ponto do selo
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * A lista vazia. Com as notificações desligadas, ela diz isso e oferece ligar; no build de debug,
 * também cria exemplos, para ver a tela antes de o app criar notificações de verdade.
 */
@Composable
private fun SemNotificacoes(
    titulo: String,
    onPermitir: (() -> Unit)?, // só com os avisos bloqueados no Android
    ativas: Boolean,
    onAtivar: () -> Unit,
    onCriarDeTeste: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // Rola com a fonte do sistema bem grande ou com a tela deitada
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (onPermitir != null) {
            AvisoDeBloqueio(
                onPermitir = onPermitir,
                modifier = Modifier.padding(start = Espacos.l, end = Espacos.l, top = Espacos.xs),
            )
        }
        EstadoVazio(
            icone = if (ativas) Icons.Outlined.NotificationsNone else Icons.Outlined.NotificationsOff,
            titulo = titulo,
            mensagem = if (ativas) {
                "Os avisos do Boletim Escolar vão aparecer aqui."
            } else {
                "As notificações estão desligadas: nenhum aviso vai chegar."
            },
            acao = if (!ativas || onCriarDeTeste != null) {
                {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Espacos.s),
                    ) {
                        if (!ativas) {
                            FilledTonalButton(onClick = onAtivar) { Text("Ativar notificações") }
                        }
                        if (onCriarDeTeste != null) {
                            TextButton(onClick = onCriarDeTeste) { Text("Criar notificações de teste") }
                        }
                    }
                }
            } else {
                null
            },
        )
    }
}

// Textos ------------------------------------------------------------------------------------

/**
 * O nome do grupo de um dia: "Hoje", "Ontem", o dia da semana com a data até uma semana atrás, e
 * depois só a data, com o ano quando ele não é o de [hoje].
 */
internal fun tituloDoDia(dia: LocalDate, hoje: LocalDate): String {
    val diasAtras = ChronoUnit.DAYS.between(dia, hoje)
    val texto = when {
        diasAtras == 0L -> "Hoje"
        diasAtras == 1L -> "Ontem"
        diasAtras in 2L..6L -> dia.format(FORMATO_DIA_DA_SEMANA)
        dia.year == hoje.year -> dia.format(FORMATO_DIA)
        else -> dia.format(FORMATO_DIA_E_ANO)
    }
    return texto.replaceFirstChar { it.titlecase(PT_BR) }
}

/**
 * "7 notificações · 2 novas"; sem novas, só quantas são. Com [nomeDoAluno] (mais de um aluno no
 * aparelho), ele vem na frente: "James · 7 notificações · 2 novas".
 */
internal fun resumoDasNotificacoes(total: Int, novas: Int, nomeDoAluno: String? = null): String {
    val quantas = if (total == 1) "1 notificação" else "$total notificações"
    val resumo = when (novas) {
        0 -> quantas
        1 -> "$quantas · 1 nova"
        else -> "$quantas · $novas novas"
    }
    return if (nomeDoAluno == null) resumo else "$nomeDoAluno · $resumo"
}

/** A lista vazia: "Nenhuma notificação", ou "Nenhuma notificação de James" com mais de um aluno. */
internal fun tituloSemNotificacoes(nomeDoAluno: String?): String =
    if (nomeDoAluno == null) "Nenhuma notificação" else "Nenhuma notificação de $nomeDoAluno"

private fun iconeDoTipo(tipo: TipoNotificacao): ImageVector = when (tipo) {
    TipoNotificacao.AVALIACAO -> Icons.Outlined.Event
    TipoNotificacao.NOTA -> Icons.Outlined.EditNote
    TipoNotificacao.MEDIA -> Icons.AutoMirrored.Outlined.TrendingDown
    TipoNotificacao.FALTAS -> Icons.Outlined.EventBusy
    TipoNotificacao.PERIODO -> Icons.Outlined.HourglassBottom
    TipoNotificacao.AVISO -> Icons.Outlined.Notifications
}

/** O aviso do Limpar: "7 notificações apagadas". */
internal fun textoDaLimpeza(quantas: Int): String =
    if (quantas == 1) "1 notificação apagada" else "$quantas notificações apagadas"

/** "07:05", sempre em 24 horas. */
internal fun textoDaHora(hora: LocalTime): String = hora.format(FORMATO_HORA)

private val PT_BR = Locale.forLanguageTag("pt-BR")
private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm")
private val FORMATO_DIA_DA_SEMANA = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR)
private val FORMATO_DIA = DateTimeFormatter.ofPattern("d 'de' MMMM", PT_BR)
private val FORMATO_DIA_E_ANO = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", PT_BR)
