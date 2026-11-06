package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.Black,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldLight,
    secondary = VioletSecondary,
    onSecondary = Color.White,
    secondaryContainer = VioletDark,
    onSecondaryContainer = VioletLight,
    tertiary = CyanAccent,
    background = VibeBackground,
    onBackground = TextPrimary,
    surface = VibeSurface,
    onSurface = TextPrimary,
    surfaceVariant = VibeSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = VibeBorder,
    outlineVariant = VibeSurfaceCard
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Editor is always dark theme for video accuracy
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = VibeBackground.toArgb()
                window.navigationBarColor = VibeBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
