package app.jammes.boletim.presentation.ui.disciplina

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.DisciplinaDetalhe
import app.jammes.boletim.domain.model.FaltaDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.Substituta
import app.jammes.boletim.domain.model.TipoAvaliacao
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.model.lancouAFinal
import app.jammes.boletim.presentation.ui.anoletivo.nomeDoPeriodo
import app.jammes.boletim.presentation.ui.boletim.LinhaDaOutraMedia
import app.jammes.boletim.presentation.ui.boletim.MediaAnimada
import app.jammes.boletim.presentation.ui.boletim.corDoStatus
import app.jammes.boletim.presentation.ui.boletim.formatarMedia
import app.jammes.boletim.presentation.ui.boletim.nomeDoFiltro
import app.jammes.boletim.presentation.ui.boletim.outraMedia
import app.jammes.boletim.presentation.ui.components.CarregandoDiscreto
import app.jammes.boletim.presentation.ui.components.ChaveTracejada
import app.jammes.boletim.presentation.ui.components.ChavesCompartilhadas
import app.jammes.boletim.presentation.ui.components.EstadoVazio
import app.jammes.boletim.presentation.ui.components.Pilula
import app.jammes.boletim.presentation.ui.components.SeloDisciplina
import app.jammes.boletim.presentation.ui.components.SeloDoTipo
import app.jammes.boletim.presentation.ui.components.elementoCompartilhado
import app.jammes.boletim.presentation.ui.components.limitesCompartilhados
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import app.jammes.boletim.presentation.ui.theme.Espacos
import app.jammes.boletim.presentation.ui.theme.IconesDisciplina
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DisciplinaDetailScreen(
    onEditar: (disciplinaId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DisciplinaViewModel = hiltViewModel()
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    // A lista aberta embaixo do resumo. Sobrevive a girar a tela, como o formulário.
    var aba by rememberSaveable { mutableStateOf(AbaDoDetalhe.AVALIACOES) }

    // O que está aberto no formulário: null = fechado, NOVA = lançando um novo.
    // Guarda só o id, que sobrevive a girar a tela; a avaliação ou a falta em si vem do state.
    var avaliacaoAbertaId by rememberSaveable { mutableStateOf<Long?>(null) }
    var faltaAbertaId by rememberSaveable { mutableStateOf<Long?>(null) }
    // A confirmação aberta de finalizar ou reabrir; o que ela diz vem do state
    var dialogo by rememberSaveable { mutableStateOf<DialogoDoFechamento?>(null) }

    val haptico = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    // O botão mostra o texto no topo da lista e encolhe para só o "+" quando a lista rola
    val fabExpandido by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            // Sem a disciplina carregada não há onde lançar nada
            val carregada = state as? DisciplinaUiState.Sucesso
            if (carregada != null) {
                // Finalizada, não se lança mais nada: o botão sai encolhendo, e volta se ela for reaberta
                AnimatedVisibility(
                    visible = !carregada.detalhe.finalizada,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    ExtendedFloatingActionButton(
                        // Lança o que a aba aberta lista
                        onClick = {
                            when (aba) {
                                AbaDoDetalhe.AVALIACOES -> avaliacaoAbertaId = NOVA
                                AbaDoDetalhe.FALTAS -> faltaAbertaId = NOVA
                            }
                        },
                        expanded = fabExpandido,
                        icon = {
                            // O FAB do Material esconde o texto do leitor de tela: quem diz o que o
                            // botão faz é a descrição do ícone, aberto ou encolhido
                            Icon(Icons.Filled.Add, contentDescription = aba.lancar)
                        },
                        text = {
                            // Ao trocar de aba o texto troca num fade, e o botão estica ou encolhe até
                            // a largura nova em vez de pular
                            AnimatedContent(
                                targetState = aba,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "textoDoFab",
                            ) { Text(it.lancar) }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
    ) { paddingValues ->

        when (val s = state) {
            DisciplinaUiState.Carregando -> CarregandoDiscreto(Modifier.fillMaxSize().padding(paddingValues))

            DisciplinaUiState.NaoEncontrada -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EstadoVazio(
                    icone = Icons.Outlined.SearchOff,
                    titulo = "Disciplina não encontrada",
                    mensagem = "Ela pode ter saído do ano letivo. Escolha outra nas abas ao lado.",
                )
            }

            is DisciplinaUiState.Sucesso -> {
                val detalhe = s.detalhe
                // O peso só muda a conta na média ponderada; nas outras, mostrar confundiria
                val mostrarPeso = detalhe.regra.tipoMedia == TipoMedia.PONDERADA

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(
                        start = Espacos.l,
                        end = Espacos.l,
                        top = Espacos.s,
                        bottom = 96.dp, // o último item não fica atrás do FAB
                    ),
                ) {
                    item(key = "resumo") {
                        Column(Modifier.padding(bottom = Espacos.l)) {
                            ResumoDisciplina(detalhe = detalhe)
                            // Logo embaixo da média: fechar o ano da disciplina (ou reabrir) e editá-la
                            AcoesDaDisciplina(
                                finalizada = detalhe.finalizada,
                                // Sem nota em nenhum período não há o que fechar: o botão explica por quê
                                onFinalizar = {
                                    dialogo = if (detalhe.fechamento == null) DialogoDoFechamento.SEM_NOTA
                                    else DialogoDoFechamento.FINALIZAR
                                },
                                onReabrir = { dialogo = DialogoDoFechamento.REABRIR },
                                onEditar = { onEditar(detalhe.disciplina.id) },
                                modifier = Modifier.padding(top = Espacos.m),
                            )
                        }
                    }

                    // As abas grudam no topo quando a lista rola: dá para trocar de lista de qualquer ponto
                    stickyHeader(key = "abas") {
                        AbasDoDetalhe(
                            aba = aba,
                            faltas = detalhe.faltas,
                            emRiscoPorFalta = detalhe.emRiscoPorFalta,
                            onSelecionar = { nova ->
                                aba = nova
                                // Com o resumo já fora da tela, a lista nova começa logo abaixo das abas
                                if (listState.firstVisibleItemIndex >= INDICE_DAS_ABAS) {
                                    scope.launch { listState.scrollToItem(INDICE_DAS_ABAS) }
                                }
                            },
                        )
                    }
                    item(key = "espaco") { Spacer(Modifier.height(Espacos.m)) }

                    // Finalizada, as listas são só para ler: nada abre o formulário
                    when (aba) {
                        AbaDoDetalhe.AVALIACOES -> listaDeAvaliacoes(
                            detalhe = detalhe,
                            mostrarPeso = mostrarPeso,
                            onAbrir = if (detalhe.finalizada) null else ({ avaliacaoAbertaId = it }),
                        )
                        AbaDoDetalhe.FALTAS -> listaDeFaltas(
                            detalhe = detalhe,
                            onAbrir = if (detalhe.finalizada) null else ({ faltaAbertaId = it }),
                        )
                    }
                }

                val avaliacaoAberta = when (avaliacaoAbertaId) {
                    null -> null
                    NOVA -> remember { viewModel.novaAvaliacao(detalhe) }
                    else -> detalhe.avaliacoes.find { it.id == avaliacaoAbertaId }
                }
                if (avaliacaoAberta != null) {
                    AvaliacaoBottomSheet(
                        avaliacao = avaliacaoAberta,
                        disciplina = detalhe.disciplina,
                        regra = detalhe.regra,
                        periodos = detalhe.periodos,
                        // As do ano inteiro: a final pode ser lançada de um período, e a média do ano
                        // decide se ela cabe
                        lancadas = detalhe.avaliacoesDoAno,
                        // Num período, a avaliação fica nele; no ano inteiro, o formulário pergunta
                        escolherPeriodo = detalhe.periodoId == null,
                        tipoPeriodo = detalhe.tipoPeriodo,
                        onDismiss = { avaliacaoAbertaId = null },
                        // Um toque curto confirma no dedo que a avaliação foi gravada ou apagada
                        onSalvar = { avaliacao ->
                            haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                            viewModel.salvar(avaliacao)
                            // A final ganhou nota: talvez o ano da disciplina tenha acabado. A pergunta
                            // espera o formulário descer; sem nota em nenhum período, não há o que fechar
                            if (avaliacao.lancouAFinal(antes = avaliacaoAberta) && detalhe.fechamento != null) {
                                dialogo = DialogoDoFechamento.FINALIZAR_DEPOIS_DA_FINAL
                            }
                        },
                        onExcluir = {
                            haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                            viewModel.excluir(avaliacaoAberta)
                        },
                    )
                }

                val faltaAberta = when (faltaAbertaId) {
                    null -> null
                    NOVA -> remember { viewModel.novaFalta(detalhe) }
                    else -> detalhe.diasDeFalta.find { it.id == faltaAbertaId }
                }
                if (faltaAberta != null) {
                    FaltaBottomSheet(
                        falta = faltaAberta,
                        // O formulário não pergunta o período: a data decide, entre os da tela
                        periodos = detalhe.periodosNoFiltro,
                        tipoPeriodo = detalhe.tipoPeriodo,
                        faltasLancadas = detalhe.diasDeFalta,
                        onDismiss = { faltaAbertaId = null },
                        onSalvar = { falta ->
                            haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                            viewModel.salvar(falta)
                        },
                        onExcluir = {
                            haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                            viewModel.excluir(faltaAberta)
                        },
                    )
                }

                // Só com os formulários fechados: a pergunta que vem depois da final espera o sheet
                // terminar de descer, e então já mostra a média com a nota dela
                if (avaliacaoAbertaId == null && faltaAbertaId == null) {
                    val fecharDialogo = { dialogo = null }
                    when (dialogo) {
                        DialogoDoFechamento.FINALIZAR, DialogoDoFechamento.FINALIZAR_DEPOIS_DA_FINAL -> {
                            val fechamento = detalhe.fechamento
                            if (fechamento == null) {
                                AvisoSemNota(detalhe.tipoPeriodo, onFechar = fecharDialogo)
                            } else {
                                ConfirmarFinalizacao(
                                    disciplina = detalhe.disciplina.nome,
                                    fechamento = fechamento,
                                    tipoPeriodo = detalhe.tipoPeriodo,
                                    depoisDaFinal = dialogo == DialogoDoFechamento.FINALIZAR_DEPOIS_DA_FINAL,
                                    onFinalizar = {
                                        haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                                        viewModel.finalizar(detalhe)
                                        dialogo = null
                                    },
                                    onCancelar = fecharDialogo,
                                )
                            }
                        }
                        DialogoDoFechamento.SEM_NOTA -> AvisoSemNota(detalhe.tipoPeriodo, onFechar = fecharDialogo)
                        DialogoDoFechamento.REABRIR -> ConfirmarReabertura(
                            disciplina = detalhe.disciplina.nome,
                            situacao = detalhe.situacao,
                            onReabrir = {
                                haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                                viewModel.reabrir()
                                dialogo = null
                            },
                            onCancelar = fecharDialogo,
                        )
                        null -> Unit
                    }
                }
            }
        }
    }
}

private const val NOVA = 0L // o mesmo id de "ainda não gravada" que os repositórios usam
private const val INDICE_DAS_ABAS = 1 // na lista, logo depois do resumo

/** As duas listas da disciplina. O FAB lança o que a aba aberta lista. */
private enum class AbaDoDetalhe(val titulo: String, val lancar: String) {
    AVALIACOES(titulo = "Avaliações", lancar = "Nova avaliação"),
    FALTAS(titulo = "Faltas", lancar = "Nova falta"),
}

@Composable
private fun AbasDoDetalhe(
    aba: AbaDoDetalhe,
    faltas: Int, // do ano inteiro: vão no contador da aba de faltas
    emRiscoPorFalta: Boolean,
    onSelecionar: (AbaDoDetalhe) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptico = LocalHapticFeedback.current
    SecondaryTabRow(
        selectedTabIndex = aba.ordinal,
        modifier = modifier,
        // O mesmo fundo da tela: com as abas grudadas no topo, a lista passa por baixo sem aparecer
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        AbaDoDetalhe.entries.forEach { opcao ->
            Tab(
                selected = aba == opcao,
                onClick = {
                    if (aba != opcao) {
                        // Um "tique" a cada troca, como no seletor de período
                        haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onSelecionar(opcao)
                    }
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(opcao.titulo)
                        // As faltas saíram do resumo: o número delas fica na própria aba
                        if (opcao == AbaDoDetalhe.FALTAS && faltas > 0) {
                            Spacer(Modifier.width(6.dp))
                            ContadorDeFaltas(faltas = faltas, emRisco = emRiscoPorFalta, selecionada = aba == opcao)
                        }
                    }
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A aba das avaliações: um card por período e, no ano letivo inteiro, o da final por último, ou o
 * convite para lançar a primeira.
 */
private fun LazyListScope.listaDeAvaliacoes(
    detalhe: DisciplinaDetalhe,
    mostrarPeso: Boolean,
    onAbrir: ((avaliacaoId: Long) -> Unit)?, // NOVA lança uma; null: finalizada, nada abre
) {
    if (detalhe.avaliacoes.isEmpty()) {
        item(key = "avaliacoes-vazio") {
            EstadoVazio(
                icone = Icons.AutoMirrored.Outlined.Assignment,
                titulo = "Nenhuma avaliação neste ${nomeDoFiltro(detalhe.periodoId)}",
                mensagem = if (onAbrir != null) "Lance as provas, trabalhos e atividades para acompanhar a média."
                else "A disciplina está finalizada. Reabra para lançar avaliações.",
                modifier = Modifier.animateItem(),
                acao = if (onAbrir != null) {
                    { BotaoLancar(texto = "Lançar avaliação", onClick = { onAbrir(NOVA) }) }
                } else {
                    null
                },
            )
        }
    } else {
        detalhe.avaliacoesPorPeriodo.forEach { grupo ->
            item(key = "avaliacoes-${grupo.periodo.id}") {
                GrupoDoPeriodo(
                    titulo = tituloDoGrupo(grupo.periodo, detalhe),
                    itens = grupo.avaliacoes,
                    modifier = Modifier.animateItem(),
                ) { avaliacao ->
                    AvaliacaoItem(
                        avaliacao = avaliacao,
                        regra = detalhe.regra,
                        mostrarPeso = mostrarPeso,
                        onClick = onAbrir?.let { abrir -> { abrir(avaliacao.id) } },
                    )
                }
            }
        }
        // A final é do ano, não de um período: tem o grupo dela, depois de todos
        if (detalhe.finais.isNotEmpty()) {
            item(key = "avaliacoes-final") {
                GrupoDoPeriodo(
                    titulo = "FINAL",
                    itens = detalhe.finais,
                    modifier = Modifier.animateItem(),
                ) { avaliacao ->
                    AvaliacaoItem(
                        avaliacao = avaliacao,
                        regra = detalhe.regra,
                        mostrarPeso = mostrarPeso,
                        onClick = onAbrir?.let { abrir -> { abrir(avaliacao.id) } },
                    )
                }
            }
        }
    }
}

/**
 * A aba das faltas: o resumo do ano (quantas, o limite e a frequência) e, embaixo, os dias de falta
 * agrupados por período, do mesmo jeito que as avaliações.
 */
private fun LazyListScope.listaDeFaltas(
    detalhe: DisciplinaDetalhe,
    onAbrir: ((faltaId: Long) -> Unit)?, // NOVA lança uma; null: finalizada, nada abre
) {
    item(key = "faltas-resumo") {
        ResumoDasFaltas(detalhe, Modifier.animateItem().padding(bottom = Espacos.m))
    }
    if (detalhe.faltasPorPeriodo.isEmpty()) {
        item(key = "faltas-vazio") {
            EstadoVazio(
                icone = Icons.Outlined.EventAvailable,
                titulo = "Nenhuma falta neste ${nomeDoFiltro(detalhe.periodoId)}",
                mensagem = if (onAbrir != null) "Quando faltar, lance o dia aqui para acompanhar a frequência."
                else "A disciplina está finalizada. Reabra para lançar faltas.",
                modifier = Modifier.animateItem(),
                acao = if (onAbrir != null) {
                    { BotaoLancar(texto = "Lançar falta", onClick = { onAbrir(NOVA) }) }
                } else {
                    null
                },
            )
        }
    } else {
        detalhe.faltasPorPeriodo.forEach { grupo ->
            item(key = "faltas-${grupo.periodo.id}") {
                GrupoDoPeriodo(
                    titulo = tituloDoGrupo(grupo.periodo, detalhe),
                    itens = grupo.faltas,
                    modifier = Modifier.animateItem(),
                ) { falta ->
                    FaltaItem(falta = falta, onClick = onAbrir?.let { abrir -> { abrir(falta.id) } })
                }
            }
        }
    }
}

/**
 * Um período da lista: o [titulo] em cima e os itens juntos num card, separados por linhas finas.
 * As duas abas usam, para avaliações e faltas se agruparem do mesmo jeito.
 *
 * Quem chama passa o animateItem: como cada aba tem as suas chaves, ao trocar o período ou a aba
 * os grupos entram e saem no lugar, e os de baixo deslizam em vez de pular.
 */
@Composable
private fun <T> GrupoDoPeriodo(
    titulo: String?,
    itens: List<T>,
    modifier: Modifier = Modifier,
    linha: @Composable (T) -> Unit,
) {
    Column(modifier.padding(bottom = Espacos.m)) {
        if (titulo != null) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = Espacos.xs, top = Espacos.xs, bottom = Espacos.s),
            )
        }
        Card(
            // Item novo ou apagado: o card cresce ou encolhe suave
            modifier = Modifier.animateContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            itens.forEachIndexed { i, item ->
                if (i > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Espacos.l),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                linha(item)
            }
        }
    }
}

/** O nome do período em cima do grupo ("1ª UNIDADE"), só no ano letivo inteiro. */
private fun tituloDoGrupo(periodo: PeriodoDomain, detalhe: DisciplinaDetalhe): String? =
    if (detalhe.periodoId == null) nomeDoPeriodo(periodo.periodo, detalhe.tipoPeriodo).uppercase() else null

/** O botão da lista vazia: o mesmo que o FAB faz, mas aqui, onde o olho já está. */
@Composable
private fun BotaoLancar(texto: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick) {
        Icon(
            Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(texto)
    }
}

/**
 * A capa da disciplina: o nome, a média do filtro e, embaixo dela, a média de cada período no
 * gráfico, contra a mínima. As faltas ficam na aba delas, com o número na própria aba.
 */
@Composable
private fun ResumoDisciplina(
    detalhe: DisciplinaDetalhe,
    modifier: Modifier = Modifier,
) {
    val disciplina = detalhe.disciplina
    val cor = CoresDisciplina.de(disciplina.cor)
    val corStatus by animateColorAsState(corDoStatus(detalhe.status), label = "corDoStatus")
    val professor = disciplina.professor?.takeIf { it.isNotBlank() }

    // Levemente tingido com a cor da disciplina: a "capa" da folha que a aba abriu
    Card(
        // A outra ponta do card da disciplina no Boletim: ele se transforma neste resumo
        modifier = modifier
            .limitesCompartilhados(
                chave = ChavesCompartilhadas.card(disciplina.id),
                forma = MaterialTheme.shapes.large,
            )
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = cor.copy(alpha = 0.08f).compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest),
        ),
        border = BorderStroke(1.dp, cor.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeloDisciplina(
                    cor = cor,
                    icone = IconesDisciplina.de(disciplina.icone),
                    tamanho = 44.dp,
                    modifier = Modifier.elementoCompartilhado(ChavesCompartilhadas.selo(disciplina.id)),
                )
                Spacer(Modifier.width(Espacos.m))
                Column {
                    Text(
                        text = disciplina.nome,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (professor != null) {
                        Text(
                            text = professor,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Espacos.l))
            Text(
                text = tituloDaMedia(detalhe.periodoId, detalhe.substituta),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Finalizada, a situação vem na frente da média. Se não couber na linha, ela
            // desce para baixo do número em vez de apertar
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Espacos.s),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                // Mesma formatação e mesma cor do card no Boletim, para as duas telas não divergirem
                MediaAnimada(detalhe.media) { media ->
                    Text(
                        text = formatarMedia(media),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = corStatus,
                    )
                }
                // Entra e sai abrindo espaço, sem pular
                AnimatedContent(
                    targetState = detalhe.situacao,
                    transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
                    label = "situacao",
                ) { situacao ->
                    if (situacao != null) SeloDaSituacao(situacao)
                }
            }
            // Abaixo da mínima: quanto falta para chegar nela. Na mínima ou acima, some
            AnimatedContent(
                targetState = quantoFalta(detalhe.media, detalhe.regra.mediaMinima),
                transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
                label = "quantoFalta",
            ) { falta ->
                if (falta != null) {
                    Text(
                        text = falta,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            // Com recuperação ou final lançada, a média que ficou de fora vem embaixo, menor
            outraMedia(detalhe.mediaRegular, detalhe.substituta, detalhe.periodoId)?.let { (rotulo, media) ->
                LinhaDaOutraMedia(rotulo = rotulo, media = media)
            }
            // O gráfico vem embaixo, separado por uma linha e com a largura toda: ao lado da média, as
            // colunas ficavam estreitas, e o valor com duas casas não cabia embaixo delas
            HorizontalDivider(
                modifier = Modifier.padding(vertical = Espacos.l),
                color = cor.copy(alpha = 0.25f),
            )
            // O leitor de tela pula esta linha: a descrição do gráfico já diz o que ele mostra e a mínima
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {},
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(Espacos.xs),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Média por ${detalhe.tipoPeriodo.displayName.lowercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // A legenda do tracejado
                ChaveTracejada(
                    texto = "mínima ${formatarMedia(detalhe.regra.mediaMinima)}",
                    cor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
            }
            Spacer(Modifier.height(Espacos.m))
            GraficoDosPeriodos(
                medias = detalhe.mediasPorPeriodo,
                mediaMinima = detalhe.regra.mediaMinima,
                periodoEscolhido = detalhe.periodoId,
                tipoPeriodo = detalhe.tipoPeriodo,
            )
        }
    }
}

@Composable
private fun AvaliacaoItem(
    avaliacao: AvaliacaoDomain,
    regra: RegraAvaliacaoDomain,
    mostrarPeso: Boolean,
    onClick: (() -> Unit)?, // null: a disciplina está finalizada, e a avaliação não abre
    modifier: Modifier = Modifier,
) {
    // O tipo vai no selo antes do nome (R ou F); o peso só na normal, que é a única que ele muda
    val descricao = listOfNotNull(
        avaliacao.data?.format(FORMATO_DATA),
        if (mostrarPeso && avaliacao.tipo == TipoAvaliacao.NORMAL) "Peso ${formatarNumero(avaliacao.peso)}" else null,
    ).joinToString(" · ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClickLabel = "Editar avaliação", onClick = onClick) else Modifier)
            .padding(horizontal = Espacos.l, vertical = Espacos.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (avaliacao.tipo != TipoAvaliacao.NORMAL) {
            SeloDoTipo(avaliacao.tipo)
            Spacer(Modifier.width(Espacos.m))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = avaliacao.nome,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (descricao.isNotEmpty()) {
                Text(
                    text = descricao,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(Espacos.l))
        val nota = avaliacao.nota
        if (nota == null) {
            // Ainda não aconteceu (ou a nota não saiu): não entra na média. Igual à do formulário.
            Pilula(texto = "Agendada", cor = MaterialTheme.colorScheme.primary, icone = Icons.Outlined.Schedule)
        } else {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    // Mesmo formato da média (7,0 · 8,5 · 8,75)
                    text = formatarMedia(nota),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = corDaNota(nota, avaliacao.notaMaxima, regra, avaliacao.tipo),
                )
                Text(
                    text = "de ${formatarNumero(avaliacao.notaMaxima)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A nota na cor do status, pelo [statusDaNota]. Sem status (a normal na soma), fica neutra. */
@Composable
internal fun corDaNota(
    nota: Double,
    notaMaxima: Double,
    regra: RegraAvaliacaoDomain,
    tipo: TipoAvaliacao = TipoAvaliacao.NORMAL,
): Color = statusDaNota(nota, notaMaxima, regra, tipo)?.let { corDoStatus(it) } ?: MaterialTheme.colorScheme.onSurface

/**
 * O que está em cima do número do resumo: "Média do período" (ou "do ano letivo") e, quando a
 * recuperação ou a final foi maior e tomou o lugar, "Média com recuperação" ou "Média com a final".
 */
private fun tituloDaMedia(periodoId: Long?, substituta: Substituta?): String = when {
    substituta == null || !substituta.substituiu -> "Média do ${nomeDoFiltro(periodoId)}"
    substituta.tipo == TipoAvaliacao.FINAL -> "Média com a final"
    else -> "Média com recuperação"
}

private val FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM")

@Composable
private fun FaltaItem(
    falta: FaltaDomain,
    onClick: (() -> Unit)?, // null: a disciplina está finalizada, e a falta não abre
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClickLabel = "Editar falta", onClick = onClick) else Modifier)
            .padding(horizontal = Espacos.l, vertical = Espacos.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = falta.data.format(FORMATO_DIA), // 5 de março
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = falta.data.format(FORMATO_DIA_DA_SEMANA).replaceFirstChar { it.titlecase(PT_BR) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(Espacos.l))
        // Como a nota de uma avaliação: o número grande e, embaixo, o que ele conta
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = falta.qtdAulas.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (falta.qtdAulas == 1) "falta" else "faltas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val PT_BR = Locale.forLanguageTag("pt-BR")
private val FORMATO_DIA = DateTimeFormatter.ofPattern("d 'de' MMMM", PT_BR)
private val FORMATO_DIA_DA_SEMANA = DateTimeFormatter.ofPattern("EEEE", PT_BR)
