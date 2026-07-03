package app.jammes.boletim.presentation.ui.boletim

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
fun BoletimScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier) {
        Column() {
            Text(
                text = "Boletim",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge
            )

            Text(text = "Aluno")
        }
    }
}