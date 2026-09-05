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
    primary = SamsungBlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = AccentPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF372864),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = AccentGreen,
    background = OneUiDarkBackground,
    onBackground = Color(0xFFF1F1F5),
    surface = OneUiDarkSurface,
    onSurface = Color(0xFFF1F1F5),
    surfaceVariant = OneUiDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF9E9EA7),
    outline = OneUiDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = SamsungBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = AccentPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF372864),
    tertiary = AccentGreen,
    background = OneUiLightBackground,
    onBackground = Color(0xFF1B1B20),
    surface = OneUiLightSurface,
    onSurface = Color(0xFF1B1B20),
    surfaceVariant = OneUiLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF6B6E78),
    outline = OneUiLightOutline
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
