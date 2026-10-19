package app.jammes.boletim.presentation.ui.boletim

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.DisciplinaResumo
import app.jammes.boletim.domain.model.StatusDisciplina
import app.jammes.boletim.domain.model.Substituta
import app.jammes.boletim.domain.usecase.mediaEmTexto
import app.jammes.boletim.presentation.ui.components.AnelDaMedia
import app.jammes.boletim.presentation.ui.components.CarregandoDiscreto
import app.jammes.boletim.presentation.ui.components.ChavesCompartilhadas
import app.jammes.boletim.presentation.ui.components.DURACAO_TRANSICAO
import app.jammes.boletim.presentation.ui.components.SeloDisciplina
import app.jammes.boletim.presentation.ui.components.elementoCompartilhado
import app.jammes.boletim.presentation.ui.components.limitesCompartilhados
import app.jammes.boletim.presentation.ui.notificacoes.PedirPermissaoAoAbrir
import app.jammes.boletim.presentation.ui.theme.BoletimTheme
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import app.jammes.boletim.presentation.ui.theme.Espacos
import app.jammes.boletim.presentation.ui.theme.IconesDisciplina
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoletimScreen(
    onAbrirDisciplina: (disciplinaId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BoletimViewModel = hiltViewModel()
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dica by viewModel.dicaDaPrimeiraNota.collectAsStateWithLifecycle()

    // As notificações já vêm ligadas, mas do Android 13 em diante só chegam com a permissão: na
    // primeira vez que o boletim abre, o Android pergunta
    PedirPermissaoAoAbrir()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->

        // Dentro da grade, só o `s`: quando o estado deixa de ser Sucesso, a grade ainda roda
        // mais uma vez antes de sumir, e um `state as Sucesso` ali quebra o app
        when (val s = state) {
            BoletimUiState.Carregando -> CarregandoDiscreto(Modifier.fillMaxSize().padding(paddingValues))

            BoletimUiState.SemDisciplinas -> Column(
                modifier = modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Nenhuma matéria neste ano", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Adicione as matérias para o boletim começar a aparecer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = {}) { Text("Adicionar matérias") }
            }

            // Um mural: cada coluna empilha os cards sem alinhar por linha, e um card mais baixo não
            // deixa vão embaixo dele. Cada card tem a altura do que mostra: a situação só ocupa
            // linha quando existe
            is BoletimUiState.Sucesso -> LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = Espacos.l,
                    end = Espacos.l,
                    top = Espacos.s,
                    bottom = 96.dp,
                ),
                verticalItemSpacing = Espacos.m,
                horizontalArrangement = Arrangement.spacedBy(Espacos.m),
            ) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    MediasPorDisciplina(
                        boletim = s.boletim,
                        modifier = Modifier.padding(bottom = Espacos.xs),
                    )
                }
                val disciplinas = s.boletim.disciplinas
                items(disciplinas, key = { it.id }) {
                    ComDicaDaPrimeiraNota(
                        mostrar = dica && it.id == disciplinas.first().id,
                        onDispensar = viewModel::dispensarDica,
                        // Disciplina que entra ou sai do ano aparece e some no lugar, e as outras
                        // deslizam para as novas posições em vez de pular
                        modifier = Modifier.animateItem(),
                    ) {
                        DisciplinaCard(
                            resumo = it,
                            onClick = {
                                // Abrir uma matéria é fazer o que a dica pede: ela já cumpriu o papel
                                if (dica) viewModel.dispensarDica()
                                onAbrirDisciplina(it.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * A dica que aparece uma vez só, logo depois do onboarding, apontando para o primeiro card: é ali
 * que se lança a primeira nota. Some no "Entendi", num toque fora ou quando um card é aberto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComDicaDaPrimeiraNota(
    mostrar: Boolean,
    onDispensar: () -> Unit,
    modifier: Modifier = Modifier,
    conteudo: @Composable () -> Unit,
) {
    if (!mostrar) {
        Box(modifier) { conteudo() }
        return
    }
    val estado = rememberTooltipState(isPersistent = true)
    // Um instante depois da tela, para não aparecer no meio da animação de entrada
    LaunchedEffect(estado) {
        delay(DURACAO_TRANSICAO.toLong())
        estado.show()
    }
    val dispensar = {
        estado.dismiss()
        onDispensar()
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = {
            RichTooltip(
                title = { Text("Lance a primeira nota") },
                action = {
                    // O TextButton usaria a primária, apagada sobre o fundo invertido
                    TextButton(
                        onClick = dispensar,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.inversePrimary),
                    ) { Text("Entendi") }
                },
                caretShape = TooltipDefaults.caretShape(),
                // Invertida, para destacar sobre os cards nos dois temas
                colors = TooltipDefaults.richTooltipColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    titleContentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionContentColor = MaterialTheme.colorScheme.inversePrimary,
                ),
            ) {
                Text("Toque numa matéria para lançar notas e faltas.")
            }
        },
        state = estado,
        onDismissRequest = dispensar,
        hasAction = true,
        modifier = modifier,
    ) {
        conteudo()
    }
}

@Composable
private fun DisciplinaCard(
    resumo: DisciplinaResumo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val corStatus by animateColorAsState(corDoStatus(resumo.status), label = "corDoStatus")

    Card(
        onClick = onClick,
        // Ao abrir a disciplina, este card se transforma no resumo do detalhe (e volta ao fechar)
        modifier = modifier.limitesCompartilhados(
            chave = ChavesCompartilhadas.card(resumo.id),
            forma = MaterialTheme.shapes.medium,
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(Espacos.l)) {
            Row {
                Box(Modifier.weight(1f)) {
                    // Duas linhas no tamanho normal, vazias, mantêm os cards alinhados na grade. O
                    // minLines no próprio nome não serve: quando a fonte encolhe, a linha encolhe junto
                    // (com a fonte do sistema aumentada), e 2px a menos já mudam a coluna dos seguintes
                    Text(
                        text = "",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        minLines = 2,
                    )
                    Text(
                        text = resumo.nome,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        // "Educação / Física" pode quebrar; "Matemáti / ca", não: aí a fonte encolhe
                        autoSize = EncolherSemPartirPalavra(
                            minimo = 11.sp,
                            maximo = MaterialTheme.typography.titleSmall.fontSize,
                        ),
                    )
                }
                Spacer(Modifier.width(Espacos.s))
                SeloDisciplina(
                    cor = CoresDisciplina.de(resumo.cor),
                    icone = IconesDisciplina.de(resumo.icone),
                    tamanho = 32.dp,
                    modifier = Modifier.elementoCompartilhado(ChavesCompartilhadas.selo(resumo.id)),
                )
            }
            Spacer(Modifier.height(Espacos.m))
            // Finalizada, a situação vem logo acima da média, numa linha só dela: ao lado do número
            // ela não cabia inteira no card estreito. Só o nome, na cor dele, sem a pílula do detalhe
            resumo.situacao?.let { situacao ->
                Text(
                    text = nomeDaSituacao(situacao),
                    style = MaterialTheme.typography.labelMedium,
                    color = corDoStatus(situacao),
                )
            }
            // A média e, ao lado, o arco com o símbolo da situação, sempre na mesma linha: se os dois
            // não couberem (fonte grande, tela estreita), o número encolhe um pouco. Sem notas, o arco
            // sai e a linha guarda a altura dele, para o card não mudar de tamanho ao trocar o período
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TAMANHO_DO_ARCO),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Numa linha só: o texto encolhe até caber, em vez de quebrar ou ser cortado
                MediaAnimada(resumo.media, Modifier.weight(1f)) { media ->
                    if (media != null) {
                        Text(
                            text = formatarMedia(media),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = corStatus,
                            maxLines = 1,
                            softWrap = false,
                            autoSize = encolherAte(MaterialTheme.typography.headlineMedium.fontSize),
                        )
                    } else {
                        Text(
                            text = "Sem notas",
                            // Mesma altura de linha da média, para o card não mudar de tamanho
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            autoSize = encolherAte(16.sp),
                        )
                    }
                }
                AnimatedVisibility(visible = resumo.media != null) {
                    AnelDaMedia(
                        media = resumo.media,
                        mediaMinima = resumo.mediaMinima,
                        cor = corStatus,
                        modifier = Modifier
                            .padding(start = Espacos.s)
                            .size(TAMANHO_DO_ARCO),
                        espessura = 4.5.dp,
                    ) {
                        Crossfade(resumo.status, label = "iconeDoStatus") { status ->
                            Icon(
                                imageVector = iconeDoStatus(status),
                                contentDescription = descricaoDoStatus(status),
                                tint = corStatus,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(Espacos.s))
            Text(
                text = textoFaltas(resumo.faltas),
                style = MaterialTheme.typography.labelMedium,
                color = if (resumo.emRiscoPorFalta) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

private val TAMANHO_DO_ARCO = 44.dp

// Do tamanho do estilo para baixo, de 1 em 1sp. O piso de 12sp só chega com tela estreita e fonte
// grande juntas, e mesmo assim o número aparece inteiro
private fun encolherAte(tamanho: TextUnit) = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = tamanho, stepSize = 1.sp)

/**
 * Como o [TextAutoSize.StepBased], mas também encolhe quando a quebra de linha parte uma palavra no
 * meio: só vale quebrar entre palavras. Desce de meio em meio sp a partir do [maximo]; no [minimo],
 * fica com o que der (uma palavra enorme ainda pode partir ou ganhar reticências).
 */
private data class EncolherSemPartirPalavra(
    private val minimo: TextUnit,
    private val maximo: TextUnit,
) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(constraints: Constraints, text: AnnotatedString): TextUnit {
        var tamanho = maximo.value
        while (tamanho > minimo.value) {
            val layout = performLayout(constraints, text, tamanho.sp)
            if (!layout.passouDoEspaco() && !layout.partiuPalavra()) return tamanho.sp
            tamanho -= 0.5f
        }
        return minimo
    }

    // Linhas a mais que o maxLines viram reticências na última
    private fun TextLayoutResult.passouDoEspaco() =
        hasVisualOverflow || (lineCount > 0 && isLineEllipsized(lineCount - 1))

    // Letra dos dois lados de uma quebra: a palavra foi partida. Espaço, hífen ou barra, não
    private fun TextLayoutResult.partiuPalavra(): Boolean {
        val texto = layoutInput.text.text
        return (0 until lineCount - 1).any { linha ->
            val fim = getLineEnd(linha)
            fim in 1 until texto.length && texto[fim - 1].isLetterOrDigit() && texto[fim].isLetterOrDigit()
        }
    }
}

/**
 * Troca a média rolando, como um contador: o número novo entra por baixo empurrando o antigo para
 * cima quando a média sobe, e o contrário quando desce. A caixa corta o que passa das bordas, então
 * os dois nunca aparecem sobrepostos. Só anima quando o valor muda com a tela aberta (trocar o
 * período); ao abrir a tela, já aparece no lugar. O [conteudo] desenha o número (ou o "sem nota").
 */
@Composable
internal fun MediaAnimada(
    media: Double?,
    modifier: Modifier = Modifier,
    conteudo: @Composable (media: Double?) -> Unit,
) {
    AnimatedContent(
        targetState = media,
        modifier = modifier,
        transitionSpec = {
            val sentido = if ((targetState ?: 0.0) >= (initialState ?: 0.0)) 1 else -1
            (slideInVertically { altura -> sentido * altura } + fadeIn()) togetherWith
                (slideOutVertically { altura -> -sentido * altura } + fadeOut()) using
                SizeTransform(clip = true)
        },
        contentAlignment = Alignment.CenterStart,
        label = "media",
    ) { valor -> conteudo(valor) }
}

@Composable
internal fun corDoStatus(status: StatusDisciplina): Color = when (status) {
    StatusDisciplina.APROVADO -> BoletimTheme.coresExtras.sucesso
    StatusDisciplina.ATENCAO -> BoletimTheme.coresExtras.atencao
    StatusDisciplina.ABAIXO -> MaterialTheme.colorScheme.error
    StatusDisciplina.REPROVADO -> MaterialTheme.colorScheme.error
    StatusDisciplina.SEM_NOTA -> MaterialTheme.colorScheme.onSurfaceVariant
}

// A cor nunca vem sozinha: o ícone diz a mesma coisa para quem não distingue as cores
private fun iconeDoStatus(status: StatusDisciplina): ImageVector = when (status) {
    StatusDisciplina.APROVADO -> Icons.Rounded.Check
    StatusDisciplina.ATENCAO -> Icons.Rounded.PriorityHigh
    StatusDisciplina.ABAIXO, StatusDisciplina.REPROVADO -> Icons.Rounded.ArrowDownward
    StatusDisciplina.SEM_NOTA -> Icons.Rounded.HourglassEmpty
}

private fun descricaoDoStatus(status: StatusDisciplina): String = when (status) {
    StatusDisciplina.APROVADO -> "Acima da mínima, com folga"
    StatusDisciplina.ATENCAO -> "Perto da mínima"
    StatusDisciplina.ABAIXO, StatusDisciplina.REPROVADO -> "Abaixo da mínima"
    StatusDisciplina.SEM_NOTA -> "Ainda sem notas"
}

/** A situação de uma disciplina finalizada: só aprovado ou reprovado. */
internal fun nomeDaSituacao(situacao: StatusDisciplina): String =
    if (situacao == StatusDisciplina.REPROVADO) "Reprovado" else "Aprovado"

/** "Sem faltas", "1 falta", "3 faltas". */
internal fun textoFaltas(faltas: Int): String = when (faltas) {
    0 -> "Sem faltas"
    1 -> "1 falta"
    else -> "$faltas faltas"
}

/**
 * A outra média, para a linha embaixo do número quando há recuperação (no período) ou final (no
 * ano) com nota. O número grande é sempre a média que vale; a linha mostra a que ficou de fora: a
 * do período ou do ano, quando a substituta foi maior ("Média do período" 5,5), ou a nota da
 * substituta, quando não foi ("Recuperação" 5,0). null sem substituta, ou sem média regular.
 */
internal fun outraMedia(regular: Double?, substituta: Substituta?, periodoId: Long?): Pair<String, Double>? = when {
    substituta == null -> null
    !substituta.substituiu -> substituta.tipo.displayname to substituta.nota
    regular == null -> null
    else -> "Média do ${if (periodoId == null) "ano" else "período"}" to regular
}

/**
 * A linha da [outraMedia], menor e apagada embaixo da média. O rótulo pode encurtar com reticências,
 * o número nunca: se faltar largura, o que não pode sumir é justamente o valor.
 */
@Composable
internal fun LinhaDaOutraMedia(rotulo: String, media: Double, modifier: Modifier = Modifier) {
    Row(modifier) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = " ${formatarMedia(media)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** O que o seletor de cima está mostrando: um período, ou o ano letivo inteiro (opção "Ano letivo"). */
internal fun nomeDoFiltro(periodoId: Long?): String =
    if (periodoId == null) "ano letivo" else "período"

// 1 ou 2 casas (7,0 · 7,5 · 6,96), pelo mediaEmTexto do domínio, que as notificações também usam
internal fun formatarMedia(media: Double?): String = media?.let(::mediaEmTexto) ?: "—"
