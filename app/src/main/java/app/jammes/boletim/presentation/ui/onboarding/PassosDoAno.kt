package app.jammes.boletim.presentation.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.Etapa
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.domain.usecase.materiasNovas
import app.jammes.boletim.domain.usecase.periodosPadrao
import app.jammes.boletim.presentation.ui.anoletivo.DatasPeriodo
import app.jammes.boletim.presentation.ui.anoletivo.EscolhaBottomSheet
import app.jammes.boletim.presentation.ui.anoletivo.FormularioAnoLetivo
import app.jammes.boletim.presentation.ui.anoletivo.LinhaDoTempoDoAno
import app.jammes.boletim.presentation.ui.anoletivo.PeriodoBottomSheet
import app.jammes.boletim.presentation.ui.anoletivo.ProblemaDatas
import app.jammes.boletim.presentation.ui.anoletivo.dataCurta
import app.jammes.boletim.presentation.ui.anoletivo.detalheDoArredondamento
import app.jammes.boletim.presentation.ui.anoletivo.exemploDoArredondamento
import app.jammes.boletim.presentation.ui.anoletivo.faltasPermitidas
import app.jammes.boletim.presentation.ui.anoletivo.nomeDoArredondamento
import app.jammes.boletim.presentation.ui.anoletivo.nomeDoPeriodo
import app.jammes.boletim.presentation.ui.anoletivo.ordinalDoPeriodo
import app.jammes.boletim.presentation.ui.anoletivo.problemasDasDatas
import app.jammes.boletim.presentation.ui.anoletivo.resumoDasDatas
import app.jammes.boletim.presentation.ui.boletim.formatarMedia
import app.jammes.boletim.presentation.ui.components.Avatar
import app.jammes.boletim.presentation.ui.components.DialogoDaMateriaNova
import app.jammes.boletim.presentation.ui.components.Pilula
import app.jammes.boletim.presentation.ui.components.SeloDisciplina
import app.jammes.boletim.presentation.ui.components.descricaoDoAno
import app.jammes.boletim.presentation.ui.disciplina.formatarNumero
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import app.jammes.boletim.presentation.ui.theme.Espacos
import app.jammes.boletim.presentation.ui.theme.IconesDisciplina
import java.time.LocalDate

private const val TOTAL_DE_PASSOS = 4

/**
 * O que todo passo tem: voltar, a barra dos 4 passos (e o Pular, quando dá), o título, o conteúdo
 * rolando e o botão no rodapé, que sobe junto com o teclado. [numero] null é o resumo: a barra fica cheia.
 */
@Composable
internal fun EstruturaDoPasso(
    numero: Int?,
    titulo: String?,
    subtitulo: String?,
    onVoltar: () -> Unit,
    onBotao: () -> Unit,
    botao: String = "Continuar",
    botaoIcone: ImageVector? = null,
    botaoHabilitado: Boolean = true,
    carregando: Boolean = false,
    onPular: (() -> Unit)? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Espacos.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
            Progresso(atual = numero ?: TOTAL_DE_PASSOS, modifier = Modifier.weight(1f).padding(horizontal = Espacos.s))
            Box(Modifier.width(72.dp), contentAlignment = Alignment.CenterEnd) {
                if (onPular != null) TextButton(onClick = onPular) { Text("Pular") }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacos.xl),
        ) {
            if (numero != null) {
                Text(
                    text = "Passo $numero de $TOTAL_DE_PASSOS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(Espacos.xs))
            }
            if (titulo != null) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (subtitulo != null) {
                Spacer(Modifier.height(Espacos.s))
                Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(Espacos.l))
            conteudo()
            Spacer(Modifier.height(Espacos.l))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Espacos.xl, vertical = Espacos.l)
        ) {
            BotaoPrincipal(texto = botao, onClick = onBotao, enabled = botaoHabilitado, carregando = carregando, icone = botaoIcone)
        }
    }
}

@Composable
private fun Progresso(atual: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics { contentDescription = "Passo $atual de $TOTAL_DE_PASSOS" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(TOTAL_DE_PASSOS) { i ->
            val cor by animateColorAsState(
                if (i < atual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                label = "corDoPasso",
            )
            Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(cor))
        }
    }
}

// 2 · Divisão do ano --------------------------------------------------------------------------

@Composable
internal fun PassoDivisao(
    formulario: FormularioOnboarding,
    onMudar: (FormularioOnboarding) -> Unit,
    onVoltar: () -> Unit,
    onPular: () -> Unit,
    onContinuar: () -> Unit,
) {
    var aberto by rememberSaveable { mutableStateOf<Int?>(null) }
    val problemas = problemasDasDatas(formulario.datas)
    val hoje = remember { LocalDate.now() }

    EstruturaDoPasso(
        numero = 2,
        titulo = "Como o ano é dividido?",
        subtitulo = "As datas já vêm sugeridas. Toque num período para ajustar.",
        onVoltar = onVoltar,
        onPular = onPular,
        botaoHabilitado = formulario.datasCertas,
        onBotao = onContinuar,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Espacos.s)) {
            TipoPeriodo.entries.chunked(2).forEach { linha ->
                Row(horizontalArrangement = Arrangement.spacedBy(Espacos.s)) {
                    linha.forEach { tipo ->
                        OpcaoDeDivisao(
                            tipo = tipo,
                            ano = formulario.ano,
                            selecionada = tipo == formulario.tipoPeriodo,
                            quantidade = if (tipo == formulario.tipoPeriodo) formulario.datas.size else null,
                            onClick = { if (tipo != formulario.tipoPeriodo) onMudar(formulario.comTipo(tipo)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Espacos.l))
        Cartao {
            Column(Modifier.padding(Espacos.l)) {
                LinhaDoTempoDoAno(
                    periodos = formulario.datas,
                    tipo = formulario.tipoPeriodo,
                    ano = formulario.ano,
                    problemas = problemas,
                )
                HorizontalDivider(Modifier.padding(top = Espacos.m), color = MaterialTheme.colorScheme.outlineVariant)
                LinhaDaQuantidade(quantidade = formulario.datas.size, onMudar = { onMudar(formulario.comQuantidade(it)) })
                formulario.datas.forEachIndexed { i, datas ->
                    HorizontalDivider(Modifier.padding(vertical = Espacos.s), color = MaterialTheme.colorScheme.outlineVariant)
                    LinhaDoPeriodo(
                        numero = i + 1,
                        tipo = formulario.tipoPeriodo,
                        datas = datas,
                        ano = formulario.ano,
                        agora = hoje in datas.inicio..datas.fim,
                        problema = problemas[i],
                        onClick = { aberto = i },
                    )
                }
            }
        }
    }

    aberto?.let { i ->
        PeriodoBottomSheet(
            numero = i + 1,
            tipo = formulario.tipoPeriodo,
            datas = formulario.datas[i],
            anterior = formulario.datas.getOrNull(i - 1),
            seguinte = formulario.datas.getOrNull(i + 1),
            ano = formulario.ano,
            onAplicar = { onMudar(formulario.comDatas(i, it)) },
            onDismiss = { aberto = null },
        )
    }
}

@Composable
private fun LinhaDaQuantidade(quantidade: Int, onMudar: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = Espacos.s), verticalAlignment = Alignment.CenterVertically) {
        Text("Quantidade", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = { onMudar(quantidade - 1) }, enabled = quantidade > 1) {
            Icon(Icons.Filled.Remove, contentDescription = "Um período a menos")
        }
        Text(
            text = "$quantidade",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp),
        )
        FilledTonalIconButton(onClick = { onMudar(quantidade + 1) }, enabled = quantidade < FormularioAnoLetivo.MAX_PERIODOS) {
            Icon(Icons.Filled.Add, contentDescription = "Um período a mais")
        }
    }
}

/** "4 bimestres", "1 unidade". */
internal fun quantidadeDePeriodos(quantidade: Int, tipo: TipoPeriodo): String =
    "$quantidade " + if (quantidade == 1) tipo.displayName.lowercase() else pluralDoTipo(tipo)

/** "bimestres", "unidades"... */
internal fun pluralDoTipo(tipo: TipoPeriodo): String = when (tipo) {
    TipoPeriodo.UNIDADE -> "unidades"
    TipoPeriodo.BIMESTRE -> "bimestres"
    TipoPeriodo.TRIMESTRE -> "trimestres"
    TipoPeriodo.SEMESTRE -> "semestres"
}

// Borda e fundo de uma opção que se escolhe tocando: a escolhida ganha a borda e um toque da primária
private fun Modifier.opcao(selecionada: Boolean, cores: ColorScheme, forma: Shape, onClick: () -> Unit) = this
    .clip(forma)
    .background(
        if (selecionada) cores.primary.copy(alpha = 0.10f).compositeOver(cores.surfaceContainerLowest)
        else cores.surfaceContainerLowest
    )
    .border(if (selecionada) 2.dp else 1.dp, if (selecionada) cores.primary else cores.outlineVariant, forma)
    .selectable(selected = selecionada, role = Role.RadioButton, onClick = onClick)

@Composable
private fun OpcaoDeDivisao(
    tipo: TipoPeriodo,
    ano: Int,
    selecionada: Boolean,
    quantidade: Int?, // a da escolhida, que o −/+ pode ter mudado; null nas outras, que mostram a padrão
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val padrao = remember(tipo, ano) { periodosPadrao(ano, tipo).size }
    val total = quantidade ?: padrao
    Column(
        modifier
            .opcao(selecionada, cores, MaterialTheme.shapes.medium, onClick)
            .padding(horizontal = 14.dp, vertical = Espacos.m),
    ) {
        // Uma miniatura da divisão: um traço por período
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(total) {
                Box(
                    Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (selecionada) cores.primary else cores.onSurfaceVariant.copy(alpha = 0.35f))
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pluralDoTipo(tipo).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text("$total no ano", style = MaterialTheme.typography.labelMedium, color = cores.onSurfaceVariant)
        }
    }
}

@Composable
private fun LinhaDoPeriodo(
    numero: Int,
    tipo: TipoPeriodo,
    datas: DatasPeriodo,
    ano: Int,
    agora: Boolean,
    problema: ProblemaDatas?,
    onClick: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClickLabel = "Ajustar as datas", onClick = onClick)
            .padding(vertical = Espacos.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(cores.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = ordinalDoPeriodo(numero, tipo),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = cores.onSecondaryContainer,
            )
        }
        Column(Modifier.weight(1f).padding(start = Espacos.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(nomeDoPeriodo(numero, tipo), style = MaterialTheme.typography.bodyLarge)
                if (agora) {
                    Spacer(Modifier.width(Espacos.s))
                    Pilula("Agora", cor = cores.tertiary)
                }
            }
            Text(
                text = problema?.mensagem ?: resumoDasDatas(datas, ano),
                style = MaterialTheme.typography.bodyMedium,
                color = if (problema != null) cores.error else cores.onSurfaceVariant,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = cores.onSurfaceVariant)
    }
}

// 3 · Regras ----------------------------------------------------------------------------------

// O jeito de cada cálculo e uma conta de exemplo, para a escolha não ficar abstrata
private val CALCULOS = listOf(
    Triple(TipoMedia.SIMPLES, "Todas as notas valem igual", "8 e 6 → 7,0"),
    Triple(TipoMedia.PONDERADA, "Cada nota conta pelo seu peso", "8 (peso 2) e 6 → 7,33"),
    Triple(TipoMedia.SOMA, "Os pontos das avaliações se somam", "4 + 3,5 + 2 → 9,5"),
)

@Composable
internal fun PassoRegras(
    formulario: FormularioOnboarding,
    onMudar: (FormularioOnboarding) -> Unit,
    onVoltar: () -> Unit,
    onPular: () -> Unit,
    onContinuar: () -> Unit,
) {
    var escolhendoArredondamento by rememberSaveable { mutableStateOf(false) }

    EstruturaDoPasso(
        numero = 3,
        titulo = "O que precisa para passar?",
        subtitulo = "Está no regimento ou no boletim da escola. Na dúvida, deixe como está.",
        onVoltar = onVoltar,
        onPular = onPular,
        onBotao = onContinuar,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Espacos.m)) {
            ValorDaRegra(
                titulo = "Média mínima",
                valor = formatarMedia(formulario.mediaMinima),
                dica = "Nota para passar",
                onMenos = { onMudar(formulario.mudarMedia(-1)) },
                onMais = { onMudar(formulario.mudarMedia(1)) },
                modifier = Modifier.weight(1f),
            )
            ValorDaRegra(
                titulo = "Frequência mínima",
                valor = "${formatarNumero(formulario.frequenciaMinima)}%",
                dica = faltasPermitidas(formulario.frequenciaMinima),
                onMenos = { onMudar(formulario.mudarFrequencia(-1)) },
                onMais = { onMudar(formulario.mudarFrequencia(1)) },
                modifier = Modifier.weight(1f),
            )
        }
        TituloDaSecao("Cálculo da média")
        Column(verticalArrangement = Arrangement.spacedBy(Espacos.s)) {
            CALCULOS.forEach { (tipo, jeito, exemplo) ->
                OpcaoDeCalculo(
                    nome = tipo.displayName,
                    jeito = jeito,
                    exemplo = exemplo,
                    selecionada = formulario.tipoMedia == tipo,
                    onClick = { onMudar(formulario.copy(tipoMedia = tipo)) },
                )
            }
        }
        Spacer(Modifier.height(Espacos.m))
        Cartao(onClick = { escolhendoArredondamento = true }) {
            LinhaDoResumo(
                inicio = { IconeDaLinha(Icons.Outlined.SwapVert) },
                titulo = "Arredondamento",
                detalhe = "${nomeDoArredondamento(formulario.arredondamento)} · " +
                    detalheDoArredondamento(formulario.arredondamento).replaceFirstChar { it.lowercase() },
                fim = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            )
        }
    }

    if (escolhendoArredondamento) {
        EscolhaBottomSheet(
            titulo = "Arredondamento",
            descricao = "Como a média de cada período é arredondada.",
            opcoes = TipoArredondamento.entries,
            selecionada = formulario.arredondamento,
            nome = { nomeDoArredondamento(it) },
            detalhe = { detalheDoArredondamento(it) },
            exemplo = { exemploDoArredondamento(it) },
            onEscolher = { onMudar(formulario.copy(arredondamento = it)) },
            onDismiss = { escolhendoArredondamento = false },
        )
    }
}

@Composable
private fun ValorDaRegra(
    titulo: String,
    valor: String,
    dica: String,
    onMenos: () -> Unit,
    onMais: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Cartao(modifier) {
        Column(Modifier.padding(Espacos.l)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            Text(valor, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
            Text(dica, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Espacos.m))
            Row(horizontalArrangement = Arrangement.spacedBy(Espacos.s)) {
                FilledTonalButton(
                    onClick = onMenos,
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.Filled.Remove, contentDescription = "Diminuir a $titulo".lowercase()) }
                FilledTonalButton(
                    onClick = onMais,
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.Filled.Add, contentDescription = "Aumentar a $titulo".lowercase()) }
            }
        }
    }
}

@Composable
private fun OpcaoDeCalculo(nome: String, jeito: String, exemplo: String, selecionada: Boolean, onClick: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .opcao(selecionada, cores, MaterialTheme.shapes.medium, onClick)
            .padding(start = Espacos.xs, end = Espacos.l, top = Espacos.s, bottom = Espacos.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selecionada, onClick = null, modifier = Modifier.padding(horizontal = Espacos.m))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(
                    text = exemplo,
                    style = MaterialTheme.typography.labelMedium,
                    color = cores.onSurfaceVariant,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(cores.surfaceContainerHigh)
                        .padding(horizontal = Espacos.s, vertical = 3.dp),
                )
            }
            Text(jeito, style = MaterialTheme.typography.bodyMedium, color = cores.onSurfaceVariant)
        }
    }
}

// 4 · Matérias --------------------------------------------------------------------------------

@Composable
internal fun PassoMaterias(
    formulario: FormularioOnboarding,
    materias: List<MateriaDomain>,
    sugeridas: Set<Long>,
    onMudar: (FormularioOnboarding) -> Unit,
    onVoltar: () -> Unit,
    onContinuar: () -> Unit,
) {
    val marcadas = formulario.materiasMarcadas(sugeridas)
    val total = formulario.totalDeMaterias(sugeridas)
    // Como as criadas aqui vão ficar: a cor e o ícone que o cadastro vai dar a elas
    val novas = remember(formulario.materiasNovas, materias) { materiasNovas(formulario.materiasNovas, materias) }
    var criando by rememberSaveable { mutableStateOf(false) }
    val haptico = LocalHapticFeedback.current
    val deCostume = when (formulario.serie.etapa) {
        Etapa.FUNDAMENTAL -> "as de costume no Ensino Fundamental"
        Etapa.MEDIO -> "as de costume no Ensino Médio"
        null -> "as mais comuns"
    }

    EstruturaDoPasso(
        numero = 4,
        titulo = "Quais são as matérias?",
        subtitulo = "Já marcamos $deCostume. Toque para tirar ou incluir.",
        onVoltar = onVoltar,
        botaoHabilitado = total > 0,
        onBotao = onContinuar,
    ) {
        Pilula(
            texto = if (total == 1) "1 marcada" else "$total marcadas",
            cor = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Espacos.m))
        // As do cadastro, as criadas aqui e, por último, o botão de criar outra
        val celulas = materias.map { Celula.DoCadastro(it) } + novas.map { Celula.Nova(it) } + Celula.Outra
        Column(verticalArrangement = Arrangement.spacedBy(Espacos.s)) {
            celulas.chunked(2).forEach { linha ->
                Row(horizontalArrangement = Arrangement.spacedBy(Espacos.s)) {
                    linha.forEach { celula ->
                        val modifier = Modifier.weight(1f)
                        when (celula) {
                            is Celula.DoCadastro -> MateriaParaMarcar(
                                materia = celula.materia,
                                marcada = celula.materia.id in marcadas,
                                onClick = {
                                    haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    onMudar(formulario.alternarMateria(celula.materia.id, sugeridas))
                                },
                                modifier = modifier,
                            )
                            // Desmarcar uma criada aqui é desistir dela: ainda não existe no cadastro
                            is Celula.Nova -> MateriaParaMarcar(
                                materia = celula.materia,
                                marcada = true,
                                onClick = {
                                    haptico.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    onMudar(formulario.semMateriaNova(celula.materia.nome))
                                },
                                modifier = modifier,
                            )
                            Celula.Outra -> OutraMateria(onClick = { criando = true }, modifier = modifier)
                        }
                    }
                    if (linha.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    if (criando) {
        DialogoDaMateriaNova(
            titulo = "Outra matéria",
            existentes = materias.map { it.nome } + formulario.materiasNovas,
            onCriar = { nome ->
                onMudar(formulario.comMateriaNova(nome))
                criando = false
            },
            onDismiss = { criando = false },
        )
    }
}

private sealed interface Celula {
    data class DoCadastro(val materia: MateriaDomain) : Celula
    data class Nova(val materia: MateriaDomain) : Celula
    data object Outra : Celula
}

@Composable
private fun OutraMateria(onClick: () -> Unit, modifier: Modifier) {
    val cores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .height(56.dp)
            .bordaTracejada(cores.outlineVariant, 14.dp)
            .clip(forma)
            .clickable(onClickLabel = "Criar uma matéria", onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(cores.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = cores.primary, modifier = Modifier.size(20.dp))
        }
        Text(
            text = "Outra matéria",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = cores.primary,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }
}

private fun Modifier.bordaTracejada(cor: Color, raio: Dp) = drawBehind {
    val espessura = 1.5.dp.toPx()
    drawRoundRect(
        color = cor,
        topLeft = Offset(espessura / 2, espessura / 2),
        size = Size(size.width - espessura, size.height - espessura),
        cornerRadius = CornerRadius(raio.toPx()),
        style = Stroke(width = espessura, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
    )
}

@Composable
private fun MateriaParaMarcar(materia: MateriaDomain, marcada: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val cores = MaterialTheme.colorScheme
    val cor = CoresDisciplina.de(materia.cor)
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(forma)
            .background(if (marcada) cor.copy(alpha = 0.13f).compositeOver(cores.surfaceContainerLowest) else cores.surfaceContainerLowest)
            .border(1.dp, if (marcada) cor.copy(alpha = 0.55f) else cores.outlineVariant, forma)
            .toggleable(value = marcada, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeloDisciplina(
            cor = if (marcada) cor else cores.onSurfaceVariant,
            icone = IconesDisciplina.de(materia.icone),
            tamanho = 30.dp,
            modifier = Modifier.alpha(if (marcada) 1f else 0.6f),
        )
        Text(
            text = materia.nome,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (marcada) FontWeight.SemiBold else FontWeight.Medium,
            color = if (marcada) cores.onSurface else cores.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        )
        Icon(
            imageVector = if (marcada) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (marcada) cor else cores.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(20.dp),
        )
    }
}

// Tudo pronto ---------------------------------------------------------------------------------

@Composable
internal fun PassoPronto(
    formulario: FormularioOnboarding,
    materias: List<MateriaDomain>,
    sugeridas: Set<Long>,
    salvando: Boolean,
    onVoltar: () -> Unit,
    onMudar: (passo: Int) -> Unit,
    onConcluir: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    val nomes = materias.filter { it.id in formulario.materiasMarcadas(sugeridas) }.map { it.nome } + formulario.materiasNovas
    val hoje = remember { LocalDate.now() }
    val agora = formulario.datas.indexOfFirst { hoje in it.inicio..it.fim }

    EstruturaDoPasso(
        numero = null,
        titulo = null,
        subtitulo = null,
        onVoltar = onVoltar,
        botao = "Abrir o boletim",
        botaoIcone = Icons.AutoMirrored.Filled.ArrowForward,
        botaoHabilitado = formulario.paraGravar(sugeridas) != null,
        carregando = salvando,
        onBotao = onConcluir,
    ) {
        Box(Modifier.fillMaxWidth().height(168.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(156.dp).clip(CircleShape).background(cores.primary.copy(alpha = 0.09f)))
            Box(Modifier.size(124.dp).clip(CircleShape).background(cores.primary.copy(alpha = 0.15f)))
            Box(Modifier.size(92.dp).clip(CircleShape).background(cores.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = cores.onPrimary, modifier = Modifier.size(52.dp))
            }
        }
        Text(
            text = "Tudo pronto!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        Spacer(Modifier.height(Espacos.xs))
        Text(
            text = "O boletim de ${formulario.nome.trim()} já pode receber notas e faltas.",
            style = MaterialTheme.typography.bodyMedium,
            color = cores.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Espacos.xl))

        // Cada linha volta para o passo dela
        Cartao {
            LinhaDoResumo(
                inicio = { Avatar(nome = formulario.nome.ifBlank { null }, modifier = Modifier.size(40.dp)) },
                titulo = formulario.nome.trim(),
                detalhe = descricaoDoAno(formulario.serie.texto(), formulario.ano),
                onClick = { onMudar(1) },
            )
            DivisoriaDoResumo()
            LinhaDoResumo(
                inicio = { IconeDaLinha(Icons.Outlined.EventAvailable) },
                titulo = quantidadeDePeriodos(formulario.datas.size, formulario.tipoPeriodo),
                detalhe = "de ${dataCurta(formulario.datas.first().inicio, formulario.ano)} a " +
                    dataCurta(formulario.datas.last().fim, formulario.ano) +
                    if (agora >= 0) " · agora: ${nomeDoPeriodo(agora + 1, formulario.tipoPeriodo)}" else "",
                onClick = { onMudar(2) },
            )
            DivisoriaDoResumo()
            LinhaDoResumo(
                inicio = { IconeDaLinha(Icons.Outlined.Functions) },
                titulo = "Média ${formatarMedia(formulario.mediaMinima)} · frequência ${formatarNumero(formulario.frequenciaMinima)}%",
                detalhe = "${formulario.tipoMedia.displayName} · " +
                    if (formulario.arredondamento == TipoArredondamento.NENHUM) "sem arredondamento"
                    else "arredondamento ${nomeDoArredondamento(formulario.arredondamento).lowercase()}",
                onClick = { onMudar(3) },
            )
            DivisoriaDoResumo()
            LinhaDoResumo(
                inicio = { IconeDaLinha(Icons.Outlined.School) },
                titulo = if (nomes.size == 1) "1 matéria" else "${nomes.size} matérias",
                detalhe = when {
                    nomes.size <= 2 -> nomes.joinToString(" e ")
                    else -> "${nomes[0]}, ${nomes[1]} e mais ${nomes.size - 2}"
                },
                onClick = { onMudar(4) },
            )
        }
        Spacer(Modifier.height(Espacos.l))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = cores.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Dá para mudar tudo depois, nos ajustes do ano letivo.",
                style = MaterialTheme.typography.labelMedium,
                color = cores.onSurfaceVariant,
            )
        }
    }
}

// Peças comuns --------------------------------------------------------------------------------

/** O cartão dos cards do app: branco (ou quase preto, no escuro) com borda fina. */
@Composable
private fun Cartao(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val cores = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    val borda = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.large, colors = cores, border = borda, content = conteudo)
    } else {
        Card(modifier = modifier, shape = MaterialTheme.shapes.large, colors = cores, border = borda, content = conteudo)
    }
}

@Composable
private fun LinhaDoResumo(
    inicio: @Composable () -> Unit,
    titulo: String,
    detalhe: String,
    onClick: (() -> Unit)? = null,
    fim: ImageVector = Icons.Outlined.Edit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClickLabel = "Mudar", onClick = onClick) else Modifier)
            .padding(horizontal = Espacos.l, vertical = Espacos.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        inicio()
        Column(Modifier.weight(1f).padding(horizontal = Espacos.m)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = detalhe,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(fim, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DivisoriaDoResumo() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun IconeDaLinha(icone: ImageVector) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
