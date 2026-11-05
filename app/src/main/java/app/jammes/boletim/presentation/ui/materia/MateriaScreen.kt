package app.jammes.boletim.presentation.ui.materia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.MateriaDoCadastro
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.domain.model.Plano
import app.jammes.boletim.domain.model.materiasQueRestam
import app.jammes.boletim.domain.usecase.semAcentos
import app.jammes.boletim.presentation.ui.anoletivo.Cartao
import app.jammes.boletim.presentation.ui.anoletivo.Divisoria
import app.jammes.boletim.presentation.ui.anoletivo.LinhaQueAbre
import app.jammes.boletim.presentation.ui.anoletivo.MolduraAjustes
import app.jammes.boletim.presentation.ui.anoletivo.TituloDaSecao
import app.jammes.boletim.presentation.ui.components.CarregandoDiscreto
import app.jammes.boletim.presentation.ui.components.EstadoVazio
import app.jammes.boletim.presentation.ui.components.OpcaoDoSeletor
import app.jammes.boletim.presentation.ui.components.SeletorEmPilula
import app.jammes.boletim.presentation.ui.components.SeloDisciplina
import app.jammes.boletim.presentation.ui.plano.EtiquetaPro
import app.jammes.boletim.presentation.ui.plano.FolhaDoPro
import app.jammes.boletim.presentation.ui.plano.rotuloDaMateriaNova
import app.jammes.boletim.presentation.ui.plano.textoDasQueRestam
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import app.jammes.boletim.presentation.ui.theme.Espacos
import app.jammes.boletim.presentation.ui.theme.IconesDisciplina

private const val TITULO = "Matérias"

/**
 * O cadastro de matérias, aberto pelo perfil: a busca e o filtro no topo e a lista agrupada entre as do
 * app e as do aluno. Tocar numa matéria abre o sheet dela, e o botão de baixo cria outra.
 *
 * As matérias do app só aparecem: não mudam nem saem, em nenhum plano. No grátis, a matéria nova depois
 * do limite é do Pro: o botão leva à folha dele, e o que ela desbloqueia já vale aqui, sem sair da tela.
 */
@Composable
fun MateriaScreen(
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MateriaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.avisos.collect { snackbarHostState.showSnackbar(it) }
    }

    when (val s = state) {
        MateriaUiState.Carregando -> MolduraAjustes(onVoltar = onVoltar, modifier = modifier, titulo = TITULO) { padding ->
            CarregandoDiscreto(Modifier.fillMaxSize().padding(padding))
        }

        is MateriaUiState.Sucesso -> CadastroDeMaterias(
            materias = s.materias,
            plano = s.plano,
            snackbarHostState = snackbarHostState,
            onSalvar = viewModel::salvar,
            onExcluir = viewModel::excluir,
            onVoltar = onVoltar,
            modifier = modifier,
        )
    }
}

@Composable
private fun CadastroDeMaterias(
    materias: List<MateriaDoCadastro>,
    plano: Plano,
    snackbarHostState: SnackbarHostState,
    onSalvar: (MateriaDomain) -> Unit,
    onExcluir: (MateriaDomain) -> Unit,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // rememberSaveable: a busca, o filtro e o sheet aberto sobrevivem a girar a tela
    var busca by rememberSaveable { mutableStateOf("") }
    var filtro by rememberSaveable { mutableStateOf(FiltroDeMaterias.TODAS) }
    // Uma cópia, e não o id: excluída, a matéria sai da lista enquanto o sheet ainda desce
    var aberta by rememberSaveable { mutableStateOf<MateriaDoCadastro?>(null) }
    // Com o Pro desbloqueado nela, a folha fecha sozinha, e o botão de criar perde a etiqueta
    var folhaDoPro by rememberSaveable { mutableStateOf(false) }

    val grupos = remember(materias, busca, filtro) { gruposDeMaterias(materias, busca, filtro) }
    val cadastro = remember(materias) { materias.map { it.materia } }
    val restam = remember(cadastro, plano) { plano.materiasQueRestam(cadastro) }
    val rolagem = rememberScrollState()

    // Uma criada aqui entra no fim da lista: ela rola até lá, para a matéria nova aparecer
    val proprias = cadastro.count { it.propria }
    var propriasAntes by remember { mutableIntStateOf(proprias) }
    LaunchedEffect(proprias) {
        if (proprias > propriasAntes) {
            withFrameNanos {} // só depois de a linha nova ser medida ela conta na rolagem
            rolagem.animateScrollTo(rolagem.maxValue)
        }
        propriasAntes = proprias
    }

    MolduraAjustes(
        onVoltar = onVoltar,
        modifier = modifier,
        titulo = TITULO,
        snackbarHostState = snackbarHostState,
        corAoRolar = MaterialTheme.colorScheme.background,
        fab = {
            BotaoNovaMateria(
                restam = restam,
                onClick = {
                    if (restam == 0) {
                        folhaDoPro = true
                    } else {
                        aberta = MateriaDoCadastro(materiaNova(coresUsadas = cadastro.map { it.cor }))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .padding(horizontal = Espacos.l)
                    .padding(top = Espacos.xs, bottom = Espacos.m),
                verticalArrangement = Arrangement.spacedBy(Espacos.m),
            ) {
                CampoDeBusca(valor = busca, onMudar = { busca = it })
                SeletorEmPilula(
                    opcoes = OPCOES_DO_FILTRO,
                    selecionada = filtro.ordinal,
                    onSelecionar = { filtro = FiltroDeMaterias.entries[it] },
                    papel = Role.RadioButton,
                )
            }
            // Com a lista rolada por baixo, uma linha separa o que fica fixo. Só a camada muda de
            // transparência: rolar não recompõe nada
            HorizontalDivider(
                modifier = Modifier.graphicsLayer { alpha = if (rolagem.value > 0) 1f else 0f },
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            if (grupos.vazios) {
                ListaVazia(busca = busca, filtro = filtro, modifier = Modifier.weight(1f))
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rolagem)
                        .padding(start = Espacos.l, end = Espacos.l, top = Espacos.s, bottom = ESPACO_DO_BOTAO),
                    verticalArrangement = Arrangement.spacedBy(Espacos.l),
                ) {
                    GrupoDeMaterias("Do app", grupos.doApp, onAbrir = { aberta = it })
                    GrupoDeMaterias("Do aluno", grupos.doAluno, onAbrir = { aberta = it })
                }
            }
        }
    }

    aberta?.let { item ->
        val materia = item.materia
        MateriaBottomSheet(
            materia = materia,
            // O próprio nome pode ficar: só não pode repetir o de outra
            existentes = cadastro.filter { it.id != materia.id }.map { it.nome },
            onSalvar = { salva ->
                // A nova precisa aparecer: nem a busca nem o filtro podem escondê-la
                if (salva.id == 0L) {
                    busca = ""
                    filtro = FiltroDeMaterias.TODAS
                }
                onSalvar(salva)
            },
            onDismiss = { aberta = null },
            anosEmUso = item.anosEmUso,
            onExcluir = { onExcluir(materia) },
        )
    }

    if (folhaDoPro) FolhaDoPro(onFechar = { folhaDoPro = false })
}

@Composable
private fun CampoDeBusca(
    valor: String,
    onMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val foco = LocalFocusManager.current
    OutlinedTextField(
        value = valor,
        onValueChange = onMudar,
        placeholder = { Text("Buscar pelo nome") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (valor.isNotEmpty()) {
            {
                IconButton(onClick = { onMudar("") }) {
                    Icon(Icons.Filled.Cancel, contentDescription = "Limpar a busca")
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Search,
        ),
        // A lista já acompanha o que é digitado: o botão do teclado só sai da frente dela
        keyboardActions = KeyboardActions(onSearch = { foco.clearFocus() }),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Um grupo da lista, com o título em cima: cada linha só tem o ícone na cor da matéria e o nome. */
@Composable
private fun GrupoDeMaterias(
    titulo: String,
    materias: List<MateriaDoCadastro>,
    onAbrir: (MateriaDoCadastro) -> Unit,
) {
    if (materias.isEmpty()) return
    Column {
        TituloDaSecao(titulo, alterada = false)
        Cartao {
            materias.forEachIndexed { i, item ->
                if (i > 0) Divisoria()
                LinhaQueAbre(
                    titulo = item.materia.nome,
                    rotuloDoClique = "Abrir a matéria",
                    onClick = { onAbrir(item) },
                    fim = {}, // sem a seta
                ) {
                    SeloDisciplina(
                        cor = CoresDisciplina.de(item.materia.cor),
                        icone = IconesDisciplina.de(item.materia.icone),
                    )
                }
            }
        }
    }
}

/** O que a busca e o filtro deixaram de fora: diz o que aconteceu, para não parecer erro. */
@Composable
private fun ListaVazia(busca: String, filtro: FiltroDeMaterias, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (busca.isBlank() && filtro == FiltroDeMaterias.DO_ALUNO) {
            EstadoVazio(
                icone = Icons.Outlined.LibraryAdd,
                titulo = "Nenhuma matéria criada ainda",
                mensagem = "Crie aqui as que a escola tem e o app não.",
            )
        } else {
            EstadoVazio(
                icone = Icons.Outlined.SearchOff,
                titulo = "Nenhuma matéria encontrada",
                mensagem = "Confira o nome ou troque o filtro.",
            )
        }
    }
}

/**
 * O botão de criar. No grátis, embaixo do texto vai quantas ainda cabem ([restam]); sem nenhuma, a
 * etiqueta do Pro avisa antes do toque, como nos outros botões de criar matéria.
 */
@Composable
private fun BotaoNovaMateria(restam: Int?, onClick: () -> Unit) {
    val soNoPro = restam == 0
    ExtendedFloatingActionButton(
        onClick = onClick,
        // O FAB do Material esconde o texto do leitor de tela: quem diz o que o botão faz é a descrição do ícone
        icon = { Icon(Icons.Filled.Add, contentDescription = rotuloDaMateriaNova(soNoPro)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Nova matéria")
                    if (soNoPro) {
                        Spacer(Modifier.width(Espacos.s))
                        EtiquetaPro(compacta = true)
                    }
                }
                if (restam != null && restam > 0) {
                    Text(text = textoDasQueRestam(restam), style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    )
}

// O fim da lista passa do botão de criar: a última linha não fica atrás dele
private val ESPACO_DO_BOTAO = 96.dp

// A busca e o filtro ------------------------------------------------------------------------

/** O filtro do topo. Se os nomes inteiros não couberem (fonte grande), o seletor usa os curtos. */
internal enum class FiltroDeMaterias(val opcao: OpcaoDoSeletor) {
    TODAS(OpcaoDoSeletor("Todas")),
    DO_APP(OpcaoDoSeletor("Padrão do app", curto = "App")),
    DO_ALUNO(OpcaoDoSeletor("Criadas por você", curto = "Suas"));

    fun aceita(materia: MateriaDomain): Boolean = when (this) {
        TODAS -> true
        DO_APP -> !materia.propria
        DO_ALUNO -> materia.propria
    }
}

private val OPCOES_DO_FILTRO = FiltroDeMaterias.entries.map { it.opcao }

/** Os dois grupos da lista, cada um na ordem do cadastro. */
internal data class GruposDeMaterias(
    val doApp: List<MateriaDoCadastro>,
    val doAluno: List<MateriaDoCadastro>,
) {
    val vazios: Boolean
        get() = doApp.isEmpty() && doAluno.isEmpty()
}

/** As matérias que passam pelo [filtro] e têm a [busca] no nome, sem ligar para maiúsculas e acentos. */
internal fun gruposDeMaterias(
    materias: List<MateriaDoCadastro>,
    busca: String,
    filtro: FiltroDeMaterias,
): GruposDeMaterias {
    val procurado = busca.trim().semAcentos()
    val (doAluno, doApp) = materias
        .filter { filtro.aceita(it.materia) && procurado in it.materia.nome.semAcentos() }
        .partition { it.materia.propria }
    return GruposDeMaterias(doApp = doApp, doAluno = doAluno)
}
