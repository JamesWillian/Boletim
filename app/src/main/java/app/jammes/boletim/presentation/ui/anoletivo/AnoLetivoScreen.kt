package app.jammes.boletim.presentation.ui.anoletivo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import kotlin.text.toInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnoLetivoScreen(
    modifier: Modifier = Modifier,
    viewModel: AnoLetivoViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showAnoLetivoForm by remember { mutableStateOf(false) }
    var showPeriodoForm by remember { mutableStateOf(false) }
    var anoLetivoDeleting by remember { mutableStateOf<AnoLetivoDomain?>(null) }
    var periodoDeleting by remember { mutableStateOf<PeriodoDomain?>(null) }
    var anoLetivoEditing by remember { mutableStateOf<AnoLetivoDomain?>(null) }
    var periodoEditing by remember { mutableStateOf<PeriodoDomain?>(null) }


    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ano Letivo",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Left,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { showAnoLetivoForm = true }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Novo Ano Letivo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                windowInsets = WindowInsets(0,0,0,0)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 14.dp)
                .fillMaxSize()
        ) {
            AnoLetivoAtualCard(state.anoLetivoPadrao?.descricao, state.periodoPadrao)

            Column(
                modifier = Modifier.padding(top = 32.dp, bottom = 24.dp)
            ) {
                LazyRow(
                    modifier = Modifier.height(50.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .clip(shape = AbsoluteRoundedCornerShape(30))
                                .fillMaxSize()
                                .background(color = Color.LightGray)
                                .clickable(onClick = { viewModel.onAnoLetivoSelected(item.id) }),
                            border = if (state.anoLetivoSelecionado?.id == item.id)
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else null
                        ) {
                            Text(
                                text = item.descricao,
                                modifier = Modifier
                                    .padding(horizontal = 14.dp)
                                    .fillMaxSize()
                                    .wrapContentSize(Alignment.Center),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    TextButton(
                        onClick = { anoLetivoEditing = state.anoLetivoSelecionado; showAnoLetivoForm = true }
                    ) {
                        Text("Editar")
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    TextButton(
                        onClick = { anoLetivoDeleting = state.anoLetivoSelecionado },
                    ) {
                        Text("Excluir", color = Color.Red)
                    }

                    if (state.anoLetivoPadrao?.id != state.anoLetivoSelecionado?.id) {
                        Spacer(modifier = Modifier.width(5.dp))

                        OutlinedButton(
                            onClick = {
                                state.anoLetivoSelecionado?.let {
                                    viewModel.setAnoLetivoPadrao(
                                        it.id
                                    )
                                }
                            }
                        ) {
                            Text("Definir como Padrão")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "PERIODOS",
                    modifier = Modifier
                        .fillMaxHeight()
                        .wrapContentSize(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(onClick = { periodoEditing = null; showPeriodoForm = !state.anoLetivoSelecionado?.id.isNullOrEmpty() }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Novo Periodo",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            state.anoLetivoSelecionado?.let { selecionado ->
                LazyColumn(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(selecionado.periodo, key = { it.id }) { item ->
                        var showPeriodoMenu by remember { mutableStateOf(false) }
                        Card(
                            modifier = Modifier
                                .clip(shape = AbsoluteRoundedCornerShape(30))
                                .fillMaxSize()
                                .background(color = Color.LightGray)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.periodo} ${state.anoLetivoSelecionado?.periodoType?.displayName}",
                                    modifier = Modifier
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .fillMaxHeight()
                                        .wrapContentSize(Alignment.Center)
                                )
                                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                                    IconButton(onClick = { showPeriodoMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Mais Opções do Periodo",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showPeriodoMenu,
                                        onDismissRequest = { showPeriodoMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Editar Periodo") },
                                            leadingIcon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = "Editar Periodo") },
                                            onClick = {
                                                showPeriodoMenu = false
                                                periodoEditing = item
                                                showPeriodoForm = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Deletar Período") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = "Deletar Período",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                showPeriodoMenu = false
                                                periodoDeleting = item
                                            }
                                        )
                                        if (state.anoLetivoPadrao?.id == state.anoLetivoSelecionado?.id) {
                                            DropdownMenuItem(
                                                text = { Text("Definir como Padrão") },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Outlined.CheckCircleOutline,
                                                        contentDescription = "Definir como Padrão"
                                                    )
                                                },
                                                onClick = {
                                                    showPeriodoMenu = false
                                                    viewModel.setPeriodoPadrao(item.id)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAnoLetivoForm) {
        AnoLetivoFormDialog(
            anoLetivoInitial = anoLetivoEditing,
            onDismiss = { showAnoLetivoForm = false },
            onConfirm = { anoLetivo ->
                viewModel.save(anoLetivo)
                showAnoLetivoForm = false
            }
        )
    }

    if (showPeriodoForm) {
        state.anoLetivoSelecionado?.let { anoLetivo ->
            PeriodoFormDialog(
                periodoEdit = periodoEditing,
                anoLetivoId = anoLetivo.id,
                onDismiss = { showPeriodoForm = false },
                onConfirm = { periodo ->
                    viewModel.savePeriodo(periodo)
                    showPeriodoForm = false
                }
            )
        }
    }

    anoLetivoDeleting?.let { item ->
        DeleteDialogForm(
            title = "Deletar Ano Letivo?",
            text = "Confirma a exclusão do ano letivo ${item.descricao}?",
            onDismiss = { anoLetivoDeleting = null },
            onConfirm = {
                viewModel.delete(item)
                anoLetivoDeleting = null
            }
        )
    }

    periodoDeleting?.let { item ->
        DeleteDialogForm(
            title = "Deletar Periodo?",
            text = "Confirma a exclusão do periodo ${item.periodo}?",
            onDismiss = { periodoDeleting = null },
            onConfirm = {
                viewModel.deletePeriodo(item)
                periodoDeleting = null
            }
        )
    }
}

@Composable
fun AnoLetivoAtualCard(anoLetivo: String?, periodo: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Ano Letivo Atual".uppercase(),
                    modifier = Modifier.padding(bottom = 3.dp),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = anoLetivo ?: "Não Definido",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Período Atual".uppercase(),
                    modifier = Modifier.padding(bottom = 3.dp),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = periodo ?: "Não Definido",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnoLetivoFormDialog(
    anoLetivoInitial: AnoLetivoDomain? = null,
    onDismiss: () -> Unit,
    onConfirm: (AnoLetivoDomain) -> Unit
) {

    var descricao by remember { mutableStateOf(anoLetivoInitial?.descricao.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        AnoLetivoDomain(
                            id = anoLetivoInitial?.id ?: "",
                            descricao = descricao,
                            periodo = emptyList()
                        )
                    )
                }
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onDismiss) { Text("Cancelar") }
        },
        title = {
            Text(
                text = "Novo Ano Letivo",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            OutlinedTextField(
                value = descricao,
                onValueChange = {descricao = it},
                label = { Text("Ano Letivo") },
                singleLine = true,
                shape = AbsoluteRoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodoFormDialog(
    periodoEdit: PeriodoDomain? = null,
    anoLetivoId: String,
    onDismiss: () -> Unit,
    onConfirm: (PeriodoDomain) -> Unit
) {

    var periodo by remember { mutableStateOf(periodoEdit?.periodo?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        PeriodoDomain(
                            id = periodoEdit?.id ?: "",
                            periodo = periodo.toInt(),
                            anoLetivoId = anoLetivoId
                        )
                    )
                }
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onDismiss) { Text("Cancelar") }
        },
        title = {
            Text(
                text = "Novo Periodo",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            OutlinedTextField(
                value = periodo,
                onValueChange = {periodo = it},
                label = { Text("Periodo") },
                suffix = { Text("Semestre") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = AbsoluteRoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun DeleteDialogForm(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onConfirm) { Text("Deletar") }
        },
        dismissButton = {
            TextButton(onDismiss) { Text("Cancelar") }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}