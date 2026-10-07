package app.jammes.boletim.presentation.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Todos os papéis do Material a partir da marca: o azul é o primary e o dourado, o container do
// tertiary. As superfícies são neutras (off-white e cinza frio), sem puxar para o lilás, para as
// cores das disciplinas e dos status serem as únicas a chamar atenção.
private val LightColorScheme = lightColorScheme(
    primary = BoletimColors.Primary,
    onPrimary = BoletimColors.OnPrimary,
    primaryContainer = Color(0xFFDCE4FF),
    onPrimaryContainer = Color(0xFF001849),
    inversePrimary = Color(0xFFB4C5FF),
    secondary = Color(0xFF545E76),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE3F6),
    onSecondaryContainer = Color(0xFF111B2D),
    // O dourado puro não tem contraste para texto sobre o claro: ele fica no container
    tertiary = Color(0xFF795900),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = BoletimColors.Brand,
    onTertiaryContainer = Color(0xFF261A00),
    background = Color(0xFFF7F8FB),
    onBackground = Color(0xFF191C21),
    surface = Color(0xFFF7F8FB),
    onSurface = Color(0xFF191C21),
    surfaceVariant = Color(0xFFE1E3EA),
    onSurfaceVariant = Color(0xFF44474F),
    surfaceTint = BoletimColors.Primary,
    inverseSurface = Color(0xFF2E3036),
    inverseOnSurface = Color(0xFFEFF0F6),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6CF),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF7F8FB),
    surfaceDim = Color(0xFFD8DAE0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F3F7),
    surfaceContainer = Color(0xFFECEEF2),
    surfaceContainerHigh = Color(0xFFE6E8ED),
    surfaceContainerHighest = Color(0xFFE0E2E8),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    onPrimary = Color(0xFF002A78),
    // No escuro o azul da marca cabe inteiro no container (FAB, chips), com texto branco
    primaryContainer = BoletimColors.Primary,
    onPrimaryContainer = BoletimColors.OnPrimary,
    inversePrimary = BoletimColors.Primary,
    secondary = Color(0xFFBCC6E0),
    onSecondary = Color(0xFF263044),
    secondaryContainer = Color(0xFF3C465B),
    onSecondaryContainer = Color(0xFFDDE3F6),
    tertiary = BoletimColors.Brand,
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5B4300),
    onTertiaryContainer = Color(0xFFFFDF9E),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E8),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E8),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6CF),
    surfaceTint = Color(0xFFB4C5FF),
    inverseSurface = Color(0xFFE2E2E8),
    inverseOnSurface = Color(0xFF2E3036),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474F),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF37393E),
    surfaceDim = Color(0xFF111318),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),
)

private val LocalCoresExtras = staticCompositionLocalOf { CoresExtrasClaras }

@Composable
fun BoletimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Cor dinâmica (Android 12+) desligada: com ela o app pegava as cores do papel de parede e
    // perdia a marca. O parâmetro fica para virar uma opção nos ajustes.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalCoresExtras provides if (darkTheme) CoresExtrasEscuras else CoresExtrasClaras
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

/** O que o app acrescenta ao [MaterialTheme], lido do mesmo jeito: `BoletimTheme.coresExtras.sucesso`. */
object BoletimTheme {
    val coresExtras: BoletimCoresExtras
        @Composable
        @ReadOnlyComposable
        get() = LocalCoresExtras.current
}
