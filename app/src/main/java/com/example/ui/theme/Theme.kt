package com.example.ui.theme

import android.app.Activity
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
import com.example.ui.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = SafetyTeal,
    onPrimary = SafeguardNavyDark,
    primaryContainer = SafeguardNavyCard,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF38BDF8),
    onSecondary = SafeguardNavyDark,
    secondaryContainer = Color(0xFF1E3A5F),
    onSecondaryContainer = Color.White,
    tertiary = EmergencyRedBright,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF5C101B),
    onTertiaryContainer = Color(0xFFFFCDD2),
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = EmergencyRedBright,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SafeguardNavy,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = SafeguardNavy,
    secondary = SafetyTealDark,
    onSecondary = Color.White,
    secondaryContainer = SafetyTealLight,
    onSecondaryContainer = SafeguardNavy,
    tertiary = EmergencyRed,
    onTertiary = Color.White,
    tertiaryContainer = EmergencyRedLight,
    onTertiaryContainer = EmergencyRedDark,
    background = Color(0xFFF1F5F9),
    surface = Color.White,
    onBackground = SlateTextDark,
    onSurface = SlateTextDark,
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateBorder,
    error = EmergencyRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = (if (isDark) DarkSurface else SafeguardNavy).toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
