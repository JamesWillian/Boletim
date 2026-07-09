package app.jammes.boletim.presentation.ui.anoletivo

import android.app.AlertDialog
import android.graphics.drawable.shapes.Shape
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PeriodoDomain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnoLetivoScreen(
    modifier: Modifier = Modifier,
    viewModel: AnoLetivoViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showAnoLetivoForm by remember { mutableStateOf(false) }

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
        Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 14.dp).fillMaxSize()) {
            AnoLetivoAtualCard(null, null)

            LazyRow(
                modifier = Modifier.padding(vertical = 32.dp).height(50.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(state.items, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .clip(shape = AbsoluteRoundedCornerShape(30))
                            .fillMaxSize()
                            .background(color = Color.LightGray)
                            .clickable(onClick = { viewModel.onAnoLetivoSelected(item.id) })
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

            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "PERIODOS",
                    modifier = Modifier.fillMaxHeight().wrapContentSize(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Novo Periodo",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            state.anoLetivoSelecionado?.let { selecionado ->
                LazyColumn(
                    modifier = Modifier.padding(vertical = 8.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(selecionado.periodo, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .clip(shape = AbsoluteRoundedCornerShape(30))
                                .fillMaxSize()
                                .background(color = Color.LightGray)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.periodo} Semestre",
                                    modifier = Modifier
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .fillMaxHeight()
                                        .wrapContentSize(Alignment.Center)
                                )
                                Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                                    IconButton(onClick = { /*TODO*/ }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Deletar Periodo",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    IconButton(onClick = { /*TODO*/ }) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Editar Periodo",
                                            tint = MaterialTheme.colorScheme.primary
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

    if (showAnoLetivoForm) {
        AnoLetivoFormDialog(
            onDismiss = { showAnoLetivoForm = false },
            onConfirm = { anoLetivo ->
                viewModel.save(anoLetivo)
                showAnoLetivoForm = false
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
    onDismiss: () -> Unit,
    onConfirm: (AnoLetivoDomain) -> Unit
) {

    var descricao by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        AnoLetivoDomain(
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