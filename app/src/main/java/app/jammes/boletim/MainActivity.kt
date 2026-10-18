package app.jammes.boletim

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.avisos.AberturaPeloAviso
import app.jammes.boletim.presentation.contexto.ContextoUiState
import app.jammes.boletim.presentation.contexto.ContextoViewModel
import app.jammes.boletim.presentation.navigation.AppNavGraph
import app.jammes.boletim.presentation.navigation.AppRoot
import app.jammes.boletim.presentation.ui.configuracoes.ConfiguracoesViewModel
import app.jammes.boletim.presentation.ui.theme.BoletimTheme
import app.jammes.boletim.presentation.ui.theme.temaEscuro
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // O mesmo ViewModel que o AppRoot pega com hiltViewModel(activity): os dois usam esta Activity
    private val contextoVm: ContextoViewModel by viewModels()
    // O tema sai daqui, antes de tudo; o AppRoot pega o mesmo, para o estilo das abas
    private val configuracoesVm: ConfiguracoesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen() // antes do super.onCreate, como a biblioteca pede
        super.onCreate(savedInstanceState)

        // A splash fica na tela até o contexto salvo e as configurações carregarem: assim o app abre
        // direto no boletim e já no tema escolhido, sem um quadro vazio ou na cor errada no meio. O
        // limite de tempo garante que ela nunca prende o app.
        val inicio = SystemClock.uptimeMillis()
        splash.setKeepOnScreenCondition {
            val carregando = contextoVm.state.value is ContextoUiState.Carregando ||
                configuracoesVm.configuracoes.value == null
            carregando && SystemClock.uptimeMillis() - inicio < LIMITE_SPLASH_MS
        }

        enableEdgeToEdge()
        setContent {
            val configuracoes by configuracoesVm.configuracoes.collectAsStateWithLifecycle()
            val escuro = temaEscuro(configuracoes?.tema)

            // Os ícones das barras do sistema seguem o tema do app, e não o do aparelho: com o app no
            // escuro e o aparelho no claro, eles ficariam escuros sobre o fundo escuro
            DisposableEffect(escuro) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { escuro },
                    navigationBarStyle = SystemBarStyle.auto(SCRIM_CLARO, SCRIM_ESCURO) { escuro },
                )
                onDispose {}
            }

            BoletimTheme(darkTheme = escuro) {
//                AppNavGraph()
                AppRoot()
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    BoletimScreen(
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
            }
        }

        // Aberto pelo toque num aviso do celular. Só na primeira criação: girar a tela recria a
        // Activity com o mesmo intent, e o pedido já foi atendido
        if (savedInstanceState == null) atenderAviso(intent)
    }

    // Com o app já aberto, o toque no aviso chega aqui, na mesma Activity
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        atenderAviso(intent)
    }

    private fun atenderAviso(intent: Intent) {
        val pedido = AberturaPeloAviso.ler(intent) ?: return
        contextoVm.abrirNotificacoes(pedido.alunoId)
    }
}

private const val LIMITE_SPLASH_MS = 1_500L

// Os mesmos véus que o enableEdgeToEdge() põe atrás da navegação por 3 botões
private val SCRIM_CLARO = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val SCRIM_ESCURO = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BoletimTheme {
        Greeting("Android")
    }
}