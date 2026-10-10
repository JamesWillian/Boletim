package app.jammes.boletim.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import app.jammes.boletim.presentation.ui.aluno.AlunoScreen
import app.jammes.boletim.presentation.ui.anoletivo.AnoLetivoScreen
import app.jammes.boletim.presentation.ui.boletim.BoletimScreen
import app.jammes.boletim.presentation.ui.components.DURACAO_SAIDA
import app.jammes.boletim.presentation.ui.components.DURACAO_TRANSICAO
import app.jammes.boletim.presentation.ui.components.LocalEscopoCompartilhado
import app.jammes.boletim.presentation.ui.components.LocalEscopoDaTela
import app.jammes.boletim.presentation.ui.disciplina.DisciplinaDetailScreen
import app.jammes.boletim.presentation.ui.materia.MateriaScreen

/**
 * Grafo de navegação: decide qual tela ocupa o centro do [AppScaffold].
 *
 * Segue a árvore de [Routes]. Tela nova que precisa das abas de disciplinas entra dentro de
 * `navigation<Routes.Boletim>`; as outras ficam fora e aparecem sem elas ([mostraAbasDisciplinas]).
 * Um `dialog` abre por cima da tela atual, e a moldura continua a dela.
 *
 * O [SharedTransitionLayout] em volta é o que deixa o card do Boletim se transformar no resumo do
 * detalhe: as telas que participam recebem a própria animação por [LocalEscopoDaTela].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    SharedTransitionLayout(modifier) {
        CompositionLocalProvider(LocalEscopoCompartilhado provides this) {
            NavHost(
                navController = navController,
                startDestination = Routes.Boletim,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { entrada },
                exitTransition = { saida },
                popEnterTransition = { entrada },
                popExitTransition = { saida },
            ) {
                // Com as abas de disciplinas
                navigation<Routes.Boletim>(startDestination = Routes.Boletim.Geral) {
                    composable<Routes.Boletim.Geral> {
                        CompositionLocalProvider(LocalEscopoDaTela provides this) {
                            BoletimScreen(onAbrirDisciplina = { id -> navController.abrirDisciplina(id) })
                        }
                    }
                    composable<Routes.Boletim.Disciplina> {
                        CompositionLocalProvider(LocalEscopoDaTela provides this) {
                            DisciplinaDetailScreen() // o id chega no ViewModel pelo SavedStateHandle
                        }
                    }
                }

                // Sem as abas
                composable<Routes.Materia> { MateriaScreen() }
                // Os ajustes sobem de leve por cima, como uma folha que se abre, e descem ao fechar
                composable<Routes.AnoLetivo>(
                    enterTransition = { subindo },
                    popExitTransition = { descendo },
                ) {
                    AnoLetivoScreen(onVoltar = { navController.fecharAjustesAnoLetivo() })
                }
                // O perfil abre como dialog, por cima da tela atual. A janela dele ocupa a tela toda,
                // e o AlunoScreen põe o card no alto
                dialog<Routes.Aluno>(
                    dialogProperties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false,
                    ),
                ) {
                    AlunoScreen(
                        onFechar = { navController.fecharPerfilDoAluno() },
                        // As telas abertas eram do aluno ou do ano anterior: volta ao Boletim Geral
                        onContextoTrocado = { navController.abrirBoletimGeral() },
                        onAnoLetivoCriado = {
                            navController.abrirBoletimGeral()
                            navController.abrirAjustesAnoLetivo()
                        },
                    )
                }
            }
        }
    }
}

// Transições ---------------------------------------------------------------------------------

// "Fade through" do Material: a tela que sai some rápido e só então a nova aparece, sem as duas
// se misturarem no meio. A troca inteira dura DURACAO_TRANSICAO, o mesmo tempo do card que se
// transforma em resumo, para os dois terminarem juntos.
private val entrada: EnterTransition =
    fadeIn(tween(durationMillis = DURACAO_TRANSICAO - DURACAO_SAIDA, delayMillis = DURACAO_SAIDA))

private val saida: ExitTransition = fadeOut(tween(durationMillis = DURACAO_SAIDA))

private val subindo: EnterTransition =
    slideInVertically(tween(DURACAO_TRANSICAO)) { altura -> altura / 10 } + fadeIn(tween(DURACAO_TRANSICAO))

private val descendo: ExitTransition =
    slideOutVertically(tween(DURACAO_TRANSICAO)) { altura -> altura / 10 } + fadeOut(tween(DURACAO_TRANSICAO))

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

/** Abre o perfil do aluno por cima da tela atual; tocar de novo na identificação não empilha outro. */
fun NavController.abrirPerfilDoAluno() {
    navigate(Routes.Aluno) {
        launchSingleTop = true
    }
}

/** Fecha o perfil. Como nos ajustes do ano, só tira ele da pilha, mesmo se for chamado duas vezes. */
fun NavController.fecharPerfilDoAluno() {
    popBackStack<Routes.Aluno>(inclusive = true)
}

/** Volta ao Boletim Geral, tirando da pilha a disciplina que estiver aberta (e o perfil, se aberto). */
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
