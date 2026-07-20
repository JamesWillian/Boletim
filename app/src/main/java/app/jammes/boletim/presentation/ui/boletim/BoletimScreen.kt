package app.jammes.boletim.presentation.ui.boletim

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoletimScreen(
    modifier: Modifier = Modifier,
    viewModel: BoletimViewModel = hiltViewModel()
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Boletim",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Left,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Nova Disciplina",
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
            .padding(horizontal = 14.dp)
            .fillMaxSize()
        ) {
            Row() {
                Text(
                    text = "Aluno: ",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = state.aluno?.nome ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(bottom = 8.dp),
                color = Color.Blue
            )
            Row() {
                Text(
                    text = "Ano: ",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${state.periodoSelecionado?.periodo.toString()} ${state.aluno?.periodoType?.displayName}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(bottom = 8.dp),
                color = Color.Blue
            )
        }
    }
}