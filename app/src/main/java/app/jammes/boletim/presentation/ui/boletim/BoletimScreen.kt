package app.jammes.boletim.presentation.ui.boletim

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.DisciplinaResumo
import app.jammes.boletim.domain.model.StatusDisciplina
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoletimScreen(
    modifier: Modifier = Modifier,
    viewModel: BoletimViewModel = hiltViewModel()
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->

        when (state) {
            BoletimUiState.Carregando -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            BoletimUiState.SemDisciplinas -> Column(
                modifier = modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Nenhuma matéria neste ano", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Adicione as matérias para o boletim começar a aparecer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = {}) { Text("Adicionar matérias") }
            }

            is BoletimUiState.Sucesso -> LazyVerticalGrid(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 96.dp,
                ),
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ResumoGeral(
                        mediaGeral = (state as BoletimUiState.Sucesso).boletim.mediaGeral,
                        totalFaltas = (state as BoletimUiState.Sucesso).boletim.totalFaltas,
                    )
                }
                items((state as BoletimUiState.Sucesso).boletim.disciplinas, key = { it.id }) {
                    DisciplinaCard(resumo = it)
                }
            }
        }
    }
}

@Composable
private fun ResumoGeral(
    mediaGeral: Double?,
    totalFaltas: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Média do período",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatarMedia(mediaGeral),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = when (totalFaltas) {
                0 -> "Sem faltas"
                1 -> "1 falta"
                else -> "$totalFaltas faltas"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

@Composable
private fun DisciplinaCard(
    resumo: DisciplinaResumo,
//    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val corStatus = corDoStatus(resumo.status)
    val corFaltas = if (resumo.emRiscoPorFalta) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
//        onClick = onClick,
        modifier = modifier.height(120.dp)
    ) {
        Row(Modifier.fillMaxSize()) {
            // Faixa de identidade da disciplina
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxSize()
                    .background(CoresDisciplina.de(resumo.cor))
            )
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                Text(
                    text = resumo.nome,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    minLines = 2, // mantém os cards alinhados na grade
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatarMedia(resumo.media),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = corStatus,
                )
                Text(
                    text = "${resumo.faltas} faltas",
                    style = MaterialTheme.typography.labelMedium,
                    color = corFaltas,
                )
            }
        }
    }
}

@Composable
private fun corDoStatus(status: StatusDisciplina): Color = when (status) {
    StatusDisciplina.APROVADO -> Color(0xFF2E7D32)
    StatusDisciplina.ATENCAO -> Color(0xFFB26A00)
    StatusDisciplina.ABAIXO -> MaterialTheme.colorScheme.error
    StatusDisciplina.REPROVADO -> MaterialTheme.colorScheme.error
    StatusDisciplina.SEM_NOTA -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun formatarMedia(media: Double?): String =
    media?.let { String.format(Locale("pt", "BR"), "%.1f", it) } ?: "—"