package app.jammes.boletim.presentation.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.BuildConfig
import app.jammes.boletim.R
import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.domain.usecase.materiasDoPrimeiroAno
import app.jammes.boletim.presentation.ui.boletim.formatarMedia
import app.jammes.boletim.presentation.ui.components.AnelDaMedia
import app.jammes.boletim.presentation.ui.components.Avatar
import app.jammes.boletim.presentation.ui.components.BarraDaMedia
import app.jammes.boletim.presentation.ui.components.DURACAO_SAIDA
import app.jammes.boletim.presentation.ui.components.DURACAO_TRANSICAO
import app.jammes.boletim.presentation.ui.components.Pilula
import app.jammes.boletim.presentation.ui.components.SeloDisciplina
import app.jammes.boletim.presentation.ui.components.descricaoDoAno
import app.jammes.boletim.presentation.ui.theme.BoletimTheme
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import app.jammes.boletim.presentation.ui.theme.Espacos
import app.jammes.boletim.presentation.ui.theme.IconesDisciplina
import java.time.LocalDate

// Na ordem em que aparecem: do aluno às matérias são os 4 passos numerados
private enum class Passo { BOAS_VINDAS, ALUNO, DIVISAO, REGRAS, MATERIAS, PRONTO }

// O azul do ícone do app; no escuro, mais fundo, para não ofuscar
private val GradienteClaro = listOf(Color(0xFF2E7BFF), Color(0xFF0049D9))
private val GradienteEscuro = listOf(Color(0xFF1F62E8), Color(0xFF0A2F92))

/**
 * O cadastro de um aluno: quatro passos que já vêm preenchidos (aluno, divisão do ano, regras e
 * matérias) e o resumo, onde o aluno e o primeiro ano letivo são gravados de uma vez e abertos.
 *
 * Tem duas entradas. No primeiro uso começa nas boas-vindas, e quem tira esta tela é o AppRoot,
 * assim que o Contexto é salvo. Pelo perfil ([novoAluno]) começa no aluno: voltar dali desiste
 * ([onFechar]) e, depois de gravar, [onConcluido] leva ao boletim do aluno novo.
 */
@Composable
fun OnboardingScreen(
    novoAluno: Boolean = false,
    onFechar: () -> Unit = {},
    onConcluido: () -> Unit = {},
    vm: OnboardingViewModel = hiltViewModel(),
) {
    val salvando by vm.salvando.collectAsStateWithLifecycle()
    val materias by vm.materias.collectAsStateWithLifecycle()
    // As boas-vindas são só do primeiro uso
    val primeiro = if (novoAluno) Passo.ALUNO else Passo.BOAS_VINDAS
    var passo by rememberSaveable { mutableStateOf(primeiro) }
    var formulario by rememberSaveable { mutableStateOf(FormularioOnboarding(ano = LocalDate.now().year)) }
    val sugeridas = remember(materias, formulario.serie) {
        materiasDoPrimeiroAno(formulario.serie.etapa, materias).map { it.id }.toSet()
    }
    val snackbar = remember { SnackbarHostState() }
    val avancar = { passo = Passo.entries[passo.ordinal + 1] }
    val anterior = { passo = Passo.entries[passo.ordinal - 1] }
    // No primeiro passo, voltar é desistir do cadastro
    val voltar = { if (passo == primeiro) onFechar() else anterior() }
    val concluido by rememberUpdatedState(onConcluido)
    // O Pular fica com as sugestões dos passos que faltam e vai direto ao resumo
    val pular = { passo = Passo.PRONTO }

    LaunchedEffect(vm) { vm.erros.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(vm) { vm.concluido.collect { concluido() } }
    // No primeiro passo, o voltar do sistema fica com quem abriu a tela: sai do app ou do cadastro
    BackHandler(enabled = passo != primeiro, onBack = anterior)

    Surface(Modifier.fillMaxSize()) {
        Box {
            AnimatedContent(
                targetState = passo,
                transitionSpec = {
                    // O fade-through das telas do app, com um deslize curto para o lado em que se anda
                    val lado = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(DURACAO_TRANSICAO, delayMillis = DURACAO_SAIDA)) +
                        slideInHorizontally(tween(DURACAO_TRANSICAO)) { largura -> lado * largura / 10 })
                        .togetherWith(fadeOut(tween(DURACAO_SAIDA)))
                },
                label = "passoDoOnboarding",
            ) { atual ->
                when (atual) {
                    Passo.BOAS_VINDAS -> BoasVindas(
                        onComecar = { passo = Passo.ALUNO },
                        onCarregarDadosDeTeste = if (BuildConfig.DEBUG) vm::carregarDadosDeTeste else null,
                    )
                    Passo.ALUNO -> PassoAluno(formulario, { formulario = it }, onVoltar = voltar, onContinuar = avancar)
                    Passo.DIVISAO -> PassoDivisao(formulario, { formulario = it }, voltar, onPular = pular, onContinuar = avancar)
                    Passo.REGRAS -> PassoRegras(formulario, { formulario = it }, voltar, onPular = pular, onContinuar = avancar)
                    Passo.MATERIAS -> PassoMaterias(
                        formulario, materias, sugeridas, { formulario = it }, onVoltar = voltar, onContinuar = avancar,
                    )
                    Passo.PRONTO -> PassoPronto(
                        formulario = formulario,
                        materias = materias,
                        sugeridas = sugeridas,
                        salvando = salvando,
                        onVoltar = voltar,
                        onMudar = { numero -> passo = Passo.entries[numero] }, // o passo 1 é o ALUNO
                        // A dica do boletim é para quem chega agora; quem cadastra outro aluno já conhece
                        onConcluir = { vm.concluir(formulario, sugeridas, pedirDica = !novoAluno) },
                    )
                }
            }
            SnackbarHost(
                hostState = snackbar,
                // Acima do botão do rodapé
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 88.dp),
            )
        }
    }
}

// Boas-vindas ---------------------------------------------------------------------------------

@Composable
private fun BoasVindas(onComecar: () -> Unit, onCarregarDadosDeTeste: (() -> Unit)?) {
    IconesClarosNaBarraDeStatus()
    Column(Modifier.fillMaxSize()) {
        Destaque(Modifier.weight(1f).fillMaxWidth())
        Column(Modifier.padding(start = Espacos.xl, end = Espacos.xl, top = Espacos.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconeDoApp(tamanho = 34.dp)
                Spacer(Modifier.width(Espacos.m))
                Text("Boletim", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(Espacos.l))
            Text(
                text = "Suas notas e faltas, sempre em dia",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(Espacos.s))
            Text(
                text = "Acompanhe a média de cada matéria, veja quanto falta para passar e não perca a conta das faltas.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Espacos.xl, vertical = Espacos.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BotaoPrincipal(texto = "Começar", onClick = onComecar)
            Spacer(Modifier.height(Espacos.m))
            Text(
                text = "Menos de 1 minuto · seus dados ficam só no aparelho",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            // Temporário, só em debug: entra direto com os dados de teste, sem passar pelo onboarding
            if (onCarregarDadosDeTeste != null) {
                TextButton(onClick = onCarregarDadosDeTeste) {
                    Icon(Icons.Outlined.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Espacos.s))
                    Text("Carregar dados de teste")
                }
            }
        }
    }
}

/** O topo das boas-vindas: pedaços do próprio boletim sobre o azul do ícone. */
@Composable
private fun Destaque(modifier: Modifier = Modifier) {
    val gradiente = if (BoletimTheme.escuro) GradienteEscuro else GradienteClaro
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.linearGradient(gradiente)),
        contentAlignment = Alignment.Center,
    ) {
        // Dois círculos claros ao fundo, como luz batendo no azul
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 96.dp, y = (-84).dp).size(300.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.07f))
        )
        Box(
            Modifier.align(Alignment.BottomStart).offset(x = (-90).dp, y = 70.dp).size(230.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.07f))
        )

        // Em telas baixas ou estreitas os cartões encolhem juntos, em vez de passar da borda
        val alturaLivre = maxHeight - WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val escala = minOf(1f, (alturaLivre - Espacos.xl) / ALTURA_DOS_CARTOES, (maxWidth - Espacos.xl) / LARGURA_DOS_CARTOES)
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .requiredSize(LARGURA_DOS_CARTOES, ALTURA_DOS_CARTOES)
                .graphicsLayer {
                    scaleX = escala
                    scaleY = escala
                }
                .clearAndSetSemantics {}, // é ilustração: os números são de mentira
        ) {
            CartaoDaMedia(Modifier.offset(x = 8.dp).rotate(-5f))
            CartaoDaMateria(
                nome = "Matemática", cor = 1, icone = IconeMateria.CALCULADORA, media = 7.6,
                corDaMedia = BoletimTheme.coresExtras.atencao, faltas = "1 falta",
                modifier = Modifier.offset(x = 16.dp, y = 216.dp).width(160.dp).rotate(-4f),
            )
            CartaoDaMateria(
                nome = "Português", cor = 0, icone = IconeMateria.LIVRO, media = 8.9,
                corDaMedia = BoletimTheme.coresExtras.sucesso, faltas = "2 faltas",
                modifier = Modifier.offset(x = 182.dp, y = 182.dp).width(172.dp).rotate(6f),
            )
        }
    }
}

private val LARGURA_DOS_CARTOES = 356.dp
private val ALTURA_DOS_CARTOES = 350.dp

// Brancos no claro; no escuro, um degrau acima do fundo, como os cards do app
@Composable
private fun corDosCartoes(): Color =
    if (BoletimTheme.escuro) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLowest

@Composable
private fun CartaoDaMedia(modifier: Modifier = Modifier) {
    val sucesso = BoletimTheme.coresExtras.sucesso
    Surface(modifier = modifier.width(300.dp), shape = RoundedCornerShape(22.dp), color = corDosCartoes(), shadowElevation = 12.dp) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Média do ano letivo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(formatarMedia(8.42), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Mínima 7,0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AnelDaMedia(media = 8.42, mediaMinima = 7.0, cor = sucesso, modifier = Modifier.size(76.dp)) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = sucesso, modifier = Modifier.size(26.dp))
                }
            }
            HorizontalDivider(Modifier.padding(vertical = Espacos.m), color = MaterialTheme.colorScheme.outlineVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(Espacos.s)) {
                Pilula("6 com folga", cor = sucesso)
                Pilula("2 no limite", cor = BoletimTheme.coresExtras.atencao)
            }
        }
    }
}

@Composable
private fun CartaoDaMateria(
    nome: String,
    cor: Int,
    icone: IconeMateria,
    media: Double,
    corDaMedia: Color,
    faltas: String,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = corDosCartoes(), shadowElevation = 12.dp) {
        Column(Modifier.padding(14.dp)) {
            Row {
                Text(
                    text = nome,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                SeloDisciplina(cor = CoresDisciplina.de(cor), icone = IconesDisciplina.de(icone), tamanho = 28.dp)
            }
            Text(
                text = formatarMedia(media),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = corDaMedia,
                modifier = Modifier.padding(top = Espacos.s, bottom = Espacos.xs),
            )
            BarraDaMedia(media = media, mediaMinima = 7.0, cor = corDaMedia)
            Text(
                text = faltas,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Espacos.s),
            )
        }
    }
}

/** O ícone do app: a arte do launcher sobre o mesmo gradiente, no recorte arredondado. */
@Composable
private fun IconeDoApp(tamanho: Dp) {
    Box(
        modifier = Modifier
            .size(tamanho)
            .clip(RoundedCornerShape(28))
            .background(Brush.linearGradient(GradienteClaro)),
        contentAlignment = Alignment.Center,
    ) {
        // O desenho tem 108dp e a arte fica nos 72 do meio, como em todo ícone adaptativo
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(tamanho * 1.5f),
        )
    }
}

/** Ícones brancos na barra de status enquanto o azul está atrás dela; ao sair, voltam como estavam. */
@Composable
private fun IconesClarosNaBarraDeStatus() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity) {
        val controle = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        val antes = controle.isAppearanceLightStatusBars
        controle.isAppearanceLightStatusBars = false
        onDispose { controle.isAppearanceLightStatusBars = antes }
    }
}

// Aluno ---------------------------------------------------------------------------------------

@Composable
private fun PassoAluno(
    formulario: FormularioOnboarding,
    onMudar: (FormularioOnboarding) -> Unit,
    onVoltar: () -> Unit,
    onContinuar: () -> Unit,
) {
    val foco = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // O teclado já abre no nome, o único campo que precisa ser preenchido (e só na primeira vez)
    LaunchedEffect(Unit) { if (formulario.nome.isBlank()) foco.requestFocus() }

    EstruturaDoPasso(
        numero = 1,
        titulo = "Para quem é o boletim?",
        subtitulo = "Pode ser o seu ou o de um filho. No topo do app, ele vai aparecer assim:",
        onVoltar = onVoltar,
        botaoHabilitado = formulario.podeConcluir,
        onBotao = onContinuar,
    ) {
        Previa(nome = formulario.nome, descricao = descricaoDoAno(formulario.serie.texto(), formulario.ano))
        Spacer(Modifier.height(Espacos.xl))
        OutlinedTextField(
            value = formulario.nome,
            onValueChange = { onMudar(formulario.copy(nome = it)) },
            label = { Text("Nome do aluno") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            trailingIcon = if (formulario.nome.isNotEmpty()) {
                {
                    IconButton(onClick = { onMudar(formulario.copy(nome = "")) }) {
                        Icon(Icons.Filled.Cancel, contentDescription = "Apagar o nome")
                    }
                }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth().focusRequester(foco),
        )
        EscolhaDaSerie(serie = formulario.serie, onEscolher = { onMudar(formulario.copy(serie = it)) })
        TituloDaSecao("Ano letivo")
        EscolhaDoAno(ano = formulario.ano, onEscolher = { onMudar(formulario.comAno(it)) })
    }
}

/** Como o aluno vai aparecer no topo do app, já enquanto se digita. */
@Composable
private fun Previa(nome: String, descricao: String) {
    val cores = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(cores.surfaceContainerLow)
            .border(1.dp, cores.outlineVariant, MaterialTheme.shapes.large)
            .padding(horizontal = Espacos.l, vertical = Espacos.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(nome = nome.ifBlank { null })
        Column(Modifier.weight(1f).padding(start = Espacos.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = nome.ifBlank { "Nome do aluno" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (nome.isBlank()) cores.onSurfaceVariant else cores.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = cores.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Text(descricao, style = MaterialTheme.typography.bodyMedium, color = cores.onSurfaceVariant)
        }
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = cores.onSurfaceVariant)
    }
}

@Composable
private fun EscolhaDaSerie(serie: Serie?, onEscolher: (Serie?) -> Unit) {
    val haptico = LocalHapticFeedback.current
    // Tocar de novo na escolhida tira a série: ela é opcional
    val escolher = { opcao: Serie ->
        haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
        onEscolher(opcao.takeUnless { it == serie })
    }

    TituloDaSecao("Série", detalhe = "Opcional")
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Os 9 anos do Fundamental cabem numa linha só; o Médio usa o mesmo tamanho de círculo
        val tamanho = (maxWidth - ESPACO_ENTRE_SERIES * 8) / 9
        Column {
            Text("Ensino Fundamental", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Espacos.s))
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(ESPACO_ENTRE_SERIES)) {
                (1..9).forEach { n ->
                    val opcao = Serie.Fundamental(n)
                    CirculoDaSerie("${n}º", selecionado = serie == opcao, tamanho = tamanho, onClick = { escolher(opcao) })
                }
            }
            Spacer(Modifier.height(Espacos.m))
            Text("Ensino Médio", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(ESPACO_ENTRE_SERIES),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                (1..3).forEach { n ->
                    val opcao = Serie.Medio(n)
                    CirculoDaSerie("${n}ª", selecionado = serie == opcao, tamanho = tamanho, onClick = { escolher(opcao) })
                }
                Spacer(Modifier.width(Espacos.s))
                FilterChip(
                    selected = serie is Serie.Outra,
                    onClick = {
                        haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onEscolher(if (serie is Serie.Outra) null else Serie.Outra())
                    },
                    label = { Text("Outra") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                    },
                )
            }
        }
    }
    AnimatedVisibility(visible = serie is Serie.Outra) {
        OutlinedTextField(
            value = (serie as? Serie.Outra)?.texto.orEmpty(),
            onValueChange = { onEscolher(Serie.Outra(it)) },
            label = { Text("Qual série?") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().padding(top = Espacos.s),
        )
    }
}

private val ESPACO_ENTRE_SERIES = 4.dp

@Composable
private fun CirculoDaSerie(texto: String, selecionado: Boolean, tamanho: Dp, onClick: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(tamanho)
            .clip(CircleShape)
            .background(if (selecionado) cores.primary else Color.Transparent)
            .border(1.dp, if (selecionado) cores.primary else cores.outlineVariant, CircleShape)
            .selectable(selected = selecionado, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selecionado) cores.onPrimary else cores.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EscolhaDoAno(ano: Int, onEscolher: (Int) -> Unit) {
    // O passado, o atual e o próximo: cobre quem cadastra atrasado e quem já prepara o seguinte
    val atual = remember { LocalDate.now().year }
    val anos = listOf(atual - 1, atual, atual + 1)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        anos.forEachIndexed { i, opcao ->
            SegmentedButton(
                selected = ano == opcao,
                onClick = { onEscolher(opcao) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = anos.size),
            ) {
                Text(opcao.toString())
            }
        }
    }
}

// Peças comuns --------------------------------------------------------------------------------

/** Título de seção como nos ajustes: em maiúsculas, na cor primária. */
@Composable
internal fun TituloDaSecao(titulo: String, detalhe: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Espacos.xl, bottom = Espacos.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = titulo.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (detalhe != null) {
            Text(detalhe, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun BotaoPrincipal(
    texto: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    carregando: Boolean = false,
    icone: ImageVector? = null,
) {
    Button(onClick = onClick, enabled = enabled && !carregando, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        if (carregando) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        } else {
            Text(texto, style = MaterialTheme.typography.titleMedium)
            if (icone != null) {
                Spacer(Modifier.width(Espacos.s))
                Icon(icone, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}
