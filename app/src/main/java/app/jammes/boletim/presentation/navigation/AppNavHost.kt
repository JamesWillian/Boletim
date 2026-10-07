package app.jammes.boletim.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import app.jammes.boletim.presentation.ui.aluno.AlunoScreen
import app.jammes.boletim.presentation.ui.anoletivo.AnoLetivoScreen
import app.jammes.boletim.presentation.ui.boletim.BoletimScreen
import app.jammes.boletim.presentation.ui.disciplina.DisciplinaDetailScreen
import app.jammes.boletim.presentation.ui.materia.MateriaScreen

/**
 * Grafo de navegação: decide qual tela ocupa o centro do [AppScaffold].
 *
 * Segue a árvore de [Routes]. Tela nova que precisa das abas de disciplinas entra dentro de
 * `navigation<Routes.Boletim>`; as outras ficam fora e aparecem sem elas ([mostraAbasDisciplinas]).
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Boletim,
        modifier = modifier,
    ) {
        // Com as abas de disciplinas
        navigation<Routes.Boletim>(startDestination = Routes.Boletim.Geral) {
            composable<Routes.Boletim.Geral> {
                BoletimScreen(onAbrirDisciplina = { id -> navController.abrirDisciplina(id) })
            }
            composable<Routes.Boletim.Disciplina> {
                DisciplinaDetailScreen() // o id chega no ViewModel pelo SavedStateHandle
            }
        }

        // Sem as abas
        composable<Routes.Materia> { MateriaScreen() }
        composable<Routes.AnoLetivo> {
            AnoLetivoScreen(onVoltar = { navController.fecharAjustesAnoLetivo() })
        }
        composable<Routes.Aluno> { AlunoScreen() }
    }
}

// Ações de navegação ------------------------------------------------------------------------

/** Abre os ajustes do ano letivo por cima da tela atual; tocar de novo no botão não empilha outra. */
fun NavController.abrirAjustesAnoLetivo() {
    navigate(Routes.AnoLetivo) {
        launchSingleTop = true
    }
}

/**
 * Fecha os ajustes do ano letivo. Só tira essa tela da pilha: se for chamado duas vezes
 * (salvou e tocou em voltar ao mesmo tempo), a segunda não leva junto a tela de baixo.
 */
fun NavController.fecharAjustesAnoLetivo() {
    popBackStack<Routes.AnoLetivo>(inclusive = true)
}

/** Volta ao Boletim Geral, tirando da pilha a disciplina que estiver aberta. */
fun NavController.abrirBoletimGeral() {
    navigate(Routes.Boletim.Geral) {
        popUpTo<Routes.Boletim.Geral>()
        launchSingleTop = true
    }
}

/**
 * Abre a disciplina no lugar da que estiver aberta: trocar de aba não empilha telas,
 * então o voltar de qualquer disciplina leva ao Boletim Geral.
 */
fun NavController.abrirDisciplina(disciplinaId: Long) {
    if (currentBackStackEntry?.disciplinaAbertaId == disciplinaId) return

    navigate(Routes.Boletim.Disciplina(disciplinaId)) {
        popUpTo<Routes.Boletim.Geral>()
    }
}

// O que a moldura (AppScaffold) precisa saber da tela atual ---------------------------------

/**
 * O card de identificação some nos ajustes do ano letivo: a tela tem barra própria, e o card
 * mostraria o ano como está gravado enquanto ele está sendo editado.
 */
val NavBackStackEntry.mostraIdentificacao: Boolean
    get() = !destination.hasRoute<Routes.AnoLetivo>()

/** As abas aparecem em toda tela que está dentro do grafo [Routes.Boletim]. */
val NavBackStackEntry.mostraAbasDisciplinas: Boolean
    get() = destination.hierarchy.any { it.hasRoute<Routes.Boletim>() }

/** Disciplina aberta no centro; null em qualquer outra tela, inclusive no Boletim Geral. */
val NavBackStackEntry.disciplinaAbertaId: Long?
    get() = if (destination.hasRoute<Routes.Boletim.Disciplina>()) {
        toRoute<Routes.Boletim.Disciplina>().disciplinaId
    } else {
        null
    }
