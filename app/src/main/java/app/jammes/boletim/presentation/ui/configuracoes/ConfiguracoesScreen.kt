package app.jammes.boletim.presentation.ui.configuracoes

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.outlined.Abc
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Interests
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.Configuracoes
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.EstiloDasAbas
import app.jammes.boletim.domain.model.Tema
import app.jammes.boletim.presentation.contexto.ContextoViewModel
import app.jammes.boletim.presentation.ui.aluno.LinhaDeEscolha
import app.jammes.boletim.presentation.ui.aluno.Selo
import app.jammes.boletim.presentation.ui.anoletivo.Cartao
import app.jammes.boletim.presentation.ui.anoletivo.Divisoria
import app.jammes.boletim.presentation.ui.anoletivo.MolduraAjustes
import app.jammes.boletim.presentation.ui.anoletivo.TituloDaSecao
import app.jammes.boletim.presentation.ui.components.AbaDisciplina
import app.jammes.boletim.presentation.ui.components.BORDA
import app.jammes.boletim.presentation.ui.components.LARGURA_ABERTA
import app.jammes.boletim.presentation.ui.components.siglaDoNome
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import app.jammes.boletim.presentation.ui.theme.Espacos
import app.jammes.boletim.presentation.ui.theme.IconesDisciplina

/**
 * As configurações do app, abertas pelo perfil: o tema e o estilo das abas laterais. No mesmo
 * painel dos ajustes do ano, mas sem Salvar: cada escolha vale na hora, e o tema novo já pinta a
 * própria tela. As abas não aparecem aqui, então o estilo delas vem com uma prévia ao lado.
 *
 * Usa os ViewModels presos à Activity: o das configurações já está carregado (foi dele que o tema
 * saiu), e o do contexto tem as disciplinas do ano aberto, para a prévia.
 */
@Composable
fun ConfiguracoesScreen(
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConfiguracoesViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
    contexto: ContextoViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    val configuracoes by viewModel.configuracoes.collectAsStateWithLifecycle()
    val disciplinas by contexto.disciplinas.collectAsStateWithLifecycle()
    val siglas by contexto.siglas.collectAsStateWithLifecycle()
    val haptico = LocalHapticFeedback.current
    // Antes de carregar não há o que marcar: a tela abre com o padrão, que é o que vale até lá
    val atuais = configuracoes ?: Configuracoes()

    MolduraAjustes(onVoltar = onVoltar, modifier = modifier, titulo = "Configurações") { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = Espacos.l, end = Espacos.l, top = Espacos.xs, bottom = Espacos.xl),
            verticalArrangement = Arrangement.spacedBy(Espacos.l),
        ) {
            SecaoTema(
                tema = atuais.tema,
                onEscolher = { tema ->
                    if (tema != atuais.tema) haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    viewModel.escolherTema(tema)
                },
            )
            SecaoAbas(
                estilo = atuais.estiloDasAbas,
                disciplinas = disciplinas,
                siglas = siglas,
                onEscolher = { estilo ->
                    if (estilo != atuais.estiloDasAbas) haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    viewModel.escolherEstiloDasAbas(estilo)
                },
            )
        }
    }
}

// Seções ------------------------------------------------------------------------------------

@Composable
private fun SecaoTema(
    tema: Tema,
    onEscolher: (Tema) -> Unit,
) {
    Column {
        TituloDaSecao("Tema", alterada = false)
        Cartao(Modifier.selectableGroup()) {
            Tema.entries.forEachIndexed { i, opcao ->
                if (i > 0) Divisoria()
                LinhaDeEscolha(
                    titulo = nomeDoTema(opcao),
                    resumo = resumoDoTema(opcao),
                    escolhida = opcao == tema,
                    onClick = { onEscolher(opcao) },
                ) { Selo(iconeDoTema(opcao)) }
            }
        }
    }
}

/**
 * As três opções à esquerda e, encostada na borda direita do card, a prévia das abas no estilo
 * escolhido. A altura é a das opções: o que não cabe na prévia fica cortado, como na tela de verdade.
 */
@Composable
private fun SecaoAbas(
    estilo: EstiloDasAbas,
    disciplinas: List<DisciplinaDomain>,
    siglas: Map<Long, String>,
    onEscolher: (EstiloDasAbas) -> Unit,
) {
    Column {
        TituloDaSecao("Abas laterais", alterada = false)
        Cartao {
            Box {
                Column(Modifier.padding(end = LARGURA_DA_PREVIA).selectableGroup()) {
                    EstiloDasAbas.entries.forEachIndexed { i, opcao ->
                        if (i > 0) Divisoria()
                        LinhaDeEscolha(
                            titulo = nomeDoEstilo(opcao),
                            resumo = resumoDoEstilo(opcao, disciplinas.mapNotNull { siglas[it.id] }),
                            escolhida = opcao == estilo,
                            onClick = { onEscolher(opcao) },
                        ) { Selo(iconeDoEstilo(opcao)) }
                    }
                }
                // matchParentSize: a prévia não conta para a altura do card, só ocupa a que as opções deram
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.TopEnd) {
                    PreviaDasAbas(
                        estilo = estilo,
                        disciplinas = disciplinas,
                        siglas = siglas,
                        modifier = Modifier.fillMaxHeight(),
                    )
                }
            }
        }
    }
}

/**
 * Um pedaço da lateral do app: a linha da borda e as abas de verdade das primeiras disciplinas do
 * ano, com a primeira aberta. Só mostra: os toques não chegam às abas, e o leitor de tela pula ela,
 * porque as opções ao lado já dizem tudo.
 */
@Composable
private fun PreviaDasAbas(
    estilo: EstiloDasAbas,
    disciplinas: List<DisciplinaDomain>,
    siglas: Map<Long, String>,
    modifier: Modifier = Modifier,
) {
    val abas = disciplinas.take(MAXIMO_NA_PREVIA)
    // A primeira aparece aberta, como com a disciplina no centro: a linha da borda é da cor dela
    val corDaBorda = abas.firstOrNull()?.let { CoresDisciplina.de(it.cor) } ?: MaterialTheme.colorScheme.primary

    Box(
        modifier
            .width(LARGURA_DA_PREVIA)
            .clipToBounds()
            .background(MaterialTheme.colorScheme.background)
            // Em pixels inteiros, como nas abas do app: a linha emenda na aba sem costura
            .drawBehind { drawRect(corDaBorda, size = Size(BORDA.roundToPx().toFloat(), size.height)) }
            .clearAndSetSemantics { }
    ) {
        Column(
            // unbounded: as abas descem o quanto precisarem, e o card corta o resto
            modifier = Modifier
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .padding(top = Espacos.s),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            abas.forEachIndexed { i, disciplina ->
                AbaDisciplina(
                    cor = CoresDisciplina.de(disciplina.cor),
                    icone = IconesDisciplina.de(disciplina.icone),
                    nome = disciplina.nome,
                    sigla = siglas[disciplina.id] ?: siglaDoNome(disciplina.nome),
                    estilo = estilo,
                    isSelected = i == 0,
                    onClick = {},
                )
            }
        }
        // Por cima das abas, fica com os toques
        Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { } })
    }
}

// A largura da lateral do app, mais uma folga para a ponta arredondada da aba aberta
private val LARGURA_DA_PREVIA = LARGURA_ABERTA + BORDA + Espacos.s

// Mais que isso não cabe na altura das opções nem no estilo de só ícones
private const val MAXIMO_NA_PREVIA = 6

// Textos ------------------------------------------------------------------------------------

internal fun nomeDoTema(tema: Tema): String = when (tema) {
    Tema.CLARO -> "Claro"
    Tema.ESCURO -> "Escuro"
    Tema.SISTEMA -> "Sistema"
}

/** O que a opção faz. */
internal fun resumoDoTema(tema: Tema): String = when (tema) {
    Tema.CLARO -> "Sempre claro"
    Tema.ESCURO -> "Sempre escuro"
    Tema.SISTEMA -> "Igual ao do aparelho"
}

private fun iconeDoTema(tema: Tema): ImageVector = when (tema) {
    Tema.CLARO -> Icons.Outlined.LightMode
    Tema.ESCURO -> Icons.Outlined.DarkMode
    Tema.SISTEMA -> Icons.Outlined.Contrast
}

internal fun nomeDoEstilo(estilo: EstiloDasAbas): String = when (estilo) {
    EstiloDasAbas.APENAS_ICONES -> "Apenas ícones"
    EstiloDasAbas.ICONES_E_SIGLA -> "Ícones + sigla"
    EstiloDasAbas.ICONES_E_DESCRICAO -> "Ícones + descrição"
}

/**
 * O que a opção faz. A da sigla mostra as do ano aberto ([siglas], na ordem das abas), ou as das
 * primeiras matérias quando ainda não há disciplinas.
 */
internal fun resumoDoEstilo(estilo: EstiloDasAbas, siglas: List<String>): String = when (estilo) {
    EstiloDasAbas.APENAS_ICONES -> "Cabem mais abas"
    EstiloDasAbas.ICONES_E_SIGLA -> {
        val exemplos = siglas.distinct().ifEmpty { listOf("MAT", "POR", "HIS") }
        exemplos.take(3).joinToString(", ") + if (exemplos.size > 3) "…" else ""
    }
    EstiloDasAbas.ICONES_E_DESCRICAO -> "O nome inteiro"
}

private fun iconeDoEstilo(estilo: EstiloDasAbas): ImageVector = when (estilo) {
    EstiloDasAbas.APENAS_ICONES -> Icons.Outlined.Interests
    EstiloDasAbas.ICONES_E_SIGLA -> Icons.Outlined.Abc
    EstiloDasAbas.ICONES_E_DESCRICAO -> Icons.AutoMirrored.Outlined.Subject
}
