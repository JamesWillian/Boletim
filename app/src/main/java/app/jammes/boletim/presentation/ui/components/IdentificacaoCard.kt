package app.jammes.boletim.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.Contexto
import app.jammes.boletim.domain.model.PeriodoDomain

/** Topo do app: aluno, ano letivo, o botão dos ajustes do ano e os chips para trocar de período. */
@Composable
fun IdentificacaoCard(
    modifier: Modifier = Modifier,
    contexto: Contexto,
    aluno: AlunoDomain?,
    anosLetivos: List<AnoLetivoDomain> = emptyList(),
    onSelectPeriodo: (PeriodoDomain) -> Unit = {},
    onAbrirAjustesAnoLetivo: () -> Unit = {}
) {
    val ano = anosLetivos.find { ano -> ano.id == contexto.anoLetivoId }
    val periodos = ano?.periodo.orEmpty()
    val tipoPeriodo = ano?.tipoPeriodo?.displayName

    Surface(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .height(IntrinsicSize.Min)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                ) {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Avatar Usuário",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)) {
                    Text(aluno?.nome ?: "")
                    Text(if (ano?.id != null) "${ano.ano} - ${ano.serie}" else "")
                }
                IconButton(
                    onClick = onAbrirAjustesAnoLetivo,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = "Ajustes do ano letivo")
                }
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(periodos) { periodo ->
                    FilterChip(
                        selected = (contexto.periodoId == periodo.id),
                        onClick = { onSelectPeriodo(periodo) },
                        label = { Text("${ periodo.periodo } $tipoPeriodo") }
                    )
                }
            }
        }
    }
}
