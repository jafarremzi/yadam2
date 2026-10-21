package app.yadavar.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EEAD4), onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF134E4A), onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFFA5B4FC), onSecondary = Color(0xFF14183A),
    background = Color(0xFF0B0F14), onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF121820), onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1B2430), onSurfaceVariant = Color(0xFF9AA7B5),
    error = Color(0xFFFF8A80), errorContainer = Color(0xFF4A1F1F), onErrorContainer = Color(0xFFFFDAD6),
    tertiaryContainer = Color(0xFF3A2F12), onTertiaryContainer = Color(0xFFFFE9A8),
    outline = Color(0xFF3A4654),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCFBF1), onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4F46E5), onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F8FA), onBackground = Color(0xFF0B0F14),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF0B0F14),
    surfaceVariant = Color(0xFFE8EDF2), onSurfaceVariant = Color(0xFF475467),
    error = Color(0xFFB3261E), errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    tertiaryContainer = Color(0xFFFFF1C2), onTertiaryContainer = Color(0xFF3A2F12),
    outline = Color(0xFFB8C2CC),
)

@Composable
fun YadAvarTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
