package app.jammes.boletim.presentation.ui.disciplina

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.DisciplinaDetalhe
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoAvaliacao
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.usecase.arredondarMedia
import app.jammes.boletim.domain.usecase.statusDaMedia
import app.jammes.boletim.presentation.ui.anoletivo.nomeDoPeriodo
import app.jammes.boletim.presentation.ui.boletim.MediaAnimada
import app.jammes.boletim.presentation.ui.boletim.corDoStatus
import app.jammes.boletim.presentation.ui.boletim.formatarMedia
import app.jammes.boletim.presentation.ui.boletim.nomeDoFiltro
import app.jammes.boletim.presentation.ui.components.BarraDaMedia
import app.jammes.boletim.presentation.ui.components.CarregandoDiscreto
import app.jammes.boletim.presentation.ui.components.ChavesCompartilhadas
import app.jammes.boletim.presentation.ui.components.Pilula
import app.jammes.boletim.presentation.ui.components.SeloDisciplina
import app.jammes.boletim.presentation.ui.components.elementoCompartilhado
import app.jammes.boletim.presentation.ui.components.limitesCompartilhados
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import app.jammes.boletim.presentation.ui.theme.Espacos
import app.jammes.boletim.presentation.ui.theme.IconesDisciplina
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DisciplinaDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: DisciplinaViewModel = hiltViewModel()
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    // Avaliação aberta no formulário: null = fechado, NOVA = criando uma.
    // Guarda só o id, que sobrevive a girar a tela; a avaliação em si vem do state.
    var abertaId by rememberSaveable { mutableStateOf<Long?>(null) }

    val haptico = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    // O botão mostra o texto no topo da lista e encolhe para só o "+" quando a lista rola
    val fabExpandido by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            // Sem a disciplina carregada não há onde lançar a avaliação
            if (state is DisciplinaUiState.Sucesso) {
                ExtendedFloatingActionButton(
                    onClick = { abertaId = NOVA },
                    expanded = fabExpandido,
                    icon = {
                        Icon(
                            Icons.Filled.Add,
                            // Aberto, o texto ao lado já diz o que o botão faz
                            contentDescription = if (fabExpandido) null else "Nova avaliação",
                        )
                    },
                    text = { Text("Nova avaliação") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { paddingValues ->

        when (val s = state) {
            DisciplinaUiState.Carregando -> CarregandoDiscreto(Modifier.fillMaxSize().padding(paddingValues))

            DisciplinaUiState.NaoEncontrada -> Box(
                Modifier.fillMaxSize().padding(paddingValues).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Disciplina não encontrada", style = MaterialTheme.typography.titleMedium)
            }

            is DisciplinaUiState.Sucesso -> {
                val detalhe = s.detalhe
                // O peso só muda a conta na média ponderada; nas outras, mostrar confundiria
                val mostrarPeso = detalhe.regra.tipoMedia == TipoMedia.PONDERADA
                // No ano letivo inteiro as avaliações aparecem separadas, com o nome do período
                val anoInteiro = detalhe.periodoId == null

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(
                        start = Espacos.l,
                        end = Espacos.l,
                        top = Espacos.s,
                        bottom = 96.dp, // a última avaliação não fica atrás do FAB
                    ),
                ) {
                    item { ResumoDisciplina(detalhe = detalhe) }

                    item {
                        Text(
                            text = "Avaliações",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = Espacos.xl, bottom = Espacos.s),
                        )
                    }

                    if (detalhe.avaliacoesPorPeriodo.isEmpty()) {
                        item {
                            Text(
                                text = "Nenhuma avaliação neste ${nomeDoFiltro(detalhe.periodoId)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.animateItem().fillMaxWidth().padding(vertical = 32.dp),
                            )
                        }
                    } else {
                        // Cada período é uma seção: o nome em cima (só no ano inteiro) e as
                        // avaliações juntas num card, separadas por linhas finas
                        detalhe.avaliacoesPorPeriodo.forEach { grupo ->
                            item(key = "periodo-${grupo.periodo.id}") {
                                // Ao trocar o período, as seções entram e saem no lugar, e as de
                                // baixo deslizam em vez de pular
                                Column(Modifier.animateItem().padding(bottom = Espacos.m)) {
                                    if (anoInteiro) {
                                        Text(
                                            text = nomeDoPeriodo(grupo.periodo.periodo, detalhe.tipoPeriodo).uppercase(),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(start = Espacos.xs, top = Espacos.xs, bottom = Espacos.s),
                                        )
                                    }
                                    Card(
                                        // Avaliação nova ou apagada: o card cresce ou encolhe suave
                                        modifier = Modifier.animateContentSize(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    ) {
                                        grupo.avaliacoes.forEachIndexed { i, avaliacao ->
                                            if (i > 0) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = Espacos.l),
                                                    color = MaterialTheme.colorScheme.outlineVariant,
                                                )
                                            }
                                            AvaliacaoItem(
                                                avaliacao = avaliacao,
                                                regra = detalhe.regra,
                                                mostrarPeso = mostrarPeso,
                                                onClick = { abertaId = avaliacao.id },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val aberta = when (abertaId) {
                    null -> null
                    NOVA -> remember { viewModel.novaAvaliacao(detalhe) }
                    else -> detalhe.avaliacoes.find { it.id == abertaId }
                }
                if (aberta != null) {
                    AvaliacaoBottomSheet(
                        avaliacao = aberta,
                        mostrarPeso = mostrarPeso,
                        // Num período, a avaliação fica nele; no ano inteiro, o formulário pergunta
                        periodos = if (anoInteiro) detalhe.periodos else emptyList(),
                        tipoPeriodo = detalhe.tipoPeriodo,
                        onDismiss = { abertaId = null },
                        // Um toque curto confirma no dedo que a avaliação foi gravada ou apagada
                        onSalvar = { avaliacao ->
                            haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                            viewModel.salvar(avaliacao)
                        },
                        onExcluir = {
                            haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                            viewModel.excluir(aberta)
                        },
                    )
                }
            }
        }
    }
}

private const val NOVA = 0L // o mesmo id de "ainda não gravada" que os repositórios usam

@Composable
private fun ResumoDisciplina(
    detalhe: DisciplinaDetalhe,
    modifier: Modifier = Modifier,
) {
    val disciplina = detalhe.disciplina
    val cor = CoresDisciplina.de(disciplina.cor)
    val corStatus by animateColorAsState(corDoStatus(detalhe.status), label = "corDoStatus")
    val dados = listOfNotNull(
        disciplina.professor?.takeIf { it.isNotBlank() },
        disciplina.totalAulas?.let { if (it == 1) "1 aula no ano" else "$it aulas no ano" },
    ).joinToString(" · ")
    val calculo = when (detalhe.regra.tipoMedia) {
        TipoMedia.SIMPLES -> "Média simples"
        TipoMedia.PONDERADA -> "Média ponderada"
        TipoMedia.SOMA -> "Soma das notas"
    }

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
                    if (dados.isNotEmpty()) {
                        Text(
                            text = dados,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            // Duas colunas, a da média e a das faltas, montadas em linhas: assim cada coisa fica
            // embaixo do seu número, e os números continuam lado a lado mesmo se um rótulo quebrar
            // em duas linhas (fonte grande).
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Média do ${nomeDoFiltro(detalhe.periodoId)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Espacos.l))
                // Faltas do ano inteiro, em qualquer filtro: o limite de frequência é anual
                Text(
                    text = "Faltas no ano",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row {
                // Mesma formatação e mesma cor do card no Boletim, para as duas telas não divergirem
                MediaAnimada(detalhe.media, Modifier.weight(1f)) { media ->
                    Text(
                        text = formatarMedia(media),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = corStatus,
                    )
                }
                Spacer(Modifier.width(Espacos.l))
                Text(
                    text = detalhe.faltas.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (detalhe.emRiscoPorFalta) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            Spacer(Modifier.height(Espacos.s))
            Row {
                // A barra é só da média: fica na coluna dela, sem passar por baixo das faltas
                Column(Modifier.weight(1f)) {
                    BarraDaMedia(media = detalhe.media, mediaMinima = detalhe.regra.mediaMinima, cor = corStatus)
                    Spacer(Modifier.height(Espacos.s))
                    Text(
                        text = "$calculo · mínima ${formatarMedia(detalhe.regra.mediaMinima)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                detalhe.limiteFaltas?.let { limite ->
                    Spacer(Modifier.width(Espacos.l))
                    Text(
                        text = "de $limite permitidas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AvaliacaoItem(
    avaliacao: AvaliacaoDomain,
    regra: RegraAvaliacaoDomain,
    mostrarPeso: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val descricao = listOfNotNull(
        avaliacao.data?.format(FORMATO_DATA),
        avaliacao.tipo.takeIf { it != TipoAvaliacao.NORMAL }?.displayname,
        if (mostrarPeso) "Peso ${formatarNumero(avaliacao.peso)}" else null,
    ).joinToString(" · ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Editar avaliação", onClick = onClick)
            .padding(horizontal = Espacos.l, vertical = Espacos.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
            // Ainda não aconteceu (ou a nota não saiu): não entra na média
            Pilula(texto = "Pendente")
        } else {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    // Mesmo formato da média (7,0 · 8,5 · 8,75)
                    text = formatarMedia(nota),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = corDaNota(nota, avaliacao.notaMaxima, regra),
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

/**
 * A nota na cor do status, como se fosse uma média: levada para a escala de 10 (a da média
 * mínima) e limpa como a tela limpa as médias. Na soma, cada nota é só uma parte do total e não
 * dá para comparar com a mínima, então fica neutra.
 */
@Composable
private fun corDaNota(nota: Double, notaMaxima: Double, regra: RegraAvaliacaoDomain): Color =
    if (regra.tipoMedia == TipoMedia.SOMA || notaMaxima <= 0.0) {
        MaterialTheme.colorScheme.onSurface
    } else {
        corDoStatus(statusDaMedia(arredondarMedia(nota / notaMaxima * 10, TipoArredondamento.NENHUM), regra))
    }

private val FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM")

// Peso, nota máxima e os campos do formulário: até 2 casas, sem zero sobrando (1 · 1,5 · 10)
internal fun formatarNumero(valor: Double): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"))
        .apply {
            maximumFractionDigits = 2
            roundingMode = RoundingMode.HALF_UP
        }
        .format(valor)
