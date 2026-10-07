package app.jammes.boletim.presentation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Os componentes do Material já leem daqui: Card usa medium, FAB usa large, chips usam small,
// campos de texto usam extraSmall e bottom sheet e diálogos usam extraLarge.
val Shapes = androidx.compose.material3.Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * A escala de espaçamentos do app. Usar sempre um destes, em vez de valores soltos (14.dp, 10.dp…),
 * mantém o mesmo ritmo entre as telas.
 */
object Espacos {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}
