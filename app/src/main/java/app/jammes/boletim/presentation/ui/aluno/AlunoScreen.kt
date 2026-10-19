package app.jammes.boletim.presentation.ui.aluno

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.AlunoComAno
import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PerfilAluno
import app.jammes.boletim.domain.model.ResumoDoAluno
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.domain.usecase.nomeDoAluno
import app.jammes.boletim.presentation.ui.anoletivo.Cartao
import app.jammes.boletim.presentation.ui.anoletivo.Divisoria
import app.jammes.boletim.presentation.ui.anoletivo.FormularioAnoLetivo
import app.jammes.boletim.presentation.ui.anoletivo.LinhaQueAbre
import app.jammes.boletim.presentation.ui.components.Avatar
import app.jammes.boletim.presentation.ui.components.DURACAO_SAIDA
import app.jammes.boletim.presentation.ui.components.DURACAO_TRANSICAO
import app.jammes.boletim.presentation.ui.components.EstadoVazio
import app.jammes.boletim.presentation.ui.components.SeloDisciplina
import app.jammes.boletim.presentation.ui.components.descricaoDoAno
import app.jammes.boletim.presentation.ui.theme.Espacos
import java.time.LocalDate

/**
 * Perfil do aluno, aberto como dialog ao tocar na identificação do topo. Em cima, o aluno e o ano
 * abertos no app, como no card de identificação; embaixo, o que dá para fazer com eles.
 *
 * Editar o aluno, trocar de aluno, trocar de ano e criar um ano abrem uma página dentro do próprio
 * card, que cresce ou encolhe até ela. O voltar do sistema volta à página anterior (o início, ou a
 * edição do aluno na exclusão dele), e no início fecha.
 *
 * O dialog ocupa a janela toda e o card fica no alto, perto da identificação que o abriu. Assim a
 * janela não muda de tamanho a cada quadro enquanto o card anima; o toque fora dele é tratado aqui.
 */
@Composable
fun AlunoScreen(
    onFechar: () -> Unit,
    onContextoTrocado: () -> Unit, // aluno ou ano trocado: o que estava aberto era do anterior
    onAnoLetivoCriado: () -> Unit, // o ano novo já está aberto: falta conferir os ajustes dele
    onNovoAluno: () -> Unit, // abre o cadastro, que é o onboarding a partir do passo do aluno
    onBackup: () -> Unit, // fecha o perfil e abre o backup e a restauração
    onConfiguracoes: () -> Unit, // fecha o perfil e abre as configurações do app
    modifier: Modifier = Modifier,
    viewModel: AlunoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resumo by viewModel.resumo.collectAsStateWithLifecycle()
    val mudanca by viewModel.mudanca.collectAsStateWithLifecycle()
    // rememberSaveable: a página aberta sobrevive a girar a tela
    var pagina by rememberSaveable { mutableStateOf(Pagina.Inicio) }
    // O perfil no toque em Excluir. O app troca de aluno (ou volta ao onboarding) e só depois apaga:
    // até o perfil fechar, o card continua como estava, sem mostrar o outro aluno nem números zerados
    var naExclusao by remember { mutableStateOf<RetratoDoPerfil?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val haptico = LocalHapticFeedback.current
    // O detector de toque do fundo não reinicia: assim ele chama sempre o onFechar atual
    val fechar by rememberUpdatedState(onFechar)

    BackHandler(enabled = pagina != Pagina.Inicio) { pagina = pagina.anterior }

    LaunchedEffect(mudanca) {
        when (val m = mudanca) {
            Mudanca.Trocou -> onContextoTrocado()
            Mudanca.CriouAnoLetivo -> {
                haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                onAnoLetivoCriado()
            }
            // O início já mostra o nome novo
            Mudanca.Renomeou -> {
                haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                pagina = Pagina.Inicio
                viewModel.mudancaVista()
            }
            // As telas abertas eram do aluno excluído. Sem nenhum aluno, o onboarding já tomou o lugar
            // do app, e com ele o do perfil
            Mudanca.Excluiu -> onContextoTrocado()
            is Mudanca.Falhou -> {
                naExclusao = null
                snackbarHostState.showSnackbar(m.motivo)
                viewModel.mudancaVista()
            }
            else -> Unit
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { fechar() } }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Espacos.l, vertical = Espacos.s),
    ) {
        val cartao = Modifier
            .align(Alignment.TopCenter)
            .widthIn(max = LARGURA_MAXIMA)
            .fillMaxWidth()

        val mostrado = naExclusao?.let { AlunoUiState.Sucesso(it.perfil) } ?: state
        when (val s = mostrado) {
            // O Room responde em milissegundos: o card já aparece pronto, sem um carregando no meio
            AlunoUiState.Carregando -> Unit

            AlunoUiState.NaoEncontrado -> CartaoDoPerfil(cartao) {
                EstadoVazio(
                    icone = Icons.Outlined.PersonOff,
                    titulo = "Aluno não encontrado",
                    mensagem = "Ele pode ter sido apagado.",
                )
            }

            is AlunoUiState.Sucesso -> {
                val perfil = s.perfil
                // Do toque até o perfil fechar. Depois de gravar, o perfil já mostra o aluno ou o ano
                // novo enquanto fecha, e as páginas não podem reagir a isso
                val mudando = mudanca == Mudanca.Mudando ||
                    mudanca == Mudanca.Trocou ||
                    mudanca == Mudanca.CriouAnoLetivo ||
                    mudanca == Mudanca.Excluiu
                CartaoDoPerfil(cartao) {
                    PaginasDoPerfil(
                        pagina = pagina,
                        perfil = perfil,
                        resumo = naExclusao?.resumo ?: resumo,
                        mudando = mudando,
                        onAbrir = { pagina = it },
                        onFechar = onFechar,
                        onEscolherAluno = { escolhido ->
                            // O que já está aberto não muda nada: só fecha
                            if (escolhido.aluno.id == perfil.aluno.id) {
                                onFechar()
                            } else {
                                haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                viewModel.trocarAluno(escolhido)
                            }
                        },
                        onEscolherAno = { escolhido ->
                            if (escolhido.id == perfil.anoLetivo?.id) {
                                onFechar()
                            } else {
                                haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                viewModel.trocarAnoLetivo(escolhido)
                            }
                        },
                        onCriarAnoLetivo = viewModel::criarAnoLetivo,
                        onRenomearAluno = viewModel::renomearAluno,
                        onExcluirAluno = {
                            haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                            naExclusao = RetratoDoPerfil(perfil, resumo)
                            viewModel.excluirAluno()
                        },
                        onNovoAluno = onNovoAluno,
                        onBackup = onBackup,
                        onConfiguracoes = onConfiguracoes,
                    )
                }
            }
        }

        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * As páginas do card. A ordem diz para que lado a troca desliza: sair do Início é ir para a frente,
 * e da edição do aluno para a exclusão dele também.
 */
private enum class Pagina {
    Inicio, EditarAluno, ExcluirAluno, TrocarAluno, NovoAnoLetivo, TrocarAnoLetivo;

    /** Para onde o voltar leva: da exclusão, de volta à edição do aluno; das outras, ao início. */
    val anterior: Pagina
        get() = if (this == ExcluirAluno) EditarAluno else Inicio
}

/** O que o card mostrava no toque em Excluir. */
private data class RetratoDoPerfil(val perfil: PerfilAluno, val resumo: ResumoDoAluno?)

@Composable
private fun CartaoDoPerfil(
    modifier: Modifier = Modifier,
    conteudo: @Composable () -> Unit,
) {
    Surface(
        // Um toque no card que não cai num botão fica nele: não chega ao fundo, que fecharia o perfil
        modifier = modifier.pointerInput(Unit) { detectTapGestures { } },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        content = conteudo,
    )
}

@Composable
private fun PaginasDoPerfil(
    pagina: Pagina,
    perfil: PerfilAluno,
    resumo: ResumoDoAluno?, // o que o aluno aberto tem; null enquanto carrega
    mudando: Boolean, // uma troca ou criação em andamento: os toques esperam ela acabar
    onAbrir: (Pagina) -> Unit,
    onFechar: () -> Unit,
    onEscolherAluno: (AlunoComAno) -> Unit,
    onEscolherAno: (AnoLetivoDomain) -> Unit,
    onCriarAnoLetivo: (ano: Int, serie: String) -> Unit,
    onRenomearAluno: (nome: String) -> Unit,
    onExcluirAluno: () -> Unit,
    onNovoAluno: () -> Unit,
    onBackup: () -> Unit,
    onConfiguracoes: () -> Unit,
) {
    AnimatedContent(
        targetState = pagina,
        transitionSpec = {
            // Abrir uma página traz ela da direita; voltar ao início traz ele da esquerda. Como na
            // navegação, a que sai some rápido e só então a nova aparece: os textos não se misturam
            val sentido = if (targetState.ordinal > initialState.ordinal) 1 else -1
            val deslize = tween<IntOffset>(DURACAO_TRANSICAO)
            (slideInHorizontally(deslize) { largura -> sentido * largura / 5 } +
                fadeIn(tween(DURACAO_TRANSICAO - DURACAO_SAIDA, delayMillis = DURACAO_SAIDA))) togetherWith
                (slideOutHorizontally(deslize) { largura -> -sentido * largura / 5 } +
                    fadeOut(tween(DURACAO_SAIDA))) using
                SizeTransform(clip = true) { _, _ -> tween(DURACAO_TRANSICAO) }
        },
        label = "paginaDoPerfil",
    ) { atual ->
        val voltar = { onAbrir(atual.anterior) }
        when (atual) {
            Pagina.Inicio -> PaginaInicio(
                perfil = perfil,
                onFechar = onFechar,
                onAbrir = onAbrir,
                onNovoAluno = onNovoAluno,
                onBackup = onBackup,
                onConfiguracoes = onConfiguracoes,
            )
            Pagina.EditarAluno -> PaginaEditarAluno(
                aluno = perfil.aluno,
                mudando = mudando,
                onVoltar = voltar,
                onSalvar = onRenomearAluno,
                onExcluir = { onAbrir(Pagina.ExcluirAluno) },
            )
            Pagina.ExcluirAluno -> PaginaExcluirAluno(
                aluno = perfil.aluno,
                resumo = resumo,
                unico = perfil.alunos.size == 1,
                mudando = mudando,
                onVoltar = voltar,
                onBackup = onBackup,
                onExcluir = onExcluirAluno,
            )
            Pagina.TrocarAluno -> PaginaTrocarAluno(
                perfil = perfil,
                mudando = mudando,
                onVoltar = voltar,
                onEscolher = onEscolherAluno,
            )
            Pagina.NovoAnoLetivo -> PaginaNovoAnoLetivo(
                anosLetivos = perfil.anosLetivos,
                mudando = mudando,
                onVoltar = voltar,
                onCriar = onCriarAnoLetivo,
            )
            Pagina.TrocarAnoLetivo -> PaginaTrocarAnoLetivo(
                perfil = perfil,
                mudando = mudando,
                onVoltar = voltar,
                onEscolher = onEscolherAno,
            )
        }
    }
}

// Páginas ------------------------------------------------------------------------------------

/** O aluno e o ano abertos, como no card de identificação, e as opções em grupos. */
@Composable
private fun PaginaInicio(
    perfil: PerfilAluno,
    onFechar: () -> Unit,
    onAbrir: (Pagina) -> Unit,
    onNovoAluno: () -> Unit,
    onBackup: () -> Unit,
    onConfiguracoes: () -> Unit,
) {
    Column(Modifier.padding(bottom = Espacos.m)) {
        // A mesma linha do card de identificação, com o fechar no lugar do calendário
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Espacos.m + Espacos.l, end = Espacos.s, top = Espacos.l, bottom = Espacos.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(nome = perfil.aluno.nome)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Espacos.m)
            ) {
                Text(
                    text = perfil.aluno.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                perfil.anoLetivo?.let { ano ->
                    Text(
                        text = descricaoDoAno(ano),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onFechar) {
                Icon(Icons.Filled.Close, contentDescription = "Fechar")
            }
        }

        CorpoDaPagina(Arrangement.spacedBy(Espacos.s)) {
            Cartao {
                LinhaQueAbre(
                    titulo = "Editar aluno",
                    resumo = "Mudar o nome ou excluir",
                    rotuloDoClique = "Mudar o nome ou excluir o aluno",
                    onClick = { onAbrir(Pagina.EditarAluno) },
                ) { Selo(Icons.Outlined.ManageAccounts) }
                Divisoria()
                LinhaQueAbre(
                    titulo = "Trocar aluno",
                    resumo = resumoDosAlunos(perfil.alunos.size),
                    rotuloDoClique = "Escolher outro aluno",
                    onClick = { onAbrir(Pagina.TrocarAluno) },
                ) { Selo(Icons.Outlined.SwitchAccount) }
                Divisoria()
                LinhaQueAbre(
                    titulo = "Novo aluno",
                    rotuloDoClique = "Cadastrar um aluno",
                    onClick = onNovoAluno,
                ) { Selo(Icons.Outlined.PersonAdd) }
            }
            Cartao {
                LinhaQueAbre(
                    titulo = "Criar novo ano letivo",
                    rotuloDoClique = "Criar um ano letivo",
                    onClick = { onAbrir(Pagina.NovoAnoLetivo) },
                ) { Selo(Icons.Outlined.EditCalendar) }
                Divisoria()
                LinhaQueAbre(
                    titulo = "Trocar ano letivo",
                    resumo = resumoDosAnos(perfil.anosLetivos.size),
                    rotuloDoClique = "Escolher outro ano letivo",
                    onClick = { onAbrir(Pagina.TrocarAnoLetivo) },
                ) { Selo(Icons.Outlined.EventRepeat) }
            }
            Cartao {
                LinhaQueAbre(
                    titulo = "Backup e Restauração",
                    rotuloDoClique = "Abrir o backup e a restauração",
                    onClick = onBackup,
                ) { Selo(Icons.Outlined.SettingsBackupRestore) }
                Divisoria()
                LinhaQueAbre(
                    titulo = "Configurações",
                    rotuloDoClique = "Abrir as configurações do app",
                    onClick = onConfiguracoes,
                ) { Selo(Icons.Outlined.Settings) }
            }
        }
    }
}

/**
 * O nome do aluno aberto, para corrigir, e embaixo a exclusão dele, que a página seguinte confirma.
 * Salvar grava o nome e volta ao início, que já mostra o novo.
 */
@Composable
private fun PaginaEditarAluno(
    aluno: AlunoDomain,
    mudando: Boolean,
    onVoltar: () -> Unit,
    onSalvar: (nome: String) -> Unit,
    onExcluir: () -> Unit,
) {
    // rememberSaveable: o que já foi digitado sobrevive a girar a tela
    var nome by rememberSaveable { mutableStateOf(aluno.nome) }
    val problema = problemaDoNome(nome)
    // Sem mudança não há o que salvar: "Ana " é o mesmo nome de "Ana"
    val podeSalvar = problema == null && nomeDoAluno(nome) != aluno.nome && !mudando
    val foco = LocalFocusManager.current

    Column(Modifier.padding(bottom = Espacos.l)) {
        CabecalhoDaPagina("Editar aluno", onVoltar)
        CorpoDaPagina(
            arranjo = Arrangement.spacedBy(Espacos.m),
            modifier = Modifier.padding(horizontal = Espacos.xs),
        ) {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome do aluno") },
                isError = problema != null,
                supportingText = problema?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    foco.clearFocus()
                    if (podeSalvar) onSalvar(nome)
                }),
                trailingIcon = if (nome.isNotEmpty()) {
                    {
                        IconButton(onClick = { nome = "" }) {
                            Icon(Icons.Filled.Cancel, contentDescription = "Apagar o nome")
                        }
                    }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Espacos.m)
            ) {
                OutlinedButton(
                    onClick = onVoltar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancelar") }

                Button(
                    onClick = { onSalvar(nome) },
                    modifier = Modifier.weight(1f),
                    enabled = podeSalvar,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Salvar") }
            }

            // Num cartão só dela, longe do Salvar
            Cartao(Modifier.padding(top = Espacos.s)) {
                LinhaQueAbre(
                    titulo = "Excluir aluno",
                    resumo = "Apaga também as notas e as faltas",
                    rotuloDoClique = "Excluir o aluno",
                    corDoTitulo = MaterialTheme.colorScheme.error,
                    onClick = onExcluir,
                ) { Selo(Icons.Outlined.PersonRemove, cor = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

/**
 * A confirmação da exclusão: quem sai, o que sai junto em números, e o atalho para um backup antes.
 * Excluir apaga e fecha o perfil; o app abre outro aluno, ou volta às boas-vindas se era o único.
 */
@Composable
private fun PaginaExcluirAluno(
    aluno: AlunoDomain,
    resumo: ResumoDoAluno?,
    unico: Boolean, // o único aluno do aparelho
    mudando: Boolean,
    onVoltar: () -> Unit,
    onBackup: () -> Unit,
    onExcluir: () -> Unit,
) {
    Column(Modifier.padding(bottom = Espacos.l)) {
        CabecalhoDaPagina("Excluir aluno", onVoltar)
        CorpoDaPagina(
            arranjo = Arrangement.spacedBy(Espacos.m),
            modifier = Modifier.padding(horizontal = Espacos.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(nome = aluno.nome)
                Spacer(Modifier.width(Espacos.m))
                Text(
                    text = aluno.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = avisoDaExclusao(resumo, unico),
                style = MaterialTheme.typography.bodyMedium,
            )

            Cartao {
                LinhaQueAbre(
                    titulo = "Fazer um backup antes",
                    resumo = "Para poder trazer tudo de volta",
                    rotuloDoClique = "Abrir o backup e a restauração",
                    onClick = onBackup,
                ) { Selo(Icons.Outlined.SettingsBackupRestore) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Espacos.m)
            ) {
                OutlinedButton(
                    onClick = onVoltar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancelar") }

                Button(
                    onClick = onExcluir,
                    modifier = Modifier.weight(1f),
                    enabled = !mudando,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Excluir") }
            }
        }
    }
}

/** Os alunos deste aparelho; o aberto vem marcado. Escolher outro troca e fecha o perfil. */
@Composable
private fun PaginaTrocarAluno(
    perfil: PerfilAluno,
    mudando: Boolean,
    onVoltar: () -> Unit,
    onEscolher: (AlunoComAno) -> Unit,
) {
    Column(Modifier.padding(bottom = Espacos.m)) {
        CabecalhoDaPagina("Trocar aluno", onVoltar)
        CorpoDaPagina {
            Cartao(Modifier.selectableGroup()) {
                perfil.alunos.forEachIndexed { i, opcao ->
                    if (i > 0) Divisoria()
                    LinhaDeEscolha(
                        titulo = opcao.aluno.nome,
                        // Sem ano letivo não há boletim para abrir: o aluno aparece, mas não dá para escolher
                        resumo = opcao.anoLetivo?.let(::descricaoDoAno) ?: "Sem ano letivo",
                        escolhida = opcao.aluno.id == perfil.aluno.id,
                        disponivel = opcao.anoLetivo != null,
                        mudando = mudando,
                        onClick = { onEscolher(opcao) },
                    ) {
                        Avatar(nome = opcao.aluno.nome)
                    }
                }
            }
        }
    }
}

/** Os anos letivos do aluno aberto, do mais recente ao mais antigo; escolher troca e fecha o perfil. */
@Composable
private fun PaginaTrocarAnoLetivo(
    perfil: PerfilAluno,
    mudando: Boolean,
    onVoltar: () -> Unit,
    onEscolher: (AnoLetivoDomain) -> Unit,
) {
    Column(Modifier.padding(bottom = Espacos.m)) {
        CabecalhoDaPagina("Trocar ano letivo", onVoltar)
        CorpoDaPagina {
            Cartao(Modifier.selectableGroup()) {
                perfil.anosLetivos.forEachIndexed { i, ano ->
                    if (i > 0) Divisoria()
                    LinhaDeEscolha(
                        titulo = ano.ano.toString(),
                        resumo = resumoDoAnoLetivo(ano),
                        escolhida = ano.id == perfil.anoLetivo?.id,
                        mudando = mudando,
                        onClick = { onEscolher(ano) },
                    ) {
                        Selo(Icons.Outlined.CalendarMonth)
                    }
                }
            }
        }
    }
}

/**
 * O ano e a série do ano novo; o resto (períodos, regras e matérias) começa no padrão. Criar grava o
 * ano, abre ele no app e leva aos ajustes dele, para conferir.
 */
@Composable
private fun PaginaNovoAnoLetivo(
    anosLetivos: List<AnoLetivoDomain>, // os que o aluno já tem: o mesmo ano não pode repetir
    mudando: Boolean,
    onVoltar: () -> Unit,
    onCriar: (ano: Int, serie: String) -> Unit,
) {
    // rememberSaveable: o que já foi digitado sobrevive a girar a tela
    var ano by rememberSaveable { mutableStateOf(anoSugerido(anosLetivos, LocalDate.now()).toString()) }
    var serie by rememberSaveable { mutableStateOf("") }

    val anoValor = FormularioAnoLetivo.Identificacao(ano = ano, serie = serie).anoValor
    // Criado, o ano entra na lista antes de o perfil fechar: sem a trava, piscaria o "Já existe"
    val problema = if (mudando) null else problemaDoAnoNovo(anoValor, anosLetivos)

    Column(Modifier.padding(bottom = Espacos.l)) {
        CabecalhoDaPagina("Novo ano letivo", onVoltar)
        CorpoDaPagina(
            arranjo = Arrangement.spacedBy(Espacos.m),
            modifier = Modifier.padding(horizontal = Espacos.xs),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacos.m)) {
                OutlinedTextField(
                    value = ano,
                    onValueChange = { ano = it },
                    label = { Text("Ano") },
                    isError = problema != null,
                    supportingText = problema?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = serie,
                    onValueChange = { serie = it },
                    label = { Text("Série") },
                    placeholder = { Text("9º Ano") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.weight(2f)
                )
            }

            Text(
                text = "Os períodos, as regras de média e as matérias começam no padrão. " +
                    "Em seguida, os ajustes do ano abrem para você conferir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Espacos.m)
            ) {
                OutlinedButton(
                    onClick = onVoltar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancelar") }

                Button(
                    onClick = { anoValor?.let { onCriar(it, serie) } },
                    modifier = Modifier.weight(1f),
                    enabled = problema == null && !mudando,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Criar") }
            }
        }
    }
}

// Peças das páginas --------------------------------------------------------------------------

/** O voltar e o título de uma página; o voltar leva ao início do perfil. */
@Composable
private fun CabecalhoDaPagina(titulo: String, onVoltar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Espacos.xs, end = Espacos.l, top = Espacos.s, bottom = Espacos.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onVoltar) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
        }
        Spacer(Modifier.width(Espacos.xs))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/**
 * O que vem embaixo do cabeçalho. Só ele rola quando não cabe (muitos alunos, tela deitada): o
 * cabeçalho, com o fechar e o voltar, fica sempre à vista.
 */
@Composable
private fun CorpoDaPagina(
    arranjo: Arrangement.Vertical = Arrangement.Top,
    modifier: Modifier = Modifier,
    conteudo: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Espacos.m)
            .then(modifier),
        verticalArrangement = arranjo,
    ) {
        conteudo()
    }
}

/**
 * Uma opção de uma lista de escolha única: o [inicio] (avatar ou selo), o nome e o [resumo], e o
 * visto na escolhida. [disponivel] falso apaga a linha e não deixa escolher. As configurações do app
 * usam a mesma.
 */
@Composable
internal fun LinhaDeEscolha(
    titulo: String,
    resumo: String,
    escolhida: Boolean,
    onClick: () -> Unit,
    mudando: Boolean = false,
    disponivel: Boolean = true,
    inicio: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = escolhida,
                // Durante uma troca a linha não responde, mas também não apaga: logo o perfil fecha
                enabled = disponivel && !mudando,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = Espacos.l, vertical = Espacos.m)
            .alpha(if (disponivel) 1f else 0.38f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        inicio()
        Spacer(Modifier.width(Espacos.m))
        Column(Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (escolhida) FontWeight.SemiBold else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = resumo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (escolhida) {
            Spacer(Modifier.width(Espacos.s))
            // O leitor de tela já diz "selecionado" pelo selectable
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** O ícone da opção num quadrado tingido de azul, como as linhas dos ajustes do ano; de vermelho, se apaga dados. */
@Composable
internal fun Selo(icone: ImageVector, cor: Color = MaterialTheme.colorScheme.primary) {
    SeloDisciplina(cor = cor, icone = icone)
}

// O máximo do Material para dialogs: num tablet ou com a tela deitada, o card não estica demais
private val LARGURA_MAXIMA = 560.dp

// Textos ------------------------------------------------------------------------------------

/** "2 alunos neste aparelho"; com um só, "1 aluno neste aparelho". */
internal fun resumoDosAlunos(quantidade: Int): String =
    if (quantidade == 1) "1 aluno neste aparelho" else "$quantidade alunos neste aparelho"

/** "3 anos letivos"; com um só, "1 ano letivo". */
internal fun resumoDosAnos(quantidade: Int): String =
    if (quantidade == 1) "1 ano letivo" else "$quantidade anos letivos"

/** "4 unidades", "1 bimestre"; null quando não há nenhum. */
internal fun quantosPeriodos(quantidade: Int, tipo: TipoPeriodo): String? = when (quantidade) {
    0 -> null
    1 -> "1 ${tipo.displayName.lowercase()}"
    else -> "$quantidade ${tipo.displayName.lowercase()}s"
}

/** "9º Ano · 4 unidades": a série e como o ano se divide, embaixo do ano na lista. */
internal fun resumoDoAnoLetivo(ano: AnoLetivoDomain): String = listOfNotNull(
    ano.serie?.takeIf { it.isNotBlank() } ?: "Sem série",
    quantosPeriodos(ano.periodo.size, ano.tipoPeriodo),
).joinToString(" · ")

/**
 * O ano que o formulário já traz: o do calendário, ou o seguinte ao mais recente do aluno quando
 * esse já foi criado (um ano que já existe não pode ser criado de novo).
 */
internal fun anoSugerido(anosLetivos: List<AnoLetivoDomain>, hoje: LocalDate): Int =
    maxOf(hoje.year, (anosLetivos.maxOfOrNull { it.ano } ?: 0) + 1)

/** O que impede de salvar o nome, embaixo do campo; null quando dá. */
internal fun problemaDoNome(digitado: String): String? =
    if (nomeDoAluno(digitado) == null) "Escreva o nome" else null

/**
 * "2 anos letivos, 10 disciplinas, 53 avaliações e 8 faltas": o que o aluno tem no aparelho, sem o
 * que está zerado. null quando ele não tem nada.
 */
internal fun oQueEDoAluno(resumo: ResumoDoAluno): String? {
    val partes = listOfNotNull(
        resumo.anosLetivos.takeIf { it > 0 }?.let(::resumoDosAnos),
        resumo.disciplinas.takeIf { it > 0 }?.let { if (it == 1) "1 disciplina" else "$it disciplinas" },
        resumo.avaliacoes.takeIf { it > 0 }?.let { if (it == 1) "1 avaliação" else "$it avaliações" },
        resumo.faltas.takeIf { it > 0 }?.let { if (it == 1) "1 falta" else "$it faltas" },
    )
    return when (partes.size) {
        0 -> null
        1 -> partes.single()
        else -> partes.dropLast(1).joinToString(", ") + " e " + partes.last()
    }
}

/**
 * "ATENÇÃO! Serão excluídos: 1 ano letivo e 5 disciplinas. Não dá para desfazer." Sem o [resumo]
 * (ainda carregando), sem a lista. Do [unico] aluno, diz também que o app recomeça.
 */
internal fun avisoDaExclusao(resumo: ResumoDoAluno?, unico: Boolean): String {
    val oQue = resumo?.let(::oQueEDoAluno)
    val sai = if (oQue == null) {
        "ATENÇÃO! Serão excluídos todos os dados do aluno."
    } else {
        "ATENÇÃO! Serão excluídos: $oQue."
    }
    val depois = if (unico) " Como é o único aluno, o app volta ao começo, para um cadastro novo." else ""
    return "$sai Não dá para desfazer.$depois"
}

/**
 * O que impede de criar o ano, curto para caber embaixo do campo; null quando dá. [ano] é o valor
 * digitado, ou null enquanto ele não tiver 4 dígitos.
 */
internal fun problemaDoAnoNovo(ano: Int?, anosLetivos: List<AnoLetivoDomain>): String? = when {
    ano == null -> "4 dígitos"
    anosLetivos.any { it.ano == ano } -> "Já existe"
    else -> null
}
