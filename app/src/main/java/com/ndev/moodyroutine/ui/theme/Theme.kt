package com.ndev.moodyroutine.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MoodyOrangeLight,
    onPrimary = Color(0xFF18181B),
    primaryContainer = Color(0xFF431407),
    onPrimaryContainer = Color(0xFFFFEDD5),
    secondary = MoodyContourGrey,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF27272A),
    onSecondaryContainer = Color(0xFFF4F4F5),
    tertiary = MoodyOrange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF27272A),
    onTertiaryContainer = Color(0xFFFFEDD5),
    error = AccentRed,
    onError = Color.White,
    background = MoodyDarkBackground,
    onBackground = Color(0xFFF4F4F5),
    surface = MoodyDarkSurface,
    onSurface = Color(0xFFF4F4F5),
    surfaceVariant = MoodyDarkSurfaceVariant,
    onSurfaceVariant = MoodySlateGrey,
    surfaceContainerLowest = Color(0xFF09090B),
    surfaceContainerLow = Color(0xFF121215),
    surfaceContainer = Color(0xFF18181B),
    surfaceContainerHigh = Color(0xFF27272A),
    surfaceContainerHighest = Color(0xFF3F3F46),
    outline = MoodyDarkOutline,
    outlineVariant = Color(0xFF27272A)
)

private val LightColorScheme = lightColorScheme(
    primary = MoodyOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF7C2D12),
    secondary = MoodyContourGrey,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4F4F5),
    onSecondaryContainer = Color(0xFF18181B),
    tertiary = MoodyOrangeLight,
    onTertiary = Color(0xFF18181B),
    tertiaryContainer = Color(0xFFFFEDD5),
    onTertiaryContainer = Color(0xFF7C2D12),
    error = AccentRed,
    onError = Color.White,
    background = MoodyLightBackground,
    onBackground = Color(0xFF18181B),
    surface = MoodyLightSurface,
    onSurface = Color(0xFF18181B),
    surfaceVariant = MoodyLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF71717A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9FAFB),
    surfaceContainer = Color(0xFFF4F4F5),
    surfaceContainerHigh = Color(0xFFE4E4E7),
    surfaceContainerHighest = Color(0xFFD4D4D8),
    outline = MoodyLightOutline,
    outlineVariant = Color(0xFFF4F4F5)
)

@Composable
fun MoodyRoutineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
