package app.jammes.boletim.presentation.ui.anoletivo

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.AjustesAnoLetivo
import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.domain.model.Lancamentos
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.domain.usecase.arredondarMedia
import app.jammes.boletim.presentation.ui.boletim.formatarMedia
import app.jammes.boletim.presentation.ui.components.CarregandoDiscreto
import app.jammes.boletim.presentation.ui.components.EstadoVazio
import app.jammes.boletim.presentation.ui.components.SeloDisciplina
import app.jammes.boletim.presentation.ui.disciplina.formatarNumero
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import app.jammes.boletim.presentation.ui.theme.Espacos
import app.jammes.boletim.presentation.ui.theme.IconesDisciplina
import kotlinx.coroutines.launch
import java.io.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Ajustes do ano letivo aberto no app, num painel por seções: o ano, os períodos, as regras de
 * média e as matérias. O painel mostra o valor de cada ajuste; o que tem mais opções abre um sheet.
 * Tudo vai para uma cópia, e a barra de baixo, que só aparece quando algo mudou, grava tudo junto.
 */
@Composable
fun AnoLetivoScreen(
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnoLetivoViewModel = hiltViewModel()
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val salvamento by viewModel.salvamento.collectAsStateWithLifecycle()

    when (val s = state) {
        AnoLetivoUiState.Carregando -> MolduraAjustes(onVoltar = onVoltar, modifier = modifier) { padding ->
            CarregandoDiscreto(Modifier.fillMaxSize().padding(padding))
        }

        AnoLetivoUiState.NaoEncontrado -> MolduraAjustes(onVoltar = onVoltar, modifier = modifier) { padding ->
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EstadoVazio(
                    icone = Icons.Outlined.EventBusy,
                    titulo = "Ano letivo não encontrado",
                    mensagem = "Ele pode ter sido apagado.",
                )
            }
        }

        is AnoLetivoUiState.Sucesso -> EditorAjustes(
            ajustes = s.ajustes,
            salvamento = salvamento,
            onSalvar = viewModel::salvar,
            onFalhaVista = viewModel::falhaVista,
            onVoltar = onVoltar,
            modifier = modifier,
        )
    }
}

/** O sheet aberto por cima do painel. Serializable para voltar aberto depois de girar a tela. */
private sealed interface Editor : Serializable {
    data object Ano : Editor
    data class Periodo(val indice: Int) : Editor // a posição do período na lista
    data object Calculo : Editor
    data object Arredondamento : Editor
    data object Materias : Editor
}

@Composable
private fun EditorAjustes(
    ajustes: AjustesAnoLetivo,
    salvamento: Salvamento,
    onSalvar: (FormularioAnoLetivo) -> Unit,
    onFalhaVista: () -> Unit,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gravado = remember(ajustes) { FormularioAnoLetivo.de(ajustes) }
    // rememberSaveable: o que já foi editado sobrevive a girar a tela
    var form by rememberSaveable { mutableStateOf(gravado) }
    // O sheet aberto; null = nenhum. Fica aqui, fora da lista, para voltar aberto depois de girar
    // a tela mesmo que a seção dele não esteja à vista
    var editor by rememberSaveable { mutableStateOf<Editor?>(null) }
    var confirmandoDescarte by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptico = LocalHapticFeedback.current

    // Refeitos só quando o formulário muda, e não a cada recomposição
    val alteracoes = remember(form, gravado) { form.alteracoes(gravado) }
    val problema = remember(form) { form.problema() }
    // Os mesmos problemas de datas pintam a linha do tempo e as linhas dos períodos
    val problemas = remember(form.periodos) { problemasDasDatas(form.periodos.visiveis) }
    val bloqueio = remember(form.periodos, ajustes) { form.periodos.bloqueioParaDiminuir(ajustes) }
    val materias = remember(ajustes) { linhasDasMaterias(ajustes) }
    val alterado = alteracoes.total > 0

    // Com alterações, sair pergunta antes: pela seta da barra e pelo voltar do sistema
    fun voltar() {
        if (alterado) confirmandoDescarte = true else onVoltar()
    }
    BackHandler(enabled = alterado, onBack = ::voltar)

    // Um aviso de cada vez: o novo toma o lugar do que estiver na tela
    fun avisar(mensagem: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(mensagem)
        }
    }

    // Descarta na hora, e o aviso deixa desfazer por alguns segundos
    fun descartar() {
        val descartado = form
        form = gravado
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val resultado = snackbarHostState.showSnackbar("Alterações descartadas", actionLabel = "Desfazer")
            if (resultado == SnackbarResult.ActionPerformed) form = descartado
        }
    }

    LaunchedEffect(salvamento) {
        when (val s = salvamento) {
            Salvamento.Salvo -> {
                haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                onVoltar()
            }
            is Salvamento.Falhou -> {
                snackbarHostState.showSnackbar(s.motivo)
                onFalhaVista()
            }
            else -> Unit
        }
    }

    MolduraAjustes(
        onVoltar = ::voltar,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        barraDeBaixo = {
            BarraDeSalvar(
                alteracoes = alteracoes.total,
                problema = problema,
                salvando = salvamento == Salvamento.Salvando,
                onDescartar = ::descartar,
                onSalvar = { onSalvar(form) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = Espacos.l,
                end = Espacos.l,
                top = Espacos.xs,
                bottom = Espacos.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(Espacos.l),
        ) {
            // Cada seção recebe só a sua parte do formulário: mexer numa não recompõe as outras
            item(key = "ano") {
                SecaoAnoLetivo(
                    identificacao = form.identificacao,
                    periodos = form.periodos,
                    problemas = problemas,
                    alterada = alteracoes.ano > 0,
                    onEditar = { editor = Editor.Ano },
                )
            }
            item(key = "periodos") {
                SecaoPeriodos(
                    periodos = form.periodos,
                    ano = form.identificacao.anoValor,
                    problemas = problemas,
                    bloqueio = bloqueio,
                    alterada = alteracoes.periodos > 0,
                    onMudar = { form = form.copy(periodos = it) },
                    onEditarPeriodo = { editor = Editor.Periodo(it) },
                    onAvisar = ::avisar,
                )
            }
            item(key = "regras") {
                SecaoRegras(
                    regras = form.regras,
                    alterada = alteracoes.regras > 0,
                    onMudar = { form = form.copy(regras = it) },
                    onEditarCalculo = { editor = Editor.Calculo },
                    onEditarArredondamento = { editor = Editor.Arredondamento },
                )
            }
            item(key = "materias") {
                SecaoMaterias(
                    linhas = materias,
                    marcadas = form.materias,
                    alterada = alteracoes.materias > 0,
                    onEditar = { editor = Editor.Materias },
                )
            }
        }
    }

    // Os sheets mudam o formulário, como o resto do painel; quem grava é a barra de baixo
    when (val aberto = editor) {
        Editor.Ano -> AnoLetivoBottomSheet(
            identificacao = form.identificacao,
            onAplicar = { form = form.copy(identificacao = it) },
            onDismiss = { editor = null },
        )
        is Editor.Periodo -> {
            val periodos = form.periodos
            PeriodoBottomSheet(
                numero = aberto.indice + 1,
                tipo = periodos.tipo,
                datas = periodos.datas[aberto.indice],
                anterior = periodos.visiveis.getOrNull(aberto.indice - 1),
                seguinte = periodos.visiveis.getOrNull(aberto.indice + 1),
                ano = form.identificacao.anoValor,
                onAplicar = { form = form.copy(periodos = form.periodos.comDatas(aberto.indice, it)) },
                onDismiss = { editor = null },
            )
        }
        Editor.Calculo -> EscolhaBottomSheet(
            titulo = "Cálculo da média",
            descricao = "Como as notas de cada período viram a média.",
            opcoes = TipoMedia.entries,
            selecionada = form.regras.tipoMedia,
            nome = { it.displayName },
            detalhe = { detalheDoCalculo(it) },
            onEscolher = { form = form.copy(regras = form.regras.copy(tipoMedia = it)) },
            onDismiss = { editor = null },
        )
        Editor.Arredondamento -> EscolhaBottomSheet(
            titulo = "Arredondamento",
            descricao = "Como a média de cada período é arredondada.",
            opcoes = TipoArredondamento.entries,
            selecionada = form.regras.arredondamento,
            nome = { nomeDoArredondamento(it) },
            detalhe = { detalheDoArredondamento(it) },
            exemplo = { exemploDoArredondamento(it) },
            onEscolher = { form = form.copy(regras = form.regras.copy(arredondamento = it)) },
            onDismiss = { editor = null },
        )
        Editor.Materias -> MateriasBottomSheet(
            linhas = materias,
            marcadas = form.materias,
            onMudar = { form = form.copy(materias = it) },
            onDismiss = { editor = null },
        )
        null -> Unit
    }

    if (confirmandoDescarte) {
        AlertDialog(
            onDismissRequest = { confirmandoDescarte = false },
            title = { Text("Descartar alterações?") },
            text = { Text("O que foi mudado aqui ainda não foi salvo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoDescarte = false
                    onVoltar()
                }) { Text("Descartar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoDescarte = false }) { Text("Continuar editando") }
            }
        )
    }
}

/** Barra com voltar e título em cima, e a de salvar embaixo; a mesma moldura enquanto carrega e depois. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MolduraAjustes(
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    barraDeBaixo: @Composable () -> Unit = {},
    conteudo: @Composable (PaddingValues) -> Unit,
) {
    // A barra de cima ganha o tom das superfícies quando a lista rola por baixo dela
    val rolagem = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier
            .imePadding() // com o teclado aberto, a barra de salvar sobe junto
            .nestedScroll(rolagem.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajustes do ano letivo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = rolagem,
            )
        },
        bottomBar = barraDeBaixo,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = conteudo,
    )
}

/**
 * A barra de baixo, que só aparece quando algo mudou: quantas alterações são, e Descartar e Salvar
 * ao alcance do polegar. Quando algo impede de gravar, ela diz o quê no lugar de "Ainda não salvas".
 */
@Composable
private fun BarraDeSalvar(
    alteracoes: Int,
    problema: String?, // o que impede de salvar; null quando dá
    salvando: Boolean,
    onDescartar: () -> Unit,
    onSalvar: () -> Unit,
) {
    AnimatedVisibility(
        visible = alteracoes > 0,
        enter = slideInVertically { altura -> altura } + fadeIn(),
        exit = slideOutVertically { altura -> altura } + fadeOut(),
    ) {
        // Descendo (descartou, ou tudo voltou ao que era), a barra fica com o último número em vez
        // de mostrar "0 alterações" no caminho
        var mostradas by remember { mutableIntStateOf(alteracoes) }
        LaunchedEffect(alteracoes) {
            if (alteracoes > 0) mostradas = alteracoes
        }

        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Espacos.l, end = Espacos.m, top = Espacos.m, bottom = Espacos.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            // Lido junto, e o leitor de tela avisa quando muda
                            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    ) {
                        Text(
                            text = if (mostradas == 1) "1 alteração" else "$mostradas alterações",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = problema ?: "Ainda não salvas",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (problema != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    TextButton(onClick = onDescartar, enabled = !salvando) { Text("Descartar") }
                    Spacer(Modifier.width(Espacos.xs))
                    Button(onClick = onSalvar, enabled = problema == null && !salvando) { Text("Salvar") }
                }
            }
        }
    }
}

// Seções ------------------------------------------------------------------------------------

/**
 * O ano e a série em destaque (tocar abre o sheet que edita os dois) e, embaixo, a linha do tempo
 * dos períodos.
 */
@Composable
private fun SecaoAnoLetivo(
    identificacao: FormularioAnoLetivo.Identificacao,
    periodos: FormularioAnoLetivo.Periodos,
    problemas: List<ProblemaDatas?>,
    alterada: Boolean,
    onEditar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        TituloDaSecao("Ano letivo", alterada = alterada)
        Cartao {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = "Editar o ano e a série", onClick = onEditar)
                    .padding(start = Espacos.l, end = Espacos.m, top = Espacos.m, bottom = Espacos.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = identificacao.ano.trim(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = identificacao.serie.trim().ifEmpty { "Sem série" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Parece um botão, mas quem recebe o toque é a linha inteira
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            LinhaDoTempoDoAno(
                periodos = periodos.visiveis,
                tipo = periodos.tipo,
                ano = identificacao.anoValor,
                problemas = problemas,
                modifier = Modifier.padding(start = Espacos.l, end = Espacos.l, bottom = Espacos.l),
            )
        }
    }
}

@Composable
private fun SecaoPeriodos(
    periodos: FormularioAnoLetivo.Periodos,
    ano: Int?, // o ano letivo: uma data só mostra o ano quando foge dele
    problemas: List<ProblemaDatas?>, // um por período visível
    bloqueio: Lancamentos?, // o que impede de tirar o último período; null quando ele pode sair
    alterada: Boolean,
    onMudar: (FormularioAnoLetivo.Periodos) -> Unit,
    onEditarPeriodo: (indice: Int) -> Unit,
    onAvisar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        TituloDaSecao("Períodos", alterada = alterada)
        // Período que entra ou sai: o card cresce ou encolhe suave
        Cartao(Modifier.animateContentSize()) {
            LinhaDoTipo(
                tipo = periodos.tipo,
                onEscolher = { onMudar(periodos.copy(tipo = it)) },
            )
            Divisoria()
            LinhaDaQuantidade(
                quantidade = periodos.quantidade,
                podeDiminuir = periodos.quantidade > 1,
                podeAumentar = periodos.quantidade < FormularioAnoLetivo.MAX_PERIODOS,
                bloqueado = bloqueio != null,
                onDiminuir = {
                    // Bloqueado, o − continua tocável e explica o porquê, no lugar de um aviso fixo
                    if (bloqueio != null) {
                        onAvisar(motivoDoBloqueio(periodos.quantidade, periodos.tipo, bloqueio))
                    } else {
                        onMudar(periodos.menosUm())
                    }
                },
                onAumentar = { onMudar(periodos.maisUm()) },
            )
            periodos.visiveis.forEachIndexed { i, datas ->
                Divisoria()
                LinhaQueAbre(
                    titulo = nomeDoPeriodo(i + 1, periodos.tipo),
                    resumo = resumoDasDatas(datas, ano),
                    aviso = problemas.getOrNull(i)?.mensagem,
                    rotuloDoClique = "Mudar as datas",
                    // Abre o calendário de intervalo: início e fim de uma vez
                    onClick = { onEditarPeriodo(i) },
                ) {
                    SeloDoPeriodo(numero = i + 1, tipo = periodos.tipo, erro = problemas.getOrNull(i) != null)
                }
            }
        }
    }
}

@Composable
private fun SecaoRegras(
    regras: FormularioAnoLetivo.Regras,
    alterada: Boolean,
    onMudar: (FormularioAnoLetivo.Regras) -> Unit,
    onEditarCalculo: () -> Unit,
    onEditarArredondamento: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        TituloDaSecao("Regras de média", alterada = alterada)
        // Os dois números que decidem as cores do boletim, lado a lado e da mesma altura
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Espacos.s),
        ) {
            BlocoNumerico(
                rotulo = "Média mínima",
                oQue = "a média mínima",
                valor = regras.mediaMinima,
                formatar = { formatarMedia(it) },
                dica = "Nota para passar",
                podeDiminuir = regras.mediaMinima > FAIXA_DA_MEDIA.start,
                podeAumentar = regras.mediaMinima < FAIXA_DA_MEDIA.endInclusive,
                onMudar = { sentido -> onMudar(regras.comMediaMinima(sentido)) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            BlocoNumerico(
                rotulo = "Frequência mínima",
                oQue = "a frequência mínima",
                valor = regras.frequenciaMinima,
                formatar = { "${formatarNumero(it)}%" },
                dica = faltasPermitidas(regras.frequenciaMinima),
                podeDiminuir = regras.frequenciaMinima > FAIXA_DA_FREQUENCIA.start,
                podeAumentar = regras.frequenciaMinima < FAIXA_DA_FREQUENCIA.endInclusive,
                onMudar = { sentido -> onMudar(regras.comFrequenciaMinima(sentido)) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Spacer(Modifier.height(Espacos.s))
        Cartao {
            LinhaQueAbre(
                titulo = "Cálculo da média",
                resumo = resumoDoCalculo(regras.tipoMedia),
                rotuloDoClique = "Escolher o cálculo da média",
                onClick = onEditarCalculo,
            ) {
                SeloDisciplina(
                    cor = MaterialTheme.colorScheme.primary,
                    icone = Icons.Outlined.Functions,
                    tamanho = TAMANHO_DO_SELO,
                )
            }
            Divisoria()
            LinhaQueAbre(
                titulo = "Arredondamento",
                resumo = resumoDoArredondamento(regras.arredondamento),
                rotuloDoClique = "Escolher o arredondamento",
                onClick = onEditarArredondamento,
            ) {
                SeloDisciplina(
                    cor = MaterialTheme.colorScheme.primary,
                    icone = Icons.Outlined.SwapVert,
                    tamanho = TAMANHO_DO_SELO,
                )
            }
        }
    }
}

/** Quantas matérias estão no ano, com os selos das primeiras; tocar abre o sheet para escolher. */
@Composable
private fun SecaoMaterias(
    linhas: List<LinhaMateria>,
    marcadas: Set<Long>, // as que entram no ano
    alterada: Boolean,
    onEditar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val noAno = remember(linhas, marcadas) { linhas.filter { it.materiaId in marcadas } }

    Column(modifier) {
        TituloDaSecao("Matérias do ano", alterada = alterada)
        Cartao {
            LinhaQueAbre(
                titulo = tituloDasMaterias(noAno.size, linhas.size),
                resumo = detalheDasMaterias(linhas.count { it.lancamentos != null }),
                rotuloDoClique = "Escolher as matérias",
                onClick = onEditar,
            ) {
                SelosEmpilhados(noAno)
            }
        }
    }
}

// Peças das seções --------------------------------------------------------------------------

/**
 * O nome da seção em cima do card, em maiúsculas e na cor primária, como os períodos no detalhe
 * da disciplina. Um ponto ao lado avisa que há mudança ainda não salva ali.
 */
@Composable
private fun TituloDaSecao(
    titulo: String,
    alterada: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Espacos.xs, end = Espacos.xs, top = Espacos.xs, bottom = Espacos.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = titulo.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        AnimatedVisibility(visible = alterada) {
            Box(
                Modifier
                    .padding(start = 6.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .semantics { contentDescription = "com alterações não salvas" }
            )
        }
    }
}

/** O card branco com borda que agrupa as linhas, o mesmo do detalhe da disciplina. */
@Composable
internal fun Cartao(
    modifier: Modifier = Modifier,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        content = conteudo,
    )
}

@Composable
internal fun Divisoria() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = Espacos.l),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** Uma linha de ajuste: o nome à esquerda e o controle à direita. */
@Composable
private fun LinhaDeAjuste(
    rotulo: String,
    modifier: Modifier = Modifier,
    controle: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = Espacos.l, vertical = Espacos.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        controle()
    }
}

/**
 * Uma linha que mostra um ajuste e abre o editor dele: o selo no [inicio], o nome, o [resumo] do
 * valor e a seta. Um [aviso] aparece embaixo, em vermelho.
 */
@Composable
internal fun LinhaQueAbre(
    titulo: String,
    rotuloDoClique: String, // o que o leitor de tela diz que o toque faz
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    resumo: String? = null,
    aviso: String? = null,
    inicio: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = rotuloDoClique, onClick = onClick)
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
            if (resumo != null) {
                Text(
                    text = resumo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (aviso != null) {
                Text(
                    text = aviso,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** O número do período num quadrado tingido, como o selo das disciplinas; vermelho se as datas têm problema. */
@Composable
private fun SeloDoPeriodo(
    numero: Int,
    tipo: TipoPeriodo,
    erro: Boolean,
) {
    val cor = if (erro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(TAMANHO_DO_SELO)
            .clip(RoundedCornerShape(TAMANHO_DO_SELO * 0.3f))
            .background(cor.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = ordinalDoPeriodo(numero, tipo),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = cor,
        )
    }
}

/**
 * Os selos das primeiras matérias, um sobre o outro, e "+N" quando há mais. Cada um tem um aro da
 * cor do card: ele separa os selos e é opaco, então o de baixo não aparece através do de cima.
 */
@Composable
private fun SelosEmpilhados(
    linhas: List<LinhaMateria>,
    modifier: Modifier = Modifier,
) {
    val aro = MaterialTheme.colorScheme.surfaceContainerLowest // a cor do card
    val forma = RoundedCornerShape(TAMANHO_DO_EMPILHADO * 0.3f)

    // Sem nenhuma no ano, um selo genérico segura o lugar
    if (linhas.isEmpty()) {
        SeloDisciplina(
            cor = MaterialTheme.colorScheme.primary,
            icone = Icons.Outlined.School,
            tamanho = TAMANHO_DO_SELO,
            modifier = modifier,
        )
        return
    }

    Row(modifier, horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
        linhas.take(SELOS_EMPILHADOS).forEach { linha ->
            Box(
                Modifier
                    .size(TAMANHO_DO_EMPILHADO)
                    .clip(forma)
                    .background(aro)
                    .padding(2.dp)
            ) {
                SeloDisciplina(
                    cor = CoresDisciplina.de(linha.cor),
                    icone = IconesDisciplina.de(linha.icone),
                    tamanho = TAMANHO_DO_EMPILHADO - 4.dp,
                )
            }
        }
        val resto = linhas.size - SELOS_EMPILHADOS
        if (resto > 0) {
            Box(
                modifier = Modifier
                    .size(TAMANHO_DO_EMPILHADO)
                    .clip(forma)
                    .background(aro)
                    .padding(2.dp)
                    .clip(forma)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$resto",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** O tipo de período, num menu. Trocar renomeia todos: a 1ª Unidade vira o 1º Bimestre. */
@Composable
private fun LinhaDoTipo(
    tipo: TipoPeriodo,
    onEscolher: (TipoPeriodo) -> Unit,
) {
    var aberto by remember { mutableStateOf(false) }

    LinhaDeAjuste(rotulo = "Tipo") {
        Box {
            Surface(
                onClick = { aberto = true },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.semantics { contentDescription = "Tipo de período: ${tipo.displayName}" },
            ) {
                Row(
                    modifier = Modifier.padding(start = Espacos.m, end = Espacos.s, top = Espacos.s, bottom = Espacos.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(tipo.displayName, style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            }
            DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
                TipoPeriodo.entries.forEach { opcao ->
                    DropdownMenuItem(
                        text = { Text(opcao.displayName) },
                        onClick = {
                            aberto = false
                            onEscolher(opcao)
                        },
                        trailingIcon = if (opcao == tipo) {
                            { Icon(Icons.Filled.Check, contentDescription = "Escolhido") }
                        } else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaDaQuantidade(
    quantidade: Int,
    podeDiminuir: Boolean,
    podeAumentar: Boolean,
    bloqueado: Boolean, // o último tem lançamentos: o − fica apagado, mas explica ao ser tocado
    onDiminuir: () -> Unit,
    onAumentar: () -> Unit,
) {
    LinhaDeAjuste(rotulo = "Quantidade") {
        BotaoDoContador(
            icone = Icons.Filled.Remove,
            descricao = "Tirar o último período",
            onClick = onDiminuir,
            enabled = podeDiminuir,
            apagado = bloqueado,
        )
        NumeroRolando(
            valor = quantidade.toDouble(),
            formatar = { it.roundToInt().toString() },
            estilo = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .widthIn(min = 40.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        BotaoDoContador(
            icone = Icons.Filled.Add,
            descricao = "Adicionar um período",
            onClick = onAumentar,
            enabled = podeAumentar,
        )
    }
}

/**
 * Um número que se ajusta no lugar: o nome, o valor grande, o que ele quer dizer e, embaixo, o −
 * e o +. Sem teclado, o valor nunca fica inválido.
 */
@Composable
private fun BlocoNumerico(
    rotulo: String,
    oQue: String, // para o leitor de tela: "Diminuir a média mínima"
    valor: Double,
    formatar: (Double) -> String,
    dica: String,
    podeDiminuir: Boolean,
    podeAumentar: Boolean,
    onMudar: (sentido: Int) -> Unit, // -1 diminui, 1 aumenta
    modifier: Modifier = Modifier,
) {
    Cartao(modifier) {
        Column(Modifier.fillMaxHeight().padding(Espacos.m)) {
            // Lidos juntos, e o leitor de tela anuncia o valor novo a cada toque
            Column(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                Text(
                    text = rotulo,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NumeroRolando(
                    valor = valor,
                    formatar = formatar,
                    estilo = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = dica,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Empurra os botões para o pé do card, alinhados com os do vizinho
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(Espacos.s))
            Row(horizontalArrangement = Arrangement.spacedBy(Espacos.s)) {
                BotaoDoContador(
                    icone = Icons.Filled.Remove,
                    descricao = "Diminuir $oQue",
                    onClick = { onMudar(-1) },
                    enabled = podeDiminuir,
                    forma = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f),
                )
                BotaoDoContador(
                    icone = Icons.Filled.Add,
                    descricao = "Aumentar $oQue",
                    onClick = { onMudar(1) },
                    enabled = podeAumentar,
                    forma = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * O − ou o + dos contadores, tonal como no lançamento de faltas, com um tique a cada toque.
 * [apagado] parece desligado mas responde ao toque, para explicar por que não dá.
 */
@Composable
private fun BotaoDoContador(
    icone: ImageVector,
    descricao: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    apagado: Boolean = false,
    forma: Shape = IconButtonDefaults.filledShape,
) {
    val haptico = LocalHapticFeedback.current
    val cores = if (apagado) {
        // As mesmas cores do botão desligado do Material
        IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    } else {
        IconButtonDefaults.filledTonalIconButtonColors()
    }

    FilledTonalIconButton(
        onClick = {
            haptico.performHapticFeedback(
                if (apagado) HapticFeedbackType.Reject else HapticFeedbackType.SegmentTick
            )
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        shape = forma,
        colors = cores,
    ) {
        Icon(icone, contentDescription = descricao)
    }
}

/**
 * O número que rola para cima quando sobe e para baixo quando desce, como no contador de faltas.
 * A caixa corta o que passa das bordas, então o velho e o novo nunca aparecem sobrepostos.
 */
@Composable
private fun NumeroRolando(
    valor: Double,
    formatar: (Double) -> String,
    estilo: TextStyle,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = valor,
        modifier = modifier,
        transitionSpec = {
            val sentido = if (targetState > initialState) 1 else -1
            (slideInVertically { altura -> sentido * altura } + fadeIn()) togetherWith
                (slideOutVertically { altura -> -sentido * altura } + fadeOut()) using
                SizeTransform(clip = true)
        },
        contentAlignment = Alignment.Center,
        label = "numero",
    ) { atual ->
        Text(
            text = formatar(atual),
            style = estilo,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

private val TAMANHO_DO_SELO = 36.dp
private val TAMANHO_DO_EMPILHADO = 34.dp
private const val SELOS_EMPILHADOS = 4

// Textos ------------------------------------------------------------------------------------

// Unidade é palavra feminina (1ª Unidade); bimestre, trimestre e semestre, masculinas (1º Bimestre)
internal val TipoPeriodo.feminino: Boolean
    get() = this == TipoPeriodo.UNIDADE

/** "1ª" ou "1º", conforme o tipo. */
internal fun ordinalDoPeriodo(numero: Int, tipo: TipoPeriodo): String =
    "$numero${if (tipo.feminino) "ª" else "º"}"

internal fun nomeDoPeriodo(numero: Int, tipo: TipoPeriodo): String =
    "${ordinalDoPeriodo(numero, tipo)} ${tipo.displayName}"

internal val MESES = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

/** "1 fev"; o ano só aparece quando não é o [ano] letivo: "31 jan 2025". */
internal fun dataCurta(data: LocalDate, ano: Int?): String =
    "${data.dayOfMonth} ${MESES[data.monthValue - 1]}" + if (data.year != ano) " ${data.year}" else ""

/** "1 fev – 30 abr · 13 semanas". Uma data só mostra o ano quando ele não é o [ano] letivo. */
internal fun resumoDasDatas(datas: DatasPeriodo, ano: Int?): String {
    val intervalo = "${dataCurta(datas.inicio, ano)} – ${dataCurta(datas.fim, ano)}"
    return duracao(datas)?.let { "$intervalo · $it" } ?: intervalo
}

/** "13 semanas"; abaixo de duas semanas, em dias. Null quando o fim vem antes do início. */
internal fun duracao(datas: DatasPeriodo): String? {
    val dias = ChronoUnit.DAYS.between(datas.inicio, datas.fim) + 1 // o primeiro e o último contam
    return when {
        dias < 1 -> null
        dias == 1L -> "1 dia"
        dias < 14 -> "$dias dias"
        else -> "${(dias / 7.0).roundToInt()} semanas"
    }
}

/** "Não dá para tirar a 4ª Unidade: ela tem 11 avaliações e 3 faltas." */
internal fun motivoDoBloqueio(numero: Int, tipo: TipoPeriodo, lancamentos: Lancamentos): String {
    val (artigo, pronome) = if (tipo.feminino) "a" to "ela" else "o" to "ele"
    return "Não dá para tirar $artigo ${nomeDoPeriodo(numero, tipo)}: $pronome tem ${descrever(lancamentos)}."
}

/** O que a frequência mínima deixa faltar: "Até 25% de faltas". */
internal fun faltasPermitidas(frequenciaMinima: Double): String = when {
    frequenciaMinima <= 0.0 -> "Sem limite de faltas"
    frequenciaMinima >= 100.0 -> "Nenhuma falta permitida"
    else -> "Até ${formatarNumero(100 - frequenciaMinima)}% de faltas"
}

/** "3 avaliações e 2 faltas", "1 avaliação", "1 falta". */
internal fun descrever(lancamentos: Lancamentos): String = listOfNotNull(
    lancamentos.avaliacoes.takeIf { it > 0 }?.let { if (it == 1) "1 avaliação" else "$it avaliações" },
    lancamentos.faltas.takeIf { it > 0 }?.let { if (it == 1) "1 falta" else "$it faltas" },
).joinToString(" e ")

/** "Ponderada · pelos pesos": o cálculo e o jeito dele, curto, para a linha do painel. */
internal fun resumoDoCalculo(tipo: TipoMedia): String = "${tipo.displayName} · " + when (tipo) {
    TipoMedia.PONDERADA -> "pelos pesos"
    TipoMedia.SIMPLES -> "todas valem igual"
    TipoMedia.SOMA -> "sem dividir"
}

// O que cada cálculo faz, inteiro, para o sheet. Segue o CalcularMediaDisciplina.
private fun detalheDoCalculo(tipo: TipoMedia): String = when (tipo) {
    TipoMedia.PONDERADA -> "Cada avaliação conta pelo seu peso."
    TipoMedia.SIMPLES -> "Todas as avaliações contam igual; o peso fica de fora."
    TipoMedia.SOMA -> "Soma as notas, sem dividir: para quando os pontos do período são " +
        "distribuídos entre as avaliações."
}

/** O nome na tela. O displayName do domínio leva o "(1 casa)", que aqui vai no detalhe. */
internal fun nomeDoArredondamento(tipo: TipoArredondamento): String = when (tipo) {
    TipoArredondamento.NENHUM -> "Nenhum"
    TipoArredondamento.MEIO_PONTO -> "Meio ponto"
    TipoArredondamento.INTEIRO -> "Inteiro"
    TipoArredondamento.CIMA -> "Para cima"
    TipoArredondamento.BAIXO -> "Para baixo"
}

private fun detalheDoArredondamento(tipo: TipoArredondamento): String = when (tipo) {
    TipoArredondamento.NENHUM -> "Fica com até 2 casas"
    TipoArredondamento.MEIO_PONTO -> "Vai para o 0,5 mais próximo"
    TipoArredondamento.INTEIRO -> "Vai para o inteiro mais próximo"
    TipoArredondamento.CIMA -> "Sobe sempre, na 1ª casa"
    TipoArredondamento.BAIXO -> "Desce sempre, na 1ª casa"
}

/**
 * "7,3 → 7,5": uma média de exemplo e como ela fica. A conta é a do próprio arredondarMedia, então
 * o exemplo nunca contradiz o boletim.
 */
internal fun exemploDoArredondamento(tipo: TipoArredondamento): String {
    val media = when (tipo) {
        TipoArredondamento.NENHUM -> 6.96
        TipoArredondamento.MEIO_PONTO -> 7.3
        TipoArredondamento.INTEIRO -> 7.5
        TipoArredondamento.CIMA -> 6.62
        TipoArredondamento.BAIXO -> 6.68
    }
    return "${formatarMedia(media)} → ${formatarMedia(arredondarMedia(media, tipo))}"
}

/** "Para cima · 6,62 → 6,7": o arredondamento e o exemplo dele, para a linha do painel. */
internal fun resumoDoArredondamento(tipo: TipoArredondamento): String =
    "${nomeDoArredondamento(tipo)} · ${exemploDoArredondamento(tipo)}"

/** "10 de 13 no ano"; sem nenhuma cadastrada, diz isso. */
internal fun tituloDasMaterias(noAno: Int, total: Int): String =
    if (total == 0) "Nenhuma matéria cadastrada" else "$noAno de $total no ano"

/** Quantas matérias não podem sair do ano; null quando nenhuma tem lançamentos. */
internal fun detalheDasMaterias(bloqueadas: Int): String? = when (bloqueadas) {
    0 -> null
    else -> "$bloqueadas com avaliações ou faltas"
}

// Matérias ----------------------------------------------------------------------------------

/** Uma matéria na lista do ano: como ela aparece e o que já foi lançado nela. */
internal data class LinhaMateria(
    val materiaId: Long,
    val nome: String,
    val cor: Int,
    val icone: IconeMateria,
    val lancamentos: Lancamentos?, // null quando ela pode sair do ano
)

/**
 * As matérias na ordem do cadastro. Uma que já está no ano aparece como a disciplina dela, como nas
 * abas (o nome, a cor ou o ícone podem ter mudado), e com o que já foi lançado nela, se houver.
 * Feito uma vez por leitura do banco, e não a cada recomposição.
 */
internal fun linhasDasMaterias(ajustes: AjustesAnoLetivo): List<LinhaMateria> {
    val disciplinaPorMateria = ajustes.disciplinas.associateBy { it.materiaId }
    return ajustes.materias.map { materia ->
        val disciplina = disciplinaPorMateria[materia.id]
        LinhaMateria(
            materiaId = materia.id,
            nome = disciplina?.nome ?: materia.nome,
            cor = disciplina?.cor ?: materia.cor,
            icone = disciplina?.icone ?: materia.icone,
            // Com algo lançado, a matéria fica no ano: tirá-la apagaria tudo junto
            lancamentos = disciplina?.let { ajustes.lancamentosPorDisciplina[it.id] }?.takeIf { !it.vazio },
        )
    }
}
