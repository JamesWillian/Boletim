package app.jammes.boletim.presentation.ui.theme

import androidx.compose.ui.graphics.Color

object BoletimColors {

    val Primary = Color(0xFF005EFF)
    val Secondary = Color(0xFF4D8FFF)
    val Brand = Color(0xFFE9BB41)

    val OnPrimary         = Color(0xFFFFFFFF)
    val OnSecondary       = Color(0xFFFFFFFF)

}
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

object CoresDisciplina {
    val paleta = listOf(
        Color(0xFFE53935), // vermelho
        Color(0xFF1E88E5), // azul
        Color(0xFF43A047), // verde
        Color(0xFFFB8C00), // amarelo
        Color(0xFF8E24AA), // roxo
        Color(0xFF00ACC1), // azul ciano
        Color(0xFFD81B60), // rosa
        Color(0xFF5E35B1), // roxo escuro
        Color(0xFF3949AB), // índigo
        Color(0xFF039BE5), // azul claro
        Color(0xFF00897B), // teal
        Color(0xFF7CB342), // verde claro
        Color(0xFFC0CA33), // lima
        Color(0xFFFDD835), // amarelo
        Color(0xFFFFB300), // âmbar
        Color(0xFFF4511E), // laranja escuro
        Color(0xFF6D4C41), // marrom
        Color(0xFF546E7A)  // azul acinzentado
    )
    fun de(indice: Int) = paleta[indice % paleta.size]
}