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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PerfilAluno
import app.jammes.boletim.domain.model.TipoPeriodo
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
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Perfil do aluno, aberto como dialog ao tocar na identificação do topo. Em cima, o aluno e o ano
 * abertos no app, como no card de identificação; embaixo, o que dá para fazer com eles.
 *
 * Trocar de aluno, trocar de ano e criar um ano abrem uma página dentro do próprio card, que cresce
 * ou encolhe até ela. O voltar do sistema volta ao início, e no início fecha.
 *
 * O dialog ocupa a janela toda e o card fica no alto, perto da identificação que o abriu. Assim a
 * janela não muda de tamanho a cada quadro enquanto o card anima; o toque fora dele é tratado aqui.
 */
@Composable
fun AlunoScreen(
    onFechar: () -> Unit,
    onContextoTrocado: () -> Unit, // aluno ou ano trocado: o que estava aberto era do anterior
    onAnoLetivoCriado: () -> Unit, // o ano novo já está aberto: falta conferir os ajustes dele
    modifier: Modifier = Modifier,
    viewModel: AlunoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mudanca by viewModel.mudanca.collectAsStateWithLifecycle()
    // rememberSaveable: a página aberta sobrevive a girar a tela
    var pagina by rememberSaveable { mutableStateOf(Pagina.Inicio) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptico = LocalHapticFeedback.current
    // O detector de toque do fundo não reinicia: assim ele chama sempre o onFechar atual
    val fechar by rememberUpdatedState(onFechar)

    BackHandler(enabled = pagina != Pagina.Inicio) { pagina = Pagina.Inicio }

    // Um aviso de cada vez: o novo toma o lugar do que estiver na tela
    fun avisar(mensagem: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(mensagem)
        }
    }

    LaunchedEffect(mudanca) {
        when (val m = mudanca) {
            Mudanca.Trocou -> onContextoTrocado()
            Mudanca.CriouAnoLetivo -> {
                haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                onAnoLetivoCriado()
            }
            is Mudanca.Falhou -> {
                snackbarHostState.showSnackbar(m.motivo)
                viewModel.falhaVista()
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

        when (val s = state) {
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
                    mudanca == Mudanca.CriouAnoLetivo
                CartaoDoPerfil(cartao) {
                    PaginasDoPerfil(
                        pagina = pagina,
                        perfil = perfil,
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
                        // TODO: abrir o onboarding, quando a tela existir
                        onNovoAluno = { avisar("O cadastro de novo aluno ainda não está disponível") },
                        // TODO: abrir as configurações de backup, quando existirem
                        onBackup = { avisar("As configurações de backup ainda não estão disponíveis") },
                    )
                }
            }
        }

        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

/** As páginas do card. A ordem diz para que lado a troca desliza: sair do Início é ir para a frente. */
private enum class Pagina { Inicio, TrocarAluno, NovoAnoLetivo, TrocarAnoLetivo }

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
    mudando: Boolean, // uma troca ou criação em andamento: os toques esperam ela acabar
    onAbrir: (Pagina) -> Unit,
    onFechar: () -> Unit,
    onEscolherAluno: (AlunoComAno) -> Unit,
    onEscolherAno: (AnoLetivoDomain) -> Unit,
    onCriarAnoLetivo: (ano: Int, serie: String) -> Unit,
    onNovoAluno: () -> Unit,
    onBackup: () -> Unit,
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
        val voltar = { onAbrir(Pagina.Inicio) }
        when (atual) {
            Pagina.Inicio -> PaginaInicio(
                perfil = perfil,
                onFechar = onFechar,
                onAbrir = onAbrir,
                onNovoAluno = onNovoAluno,
                onBackup = onBackup,
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
                    titulo = "Configurações de backup",
                    rotuloDoClique = "Abrir as configurações de backup",
                    onClick = onBackup,
                ) { Selo(Icons.Outlined.Backup) }
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
 * visto na escolhida. [disponivel] falso apaga a linha e não deixa escolher.
 */
@Composable
private fun LinhaDeEscolha(
    titulo: String,
    resumo: String,
    escolhida: Boolean,
    mudando: Boolean,
    onClick: () -> Unit,
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

/** O ícone da opção num quadrado tingido de azul, como as linhas dos ajustes do ano. */
@Composable
private fun Selo(icone: ImageVector) {
    SeloDisciplina(cor = MaterialTheme.colorScheme.primary, icone = icone)
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

/**
 * O que impede de criar o ano, curto para caber embaixo do campo; null quando dá. [ano] é o valor
 * digitado, ou null enquanto ele não tiver 4 dígitos.
 */
internal fun problemaDoAnoNovo(ano: Int?, anosLetivos: List<AnoLetivoDomain>): String? = when {
    ano == null -> "4 dígitos"
    anosLetivos.any { it.ano == ano } -> "Já existe"
    else -> null
}
