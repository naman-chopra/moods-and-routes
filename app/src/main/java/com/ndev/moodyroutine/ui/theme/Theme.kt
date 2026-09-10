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

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.ndev.moodyroutine.util.PreferencesManager

fun buildColorScheme(preset: AppThemePreset, darkTheme: Boolean): ColorScheme {
    val primary = Color(preset.primaryColor)
    val primaryLight = Color(preset.primaryLightColor)
    val appBg = Color(preset.appBgColor)
    val cardBg = Color(preset.cardBgColor)
    val cardBorder = Color(preset.cardBorderColor)
    val popupBg = Color(preset.popupBgColor)
    val contourGrey = Color(preset.contourGreyColor)

    return if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.2f),
            onPrimaryContainer = primaryLight,
            secondary = contourGrey,
            onSecondary = Color.White,
            secondaryContainer = cardBorder,
            onSecondaryContainer = Color(0xFFF4F4F5),
            tertiary = primaryLight,
            onTertiary = Color.White,
            tertiaryContainer = cardBg,
            onTertiaryContainer = Color(0xFFFFEDD5),
            error = AccentRed,
            onError = Color.White,
            background = appBg,
            onBackground = Color(0xFFF4F4F5),
            surface = cardBg,
            onSurface = Color(0xFFF4F4F5),
            surfaceVariant = cardBg,
            onSurfaceVariant = Color(0xFFA1A1AA),
            surfaceContainerLowest = popupBg,
            surfaceContainerLow = appBg,
            surfaceContainer = cardBg,
            surfaceContainerHigh = cardBorder,
            surfaceContainerHighest = contourGrey.copy(alpha = 0.3f),
            outline = cardBorder,
            outlineVariant = cardBorder.copy(alpha = 0.6f),
            surfaceTint = Color.Transparent
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFEDD5),
            onPrimaryContainer = Color(0xFF7C2D12),
            secondary = contourGrey,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFF4F4F5),
            onSecondaryContainer = Color(0xFF18181B),
            tertiary = primaryLight,
            onTertiary = Color(0xFF18181B),
            tertiaryContainer = Color(0xFFFFEDD5),
            onTertiaryContainer = Color(0xFF7C2D12),
            error = AccentRed,
            onError = Color.White,
            background = Color(0xFFF9FAFB),
            onBackground = Color(0xFF18181B),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF18181B),
            surfaceVariant = Color(0xFFF4F4F5),
            onSurfaceVariant = Color(0xFF71717A),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF9FAFB),
            surfaceContainer = Color(0xFFF4F4F5),
            surfaceContainerHigh = Color(0xFFE4E4E7),
            surfaceContainerHighest = Color(0xFFD4D4D8),
            outline = Color(0xFFE4E4E7),
            outlineVariant = Color(0xFFF4F4F5),
            surfaceTint = Color.Transparent
        )
    }
}

@Composable
fun MoodyRoutineTheme(
    preset: AppThemePreset? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val observedPreset by PreferencesManager.themePreset.collectAsState()
    val activePreset = preset ?: observedPreset
    val colorScheme = remember(activePreset, darkTheme) {
        buildColorScheme(activePreset, darkTheme)
    }
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
