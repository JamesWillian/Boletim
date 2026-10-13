package app.jammes.boletim.presentation.ui.disciplina

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.StatusDisciplina
import app.jammes.boletim.domain.model.TipoAvaliacao
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.domain.model.intervalo
import app.jammes.boletim.domain.model.ocupandoAVaga
import app.jammes.boletim.presentation.ui.anoletivo.LinhaQueAbre
import app.jammes.boletim.presentation.ui.anoletivo.nomeDoPeriodo
import app.jammes.boletim.presentation.ui.boletim.corDoStatus
import app.jammes.boletim.presentation.ui.components.OpcaoDoSeletor
import app.jammes.boletim.presentation.ui.components.Pilula
import app.jammes.boletim.presentation.ui.components.SeloDisciplina
import app.jammes.boletim.presentation.ui.components.SeletorEmPilula
import app.jammes.boletim.presentation.ui.components.opcaoDoPeriodo
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import app.jammes.boletim.presentation.ui.theme.Espacos
import app.jammes.boletim.presentation.ui.theme.IconesDisciplina
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Formulário de avaliação: cria quando o id é 0, senão edita e permite excluir. Só monta a
 * avaliação; quem grava e exclui é a tela, pelos callbacks.
 *
 * A nota fica no meio, grande, e o próprio card vira o campo ao ser tocado. Vazia, a avaliação
 * fica agendada: o card fica tracejado e o botão diz "Agendar avaliação". Uma avaliação depois de
 * hoje só pode ficar agendada, então a nota não abre.
 *
 * O cabeçalho e o botão ficam fixos; o meio rola. A data e "Mais opções" abrem ali mesmo, uma de
 * cada vez. O calendário só mostra os dias que dá para escolher, e um toque no dia já escolhe.
 * "Mais opções" tem o tipo, quanto a avaliação vale, o peso (só na média ponderada) e o período
 * (só com [escolherPeriodo], no ano letivo inteiro, onde o período também acompanha a data).
 *
 * A recuperação e a final tomam o lugar da média (do período e do ano), então não têm peso. Só
 * cabe uma recuperação por período e uma final no ano: [lancadas] diz se a vaga já está ocupada.
 * A final é do ano e não tem período, então só aparece como opção no ano letivo inteiro.
 *
 * Numa avaliação nova, "Outra" grava e deixa o formulário pronto para a próxima, com a mesma
 * data, período, tipo, valor e peso: é o caminho para lançar as notas de várias provas seguidas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvaliacaoBottomSheet(
    avaliacao: AvaliacaoDomain,
    disciplina: DisciplinaDomain,
    regra: RegraAvaliacaoDomain,
    periodos: List<PeriodoDomain>, // todos os do ano: o cabeçalho diz em qual a avaliação fica
    lancadas: List<AvaliacaoDomain>, // as da disciplina na tela, para a vaga da recuperação e da final
    escolherPeriodo: Boolean,
    tipoPeriodo: TipoPeriodo,
    onDismiss: () -> Unit,
    onSalvar: (AvaliacaoDomain) -> Unit,
    onExcluir: () -> Unit,
) {
    val nova = avaliacao.id == 0L
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptico = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val hoje = remember { LocalDate.now() }

    // rememberSaveable: o que já foi digitado sobrevive a girar a tela
    var formulario by rememberSaveable { mutableStateOf(FormularioAvaliacao.de(avaliacao)) }
    // O peso só muda a conta da normal na média ponderada; fora dela ele fica escondido e não muda
    val comPeso = regra.tipoMedia == TipoMedia.PONDERADA && formulario.tipo == TipoAvaliacao.NORMAL
    // A final é do ano: num período ela não é opção (a não ser que seja a que está sendo editada)
    val tipos = remember(escolherPeriodo) {
        if (escolherPeriodo || avaliacao.tipo == TipoAvaliacao.FINAL) TipoAvaliacao.entries
        else TipoAvaliacao.entries - TipoAvaliacao.FINAL
    }
    // A recuperação que já está no período, ou a final que já está no ano, se houver
    val ocupante = lancadas.ocupandoAVaga(formulario.tipo, formulario.periodoId, alemDe = avaliacao.id)
    val periodosDaData = if (escolherPeriodo) periodos else emptyList()
    var secaoAberta by rememberSaveable { mutableStateOf<Secao?>(null) }
    // Quanto a avaliação vale: "Outro" fica escolhido quando a nota máxima não é uma das prontas
    var outraMaxima by rememberSaveable { mutableStateOf(!formulario.notaMaximaComum) }
    var confirmandoExclusao by rememberSaveable { mutableStateOf(false) }
    // O nome e a data vazios só ficam vermelhos depois de tentar gravar: ao abrir, não é erro
    var tentouGravar by rememberSaveable { mutableStateOf(false) }
    // "Nota lançada: Prova 2", no cabeçalho por um instante depois de "Outra"
    var confirmacao by remember { mutableStateOf<String?>(null) }

    val focoNome = remember { FocusRequester() }
    val focoNota = remember { FocusRequester() }
    val rolagem = rememberScrollState()

    // A data e "Mais opções" abrem embaixo da nota: enquanto a seção cresce, a rolagem vai junto
    // até o fim, senão o que acabou de abrir ficaria escondido atrás do botão
    LaunchedEffect(secaoAberta) {
        if (secaoAberta != null) {
            withTimeoutOrNull(DURACAO_PARA_ACOMPANHAR) {
                snapshotFlow { rolagem.maxValue }.collect { rolagem.scrollTo(it) }
            }
        }
    }
    LaunchedEffect(confirmacao) {
        if (confirmacao != null) {
            delay(DURACAO_DA_CONFIRMACAO)
            confirmacao = null
        }
    }

    // Uma seção aberta por vez; o teclado sai da frente do que abriu
    fun abrir(secao: Secao?) {
        focusManager.clearFocus()
        secaoAberta = secao
    }

    // Esconde com a animação e só então avisa a tela, que tira o sheet da composição
    fun fechar(depois: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                depois()
                onDismiss()
            }
        }
    }

    /**
     * O botão nunca fica desligado: com algo errado, ele leva até o campo e diz o que é. Com
     * [eOutra], grava e recomeça o formulário para a próxima avaliação, sem fechar.
     */
    fun gravar(eOutra: Boolean = false) {
        val pronta = formulario.paraGravar(avaliacao, comPeso, hoje, vagaOcupada = ocupante != null)
        if (pronta == null) {
            tentouGravar = true
            haptico.performHapticFeedback(HapticFeedbackType.Reject)
            when (formulario.problema(comPeso, hoje, vagaOcupada = ocupante != null)) {
                FormularioAvaliacao.Problema.SEM_NOME -> focoNome.requestFocus()
                FormularioAvaliacao.Problema.NOTA_FORA_DA_ESCALA,
                FormularioAvaliacao.Problema.NOTA_NO_FUTURO -> focoNota.requestFocus()
                FormularioAvaliacao.Problema.SEM_DATA -> abrir(Secao.DATA)
                FormularioAvaliacao.Problema.VAGA_OCUPADA,
                FormularioAvaliacao.Problema.NOTA_MAXIMA,
                FormularioAvaliacao.Problema.PESO -> abrir(Secao.OPCOES)
                null -> Unit
            }
            return
        }
        onSalvar(pronta)
        if (!eOutra) {
            fechar()
            return
        }
        confirmacao = if (pronta.nota == null) "Agendada: ${pronta.nome}" else "Nota lançada: ${pronta.nome}"
        formulario = formulario.paraAProxima(periodosDaData)
        tentouGravar = false
        secaoAberta = null
        scope.launch { rolagem.animateScrollTo(0) }
        focoNome.requestFocus()
    }

    val periodo = periodos.find { it.id == formulario.periodoId }
    // No ano letivo inteiro dá para escolher qualquer dia dos períodos, e o período vai junto com a
    // data; num período, só os dias dele. A final pode cair depois do último período.
    val intervalo = when {
        formulario.tipo == TipoAvaliacao.FINAL -> intervaloDaFinal(periodos)
        escolherPeriodo -> periodos.intervalo()
        else -> periodo?.let { it.dataInicio..it.dataFim }
    }
    val opcoesDePeriodo = remember(periodos, tipoPeriodo) { periodos.map { opcaoDoPeriodo(it.periodo, tipoPeriodo) } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        CabecalhoDoFormulario(
            titulo = if (nova) "Nova avaliação" else "Editar avaliação",
            subtitulo = listOfNotNull(
                disciplina.nome,
                // A final é do ano inteiro, não de um período
                if (formulario.tipo == TipoAvaliacao.FINAL) "Ano letivo"
                else periodo?.let { nomeDoPeriodo(it.periodo, tipoPeriodo) },
            ).joinToString(" · "),
            confirmacao = confirmacao,
            cor = CoresDisciplina.de(disciplina.cor),
            icone = IconesDisciplina.de(disciplina.icone),
            onExcluir = if (nova) null else ({ confirmandoExclusao = true }),
            onFechar = { fechar() },
        )
        // Com o meio rolado por baixo, uma linha separa o cabeçalho fixo. Só a camada muda
        // de transparência: rolar não recompõe nada.
        HorizontalDivider(
            modifier = Modifier.graphicsLayer { alpha = if (rolagem.value > 0) 1f else 0f },
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rolagem)
                .padding(horizontal = MARGEM)
                .padding(top = Espacos.s, bottom = Espacos.xs),
            verticalArrangement = Arrangement.spacedBy(Espacos.m),
        ) {
            CampoDoNome(
                nome = formulario.nome,
                erro = tentouGravar && formulario.nome.isBlank(),
                foco = focoNome,
                onNome = { formulario = formulario.copy(nome = it) },
                onProximo = {
                    // Do nome direto para a nota, a não ser que ela esteja fechada (avaliação no futuro)
                    if (formulario.notaBloqueada(hoje)) focusManager.clearFocus() else focoNota.requestFocus()
                },
            )

            val notaValor = formulario.notaValor
            val maxima = formulario.notaMaximaValor
            val aviso = avisoDaNota(
                formulario = formulario,
                hoje = hoje,
                soma = regra.tipoMedia == TipoMedia.SOMA,
                doPeriodo = periodo?.let { doPeriodo(it, tipoPeriodo) },
            )
            CartaoDaNota(
                nota = formulario.nota,
                maxima = maxima?.let(::formatarNumero) ?: formulario.notaMaxima,
                corDoNumero = when {
                    aviso.erro -> MaterialTheme.colorScheme.error
                    notaValor != null && maxima != null -> corDaNota(notaValor, maxima, regra, formulario.tipo)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                agendada = formulario.agendada,
                bloqueada = formulario.notaBloqueada(hoje),
                status = if (notaValor != null && maxima != null) {
                    statusDaNota(notaValor, maxima, regra, formulario.tipo)
                } else {
                    null
                },
                aviso = aviso,
                foco = focoNota,
                onNota = { formulario = formulario.copy(nota = it) },
                onConcluir = { focusManager.clearFocus() },
                // Ao sair do campo, "9.5" vira "9,5", como o app mostra os números
                onFocoPerdido = { formulario = formulario.notaArrumada() },
            )

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                val dataAberta = secaoAberta == Secao.DATA
                LinhaQueAbre(
                    titulo = "Data",
                    rotuloDoClique = if (dataAberta) "Fechar o calendário" else "Escolher a data",
                    onClick = { abrir(if (dataAberta) null else Secao.DATA) },
                    resumo = resumoDaData(formulario.data, hoje),
                    aviso = if (tentouGravar && formulario.data == null) "A avaliação precisa de uma data" else null,
                    fim = { SetaDaSecao(aberta = dataAberta) },
                ) { SeloDoIcone(Icons.Outlined.CalendarMonth) }
                AnimatedVisibility(visible = dataAberta) {
                    CalendarioNoLugar(
                        data = formulario.data,
                        dias = formulario.diasDoCalendario(intervalo, hoje),
                        hoje = hoje,
                        aviso = if (formulario.agendada) null else "Com nota lançada, a data vai até hoje.",
                        onEscolher = { dia ->
                            formulario = formulario.comData(dia, periodosDaData)
                            abrir(null)
                        },
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val opcoesAbertas = secaoAberta == Secao.OPCOES
                LinhaQueAbre(
                    titulo = "Mais opções",
                    rotuloDoClique = if (opcoesAbertas) "Fechar mais opções" else "Abrir mais opções",
                    onClick = { abrir(if (opcoesAbertas) null else Secao.OPCOES) },
                    resumo = formulario.resumoDasOpcoes(comPeso),
                    // Aberta, o erro já aparece no campo; fechada, a linha avisa
                    aviso = if (opcoesAbertas) null else formulario.avisoDasOpcoes(comPeso, vagaOcupada = ocupante != null),
                    fim = { SetaDaSecao(aberta = opcoesAbertas) },
                ) { SeloDoIcone(Icons.Outlined.Tune) }
                AnimatedVisibility(visible = opcoesAbertas) {
                    MaisOpcoes(
                        tipo = formulario.tipo,
                        tipos = tipos,
                        erroDoTipo = ocupante?.let { textoDaVagaOcupada(it, periodo?.let { p -> noPeriodo(p, tipoPeriodo) }) },
                        dicaDoTipo = if (TipoAvaliacao.FINAL in tipos) null else "Para lançar a final, escolha Ano letivo no topo.",
                        notaMaxima = formulario.notaMaxima,
                        notaMaximaValor = formulario.notaMaximaValor,
                        outraMaxima = outraMaxima,
                        peso = if (comPeso) formulario.pesoValor ?: 1.0 else null,
                        periodoSelecionado = periodos.indexOfFirst { it.id == formulario.periodoId },
                        // A final não tem período: o seletor sai enquanto ela estiver escolhida
                        opcoesDePeriodo = if (escolherPeriodo && formulario.tipo != TipoAvaliacao.FINAL) {
                            opcoesDePeriodo
                        } else {
                            emptyList()
                        },
                        onTipo = { formulario = formulario.comTipo(it, periodosDaData) },
                        onMaximaPronta = { valor ->
                            outraMaxima = false
                            formulario = formulario.copy(notaMaxima = formatarNumero(valor))
                        },
                        onOutraMaxima = { outraMaxima = true },
                        onNotaMaxima = { formulario = formulario.copy(notaMaxima = it) },
                        onPassoNoPeso = { formulario = formulario.comPassoNoPeso(it) },
                        onPeriodo = { i -> formulario = formulario.comPeriodo(periodos[i]) },
                    )
                }
            }
        }

        // Fixo embaixo: com o teclado aberto, fica logo acima dele
        BarraDeGravar(
            acao = formulario.acao(avaliacao),
            comOutra = nova,
            onGravar = { gravar() },
            onGravarEOutra = { gravar(eOutra = true) },
        )
    }

    if (confirmandoExclusao) {
        AlertDialog(
            onDismissRequest = { confirmandoExclusao = false },
            title = { Text("Excluir avaliação?") },
            text = { Text("\"${avaliacao.nome}\" será apagada, e não dá para desfazer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoExclusao = false
                    // Exclui só depois de fechar: se a avaliação sumisse da lista antes,
                    // o sheet sairia da tela sem a animação
                    fechar(depois = onExcluir)
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoExclusao = false }) { Text("Cancelar") }
            }
        )
    }
}

/** As partes do formulário que abrem ali mesmo, uma de cada vez. */
private enum class Secao { DATA, OPCOES }

// A margem dos lados do sheet, a mesma dos outros formulários do app
private val MARGEM = 20.dp

// Um pouco mais que a animação de abrir uma seção (expandVertically, com mola)
private const val DURACAO_PARA_ACOMPANHAR = 600L

// Quanto o "Nota lançada: Prova 2" fica no cabeçalho depois de "Outra"
private const val DURACAO_DA_CONFIRMACAO = 2500L

/**
 * O selo da disciplina, o título e "Português · 4º Bimestre", com excluir (editando) e fechar. A
 * [confirmacao], quando há, toma o lugar do subtítulo por um instante.
 */
@Composable
private fun CabecalhoDoFormulario(
    titulo: String,
    subtitulo: String,
    confirmacao: String?,
    cor: Color,
    icone: ImageVector,
    onExcluir: (() -> Unit)?,
    onFechar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = MARGEM, end = Espacos.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeloDisciplina(cor = cor, icone = icone, tamanho = 40.dp)
        Spacer(Modifier.width(Espacos.m))
        Column(Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            AnimatedContent(
                targetState = confirmacao,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "subtitulo",
            ) { mensagem ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mensagem != null) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(Espacos.xs))
                    }
                    Text(
                        text = mensagem ?: subtitulo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (mensagem != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (onExcluir != null) {
            IconButton(onClick = onExcluir) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Excluir avaliação",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        IconButton(onClick = onFechar) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Fechar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CampoDoNome(
    nome: String,
    erro: Boolean,
    foco: FocusRequester,
    onNome: (String) -> Unit,
    onProximo: () -> Unit,
) {
    OutlinedTextField(
        value = nome,
        onValueChange = onNome,
        label = { Text("Nome") },
        placeholder = { Text("Prova 1, trabalho, seminário…") },
        isError = erro,
        supportingText = if (erro) {
            { Text("Dê um nome à avaliação") }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onNext = { onProximo() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(foco),
    )
}

/** O que a pílula da nota mostra: "Agendada", a situação perto da mínima, ou nada. */
private sealed interface PilulaDaNota {
    data object Agendada : PilulaDaNota
    data class DaSituacao(val status: StatusDisciplina) : PilulaDaNota
}

/**
 * A nota no meio do formulário: o número grande "de 10", a pílula e a frase do que ela faz na
 * média. O card inteiro é o campo: tocar nele abre o teclado numérico ali mesmo. [bloqueada]
 * (avaliação depois de hoje, sem nota), o toque só vibra, e a frase diz quando a nota abre.
 *
 * As mudanças de estado são animadas (borda, cor do número, pílula e frase), e passar de uma
 * situação para outra digitando (de "Perto da mínima" para "Com folga") dá um tique no dedo.
 */
@Composable
private fun CartaoDaNota(
    nota: String,
    maxima: String,
    corDoNumero: Color,
    agendada: Boolean,
    bloqueada: Boolean,
    status: StatusDisciplina?,
    aviso: AvisoDaNota,
    foco: FocusRequester,
    onNota: (String) -> Unit,
    onConcluir: () -> Unit,
    onFocoPerdido: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    val haptico = LocalHapticFeedback.current
    val teclado = LocalSoftwareKeyboardController.current
    var focada by remember { mutableStateOf(false) }

    // A borda conta o estado: azul digitando, vermelha com erro, tracejada sem nota. A cor e a
    // espessura são lidas só no desenho, então a animação não recompõe o card a cada quadro.
    val corDaBorda by animateColorAsState(
        targetValue = when {
            aviso.erro -> cores.error
            focada -> cores.primary
            agendada -> cores.outline
            else -> cores.outlineVariant
        },
        label = "corDaBorda",
    )
    val espessura by animateDpAsState(if (focada || aviso.erro) 2.dp else 1.dp, label = "espessuraDaBorda")
    val corAnimadaDoNumero by animateColorAsState(corDoNumero, label = "corDoNumero")
    // Sem textAlign: o campo já tem o tamanho do número, e numa linha só o campo mede o texto
    // com largura infinita, então centralizar empurrava o número para fora (sumia o "9" de "9,5")
    val estiloDoNumero = MaterialTheme.typography.displayMedium.copy(
        fontWeight = FontWeight.SemiBold,
        color = corAnimadaDoNumero,
    )

    // Um tique quando a situação muda digitando; aparecer ou sumir (nota vazia) não conta
    var situacaoAnterior by remember { mutableStateOf(status) }
    LaunchedEffect(status) {
        if (status != null && situacaoAnterior != null && status != situacaoAnterior) {
            haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
        situacaoAnterior = status
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(cores.surfaceContainerLowest)
            .borda({ corDaBorda }, { espessura }, tracejada = agendada && !focada && !aviso.erro)
            .clickable(onClickLabel = if (bloqueada) null else "Lançar nota") {
                if (bloqueada) {
                    haptico.performHapticFeedback(HapticFeedbackType.Reject)
                } else {
                    foco.requestFocus()
                    teclado?.show()
                }
            }
            .padding(horizontal = MARGEM, vertical = Espacos.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Nota",
            style = MaterialTheme.typography.labelLarge,
            color = when {
                aviso.erro -> cores.error
                focada -> cores.primary
                else -> cores.onSurfaceVariant
            },
        )
        Spacer(Modifier.height(Espacos.xs))
        Row {
            BasicTextField(
                value = nota,
                onValueChange = { if (FormularioAvaliacao.aceitaComoNota(it)) onNota(it) },
                enabled = !bloqueada,
                singleLine = true,
                textStyle = estiloDoNumero,
                cursorBrush = SolidColor(if (aviso.erro) cores.error else cores.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConcluir() }),
                modifier = Modifier
                    .alignByBaseline()
                    // Do tamanho do número numa linha, para o "de 10" ficar colado nele
                    .width(IntrinsicSize.Max)
                    .focusRequester(foco)
                    .onFocusChanged { estado ->
                        if (focada && !estado.isFocused) onFocoPerdido()
                        focada = estado.isFocused
                    }
                    .semantics { contentDescription = "Nota, de 0 a $maxima" },
                decorationBox = { campo ->
                    Box(contentAlignment = Alignment.Center) {
                        // Vazia, o traço segura o lugar do número; digitando, fica só o cursor
                        if (nota.isEmpty() && !focada) {
                            Text("—", style = estiloDoNumero.copy(color = cores.outline))
                        }
                        campo()
                    }
                },
            )
            Spacer(Modifier.width(Espacos.s))
            Text(
                text = "de $maxima",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = cores.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Spacer(Modifier.height(Espacos.m))
        val pilula = when {
            agendada -> PilulaDaNota.Agendada
            status != null -> PilulaDaNota.DaSituacao(status)
            else -> null
        }
        // A pílula nova entra crescendo de leve, por cima da que sai
        AnimatedContent(
            targetState = pilula,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith fadeOut() using SizeTransform(clip = false)
            },
            contentAlignment = Alignment.Center,
            label = "pilulaDaNota",
        ) { atual ->
            when (atual) {
                PilulaDaNota.Agendada -> Pilula(texto = "Agendada", cor = cores.primary, icone = Icons.Outlined.Schedule)
                is PilulaDaNota.DaSituacao -> Pilula(
                    texto = textoDoStatusDaNota(atual.status),
                    cor = corDoStatus(atual.status),
                )
                null -> Unit
            }
        }
        Spacer(Modifier.height(6.dp))
        AnimatedContent(
            targetState = aviso,
            transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
            label = "avisoDaNota",
        ) { atual ->
            Text(
                text = atual.texto,
                style = MaterialTheme.typography.bodySmall,
                color = if (atual.erro) cores.error else cores.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * A borda do card da nota, por cima do conteúdo; [tracejada] quando a avaliação está sem nota. A
 * [cor] e a [espessura] vêm em funções para serem lidas no desenho: animando, só redesenha.
 */
private fun Modifier.borda(cor: () -> Color, espessura: () -> Dp, tracejada: Boolean): Modifier = drawWithCache {
    val tracos = if (tracejada) PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())) else null
    onDrawWithContent {
        drawContent()
        // O traço fica todo para dentro, com o raio do card (shapes.medium)
        val largura = espessura().toPx()
        drawRoundRect(
            color = cor(),
            topLeft = Offset(largura / 2, largura / 2),
            size = Size(size.width - largura, size.height - largura),
            cornerRadius = CornerRadius(16.dp.toPx() - largura / 2),
            style = Stroke(width = largura, pathEffect = tracos),
        )
    }
}

/** O ícone de uma linha num quadrado tingido, como o selo das disciplinas. */
@Composable
private fun SeloDoIcone(icone: ImageVector) {
    SeloDisciplina(cor = MaterialTheme.colorScheme.primary, icone = icone, tamanho = 36.dp)
}

/** A seta de uma seção que abre ali mesmo: para baixo fechada, para cima aberta. */
@Composable
private fun SetaDaSecao(aberta: Boolean) {
    Icon(
        imageVector = if (aberta) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * O calendário dentro do card, sem cabeçalho nem botões: um toque no dia já escolhe. Só os [dias]
 * que podem ser escolhidos ficam ativos. Abre no mês da data (sem ela, no de hoje), puxado para
 * dentro dos dias que podem. O [aviso], quando há, explica um limite que não é óbvio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarioNoLugar(
    data: LocalDate?,
    dias: DiasDoCalendario,
    hoje: LocalDate,
    aviso: String?,
    onEscolher: (LocalDate) -> Unit,
) {
    val escolher by rememberUpdatedState(onEscolher)
    val mostrado = dias.maisPerto(data ?: hoje)
    // O DatePicker exige que a data marcada e o mês mostrado caibam nos anos dele
    val anos = listOfNotNull(dias.inicio?.year, dias.fim?.year, mostrado.year, data?.year)
    val calendario = rememberDatePickerState(
        initialSelectedDateMillis = data?.paraMillisUtc(),
        initialDisplayedMonthMillis = mostrado.paraMillisUtc(),
        yearRange = anos.min()..anos.max(),
        selectableDates = DiasQuePodem(dias),
    )
    // O primeiro valor é o dia que já estava marcado; os seguintes são toques
    LaunchedEffect(calendario) {
        snapshotFlow { calendario.selectedDateMillis }
            .drop(1)
            .filterNotNull()
            .collect { escolher(millisUtcParaData(it)) }
    }

    Column(Modifier.fillMaxWidth()) {
        if (aviso != null) {
            Text(
                text = aviso,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Espacos.l),
            )
        }
        DatePicker(
            state = calendario,
            title = null,
            headline = null,
            showModeToggle = false,
            // O fundo é o do card
            colors = DatePickerDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.encolherParaCaber(LARGURA_DO_CALENDARIO),
        )
    }
}

// O DatePicker não fica mais estreito que isso (DatePickerModalTokens.ContainerWidth)
private val LARGURA_DO_CALENDARIO = 360.dp

/**
 * Mede o conteúdo com pelo menos [minimo] de largura e, se não houver tanto espaço (tela
 * estreita), encolhe tudo por igual para caber, em vez de cortar as pontas.
 */
private fun Modifier.encolherParaCaber(minimo: Dp): Modifier = layout { medivel, restricoes ->
    val largura = minimo.roundToPx()
    if (restricoes.maxWidth >= largura) {
        val conteudo = medivel.measure(restricoes)
        layout(conteudo.width, conteudo.height) { conteudo.place(0, 0) }
    } else {
        val escala = restricoes.maxWidth.toFloat() / largura
        val conteudo = medivel.measure(restricoes.copy(minWidth = largura, maxWidth = largura))
        layout(restricoes.maxWidth, (conteudo.height * escala).roundToInt()) {
            conteudo.placeWithLayer(0, 0) {
                scaleX = escala
                scaleY = escala
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

/** O calendário só deixa tocar nos dias que cabem em [dias]. */
@OptIn(ExperimentalMaterial3Api::class)
private class DiasQuePodem(private val dias: DiasDoCalendario) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = millisUtcParaData(utcTimeMillis) in dias

    override fun isSelectableYear(year: Int): Boolean =
        (dias.inicio == null || year >= dias.inicio.year) && (dias.fim == null || year <= dias.fim.year)
}

/**
 * O que quase sempre fica como está: o tipo (só os [tipos] que cabem na tela), quanto a avaliação
 * vale (10, 5, 100 ou outro valor), o peso, com − e + (só com [peso], na média ponderada) e o
 * período (só com [opcoesDePeriodo], no ano letivo inteiro). O tipo e o período usam o mesmo
 * seletor do topo do app. Embaixo do tipo vem o [erroDoTipo] (a vaga já está ocupada) ou a
 * [dicaDoTipo] (onde fica a final).
 */
@Composable
private fun MaisOpcoes(
    tipo: TipoAvaliacao,
    tipos: List<TipoAvaliacao>,
    erroDoTipo: String?,
    dicaDoTipo: String?,
    notaMaxima: String,
    notaMaximaValor: Double?,
    outraMaxima: Boolean,
    peso: Double?,
    periodoSelecionado: Int,
    opcoesDePeriodo: List<OpcaoDoSeletor>,
    onTipo: (TipoAvaliacao) -> Unit,
    onMaximaPronta: (Double) -> Unit,
    onOutraMaxima: () -> Unit,
    onNotaMaxima: (String) -> Unit,
    onPassoNoPeso: (Int) -> Unit,
    onPeriodo: (Int) -> Unit,
) {
    val haptico = LocalHapticFeedback.current
    val opcoesDeTipo = remember(tipos) { tipos.map { OpcaoDoSeletor(it.displayname) } }
    val focoOutra = remember { FocusRequester() }
    // Tocar em "Outro" leva o cursor para o campo, que só existe depois de aparecer
    var pedidosDeFoco by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.padding(start = Espacos.l, end = Espacos.l, top = Espacos.xs, bottom = Espacos.l),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Espacos.s)) {
            Rotulo("Tipo")
            SeletorEmPilula(
                opcoes = opcoesDeTipo,
                selecionada = tipos.indexOf(tipo),
                onSelecionar = { onTipo(tipos[it]) },
                papel = Role.RadioButton,
            )
            (erroDoTipo ?: dicaDoTipo)?.let { texto ->
                Text(
                    text = texto,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (erroDoTipo != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Espacos.s)) {
            Rotulo("Vale quanto")
            Row(horizontalArrangement = Arrangement.spacedBy(Espacos.s)) {
                NOTAS_MAXIMAS_COMUNS.forEach { valor ->
                    ChipDeValor(
                        texto = formatarNumero(valor),
                        escolhido = !outraMaxima && notaMaximaValor == valor,
                        onClick = {
                            haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onMaximaPronta(valor)
                        },
                    )
                }
                ChipDeValor(
                    texto = "Outro",
                    escolhido = outraMaxima,
                    onClick = {
                        haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onOutraMaxima()
                        pedidosDeFoco++
                    },
                )
            }
            AnimatedVisibility(visible = outraMaxima) {
                LaunchedEffect(pedidosDeFoco) {
                    if (pedidosDeFoco > 0) focoOutra.requestFocus()
                }
                // O valor que estava já vem selecionado: digitar troca ele de uma vez
                var campo by remember { mutableStateOf(TextFieldValue(notaMaxima, TextRange(0, notaMaxima.length))) }
                OutlinedTextField(
                    value = campo,
                    onValueChange = { novo ->
                        if (FormularioAvaliacao.aceitaComoNota(novo.text)) {
                            campo = novo
                            onNotaMaxima(novo.text)
                        }
                    },
                    label = { Text("Nota máxima") },
                    isError = notaMaximaValor == null,
                    supportingText = if (notaMaximaValor == null) {
                        { Text("Use um número maior que zero") }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Espacos.xs)
                        .focusRequester(focoOutra),
                )
            }
        }

        if (peso != null) {
            SeletorDePeso(peso = peso, onPasso = onPassoNoPeso)
        }

        if (opcoesDePeriodo.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Espacos.s)) {
                Rotulo("Período")
                SeletorEmPilula(
                    opcoes = opcoesDePeriodo,
                    selecionada = periodoSelecionado,
                    onSelecionar = onPeriodo,
                    papel = Role.RadioButton,
                )
            }
        }
    }
}

@Composable
private fun ChipDeValor(texto: String, escolhido: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = escolhido,
        onClick = onClick,
        label = { Text(texto) },
        leadingIcon = if (escolhido) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else null,
    )
}

/** "Peso" com − e + dos lados do número, como nas faltas, em passos de meio. */
@Composable
private fun SeletorDePeso(peso: Double, onPasso: (Int) -> Unit) {
    val haptico = LocalHapticFeedback.current
    fun passo(sentido: Int) {
        haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
        onPasso(sentido)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(text = "Peso", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Conta na média ponderada",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalIconButton(onClick = { passo(-1) }, enabled = peso > FAIXA_DO_PESO.start) {
            Icon(Icons.Filled.Remove, contentDescription = "Menos peso")
        }
        Text(
            text = formatarNumero(peso),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            // O leitor de tela anuncia o peso novo a cada toque
            modifier = Modifier
                .width(56.dp)
                .semantics { contentDescription = "Peso ${formatarNumero(peso)}" },
        )
        FilledTonalIconButton(onClick = { passo(1) }, enabled = peso < FAIXA_DO_PESO.endInclusive) {
            Icon(Icons.Filled.Add, contentDescription = "Mais peso")
        }
    }
}

@Composable
private fun Rotulo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * O botão de gravar, fixo embaixo, que diz o que vai acontecer ("Agendar avaliação", "Lançar
 * nota"); a troca de um para o outro desliza, para ser notada. Com [comOutra] (avaliação nova),
 * "Outra" ao lado grava e já recomeça o formulário.
 */
@Composable
private fun BarraDeGravar(
    acao: FormularioAvaliacao.Acao,
    comOutra: Boolean,
    onGravar: () -> Unit,
    onGravarEOutra: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MARGEM, vertical = Espacos.l),
        horizontalArrangement = Arrangement.spacedBy(Espacos.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onGravar,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
        ) {
            AnimatedContent(
                targetState = acao,
                transitionSpec = {
                    (slideInVertically { it / 2 } + fadeIn()) togetherWith
                        (slideOutVertically { -it / 2 } + fadeOut()) using SizeTransform(clip = false)
                },
                label = "acaoDoBotao",
            ) { atual ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(atual.texto, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(Espacos.s))
                    Icon(
                        imageVector = if (atual == FormularioAvaliacao.Acao.AGENDAR) Icons.Outlined.Event else Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        if (comOutra) {
            OutlinedButton(onClick = onGravarEOutra, modifier = Modifier.height(56.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Espacos.xs))
                Text(
                    text = "Outra",
                    style = MaterialTheme.typography.titleMedium,
                    // "Outra" sozinho não diz que também grava
                    modifier = Modifier.semantics { contentDescription = "Gravar e lançar outra" },
                )
            }
        }
    }
}

internal val FORMATO_DATA_COMPLETA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// O DatePicker trabalha com a meia-noite UTC do dia escolhido, em milissegundos
internal fun LocalDate.paraMillisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun millisUtcParaData(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
