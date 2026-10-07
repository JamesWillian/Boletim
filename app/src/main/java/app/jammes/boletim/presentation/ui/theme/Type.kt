package app.jammes.boletim.presentation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.jammes.boletim.R

// Plus Jakarta Sans (licença OFL, o texto vai junto em assets/licencas). É uma fonte variável:
// um arquivo só com todos os pesos, e cada peso aqui só aponta o eixo "wght" para o valor certo.
@OptIn(ExperimentalTextApi::class) // variationSettings ainda é experimental no Compose
private fun jakarta(peso: Int) = Font(
    resId = R.font.plus_jakarta_sans,
    weight = FontWeight(peso),
    variationSettings = FontVariation.Settings(FontVariation.weight(peso)),
)

val PlusJakartaSans = FontFamily(
    jakarta(400),
    jakarta(500),
    jakarta(600),
    jakarta(700),
)

// Números tabulares (todos os algarismos com a mesma largura) nos estilos grandes, que são os das
// médias e das notas: 7,61 · 10,0 · 8,5 alinham na coluna e o número não "dança" quando muda.
// No texto corrido (body, label) ficam os proporcionais, que leem melhor.
private fun TextStyle.jakarta(tabular: Boolean = false) = copy(
    fontFamily = PlusJakartaSans,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

private val padrao = Typography()

val Typography = Typography(
    displayLarge = padrao.displayLarge.jakarta(tabular = true),
    displayMedium = padrao.displayMedium.jakarta(tabular = true),
    displaySmall = padrao.displaySmall.jakarta(tabular = true),
    headlineLarge = padrao.headlineLarge.jakarta(tabular = true),
    headlineMedium = padrao.headlineMedium.jakarta(tabular = true),
    headlineSmall = padrao.headlineSmall.jakarta(tabular = true),
    titleLarge = padrao.titleLarge.jakarta(tabular = true),
    titleMedium = padrao.titleMedium.jakarta(),
    titleSmall = padrao.titleSmall.jakarta(),
    bodyLarge = padrao.bodyLarge.jakarta(),
    bodyMedium = padrao.bodyMedium.jakarta(),
    bodySmall = padrao.bodySmall.jakarta(),
    labelLarge = padrao.labelLarge.jakarta(),
    labelMedium = padrao.labelMedium.jakarta(),
    labelSmall = padrao.labelSmall.jakarta(),
)
