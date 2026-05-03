package com.manekelsa.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val LocalHighContrastMode = staticCompositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = SaffronPrimary,
    onPrimary = TextDark,
    primaryContainer = SaffronLight,
    onPrimaryContainer = SaffronDark,
    secondary = DeepGreenSecondary,
    onSecondary = TextLight,
    secondaryContainer = DeepGreenLight,
    onSecondaryContainer = DeepGreenDark,
    background = CreamWhite,
    onBackground = TextDark,
    surface = SoftGrey,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFE9F1FF),
    onSurfaceVariant = Color(0xFF50607A),
    outline = Color(0x99FFFFFF),
    error = ErrorRed,
    onError = TextLight
)

private val HighContrastColorScheme = lightColorScheme(
    primary = DeepGreenSecondary, // Using Dark Green for better visibility
    onPrimary = Color.White,
    secondary = Color.Black,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    outline = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    primary = SaffronLight,
    onPrimary = TextDark,
    secondary = DeepGreenLight,
    onSecondary = TextDark,
    background = Color(0xFF1C1B1B),
    surface = Color(0xFF252423),
    onBackground = SoftGrey,
    onSurface = SoftGrey
)

@Composable
fun ManeKelsaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrastMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrastMode -> HighContrastColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme && !highContrastMode
        }
    }

    CompositionLocalProvider(LocalHighContrastMode provides highContrastMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
