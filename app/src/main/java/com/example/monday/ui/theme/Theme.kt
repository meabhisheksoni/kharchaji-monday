package com.example.monday.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = NewPrimary,
    onPrimary = NewOnPrimary,
    primaryContainer = Color(0xFFD0E7E7),
    secondary = NewPrimary,
    onSecondary = NewOnPrimary,
    secondaryContainer = Color(0xFFB8D9D9),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onBackground = NewOnSurfaceLight,
    onSurface = NewOnSurfaceLight,
    surfaceVariant = Color(0xFFEEF7F7),
    onSurfaceVariant = NewOnSurfaceLightSecondary,
    error = Error
)

private val DarkColorScheme = darkColorScheme(
    primary = NewPrimaryDark,
    onPrimary = NewOnPrimary,
    primaryContainer = NewPrimary,
    secondary = NewPrimary,
    onSecondary = NewOnPrimary,
    secondaryContainer = NewPrimaryDark,
    background = NewBackgroundDark,
    surface = NewSurfaceLight,
    onBackground = NewOnBackgroundDark,
    onSurface = NewOnSurfaceLight,
    surfaceVariant = Color(0xFF424242),
    onSurfaceVariant = NewOnSurfaceLightSecondary,
    error = Error
)

@Composable
fun KharchajiTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = true,
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val useDarkIcons = !darkTheme
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkIcons
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}