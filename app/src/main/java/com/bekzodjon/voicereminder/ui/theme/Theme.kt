package com.bekzodjon.voicereminder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF0F766E)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCEFEA),
    onPrimaryContainer = Color(0xFF00201D),
    background = Color(0xFFF7FAF9),
    surface = Color(0xFFF7FAF9),
    surfaceVariant = Color(0xFFE6EEEC),
    onSurfaceVariant = Color(0xFF3F4947),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FD8C9),
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFCCEFEA),
    background = Color(0xFF0E1514),
    surface = Color(0xFF0E1514),
    surfaceVariant = Color(0xFF1E2928),
    onSurfaceVariant = Color(0xFFBEC9C6),
    error = Color(0xFFFFB4AB)
)

@Composable
fun VoiceReminderTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
