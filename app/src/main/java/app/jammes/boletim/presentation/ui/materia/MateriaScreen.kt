package app.jammes.boletim.presentation.ui.materia

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.presentation.ui.theme.BoletimColors
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriaScreen(
    modifier: Modifier = Modifier,
    viewModel: MateriaViewModel = hiltViewModel()
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedMateria by remember { mutableStateOf(MateriaDomain()) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Matérias",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Left,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { selectedMateria = MateriaDomain(); showBottomSheet = true }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Nova Matéria",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                windowInsets = WindowInsets(0,0,0,0)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->

        Column(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
        ) {
            if (state.items.isEmpty() && !state.isLoading) {
                Text(
                    text = "Nenhuma matéria cadastrada",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = {
                                    selectedMateria = item
                                    showBottomSheet = true
                                }),
                            colors = CardDefaults.cardColors(CoresDisciplina.de(item.cor)),
                            shape = AbsoluteRoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = item.nome,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        MateriaBottomSheet(
            materia = selectedMateria,
            onDismiss = { showBottomSheet = false },
            onConfirm = {
                viewModel.salvar(it)
                showBottomSheet = false
            },
            onDelete = {
                viewModel.deletar(selectedMateria)
                showBottomSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriaBottomSheet(
    materia: MateriaDomain = MateriaDomain(),
    onDismiss: () -> Unit,
    onConfirm: (materia: MateriaDomain) -> Unit,
    onDelete: () -> Unit
) {

    val sheetState = rememberModalBottomSheetState()

    var nome by remember { mutableStateOf(materia.nome) }
    var abreviacao by remember { mutableStateOf(materia.abreviacao ?: "") }
    var selectedColor by remember { mutableIntStateOf(materia.cor) }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            materia.nome.isEmpty().let { isNew ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(35.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isNew) "Nova Matéria" else "Editar Matéria",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .fillMaxHeight()
                            .wrapContentSize(Alignment.Center)
                    )

                    if (!isNew)
                        TextButton(
                            onClick = { onDelete() },
                            modifier = Modifier.fillMaxHeight(),
                        ) {
                            Text("Deletar")
                        }
                }
            }
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = abreviacao,
                onValueChange = { abreviacao = it },
                label = { Text("Abreviação") },
                modifier = Modifier.fillMaxWidth()
            )
            ColorPicker(
                modifier = Modifier.padding(vertical = 8.dp),
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancelar") }

                Button(
                    onClick = {
                        onConfirm(
                            materia.copy(nome = nome, abreviacao = abreviacao, cor = selectedColor)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = nome.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Salvar") }
            }
        }
    }
}

@Composable
fun ColorPicker(
    modifier: Modifier = Modifier,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(CoresDisciplina.paleta.size) { color ->
            val isSelected = color == selectedColor

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CoresDisciplina.de(color))
                    .then(
                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier
                    )
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(BoletimColors.OnPrimary)
                    )
                }
            }
        }
    }
}