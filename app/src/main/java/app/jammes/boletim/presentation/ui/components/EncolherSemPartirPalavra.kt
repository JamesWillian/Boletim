package app.jammes.boletim.presentation.ui.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Como o [TextAutoSize.StepBased], mas também encolhe quando a quebra de linha parte uma palavra no
 * meio: só vale quebrar entre palavras. Desce de meio em meio sp a partir do [maximo]; no [minimo],
 * fica com o que der (uma palavra enorme ainda pode partir ou ganhar reticências).
 *
 * É o do nome das matérias nos cards: "Educação / Física" pode quebrar; "Matemáti / ca", não. Com
 * maxLines = 1, só encolhe até caber na linha.
 */
internal data class EncolherSemPartirPalavra(
    private val minimo: TextUnit,
    private val maximo: TextUnit,
) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(constraints: Constraints, text: AnnotatedString): TextUnit {
        var tamanho = maximo.value
        while (tamanho > minimo.value) {
            val layout = performLayout(constraints, text, tamanho.sp)
            if (!layout.passouDoEspaco() && !layout.partiuPalavra()) return tamanho.sp
            tamanho -= 0.5f
        }
        return minimo
    }

    // Linhas a mais que o maxLines viram reticências na última
    private fun TextLayoutResult.passouDoEspaco() =
        hasVisualOverflow || (lineCount > 0 && isLineEllipsized(lineCount - 1))

    // Letra dos dois lados de uma quebra: a palavra foi partida. Espaço, hífen ou barra, não
    private fun TextLayoutResult.partiuPalavra(): Boolean {
        val texto = layoutInput.text.text
        return (0 until lineCount - 1).any { linha ->
            val fim = getLineEnd(linha)
            fim in 1 until texto.length && texto[fim - 1].isLetterOrDigit() && texto[fim].isLetterOrDigit()
        }
    }
}
