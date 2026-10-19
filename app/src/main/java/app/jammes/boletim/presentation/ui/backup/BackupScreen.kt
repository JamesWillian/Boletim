package app.jammes.boletim.presentation.ui.backup

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.Configuracoes
import app.jammes.boletim.domain.model.ResumoDoBackup
import app.jammes.boletim.presentation.ui.aluno.Selo
import app.jammes.boletim.presentation.ui.aluno.resumoDosAnos
import app.jammes.boletim.presentation.ui.anoletivo.Cartao
import app.jammes.boletim.presentation.ui.anoletivo.Divisoria
import app.jammes.boletim.presentation.ui.anoletivo.LinhaQueAbre
import app.jammes.boletim.presentation.ui.anoletivo.MolduraAjustes
import app.jammes.boletim.presentation.ui.anoletivo.TituloDaSecao
import app.jammes.boletim.presentation.ui.configuracoes.ConfiguracoesViewModel
import app.jammes.boletim.presentation.ui.disciplina.FORMATO_DATA_COMPLETA
import app.jammes.boletim.presentation.ui.theme.Espacos
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Backup e restauração, aberta pelo perfil, no mesmo painel das configurações. Em cima, a chave do
 * backup automático do Android na conta Google; embaixo, o backup em arquivo: exportar os dados de
 * todos os alunos para um arquivo, e importar um, que troca tudo o que está no aparelho.
 *
 * Os arquivos são escolhidos no seletor do próprio Android, que mostra o aparelho, o Drive e os
 * outros apps de arquivos; assim o app não precisa de permissão nenhuma. A chave usa o ViewModel das
 * configurações preso à Activity, que já está carregado: ela abre na posição certa, sem pular.
 */
@Composable
fun BackupScreen(
    onVoltar: () -> Unit,
    onImportado: () -> Unit, // os dados foram trocados: o que estava aberto era dos de antes
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel(),
    configuracoesViewModel: ConfiguracoesViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    val andamento by viewModel.andamento.collectAsStateWithLifecycle()
    val aviso by viewModel.aviso.collectAsStateWithLifecycle()
    val configuracoes by configuracoesViewModel.configuracoes.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptico = LocalHapticFeedback.current
    val importado by rememberUpdatedState(onImportado)

    // null: a pessoa desistiu no seletor
    val exportarPara = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(TIPO_DO_ARQUIVO)) { uri ->
        if (uri != null) viewModel.exportar(uri.toString()) else viewModel.cancelarExportacao()
    }
    val escolherBackup = rememberEscolherBackup(viewModel)

    LaunchedEffect(aviso) {
        val mensagem = aviso ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(mensagem)
        viewModel.avisoVisto()
    }

    LaunchedEffect(viewModel) {
        viewModel.importou.collect {
            haptico.performHapticFeedback(HapticFeedbackType.Confirm)
            importado()
        }
    }

    // Enquanto os dados são trocados a tela fica: voltar no meio levaria às telas dos dados de antes
    val trocando = andamento == Andamento.Importando
    BackHandler(enabled = trocando) {}

    MolduraAjustes(
        onVoltar = { if (!trocando) onVoltar() },
        modifier = modifier,
        titulo = "Backup e Restauração",
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
            SecaoGoogle(
                // Antes de carregar vale o padrão, como nas configurações
                ligado = (configuracoes ?: Configuracoes()).backupAutomatico,
                onMudar = { ligado ->
                    haptico.performHapticFeedback(if (ligado) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    configuracoesViewModel.escolherBackupAutomatico(ligado)
                },
            )
            SecaoArquivo(
                andamento = andamento,
                onExportar = viewModel::pedirSenhaParaExportar, // o seletor vem depois da senha
                onImportar = escolherBackup,
            )
        }
    }

    if (andamento == Andamento.DefinindoSenha) {
        SenhaParaExportarBottomSheet(
            onExportar = { senha ->
                viewModel.senhaDefinida(senha)
                abrirSeletor(viewModel) { exportarPara.launch(nomeDoArquivo(LocalDate.now())) }
            },
            onDismiss = viewModel::cancelarExportacao,
        )
    }
    EtapasDaImportacao(andamento, substituindo = true, viewModel = viewModel)
}

/**
 * O seletor de arquivos para importar, ligado ao [viewModel]: o arquivo escolhido é lido e conferido,
 * e o ViewModel passa a pedir a confirmação. Só abre com nada em andamento.
 */
@Composable
internal fun rememberEscolherBackup(viewModel: BackupViewModel): () -> Unit {
    // null: a pessoa desistiu no seletor
    val importarDe = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.lerArquivo(uri.toString())
    }
    return {
        if (viewModel.andamento.value == Andamento.Parado) {
            abrirSeletor(viewModel) { importarDe.launch(TIPOS_PARA_IMPORTAR) }
        }
    }
}

// Num aparelho sem o app de arquivos do sistema, o seletor não abre
private fun abrirSeletor(viewModel: BackupViewModel, abrir: () -> Unit) {
    try {
        abrir()
    } catch (e: ActivityNotFoundException) {
        viewModel.semSeletorDeArquivos()
    }
}

// Seções ------------------------------------------------------------------------------------

@Composable
private fun SecaoGoogle(
    ligado: Boolean,
    onMudar: (Boolean) -> Unit,
) {
    Column {
        TituloDaSecao("Google", alterada = false)
        Cartao {
            LinhaComChave(
                titulo = "Backup automático no Google",
                resumo = "O app faz uma cópia segura dos seus dados na sua conta Google, que pode ser restaurada depois",
                ligada = ligado,
                onMudar = onMudar,
            ) { Selo(Icons.Outlined.Backup) }
        }
    }
}

/** Exportar e importar. A linha da operação em andamento troca a seta por uma roda girando. */
@Composable
private fun SecaoArquivo(
    andamento: Andamento,
    onExportar: () -> Unit,
    onImportar: () -> Unit,
) {
    Column {
        TituloDaSecao("Arquivo", alterada = false)
        Cartao {
            LinhaQueAbre(
                titulo = "Exportar dados",
                resumo = "Salva uma cópia dos seus dados localmente",
                rotuloDoClique = "Escolher a senha e onde salvar o arquivo",
                onClick = onExportar,
                fim = if (andamento == Andamento.Exportando) {
                    { Rodando() }
                } else {
                    null
                },
            ) { Selo(Icons.Outlined.SaveAlt) }
            Divisoria()
            LinhaQueAbre(
                titulo = "Importar dados",
                resumo = "Restaura dados de um arquivo de backup",
                rotuloDoClique = "Escolher o arquivo para importar",
                onClick = onImportar,
                fim = if (andamento == Andamento.Lendo || andamento == Andamento.Importando) {
                    { Rodando() }
                } else {
                    null
                },
            ) { Selo(Icons.Outlined.FileOpen) }
        }
    }
}

// Peças -------------------------------------------------------------------------------------

/**
 * Uma opção de ligar e desligar: a linha toda responde ao toque, e a chave só mostra o estado. A das
 * notificações também usa.
 */
@Composable
internal fun LinhaComChave(
    titulo: String,
    resumo: String,
    ligada: Boolean,
    onMudar: (Boolean) -> Unit,
    inicio: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = ligada, role = Role.Switch, onValueChange = onMudar)
            .padding(horizontal = Espacos.l, vertical = Espacos.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        inicio()
        Spacer(Modifier.width(Espacos.m))
        Column(Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = resumo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(Espacos.m))
        Switch(checked = ligada, onCheckedChange = null)
    }
}

@Composable
private fun Rodando() {
    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
}

/**
 * O que a importação mostra por cima da tela, conforme o [andamento]: a senha, se o arquivo tem uma,
 * e depois a confirmação. [substituindo], como no [ConfirmarImportacao].
 */
@Composable
internal fun EtapasDaImportacao(
    andamento: Andamento,
    substituindo: Boolean,
    viewModel: BackupViewModel,
) {
    when (andamento) {
        is Andamento.PedindoSenha -> SenhaParaImportarBottomSheet(
            abrindo = andamento.abrindo,
            senhaIncorreta = andamento.senhaIncorreta,
            onAbrir = viewModel::abrirComSenha,
            onDismiss = viewModel::cancelarImportacao,
        )
        is Andamento.Confirmando -> ConfirmarImportacao(
            resumo = andamento.backup.resumo,
            substituindo = substituindo,
            onConfirmar = viewModel::importar,
            onCancelar = viewModel::cancelarImportacao,
        )
        else -> Unit
    }
}

/**
 * O que o arquivo traz, para a pessoa conferir antes de gravar. [substituindo]: há dados no aparelho
 * que vão embora, e o diálogo avisa; nas boas-vindas ainda não há nada, então é só restaurar.
 */
@Composable
internal fun ConfirmarImportacao(
    resumo: ResumoDoBackup,
    substituindo: Boolean,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (substituindo) "Substituir os dados?" else "Restaurar este backup?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacos.m)) {
                Text(descricaoDoBackup(resumo, ZoneId.systemDefault()))
                if (substituindo) {
                    Text("Os dados que estão neste aparelho vão ser apagados e trocados pelos do arquivo. Não dá para desfazer.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text(if (substituindo) "Substituir" else "Restaurar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        },
    )
}

// O arquivo ---------------------------------------------------------------------------------

private const val TIPO_DO_ARQUIVO = "application/json"

// Um .json chega com outro tipo dependendo de onde veio (um anexo, um app de arquivos antigo):
// o conteúdo é conferido depois, de qualquer jeito
private val TIPOS_PARA_IMPORTAR = arrayOf(TIPO_DO_ARQUIVO, "application/octet-stream", "text/plain")

/** O nome que o seletor já traz preenchido, com a data: "boletim-2026-10-15.json". */
internal fun nomeDoArquivo(hoje: LocalDate): String = "boletim-$hoje.json"

// Textos ------------------------------------------------------------------------------------

/** "Backup de 14/10/2026 com James e Maria: 3 anos letivos e 60 avaliações." */
internal fun descricaoDoBackup(resumo: ResumoDoBackup, zona: ZoneId): String {
    val dia = Instant.ofEpochMilli(resumo.exportadoEm.toEpochMilliseconds()).atZone(zona).toLocalDate()
    return "Backup de ${dia.format(FORMATO_DATA_COMPLETA)} com ${listaDeNomes(resumo.alunos)}: " +
        "${resumoDosAnos(resumo.anosLetivos)} e ${quantasAvaliacoes(resumo.avaliacoes)}."
}

/** "Ana", "Ana e Bia", "Ana, Bia e Caio"; com mais de 3, os 2 primeiros e quantos faltam. */
internal fun listaDeNomes(nomes: List<String>): String = when {
    nomes.size <= 1 -> nomes.firstOrNull().orEmpty()
    nomes.size <= 3 -> nomes.dropLast(1).joinToString(", ") + " e " + nomes.last()
    else -> nomes.take(2).joinToString(", ") + " e mais ${nomes.size - 2} alunos"
}

internal fun quantasAvaliacoes(quantidade: Int): String = when (quantidade) {
    0 -> "nenhuma avaliação"
    1 -> "1 avaliação"
    else -> "$quantidade avaliações"
}
