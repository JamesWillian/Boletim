package app.jammes.boletim.presentation.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.Configuracoes
import app.jammes.boletim.presentation.contexto.ContextoUiState
import app.jammes.boletim.presentation.contexto.ContextoViewModel
import app.jammes.boletim.presentation.ui.components.CarregandoDiscreto
import app.jammes.boletim.presentation.ui.configuracoes.ConfiguracoesViewModel
import app.jammes.boletim.presentation.ui.onboarding.OnboardingScreen
import java.util.UUID

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
    // O mesmo da MainActivity, que já tirou dele o tema
    val configuracoesVm: ConfiguracoesViewModel = hiltViewModel(owner)
    val configuracoes by configuracoesVm.configuracoes.collectAsStateWithLifecycle()

    when (val s = state) {
        ContextoUiState.Carregando -> CarregandoDiscreto(Modifier.fillMaxSize()) //TelaCarregando()

        ContextoUiState.Vazio -> {
            // Um cadastro do zero a cada vez que o app fica sem contexto: no primeiro uso e depois de
            // excluir o último aluno. O ViewModel fica na Activity, e o de uma vez anterior ainda
            // estaria "salvando", com o botão do fim travado
            val vez = rememberSaveable { UUID.randomUUID().toString() }
            OnboardingScreen(vm = hiltViewModel(owner, key = "onboarding-$vez"))
        }

        is ContextoUiState.Definido -> {
            val alunos by contextoVm.alunos.collectAsStateWithLifecycle()
            val anosLetivos by contextoVm.anosLetivos.collectAsStateWithLifecycle()
            val disciplinas by contextoVm.disciplinas.collectAsStateWithLifecycle()
            val siglas by contextoVm.siglas.collectAsStateWithLifecycle()
            val temNotificacoesNovas by contextoVm.temNotificacoesNovas.collectAsStateWithLifecycle()

            AppScaffold(
                contexto = s.contexto,
                aluno = alunos.find { it.id == s.contexto.alunoId },
                anosLetivos = anosLetivos,
                disciplinas = disciplinas,
                siglas = siglas,
                estiloDasAbas = (configuracoes ?: Configuracoes()).estiloDasAbas,
                temNotificacoesNovas = temNotificacoesNovas,
                irParaNotificacoes = contextoVm.irParaNotificacoes,
                onSelecionarPeriodo = { periodo -> contextoVm.trocarPeriodo(periodo?.id) }
            )
        }
    }
}
