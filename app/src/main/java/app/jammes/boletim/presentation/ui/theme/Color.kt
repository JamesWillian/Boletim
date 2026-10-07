package app.jammes.boletim.presentation.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

object BoletimColors {

    val Primary = Color(0xFF005EFF)
    val Secondary = Color(0xFF4D8FFF)
    val Brand = Color(0xFFE9BB41)

    val OnPrimary         = Color(0xFFFFFFFF)
    val OnSecondary       = Color(0xFFFFFFFF)

}

/**
 * Cores de status que o Material não tem (o vermelho é o `error` do tema). Lidas por
 * [BoletimTheme.coresExtras], que já entrega a versão clara ou a escura.
 */
@Immutable
data class BoletimCoresExtras(
    val sucesso: Color,
    val onSucesso: Color,
    val sucessoContainer: Color,
    val onSucessoContainer: Color,
    val atencao: Color,
    val onAtencao: Color,
    val atencaoContainer: Color,
    val onAtencaoContainer: Color,
)

internal val CoresExtrasClaras = BoletimCoresExtras(
    sucesso = Color(0xFF2E7D32),
    onSucesso = Color(0xFFFFFFFF),
    sucessoContainer = Color(0xFFC3EFC0),
    onSucessoContainer = Color(0xFF002106),
    atencao = Color(0xFFB26A00),
    onAtencao = Color(0xFFFFFFFF),
    atencaoContainer = Color(0xFFFFDDB8),
    onAtencaoContainer = Color(0xFF2C1700),
)

// No escuro, tons claros: o verde e o âmbar do claro somem sobre o fundo escuro
internal val CoresExtrasEscuras = BoletimCoresExtras(
    sucesso = Color(0xFF86D68A),
    onSucesso = Color(0xFF00390F),
    sucessoContainer = Color(0xFF1D5124),
    onSucessoContainer = Color(0xFFC3EFC0),
    atencao = Color(0xFFFFB95C),
    onAtencao = Color(0xFF462A00),
    atencaoContainer = Color(0xFF643F00),
    onAtencaoContainer = Color(0xFFFFDDB8),
)

/**
 * A disciplina guarda o índice da cor, então a ordem e as famílias de cor não mudam (índice 0 é
 * sempre vermelho). Os tons foram nivelados em luminosidade e saturação (OKLCH) para nenhuma
 * gritar mais que a outra; os amarelos ficam mais claros porque escuros viram verde-oliva.
 */
object CoresDisciplina {
    val paleta = listOf(
        Color(0xFFD24C49), // vermelho
        Color(0xFF1F83DB), // azul
        Color(0xFF439D47), // verde
        Color(0xFFEF8B26), // laranja
        Color(0xFFB469CA), // roxo
        Color(0xFF059EB1), // azul ciano
        Color(0xFFD0598E), // rosa
        Color(0xFF9176E3), // roxo escuro
        Color(0xFF6B83EA), // índigo
        Color(0xFF51BBED), // azul claro
        Color(0xFF2FA091), // teal
        Color(0xFF8EBD47), // verde claro
        Color(0xFFC1CB35), // lima
        Color(0xFFF2CF3B), // amarelo
        Color(0xFFF5AF20), // âmbar
        Color(0xFFE77140), // laranja escuro
        Color(0xFF9A776B), // marrom
        Color(0xFF708B97)  // azul acinzentado
    )
    fun de(indice: Int) = paleta[indice % paleta.size]
}
