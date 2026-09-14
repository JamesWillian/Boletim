package app.jammes.boletim.presentation.navigation

import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.presentation.contexto.Contexto
import app.jammes.boletim.presentation.contexto.ContextoUiState
import app.jammes.boletim.presentation.contexto.ContextoViewModel
import app.jammes.boletim.presentation.ui.aluno.AlunoScreen
import app.jammes.boletim.presentation.ui.anoletivo.AnoLetivoScreen
import app.jammes.boletim.presentation.ui.boletim.BoletimScreen
import app.jammes.boletim.presentation.ui.materia.MateriaScreen

@Composable
fun AppScaffold(modifier: Modifier = Modifier) {
    val owner = LocalActivity.current as ViewModelStoreOwner
    val contextoVm: ContextoViewModel = hiltViewModel(owner)
    val state by contextoVm.state.collectAsStateWithLifecycle()
    val alunos by contextoVm.alunos.collectAsStateWithLifecycle()
    val anosLetivos by contextoVm.anosLetivos.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    when (val s = state) {
        ContextoUiState.Carregando -> CircularProgressIndicator(modifier = Modifier.fillMaxSize().wrapContentSize(align = Alignment.Center)) //TelaCarregando()
        ContextoUiState.Vazio -> {}//OnboardingScreen()
        is ContextoUiState.Definido ->
        Scaffold(
            topBar = {
                IdentifacaoCard(
                    modifier = Modifier.padding(top = 24.dp),
                    contexto = s.contexto,
                    aluno = alunos.find { it.id == s.contexto.alunoId },
                    anosLetivos = anosLetivos,
                    onSelectPeriodo = { periodo ->
                        contextoVm.trocarPeriodo(periodo.id)
                    }
                )
            }
        ) { paddingValues ->

            NavHost(
                navController = navController,
                startDestination = Routes.BOLETIM,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Routes.MATERIA) {
                    MateriaScreen()
                }
                composable(Routes.ANO_LETIVO) {
                    AnoLetivoScreen()
                }
                composable(Routes.BOLETIM) {
                    BoletimScreen()
                }
                composable(Routes.ALUNO) {
                    AlunoScreen()
                }
            }
        }
    }
}

@Composable
fun IdentifacaoCard(
    modifier: Modifier = Modifier,
    contexto: Contexto,
    aluno: AlunoDomain?,
    anosLetivos: List<AnoLetivoDomain> = emptyList(),
    onSelectPeriodo: (PeriodoDomain) -> Unit = {}
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
                    .fillMaxWidth()
                    .padding(start = 12.dp)) {
                    Text(aluno?.nome ?: "")
                    Text(if (ano?.id != null) "${ano.ano} - ${ano.serie}" else "")
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