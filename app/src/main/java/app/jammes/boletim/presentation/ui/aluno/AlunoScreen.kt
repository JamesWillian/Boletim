package app.jammes.boletim.presentation.ui.aluno

import android.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.PeriodoDomain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlunoScreen(
    modifier: Modifier = Modifier,
    viewModel: AlunoViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var alunoEdicao by remember { mutableStateOf<AlunoDomain?>(null) }

    LaunchedEffect(state.isLoading, state.aluno) {
        if (!state.isLoading && state.aluno == null) {
            alunoEdicao = null
            showEditDialog = true
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Aluno",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Left,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                windowInsets = WindowInsets(0,0,0,0)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->

        when {
            state.isLoading -> {
                Box(modifier = Modifier.padding(paddingValues).fillMaxSize()
                ) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            state.aluno != null -> {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(horizontal = 14.dp)
                        .fillMaxSize()
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                                .height(IntrinsicSize.Min)
                        ) {
                            Icon(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(end = 12.dp)
                                    .aspectRatio(1f),
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "Icone do Aluno",
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column() {
                                    Text(
                                        text = "Bem-vindo",
                                        textAlign = TextAlign.Start,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        text = state.aluno?.nome ?: "",
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                }

                                IconButton(onClick = {
                                    alunoEdicao = state.aluno; showEditDialog = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar Nome do Aluno",
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

    if (showEditDialog) {
        AlunoFormDialog(
            alunoInitial = alunoEdicao,
            onDismiss = { showEditDialog = false },
            onConfirm = { aluno ->
                viewModel.save(aluno)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun AlunoFormDialog(
    modifier: Modifier = Modifier,
    alunoInitial: AlunoDomain? = null,
    onDismiss: () -> Unit,
    onConfirm: (AlunoDomain) -> Unit
) {
    var nome by remember { mutableStateOf(alunoInitial?.nome.orEmpty()) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = { if (alunoInitial != null) onDismiss() },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        AlunoDomain(
                            id = alunoInitial?.id ?: "",
                            nome = nome,
                            anoLetivoId = alunoInitial?.anoLetivoId,
                            periodoId = alunoInitial?.periodoId
                        )
                    )
                },
                enabled = nome.isNotBlank()
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = alunoInitial?.id?.isNotBlank() ?: false
            ) { Text("Cancelar") }
        },
        title = {
            Text(
                text = "Registro de Aluno",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            OutlinedTextField(
                value = nome,
                onValueChange = {nome = it},
                label = { Text("Nome") },
                singleLine = true,
                shape = AbsoluteRoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}