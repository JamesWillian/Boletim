package app.jammes.boletim.presentation.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.graphics.vector.ImageVector
import app.jammes.boletim.domain.model.IconeMateria

/**
 * A imagem de cada [IconeMateria]. Por enquanto vêm todas do Material; para usar ícones próprios
 * basta trocar o ramo aqui (por exemplo, `ImageVector.vectorResource(R.drawable.x)`, que pede
 * `@Composable` nesta função).
 */
object IconesDisciplina {
    fun de(icone: IconeMateria): ImageVector = when (icone) {
        IconeMateria.LIVRO -> Icons.AutoMirrored.Filled.MenuBook
        IconeMateria.CALCULADORA -> Icons.Filled.Calculate
        IconeMateria.PERGAMINHO -> Icons.Filled.HistoryEdu
        IconeMateria.GLOBO -> Icons.Filled.Public
        IconeMateria.MICROSCOPIO -> Icons.Filled.Biotech
        IconeMateria.IDIOMA -> Icons.Filled.Translate
        IconeMateria.PALETA -> Icons.Filled.Palette
        IconeMateria.BOLA -> Icons.Filled.SportsSoccer
        IconeMateria.RAIO -> Icons.Filled.Bolt
        IconeMateria.FRASCO -> Icons.Filled.Science
        IconeMateria.FOLHA -> Icons.Filled.Eco
        IconeMateria.MENTE -> Icons.Filled.Psychology
        IconeMateria.PESSOAS -> Icons.Filled.Groups
        IconeMateria.ESCOLA -> Icons.Filled.School
        IconeMateria.NOTA_MUSICAL -> Icons.Filled.MusicNote
        IconeMateria.COMPUTADOR -> Icons.Filled.Computer
        IconeMateria.CODIGO -> Icons.Filled.Code
        IconeMateria.REDACAO -> Icons.Filled.EditNote
        IconeMateria.DINHEIRO -> Icons.Filled.Savings
        IconeMateria.ROBO -> Icons.Filled.SmartToy
        IconeMateria.TEATRO -> Icons.Filled.TheaterComedy
        IconeMateria.REGUA -> Icons.Filled.SquareFoot
    }
}
