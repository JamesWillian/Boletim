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
import app.jammes.boletim.presentation.ui.backup.BackupScreen
import app.jammes.boletim.presentation.ui.boletim.BoletimScreen
import app.jammes.boletim.presentation.ui.configuracoes.ConfiguracoesScreen
import app.jammes.boletim.presentation.ui.components.DURACAO_SAIDA
import app.jammes.boletim.presentation.ui.components.DURACAO_TRANSICAO
import app.jammes.boletim.presentation.ui.components.LocalEscopoCompartilhado
import app.jammes.boletim.presentation.ui.components.LocalEscopoDaTela
import app.jammes.boletim.presentation.ui.disciplina.DisciplinaDetailScreen
import app.jammes.boletim.presentation.ui.disciplina.EditarDisciplinaScreen
import app.jammes.boletim.presentation.ui.materia.MateriaScreen
import app.jammes.boletim.presentation.ui.notificacoes.ConfiguracoesDasNotificacoesScreen
import app.jammes.boletim.presentation.ui.notificacoes.NotificacoesScreen
import app.jammes.boletim.presentation.ui.onboarding.OnboardingScreen

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
                            DisciplinaDetailScreen( // o id chega no ViewModel pelo SavedStateHandle
                                onEditar = { id -> navController.abrirEdicaoDaDisciplina(id) },
                            )
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
                // A edição da disciplina sobe por cima do detalhe, como os ajustes
                composable<Routes.EditarDisciplina>(
                    enterTransition = { subindo },
                    popExitTransition = { descendo },
                ) {
                    EditarDisciplinaScreen( // o id chega no ViewModel pelo SavedStateHandle
                        onVoltar = { navController.fecharEdicaoDaDisciplina() },
                    )
                }
                // O cadastro de outro aluno é o onboarding a partir do passo do aluno; sobe como os ajustes
                composable<Routes.NovoAluno>(
                    enterTransition = { subindo },
                    popExitTransition = { descendo },
                ) {
                    OnboardingScreen(
                        novoAluno = true,
                        onFechar = { navController.fecharNovoAluno() },
                        // O aluno novo já está aberto: o boletim dele toma o lugar de tudo
                        onConcluido = { navController.abrirBoletimGeral() },
                    )
                }
                // As configurações sobem como os ajustes
                composable<Routes.Configuracoes>(
                    enterTransition = { subindo },
                    popExitTransition = { descendo },
                ) {
                    ConfiguracoesScreen(onVoltar = { navController.fecharConfiguracoes() })
                }
                // O backup e a restauração também
                composable<Routes.Backup>(
                    enterTransition = { subindo },
                    popExitTransition = { descendo },
                ) {
                    BackupScreen(
                        onVoltar = { navController.fecharBackup() },
                        // As telas abertas eram dos dados de antes: o boletim importado toma o lugar de tudo
                        onImportado = { navController.abrirBoletimGeral() },
                    )
                }
                // As notificações sobem como os ajustes, e as configurações delas sobem por cima da lista
                composable<Routes.Notificacoes>(
                    enterTransition = { subindo },
                    popExitTransition = { descendo },
                ) {
                    NotificacoesScreen(
                        onVoltar = { navController.fecharNotificacoes() },
                        onConfigurar = { navController.abrirConfiguracoesDasNotificacoes() },
                    )
                }
                composable<Routes.ConfiguracoesDasNotificacoes>(
                    enterTransition = { subindo },
                    popExitTransition = { descendo },
                ) {
                    ConfiguracoesDasNotificacoesScreen(
                        onVoltar = { navController.fecharConfiguracoesDasNotificacoes() },
                    )
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
                        onNovoAluno = { navController.abrirNovoAluno() },
                        onBackup = { navController.abrirBackup() },
                        onConfiguracoes = { navController.abrirConfiguracoes() },
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

/** Abre a edição da disciplina por cima do detalhe; tocar de novo no Editar não empilha outra. */
fun NavController.abrirEdicaoDaDisciplina(disciplinaId: Long) {
    navigate(Routes.EditarDisciplina(disciplinaId)) {
        launchSingleTop = true
    }
}

/** Fecha a edição. Como nos ajustes do ano, só tira ela da pilha, mesmo se for chamado duas vezes. */
fun NavController.fecharEdicaoDaDisciplina() {
    popBackStack<Routes.EditarDisciplina>(inclusive = true)
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

/**
 * Volta ao Boletim Geral, tirando da pilha o que estiver por cima dele: a disciplina aberta, o perfil
 * ou o cadastro de aluno.
 */
fun NavController.abrirBoletimGeral() {
    navigate(Routes.Boletim.Geral) {
        popUpTo<Routes.Boletim.Geral>()
        launchSingleTop = true
    }
}

/** Fecha o perfil e abre no lugar dele o cadastro de outro aluno. */
fun NavController.abrirNovoAluno() {
    navigate(Routes.NovoAluno) {
        popUpTo<Routes.Aluno> { inclusive = true }
        launchSingleTop = true
    }
}

/** Desiste do cadastro. Como nos ajustes, só tira ele da pilha, mesmo se for chamado duas vezes. */
fun NavController.fecharNovoAluno() {
    popBackStack<Routes.NovoAluno>(inclusive = true)
}

/** Fecha o perfil e abre no lugar dele as configurações, como o cadastro de outro aluno. */
fun NavController.abrirConfiguracoes() {
    navigate(Routes.Configuracoes) {
        popUpTo<Routes.Aluno> { inclusive = true }
        launchSingleTop = true
    }
}

/** Fecha as configurações. Como nos ajustes, só tira elas da pilha, mesmo se for chamado duas vezes. */
fun NavController.fecharConfiguracoes() {
    popBackStack<Routes.Configuracoes>(inclusive = true)
}

/** Fecha o perfil e abre no lugar dele o backup e a restauração, como as configurações. */
fun NavController.abrirBackup() {
    navigate(Routes.Backup) {
        popUpTo<Routes.Aluno> { inclusive = true }
        launchSingleTop = true
    }
}

/** Fecha o backup. Como nos ajustes, só tira ele da pilha, mesmo se for chamado duas vezes. */
fun NavController.fecharBackup() {
    popBackStack<Routes.Backup>(inclusive = true)
}

/** Abre as notificações por cima da tela atual; tocar de novo no sino não empilha outra. */
fun NavController.abrirNotificacoes() {
    navigate(Routes.Notificacoes) {
        launchSingleTop = true
    }
}

/** Fecha as notificações. Como nos ajustes, só tira elas da pilha, mesmo se for chamado duas vezes. */
fun NavController.fecharNotificacoes() {
    popBackStack<Routes.Notificacoes>(inclusive = true)
}

/** Abre as configurações das notificações por cima da lista; o voltar leva de novo a ela. */
fun NavController.abrirConfiguracoesDasNotificacoes() {
    navigate(Routes.ConfiguracoesDasNotificacoes) {
        launchSingleTop = true
    }
}

/** Fecha as configurações das notificações, só elas, mesmo se for chamado duas vezes. */
fun NavController.fecharConfiguracoesDasNotificacoes() {
    popBackStack<Routes.ConfiguracoesDasNotificacoes>(inclusive = true)
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
 * O card de identificação some nos ajustes do ano letivo e na edição da disciplina, que têm barra
 * própria (e o card mostraria o ano como está gravado enquanto ele é editado), no cadastro de outro
 * aluno, que ainda não existe, e nas configurações, no backup e nas notificações, que não são de
 * nenhum aluno.
 */
val NavBackStackEntry.mostraIdentificacao: Boolean
    get() = !destination.hasRoute<Routes.AnoLetivo>() &&
        !destination.hasRoute<Routes.EditarDisciplina>() &&
        !destination.hasRoute<Routes.NovoAluno>() &&
        !destination.hasRoute<Routes.Configuracoes>() &&
        !destination.hasRoute<Routes.Backup>() &&
        !destination.hasRoute<Routes.Notificacoes>() &&
        !destination.hasRoute<Routes.ConfiguracoesDasNotificacoes>()

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
