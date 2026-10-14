package app.jammes.boletim.presentation.ui.disciplina

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.EdicaoDisciplina
import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.model.intervalo
import app.jammes.boletim.domain.usecase.CalcularFrequencia
import app.jammes.boletim.presentation.ui.anoletivo.BarraDeSalvar
import app.jammes.boletim.presentation.ui.anoletivo.Cartao
import app.jammes.boletim.presentation.ui.anoletivo.ConfirmarDescarte
import app.jammes.boletim.presentation.ui.anoletivo.DatasPeriodo
import app.jammes.boletim.presentation.ui.anoletivo.Divisoria
import app.jammes.boletim.presentation.ui.anoletivo.FormularioAnoLetivo
import app.jammes.boletim.presentation.ui.anoletivo.LinhaQueAbre
import app.jammes.boletim.presentation.ui.anoletivo.MolduraAjustes
import app.jammes.boletim.presentation.ui.anoletivo.Salvamento
import app.jammes.boletim.presentation.ui.anoletivo.TituloDaSecao
import app.jammes.boletim.presentation.ui.anoletivo.dataCurta
import app.jammes.boletim.presentation.ui.anoletivo.faltasPermitidas
import app.jammes.boletim.presentation.ui.anoletivo.resumoDasDatas
import app.jammes.boletim.presentation.ui.anoletivo.resumoDaEscolha
import app.jammes.boletim.presentation.ui.boletim.formatarMedia
import app.jammes.boletim.presentation.ui.components.CarregandoDiscreto
import app.jammes.boletim.presentation.ui.components.EstadoVazio
import app.jammes.boletim.presentation.ui.components.SeloDisciplina
import app.jammes.boletim.presentation.ui.components.SeletorDeCor
import app.jammes.boletim.presentation.ui.components.SeletorDeIcone
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import app.jammes.boletim.presentation.ui.theme.Espacos
import app.jammes.boletim.presentation.ui.theme.IconesDisciplina
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * A edição da disciplina, aberta pelo Editar do detalhe, no mesmo painel dos ajustes do ano: a capa
 * com o nome e o professor (editados num sheet), a cor e o ícone (escolhidos ali mesmo, com
 * "Redefinir" para voltar aos da matéria), o total de aulas e o período, e as regras de média
 * próprias da disciplina, que valem no lugar das do ano (cada um no seu sheet). Tudo vai para uma
 * cópia, e a barra de baixo, que só aparece quando algo mudou, grava tudo junto.
 */
@Composable
fun EditarDisciplinaScreen(
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditarDisciplinaViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val salvamento by viewModel.salvamento.collectAsStateWithLifecycle()

    when (val s = state) {
        EdicaoDisciplinaUiState.Carregando ->
            MolduraAjustes(onVoltar = onVoltar, modifier = modifier, titulo = TITULO) { padding ->
                CarregandoDiscreto(Modifier.fillMaxSize().padding(padding))
            }

        EdicaoDisciplinaUiState.NaoEncontrada ->
            MolduraAjustes(onVoltar = onVoltar, modifier = modifier, titulo = TITULO) { padding ->
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    EstadoVazio(
                        icone = Icons.Outlined.SearchOff,
                        titulo = "Disciplina não encontrada",
                        mensagem = "Ela pode ter saído do ano letivo.",
                    )
                }
            }

        is EdicaoDisciplinaUiState.Sucesso -> EditorDaDisciplina(
            edicao = s.edicao,
            salvamento = salvamento,
            onSalvar = viewModel::salvar,
            onVoltar = onVoltar,
            modifier = modifier,
        )
    }
}

private const val TITULO = "Editar disciplina"

/** O sheet aberto por cima do painel. Enum é Serializable: volta aberto depois de girar a tela. */
private enum class Editor { NOME, TOTAL_DE_AULAS, PERIODO, REGRAS }

@Composable
private fun EditorDaDisciplina(
    edicao: EdicaoDisciplina,
    salvamento: Salvamento,
    onSalvar: (FormularioDisciplina) -> Unit,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gravado = remember(edicao) { FormularioDisciplina.de(edicao.disciplina, edicao.regraPropria) }
    // As do ano letivo: as que valem quando a disciplina não tem as próprias
    val regrasDoAno = remember(edicao) { regrasDe(edicao.regraDoAno) }
    // rememberSaveable: o que já foi editado sobrevive a girar a tela
    var form by rememberSaveable { mutableStateOf(gravado) }
    // O sheet aberto; null = nenhum. Fica aqui, fora da lista, como nos ajustes do ano
    var editor by rememberSaveable { mutableStateOf<Editor?>(null) }
    var confirmandoDescarte by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptico = LocalHapticFeedback.current

    // Refeitas só quando o formulário muda, e não a cada recomposição
    val alteracoes = remember(form, gravado) { form.alteracoes(gravado, regrasDoAno) }
    val alterado = alteracoes.total > 0
    // A regra com o que está na tela: o limite de faltas acompanha a frequência própria, se houver
    val regra = remember(form.regras, edicao) { form.regraEfetiva(edicao) }
    // Do primeiro ao último dia dos períodos: o "ano letivo inteiro" do período da disciplina
    val doAno = remember(edicao) {
        edicao.anoLetivo?.periodo?.intervalo()?.let { DatasPeriodo(it.start, it.endInclusive) }
    }
    val ano = edicao.anoLetivo?.ano
    val materia = edicao.materia

    // Com alterações, sair pergunta antes: pela seta da barra e pelo voltar do sistema
    fun voltar() {
        if (alterado) confirmandoDescarte = true else onVoltar()
    }
    BackHandler(enabled = alterado, onBack = ::voltar)

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
        if (salvamento == Salvamento.Salvo) {
            haptico.performHapticFeedback(HapticFeedbackType.Confirm)
            onVoltar()
        }
    }

    MolduraAjustes(
        onVoltar = ::voltar,
        modifier = modifier,
        titulo = TITULO,
        snackbarHostState = snackbarHostState,
        barraDeBaixo = {
            BarraDeSalvar(
                alteracoes = alteracoes.total,
                problema = form.problema(),
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
            item(key = "disciplina") {
                SecaoDisciplina(
                    nome = form.nome,
                    professor = form.professor,
                    cor = form.cor,
                    icone = form.icone,
                    alterada = alteracoes.disciplina > 0,
                    onEditar = { editor = Editor.NOME },
                )
            }
            item(key = "cor") {
                SecaoCor(
                    cor = form.cor,
                    daMateria = materia?.cor,
                    alterada = alteracoes.cor > 0,
                    onEscolher = { form = form.copy(cor = it) },
                )
            }
            item(key = "icone") {
                SecaoIcone(
                    icone = form.icone,
                    cor = form.cor,
                    daMateria = materia?.icone,
                    alterada = alteracoes.icone > 0,
                    onEscolher = { form = form.copy(icone = it) },
                )
            }
            item(key = "aulas") {
                SecaoAulas(
                    totalAulas = form.totalAulas,
                    periodo = form.periodo,
                    doAno = doAno,
                    ano = ano,
                    regra = regra,
                    alterada = alteracoes.aulas > 0,
                    onEditarTotal = { editor = Editor.TOTAL_DE_AULAS },
                    onEditarPeriodo = { editor = Editor.PERIODO },
                )
            }
            item(key = "regras") {
                SecaoRegras(
                    regras = form.regras ?: regrasDoAno,
                    proprias = form.regras != null,
                    alterada = alteracoes.regras > 0,
                    onEditar = { editor = Editor.REGRAS },
                    onRedefinir = { form = form.copy(regras = null) },
                )
            }
        }
    }

    // Os sheets mudam o formulário, como o resto do painel; quem grava é a barra de baixo
    when (editor) {
        Editor.NOME -> NomeDaDisciplinaBottomSheet(
            nome = form.nome,
            professor = form.professor,
            onAplicar = { nome, professor -> form = form.copy(nome = nome, professor = professor) },
            onDismiss = { editor = null },
        )
        Editor.TOTAL_DE_AULAS -> TotalDeAulasBottomSheet(
            totalAulas = form.totalAulas,
            regra = regra,
            onAplicar = { form = form.copy(totalAulas = it) },
            onDismiss = { editor = null },
        )
        Editor.PERIODO -> PeriodoDaDisciplinaBottomSheet(
            periodo = form.periodo,
            doAno = doAno,
            ano = ano,
            onAplicar = { form = form.copy(periodo = it) },
            onDismiss = { editor = null },
        )
        Editor.REGRAS -> RegrasDaDisciplinaBottomSheet(
            regras = form.regras ?: regrasDoAno,
            doAno = regrasDoAno,
            disciplina = form.nome.trim(),
            // Com o total de aulas, a frequência já diz quantas faltas cabem
            dicaDaFrequencia = { dicaDaFrequencia(it, form.totalAulas, edicao.regraDoAno) },
            onAplicar = { form = form.copy(regras = it) },
            onDismiss = { editor = null },
        )
        null -> Unit
    }

    if (confirmandoDescarte) {
        ConfirmarDescarte(
            onDescartar = {
                confirmandoDescarte = false
                onVoltar()
            },
            onContinuar = { confirmandoDescarte = false },
        )
    }
}

// Seções ------------------------------------------------------------------------------------

/**
 * A capa da disciplina, tingida como no detalhe: o selo, o nome e o professor, já com a cor e o
 * ícone escolhidos embaixo. Tocar abre o sheet do nome e do professor.
 */
@Composable
private fun SecaoDisciplina(
    nome: String,
    professor: String,
    cor: Int,
    icone: IconeMateria,
    alterada: Boolean,
    onEditar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A capa acompanha a cor escolhida embaixo, sem pular de uma para a outra
    val corDaDisciplina by animateColorAsState(CoresDisciplina.de(cor), label = "corDaCapa")

    Column(modifier) {
        TituloDaSecao("Disciplina", alterada = alterada)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = corDaDisciplina.copy(alpha = 0.08f)
                    .compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest),
            ),
            border = BorderStroke(1.dp, corDaDisciplina.copy(alpha = 0.35f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = "Editar o nome e o professor", onClick = onEditar)
                    .padding(start = 20.dp, end = Espacos.m, top = 20.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // O ícone novo entra num fade, no lugar do outro
                Crossfade(targetState = icone, label = "iconeDaCapa") { atual ->
                    SeloDisciplina(
                        cor = corDaDisciplina,
                        icone = IconesDisciplina.de(atual),
                        tamanho = 44.dp,
                    )
                }
                Spacer(Modifier.width(Espacos.m))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = nome.trim(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = professor.trim().ifEmpty { "Sem professor" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(Espacos.s))
                // Parece um botão, mas quem recebe o toque é a linha inteira, como no ano dos ajustes
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
        }
    }
}

@Composable
private fun SecaoCor(
    cor: Int,
    daMateria: Int?, // a cor da matéria; null quando ela não foi encontrada
    alterada: Boolean,
    onEscolher: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        TituloDaSecao("Cor", alterada = alterada) {
            Redefinir(
                visivel = daMateria != null && cor != daMateria,
                rotuloDoClique = "Voltar à cor da matéria",
                onClick = { daMateria?.let(onEscolher) },
            )
        }
        Cartao {
            SeletorDeCor(
                selecionada = cor,
                onSelecionar = onEscolher,
                contentPadding = PaddingValues(Espacos.l),
            )
        }
    }
}

@Composable
private fun SecaoIcone(
    icone: IconeMateria,
    cor: Int,
    daMateria: IconeMateria?, // o ícone da matéria; null quando ela não foi encontrada
    alterada: Boolean,
    onEscolher: (IconeMateria) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        TituloDaSecao("Ícone", alterada = alterada) {
            Redefinir(
                visivel = daMateria != null && icone != daMateria,
                rotuloDoClique = "Voltar ao ícone da matéria",
                onClick = { daMateria?.let(onEscolher) },
            )
        }
        Cartao {
            SeletorDeIcone(
                selecionado = icone,
                // Na cor escolhida em cima: cada opção já aparece como o selo vai ficar
                cor = CoresDisciplina.de(cor),
                onSelecionar = onEscolher,
                contentPadding = PaddingValues(Espacos.l),
            )
        }
    }
}

/** O total de aulas e o período da disciplina, cada um abrindo o seu sheet. */
@Composable
private fun SecaoAulas(
    totalAulas: Int?,
    periodo: DatasPeriodo?,
    doAno: DatasPeriodo?,
    ano: Int?,
    regra: RegraAvaliacaoDomain,
    alterada: Boolean,
    onEditarTotal: () -> Unit,
    onEditarPeriodo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        TituloDaSecao("Aulas", alterada = alterada)
        Cartao {
            LinhaQueAbre(
                titulo = "Total de aulas",
                resumo = resumoDoTotalDeAulas(totalAulas, regra),
                rotuloDoClique = "Mudar o total de aulas",
                onClick = onEditarTotal,
            ) {
                SeloDisciplina(
                    cor = MaterialTheme.colorScheme.primary,
                    icone = Icons.Outlined.Numbers,
                    tamanho = TAMANHO_DO_SELO,
                )
            }
            Divisoria()
            LinhaQueAbre(
                titulo = "Período",
                resumo = resumoDoPeriodo(periodo, doAno, ano),
                aviso = avisoDoPeriodo(periodo, doAno, ano),
                rotuloDoClique = "Escolher o período",
                onClick = onEditarPeriodo,
            ) {
                SeloDisciplina(
                    cor = MaterialTheme.colorScheme.primary,
                    icone = Icons.Outlined.DateRange,
                    tamanho = TAMANHO_DO_SELO,
                )
            }
        }
    }
}

/**
 * As regras de média que valem para a disciplina: as do ano letivo ou as próprias dela, que valem no
 * lugar das do ano. Tocar abre o sheet com as quatro; o "Redefinir" volta a seguir as do ano.
 */
@Composable
private fun SecaoRegras(
    regras: FormularioAnoLetivo.Regras, // as que valem: as próprias, ou as do ano
    proprias: Boolean,
    alterada: Boolean,
    onEditar: () -> Unit,
    onRedefinir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        TituloDaSecao("Regras de média", alterada = alterada) {
            Redefinir(
                visivel = proprias,
                rotuloDoClique = "Voltar às regras do ano letivo",
                onClick = onRedefinir,
            )
        }
        Cartao {
            LinhaQueAbre(
                titulo = tituloDasRegras(proprias),
                resumo = resumoDasRegras(regras),
                rotuloDoClique = "Mudar as regras de média",
                onClick = onEditar,
            ) {
                SeloDisciplina(
                    cor = MaterialTheme.colorScheme.primary,
                    icone = Icons.Outlined.Functions,
                    tamanho = TAMANHO_DO_SELO,
                )
            }
        }
    }
}

/**
 * O "Redefinir" na ponta do título da seção: volta a cor ou o ícone ao da matéria (e as regras às do
 * ano), e só aparece quando eles são diferentes. Tem a altura do título, para a linha não crescer quando ele aparece;
 * o toque pega além do texto, porque o Compose estende os alvos pequenos até o tamanho mínimo.
 */
@Composable
private fun Redefinir(
    visivel: Boolean,
    rotuloDoClique: String, // o que o leitor de tela diz que o toque faz
    onClick: () -> Unit,
) {
    val haptico = LocalHapticFeedback.current
    AnimatedVisibility(visible = visivel, enter = fadeIn(), exit = fadeOut()) {
        Text(
            text = "Redefinir",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClickLabel = rotuloDoClique, role = Role.Button) {
                    // O mesmo "tique" de escolher no seletor
                    haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onClick()
                }
                .padding(horizontal = Espacos.s),
        )
    }
}

private val TAMANHO_DO_SELO = 36.dp

// Textos ------------------------------------------------------------------------------------

/** "80 aulas · até 24 faltas"; sem o total, diz que as faltas ficam sem limite. */
internal fun resumoDoTotalDeAulas(totalAulas: Int?, regra: RegraAvaliacaoDomain): String {
    if (totalAulas == null) return "Não informado · faltas sem limite"
    val aulas = if (totalAulas == 1) "1 aula" else "$totalAulas aulas"
    return "$aulas · ${limiteDeFaltas(totalAulas, regra)}"
}

/** O que o sheet diz embaixo do campo, enquanto o total é digitado. */
internal fun dicaDoTotalDeAulas(digitado: TotalDigitado, regra: RegraAvaliacaoDomain): String = when (digitado) {
    TotalDigitado.Vazio -> "Sem o total, as faltas ficam sem limite"
    TotalDigitado.Invalido -> "Pelo menos 1 aula"
    is TotalDigitado.Valido ->
        limiteDeFaltas(digitado.aulas, regra).replaceFirstChar { it.uppercase() } +
            ", com a frequência mínima de ${formatarNumero(regra.frequenciaMinima)}%"
}

/**
 * A dica embaixo da frequência mínima, no sheet das regras: com o total de aulas, quantas faltas cabem
 * ("Até 24 faltas"); sem ele, a porcentagem, como nos ajustes do ano. A conta usa a [base] com a
 * frequência que está na tela.
 */
internal fun dicaDaFrequencia(frequenciaMinima: Double, totalAulas: Int?, base: RegraAvaliacaoDomain): String =
    if (totalAulas == null) {
        faltasPermitidas(frequenciaMinima)
    } else {
        limiteDeFaltas(totalAulas, base.copy(frequenciaMinima = frequenciaMinima)).replaceFirstChar { it.uppercase() }
    }

/** De onde vêm as regras que valem para a disciplina. */
internal fun tituloDasRegras(proprias: Boolean): String =
    if (proprias) "Próprias desta disciplina" else "Geral do ano letivo"

/** "Mínima 8,0 · frequência 70%" e, embaixo, "Média simples · arredonda para cima". */
internal fun resumoDasRegras(regras: FormularioAnoLetivo.Regras): String {
    val calculo = when (regras.tipoMedia) {
        TipoMedia.SIMPLES -> "Média simples"
        TipoMedia.PONDERADA -> "Média ponderada"
        TipoMedia.SOMA -> "Soma das notas"
    }
    val arredondamento = when (regras.arredondamento) {
        TipoArredondamento.NENHUM -> "sem arredondar"
        TipoArredondamento.MEIO_PONTO -> "arredonda para meio ponto"
        TipoArredondamento.INTEIRO -> "arredonda para o inteiro"
        TipoArredondamento.CIMA -> "arredonda para cima"
        TipoArredondamento.BAIXO -> "arredonda para baixo"
    }
    return "Mínima ${formatarMedia(regras.mediaMinima)} · frequência ${formatarNumero(regras.frequenciaMinima)}%\n" +
        "$calculo · $arredondamento"
}

/** "até 24 faltas", "até 1 falta", "nenhuma falta": a mesma conta do limite no detalhe e no Boletim. */
private fun limiteDeFaltas(totalAulas: Int, regra: RegraAvaliacaoDomain): String =
    when (val limite = CalcularFrequencia.limiteFaltas(totalAulas, regra) ?: 0) {
        0 -> "nenhuma falta"
        1 -> "até 1 falta"
        else -> "até $limite faltas"
    }

/** "Ano letivo inteiro · 1 fev – 30 nov", ou as datas próprias: "1 fev – 30 jun · 21 semanas". */
internal fun resumoDoPeriodo(periodo: DatasPeriodo?, doAno: DatasPeriodo?, ano: Int?): String = when {
    periodo != null -> resumoDasDatas(periodo, ano)
    doAno != null -> "Ano letivo inteiro · ${dataCurta(doAno.inicio, ano)} – ${dataCurta(doAno.fim, ano)}"
    else -> "Ano letivo inteiro"
}

/** O que está marcado no calendário do período, com o ano inteiro dito como tal. */
internal fun resumoDaEscolhaDoPeriodo(inicio: LocalDate?, fim: LocalDate?, doAno: DatasPeriodo?, ano: Int?): String =
    if (doAno != null && inicio == doAno.inicio && fim == doAno.fim) {
        resumoDoPeriodo(null, doAno, ano)
    } else {
        resumoDaEscolha(inicio, fim, ano)
    }

/**
 * Quando o período gravado passa das datas do ano letivo (o ano mudou depois): o calendário não
 * deixa escolher assim, então só acontece com um período antigo. null quando ele cabe no ano.
 */
internal fun avisoDoPeriodo(periodo: DatasPeriodo?, doAno: DatasPeriodo?, ano: Int?): String? {
    if (periodo == null || doAno == null) return null
    if (periodo.inicio >= doAno.inicio && periodo.fim <= doAno.fim) return null
    return "Fora do ano letivo, que vai de ${dataCurta(doAno.inicio, ano)} a ${dataCurta(doAno.fim, ano)}"
}
