package app.jammes.boletim.presentation.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.Contexto
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.presentation.ui.components.AbasDisciplinas
import app.jammes.boletim.presentation.ui.components.IdentificacaoCard

/**
 * Moldura do app quando já existe um contexto. Só monta o layout:
 *
 * ```
 * ┌─────────────────────────────────┐
 * │ IdentificacaoCard               │
 * ├────────────────────────────┬────┤
 * │                            │    │
 * │ AppNavHost                 │ ◄──── AbasDisciplinas, só nas telas
 * │ (a tela aberta no momento) │    │   do grafo Routes.Boletim
 * │                            │    │
 * └────────────────────────────┴────┘
 * ```
 *
 * Os dados chegam prontos do [AppRoot], e quais telas existem é o [AppNavHost] que define.
 */
@Composable
fun AppScaffold(
    contexto: Contexto,
    aluno: AlunoDomain?,
    anosLetivos: List<AnoLetivoDomain>,
    disciplinas: List<DisciplinaDomain>,
    onSelecionarPeriodo: (PeriodoDomain) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val entradaAtual by navController.currentBackStackEntryAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            IdentificacaoCard(
                modifier = Modifier.statusBarsPadding(),
                contexto = contexto,
                aluno = aluno,
                anosLetivos = anosLetivos,
                onSelectPeriodo = onSelecionarPeriodo
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .padding(paddingValues)
                // As telas têm Scaffold próprio: assim elas não somam de novo as barras do sistema
                .consumeWindowInsets(paddingValues)
                .fillMaxSize(),
        ) {
            AppNavHost(
                navController = navController,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            if (entradaAtual?.mostraAbasDisciplinas == true) {
                AbasDisciplinas(
                    disciplinas = disciplinas,
                    disciplinaAbertaId = entradaAtual?.disciplinaAbertaId,
                    onAbrirBoletimGeral = { navController.abrirBoletimGeral() },
                    onAbrirDisciplina = { id -> navController.abrirDisciplina(id) }
                )
            }
        }
    }
}
