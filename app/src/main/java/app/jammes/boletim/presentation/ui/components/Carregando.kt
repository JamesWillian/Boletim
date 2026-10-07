package app.jammes.boletim.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Carregando, sem piscar: o Room quase sempre responde em poucos milissegundos, e um spinner que
 * aparece por um quadro só parece um tremido na tela. Ele só surge se a espera passar de [atrasoMs].
 */
@Composable
fun CarregandoDiscreto(
    modifier: Modifier = Modifier,
    atrasoMs: Long = 400,
) {
    var mostrar by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(atrasoMs)
        mostrar = true
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = mostrar, enter = fadeIn()) {
            CircularProgressIndicator()
        }
    }
}
