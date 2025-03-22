package com.example.gymrace.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xffff9241),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF03DAC5),
    onSecondary = Color(0xffff00fc),
    background = Color(0xffffffff),
    onBackground = Color(0xff000000),
    surface = Color(0xffcccccc),
    onSurface = Color(0xff000000),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xffff9241),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF03DAC5),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF121212),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xff505050),
    onSurface = Color(0xFFFFFFFF)
)

@Composable
fun GymRaceTheme(content: @Composable () -> Unit) {
    // Usar el estado del tema desde el ThemeManager
    val isDarkTheme = rememberThemeState().value

    MaterialTheme(
        colorScheme = if (isDarkTheme) DarkColors else LightColors,
        content = content
    )
}