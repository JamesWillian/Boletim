package app.jammes.boletim

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.jammes.boletim.presentation.contexto.ContextoUiState
import app.jammes.boletim.presentation.contexto.ContextoViewModel
import app.jammes.boletim.presentation.navigation.AppNavGraph
import app.jammes.boletim.presentation.navigation.AppRoot
import app.jammes.boletim.presentation.ui.theme.BoletimTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // O mesmo ViewModel que o AppRoot pega com hiltViewModel(activity): os dois usam esta Activity
    private val contextoVm: ContextoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen() // antes do super.onCreate, como a biblioteca pede
        super.onCreate(savedInstanceState)

        // A splash fica na tela até o contexto salvo carregar: assim o app abre direto no boletim,
        // sem um quadro vazio no meio. O limite de tempo garante que ela nunca prende o app.
        val inicio = SystemClock.uptimeMillis()
        splash.setKeepOnScreenCondition {
            contextoVm.state.value is ContextoUiState.Carregando &&
                SystemClock.uptimeMillis() - inicio < LIMITE_SPLASH_MS
        }

        enableEdgeToEdge()
        setContent {
            BoletimTheme {
//                AppNavGraph()
                AppRoot()
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    BoletimScreen(
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
            }
        }
    }
}

private const val LIMITE_SPLASH_MS = 1_500L

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