package app.jammes.boletim.presentation.ui.anoletivo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.AjustesAnoLetivo
import app.jammes.boletim.domain.model.Lancamentos
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.presentation.ui.disciplina.FORMATO_DATA_COMPLETA
import app.jammes.boletim.presentation.ui.disciplina.millisUtcParaData
import app.jammes.boletim.presentation.ui.disciplina.paraMillisUtc
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import java.io.Serializable
import java.time.LocalDate

/**
 * Ajustes do ano letivo aberto no app, em seções: o ano, os períodos e suas datas, as regras de
 * média e as matérias do ano. Edita uma cópia e grava tudo junto no Salvar.
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
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }

        AnoLetivoUiState.NaoEncontrado -> MolduraAjustes(onVoltar = onVoltar, modifier = modifier) { padding ->
            Box(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Ano letivo não encontrado", style = MaterialTheme.typography.titleMedium)
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
    var confirmandoDescarte by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val alterado = form.semGuardados() != gravado
    val podeSalvar = alterado && form.paraGravar(ajustes) != null && salvamento != Salvamento.Salvando

    // Com alterações, sair pergunta antes: pela seta da barra e pelo voltar do sistema
    fun voltar() {
        if (alterado) confirmandoDescarte = true else onVoltar()
    }
    BackHandler(enabled = alterado, onBack = ::voltar)

    LaunchedEffect(salvamento) {
        when (val s = salvamento) {
            Salvamento.Salvo -> onVoltar()
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
        acoes = {
            TextButton(onClick = { onSalvar(form) }, enabled = podeSalvar) { Text("Salvar") }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding() // o campo digitado não fica atrás do teclado
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SecaoAnoLetivo(form = form, onForm = { form = it })
            SecaoPeriodos(form = form, onForm = { form = it }, bloqueio = form.bloqueioParaDiminuir(ajustes))
            SecaoRegras(form = form, onForm = { form = it })
            SecaoMaterias(ajustes = ajustes, form = form, onForm = { form = it })
        }
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

/** Barra com voltar, título e as ações; é a mesma enquanto carrega e depois. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MolduraAjustes(
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    acoes: @Composable RowScope.() -> Unit = {},
    conteudo: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajustes do Ano Letivo",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = acoes,
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = conteudo,
    )
}

// Seções ------------------------------------------------------------------------------------

@Composable
private fun SecaoAnoLetivo(
    form: FormularioAnoLetivo,
    onForm: (FormularioAnoLetivo) -> Unit,
) {
    Secao(titulo = "Ano letivo") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val anoInvalido = form.anoValor == null
            OutlinedTextField(
                value = form.ano,
                onValueChange = { onForm(form.copy(ano = it)) },
                label = { Text("Ano") },
                isError = anoInvalido,
                supportingText = if (anoInvalido) {
                    { Text("4 dígitos") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = form.serie,
                onValueChange = { onForm(form.copy(serie = it)) },
                label = { Text("Série") },
                placeholder = { Text("9º Ano") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.weight(2f)
            )
        }
    }
}

@Composable
private fun SecaoPeriodos(
    form: FormularioAnoLetivo,
    onForm: (FormularioAnoLetivo) -> Unit,
    bloqueio: Lancamentos?, // o que impede de tirar o último período; null quando ele pode sair
) {
    // Data aberta no calendário: null = nenhuma
    var dataAberta by rememberSaveable { mutableStateOf<DataAberta?>(null) }
    val problemas = problemasDasDatas(form.visiveis)

    Secao(
        titulo = "Períodos",
        descricao = "Aumentar a quantidade cria períodos no fim; diminuir tira os últimos. " +
            "Um período com avaliações ou faltas não pode sair."
    ) {
        Opcoes(
            rotulo = "Tipo",
            opcoes = TipoPeriodo.entries,
            selecionada = form.tipoPeriodo,
            nome = { it.displayName },
            onSelecionar = { onForm(form.copy(tipoPeriodo = it)) },
        )

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Quantidade",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onForm(form.menosUmPeriodo()) },
                    enabled = form.quantidade > 1 && bloqueio == null
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Tirar o último período")
                }
                Text(
                    text = form.quantidade.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 32.dp)
                )
                IconButton(
                    onClick = { onForm(form.maisUmPeriodo()) },
                    enabled = form.quantidade < FormularioAnoLetivo.MAX_PERIODOS
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Adicionar um período")
                }
            }
            if (bloqueio != null) {
                val tipo = form.tipoPeriodo
                Text(
                    text = "Não dá para diminuir: ${if (tipo.feminino) "a" else "o"} " +
                        "${nomeDoPeriodo(form.quantidade, tipo)} tem ${descrever(bloqueio)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        form.visiveis.forEachIndexed { i, datas ->
            PeriodoLinha(
                nome = nomeDoPeriodo(i + 1, form.tipoPeriodo),
                datas = datas,
                problema = problemas[i],
                onEscolherInicio = { dataAberta = DataAberta(periodo = i, inicio = true) },
                onEscolherFim = { dataAberta = DataAberta(periodo = i, inicio = false) },
            )
        }
    }

    dataAberta?.let { aberta ->
        val datas = form.periodos[aberta.periodo]
        EscolherData(
            data = if (aberta.inicio) datas.inicio else datas.fim,
            onEscolher = { escolhida ->
                val novas = if (aberta.inicio) datas.copy(inicio = escolhida) else datas.copy(fim = escolhida)
                onForm(form.comDatas(aberta.periodo, novas))
                dataAberta = null
            },
            onDismiss = { dataAberta = null },
        )
    }
}

@Composable
private fun SecaoRegras(
    form: FormularioAnoLetivo,
    onForm: (FormularioAnoLetivo) -> Unit,
) {
    Secao(
        titulo = "Regras de média",
        descricao = "Valem para todas as matérias do ano. A frequência mínima define quantas " +
            "faltas cada uma aceita."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val mediaInvalida = form.mediaMinimaValor == null
            OutlinedTextField(
                value = form.mediaMinima,
                onValueChange = { onForm(form.copy(mediaMinima = it)) },
                label = { Text("Média mínima") },
                isError = mediaInvalida,
                supportingText = if (mediaInvalida) {
                    { Text("Maior que zero") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            val frequenciaInvalida = form.frequenciaMinimaValor == null
            OutlinedTextField(
                value = form.frequenciaMinima,
                onValueChange = { onForm(form.copy(frequenciaMinima = it)) },
                label = { Text("Frequência mínima") },
                suffix = { Text("%") },
                isError = frequenciaInvalida,
                supportingText = if (frequenciaInvalida) {
                    { Text("De 0 a 100") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        Opcoes(
            rotulo = "Cálculo da média",
            opcoes = TipoMedia.entries,
            selecionada = form.tipoMedia,
            nome = { it.displayName },
            onSelecionar = { onForm(form.copy(tipoMedia = it)) },
            explicacao = explicacao(form.tipoMedia),
        )

        Opcoes(
            rotulo = "Arredondamento",
            opcoes = TipoArredondamento.entries,
            selecionada = form.arredondamento,
            nome = { it.displayName },
            onSelecionar = { onForm(form.copy(arredondamento = it)) },
            explicacao = explicacao(form.arredondamento),
        )
    }
}

@Composable
private fun SecaoMaterias(
    ajustes: AjustesAnoLetivo,
    form: FormularioAnoLetivo,
    onForm: (FormularioAnoLetivo) -> Unit,
) {
    Secao(
        titulo = "Matérias do ano",
        descricao = "Marque as que o aluno tem neste ano. Uma matéria com avaliações ou faltas " +
            "não pode sair."
    ) {
        if (ajustes.materias.isEmpty()) {
            Text(
                text = "Nenhuma matéria cadastrada",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column {
            ajustes.materias.forEach { materia ->
                // Já no ano, mostra a disciplina como as abas mostram (ela pode ter outro nome ou cor)
                val disciplina = ajustes.disciplinas.find { it.materiaId == materia.id }
                // Com algo lançado, a matéria fica no ano: tirá-la apagaria tudo junto
                val lancamentos = disciplina
                    ?.let { ajustes.lancamentosPorDisciplina[it.id] }
                    ?.takeIf { !it.vazio }
                val marcada = materia.id in form.materias

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(
                            value = marcada,
                            enabled = lancamentos == null,
                            role = Role.Checkbox,
                            onValueChange = { marcar ->
                                val materias = if (marcar) form.materias + materia.id else form.materias - materia.id
                                onForm(form.copy(materias = materias))
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = marcada, onCheckedChange = null, enabled = lancamentos == null)
                    Spacer(Modifier.width(12.dp))
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(CoresDisciplina.de(disciplina?.cor ?: materia.cor))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = disciplina?.nome ?: materia.nome,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (lancamentos != null) {
                            Text(
                                text = descrever(lancamentos),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// Peças das seções --------------------------------------------------------------------------

/** Um cartão com título, uma explicação curta e os campos de uma coisa só. */
@Composable
private fun Secao(
    titulo: String,
    modifier: Modifier = Modifier,
    descricao: String? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = titulo, style = MaterialTheme.typography.titleMedium)
                if (descricao != null) {
                    Text(
                        text = descricao,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            conteudo()
        }
    }
}

/** Escolha entre poucas opções, em chips. A [explicacao] fala da que está marcada. */
@Composable
private fun <T> Opcoes(
    rotulo: String,
    opcoes: List<T>,
    selecionada: T,
    nome: (T) -> String,
    onSelecionar: (T) -> Unit,
    explicacao: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opcoes.forEach { opcao ->
                FilterChip(
                    selected = opcao == selecionada,
                    onClick = { onSelecionar(opcao) },
                    label = { Text(nome(opcao)) }
                )
            }
        }
        if (explicacao != null) {
            Text(
                text = explicacao,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PeriodoLinha(
    nome: String,
    datas: DatasPeriodo,
    problema: ProblemaDatas?,
    onEscolherInicio: () -> Unit,
    onEscolherFim: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = nome, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoData(
                rotulo = "Início",
                data = datas.inicio,
                isError = problema == ProblemaDatas.COMECA_ANTES_DO_FIM_DO_ANTERIOR,
                onClick = onEscolherInicio,
                modifier = Modifier.weight(1f)
            )
            CampoData(
                rotulo = "Fim",
                data = datas.fim,
                isError = problema == ProblemaDatas.FIM_ANTES_DO_INICIO,
                onClick = onEscolherFim,
                modifier = Modifier.weight(1f)
            )
        }
        if (problema != null) {
            Text(
                text = problema.mensagem,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/** Campo de data que não aceita digitação: tocar nele abre o calendário. */
@Composable
private fun CampoData(
    rotulo: String,
    data: LocalDate,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clique by rememberUpdatedState(onClick)
    val toque = remember { MutableInteractionSource() }
    LaunchedEffect(toque) {
        toque.interactions.collect {
            if (it is PressInteraction.Release) clique()
        }
    }
    OutlinedTextField(
        value = data.format(FORMATO_DATA_COMPLETA),
        onValueChange = {},
        readOnly = true,
        label = { Text(rotulo) },
        isError = isError,
        singleLine = true,
        interactionSource = toque,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EscolherData(
    data: LocalDate,
    onEscolher: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val calendario = rememberDatePickerState(initialSelectedDateMillis = data.paraMillisUtc())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val escolhida = calendario.selectedDateMillis
                if (escolhida != null) onEscolher(millisUtcParaData(escolhida)) else onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    ) {
        DatePicker(state = calendario)
    }
}

// Textos ------------------------------------------------------------------------------------

/** Qual data do calendário está aberta: o início ou o fim de um período, pela posição dele. */
private data class DataAberta(val periodo: Int, val inicio: Boolean) : Serializable

// Unidade é palavra feminina (1ª Unidade); bimestre, trimestre e semestre, masculinas (1º Bimestre)
private val TipoPeriodo.feminino: Boolean
    get() = this == TipoPeriodo.UNIDADE

private fun nomeDoPeriodo(numero: Int, tipo: TipoPeriodo): String =
    "$numero${if (tipo.feminino) "ª" else "º"} ${tipo.displayName}"

/** "3 avaliações e 2 faltas", "1 avaliação", "1 falta". */
private fun descrever(lancamentos: Lancamentos): String = listOfNotNull(
    lancamentos.avaliacoes.takeIf { it > 0 }?.let { if (it == 1) "1 avaliação" else "$it avaliações" },
    lancamentos.faltas.takeIf { it > 0 }?.let { if (it == 1) "1 falta" else "$it faltas" },
).joinToString(" e ")

// Os exemplos seguem o arredondarMedia e o CalcularMediaDisciplina
private fun explicacao(tipo: TipoMedia): String = when (tipo) {
    TipoMedia.PONDERADA -> "Cada avaliação conta pelo seu peso."
    TipoMedia.SIMPLES -> "Todas as avaliações contam igual; o peso fica de fora."
    TipoMedia.SOMA -> "Soma as notas, sem dividir: para quando os pontos do período são " +
        "distribuídos entre as avaliações."
}

private fun explicacao(tipo: TipoArredondamento): String = when (tipo) {
    TipoArredondamento.NENHUM -> "A média fica com até 2 casas: 6,96 continua 6,96."
    TipoArredondamento.MEIO_PONTO -> "Vai para o 0,5 mais próximo: 7,3 vira 7,5."
    TipoArredondamento.INTEIRO -> "Vai para o inteiro mais próximo: 7,4 vira 7 e 7,5 vira 8."
    TipoArredondamento.CIMA -> "Sobe sempre, na 1ª casa: 6,62 vira 6,7."
    TipoArredondamento.BAIXO -> "Desce sempre, na 1ª casa: 6,68 vira 6,6."
}
