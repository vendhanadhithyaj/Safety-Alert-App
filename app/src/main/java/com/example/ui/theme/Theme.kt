package com.example.ui.theme

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
    primary = SafetyTeal,
    onPrimary = SafeguardNavyDark,
    primaryContainer = SafeguardNavyCard,
    onPrimaryContainer = Color.White,
    secondary = SafetyTealDark,
    onSecondary = Color.White,
    tertiary = EmergencyRedBright,
    onTertiary = Color.White,
    background = SafeguardNavy,
    surface = SafeguardNavySurface,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = SafeguardNavyCard,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = EmergencyRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SafeguardNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = SafeguardNavy,
    secondary = SafetyTealDark,
    onSecondary = Color.White,
    secondaryContainer = SafetyTealLight,
    onSecondaryContainer = SafeguardNavy,
    tertiary = EmergencyRed,
    onTertiary = Color.White,
    tertiaryContainer = EmergencyRedLight,
    onTertiaryContainer = EmergencyRedDark,
    background = SlateLight,
    surface = Color.White,
    onBackground = SlateTextDark,
    onSurface = SlateTextDark,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = SlateTextMuted,
    error = EmergencyRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep branded SAFEGUARD identity by default
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
