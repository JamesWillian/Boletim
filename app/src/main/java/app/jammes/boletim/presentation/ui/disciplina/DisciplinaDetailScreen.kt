package app.jammes.boletim.presentation.ui.disciplina

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.DisciplinaDetalhe
import app.jammes.boletim.domain.model.TipoAvaliacao
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.presentation.ui.anoletivo.nomeDoPeriodo
import app.jammes.boletim.presentation.ui.boletim.corDoStatus
import app.jammes.boletim.presentation.ui.boletim.formatarMedia
import app.jammes.boletim.presentation.ui.boletim.nomeDoFiltro
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
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

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            // Sem a disciplina carregada não há onde lançar a avaliação
            if (state is DisciplinaUiState.Sucesso) {
                FloatingActionButton(onClick = { abertaId = NOVA }) {
                    Icon(Icons.Filled.Add, contentDescription = "Nova avaliação")
                }
            }
        },
    ) { paddingValues ->

        when (val s = state) {
            DisciplinaUiState.Carregando -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

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
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 96.dp, // a última avaliação não fica atrás do FAB
                    ),
                ) {
                    item { ResumoDisciplina(detalhe = detalhe) }

                    item {
                        Text(
                            text = "Avaliações",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
                        )
                    }

                    if (detalhe.avaliacoesPorPeriodo.isEmpty()) {
                        item {
                            Text(
                                text = "Nenhuma avaliação neste ${nomeDoFiltro(detalhe.periodoId)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            )
                        }
                    } else {
                        detalhe.avaliacoesPorPeriodo.forEach { grupo ->
                            if (anoInteiro) {
                                item(key = "periodo-${grupo.periodo.id}") {
                                    Text(
                                        text = nomeDoPeriodo(grupo.periodo.periodo, detalhe.tipoPeriodo).uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                    )
                                }
                            }
                            itemsIndexed(
                                grupo.avaliacoes,
                                key = { _, avaliacao -> avaliacao.id }
                            ) { i, avaliacao ->
                                if (i > 0) HorizontalDivider()
                                AvaliacaoItem(
                                    avaliacao = avaliacao,
                                    mostrarPeso = mostrarPeso,
                                    onClick = { abertaId = avaliacao.id },
                                )
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
                        onSalvar = viewModel::salvar,
                        onExcluir = { viewModel.excluir(aberta) },
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
    val dados = listOfNotNull(
        disciplina.professor?.takeIf { it.isNotBlank() },
        disciplina.totalAulas?.let { if (it == 1) "1 aula no ano" else "$it aulas no ano" },
    ).joinToString(" · ")
    val calculo = when (detalhe.regra.tipoMedia) {
        TipoMedia.SIMPLES -> "Média simples"
        TipoMedia.PONDERADA -> "Média ponderada"
        TipoMedia.SOMA -> "Soma das notas"
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Faixa de identidade da disciplina, a mesma do card no Boletim
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(CoresDisciplina.de(disciplina.cor))
            )
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = disciplina.nome,
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (dados.isNotEmpty()) {
                    Text(
                        text = dados,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Média do ${nomeDoFiltro(detalhe.periodoId)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // Mesma formatação e mesma cor do card no Boletim, para as duas telas não divergirem
                        Text(
                            text = formatarMedia(detalhe.media),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = corDoStatus(detalhe.status),
                        )
                        Text(
                            text = "$calculo · mínima ${formatarMedia(detalhe.regra.mediaMinima)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    // Faltas do ano inteiro, em qualquer filtro: o limite de frequência é anual
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Faltas no ano",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = detalhe.faltas.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            color = if (detalhe.emRiscoPorFalta) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        detalhe.limiteFaltas?.let { limite ->
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
    }
}

@Composable
private fun AvaliacaoItem(
    avaliacao: AvaliacaoDomain,
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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = avaliacao.nome,
                style = MaterialTheme.typography.bodyLarge,
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
        Spacer(Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                // Mesmo formato da média (7,0 · 8,5 · 8,75); sem nota ainda, mostra "—"
                text = formatarMedia(avaliacao.nota),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "de ${formatarNumero(avaliacao.notaMaxima)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
