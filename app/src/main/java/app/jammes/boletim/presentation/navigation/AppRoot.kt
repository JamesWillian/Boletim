package app.jammes.boletim.presentation.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.presentation.contexto.ContextoUiState
import app.jammes.boletim.presentation.contexto.ContextoViewModel

/**
 * Raiz da UI. O contexto salvo (aluno, ano e período) decide em que fase o app está:
 *
 * ```
 * AppRoot
 * ├── Carregando → spinner
 * ├── Vazio      → onboarding, em tela cheia (sem contexto não há o que identificar)
 * └── Definido   → AppScaffold → AppNavHost → telas
 * ```
 */
@Composable
fun AppRoot() {
    val owner = LocalActivity.current as ViewModelStoreOwner
    val contextoVm: ContextoViewModel = hiltViewModel(owner)
    val state by contextoVm.state.collectAsStateWithLifecycle()

    when (val s = state) {
        ContextoUiState.Carregando -> CircularProgressIndicator(modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(align = Alignment.Center)) //TelaCarregando()

        ContextoUiState.Vazio -> {} //OnboardingScreen()

        is ContextoUiState.Definido -> {
            val alunos by contextoVm.alunos.collectAsStateWithLifecycle()
            val anosLetivos by contextoVm.anosLetivos.collectAsStateWithLifecycle()
            val disciplinas by contextoVm.disciplinas.collectAsStateWithLifecycle()

            AppScaffold(
                contexto = s.contexto,
                aluno = alunos.find { it.id == s.contexto.alunoId },
                anosLetivos = anosLetivos,
                disciplinas = disciplinas,
                onSelecionarPeriodo = { periodo -> contextoVm.trocarPeriodo(periodo.id) }
            )
        }
    }
}
