package com.example.gymrace.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// TEMA CLARO
private val LightColors = lightColorScheme(
    // Colores primarios
    primary = Color(0xFFFF9241),           // Naranja principal (mantenido)
    onPrimary = Color(0xFFFFFFFF),         // Blanco sobre naranja
    primaryContainer = Color(0xFFFFDBC3),  // Contenedor naranja claro
    onPrimaryContainer = Color(0xFF331D00), // Texto oscuro sobre contenedor naranja

    // Colores secundarios
    secondary = Color(0xFF5E6DDC),         // Azul complementario
    onSecondary = Color(0xFFFFFFFF),       // Blanco sobre azul
    secondaryContainer = Color(0xFFDFE0FF), // Contenedor azul claro
    onSecondaryContainer = Color(0xFF001453), // Texto oscuro sobre contenedor azul

    // Colores terciarios
    tertiary = Color(0xFF4CAF50),          // Verde para elementos de progreso
    onTertiary = Color(0xFFFFFFFF),        // Blanco sobre verde
    tertiaryContainer = Color(0xFFB7FFA8), // Contenedor verde claro
    onTertiaryContainer = Color(0xFF002201), // Texto oscuro sobre contenedor verde

    // Colores de fondo
    background = Color(0xFFF5F5F5),        // Fondo claro, casi blanco
    onBackground = Color(0xFF333333),      // Texto oscuro sobre fondo

    // Colores de superficie
    surface = Color(0xFFFFFFFF),           // Superficies blancas
    onSurface = Color(0xFF333333),         // Texto oscuro sobre superficies
    surfaceVariant = Color(0xFFF0E0D0),    // Variante de superficie
    onSurfaceVariant = Color(0xFF52443C),  // Texto sobre variante de superficie
    surfaceTint = Color(0xFFFF9241),       // Tinte para superficies (naranja)

    // Colores inversas
    inverseSurface = Color(0xFF333333),    // Superficie inversa (oscura)
    inverseOnSurface = Color(0xFFFFFFFF),  // Texto sobre superficie inversa
    inversePrimary = Color(0xFFFFB77C),    // Primario inverso

    // Colores de error
    error = Color(0xFFE53935),             // Rojo para errores
    onError = Color(0xFFFFFFFF),           // Texto sobre error
    errorContainer = Color(0xFFFFDAD6),    // Contenedor de error
    onErrorContainer = Color(0xFF410002),  // Texto sobre contenedor de error

    // Otros colores
    outline = Color(0xFFDDDDDD),           // Contorno gris claro
    outlineVariant = Color(0xFFD5C3B5),    // Variante de contorno
    scrim = Color(0x99000000),             // Tinte oscuro para modales
)

// TEMA OSCURO
private val DarkColors = darkColorScheme(
    // Colores primarios
    primary = Color(0xFFFF9241),           // Naranja principal (mantenido)
    onPrimary = Color(0xFF000000),         // Negro sobre naranja para contraste
    primaryContainer = Color(0xFF994800),  // Contenedor naranja oscuro
    onPrimaryContainer = Color(0xFFFFDBC3), // Texto claro sobre contenedor naranja

    // Colores secundarios
    secondary = Color(0xFF738AFF),         // Azul más brillante para modo oscuro
    onSecondary = Color(0xFF000000),       // Negro sobre azul para contraste
    secondaryContainer = Color(0xFF293B85), // Contenedor azul oscuro
    onSecondaryContainer = Color(0xFFDFE0FF), // Texto claro sobre contenedor azul

    // Colores terciarios
    tertiary = Color(0xFF66BB6A),          // Verde más brillante para modo oscuro
    onTertiary = Color(0xFF000000),        // Negro sobre verde para contraste
    tertiaryContainer = Color(0xFF005107), // Contenedor verde oscuro
    onTertiaryContainer = Color(0xFFB7FFA8), // Texto claro sobre contenedor verde

    // Colores de fondo
    background = Color(0xFF121212),        // Fondo oscuro
    onBackground = Color(0xFFE0E0E0),      // Texto claro sobre fondo

    // Colores de superficie
    surface = Color(0xFF242424),           // Superficies oscuras
    onSurface = Color(0xFFE0E0E0),         // Texto claro sobre superficies
    surfaceVariant = Color(0xFF52443C),    // Variante de superficie
    onSurfaceVariant = Color(0xFFD5C3B5),  // Texto sobre variante de superficie
    surfaceTint = Color(0xFFFF9241),       // Tinte para superficies (naranja)

    // Colores inversas
    inverseSurface = Color(0xFFE0E0E0),    // Superficie inversa (clara)
    inverseOnSurface = Color(0xFF242424),  // Texto sobre superficie inversa
    inversePrimary = Color(0xFF994800),    // Primario inverso

    // Colores de error
    error = Color(0xFFEF5350),             // Rojo más brillante para modo oscuro
    onError = Color(0xFF000000),           // Texto sobre error
    errorContainer = Color(0xFF8C0009),    // Contenedor de error
    onErrorContainer = Color(0xFFFFDAD6),  // Texto sobre contenedor de error

    // Otros colores
    outline = Color(0xFF444444),           // Contorno gris oscuro
    outlineVariant = Color(0xFF52443C),    // Variante de contorno
    scrim = Color(0x99000000),             // Tinte oscuro para modales
)


// Función para aplicar el tema
@Composable
fun GymRaceTheme(content: @Composable () -> Unit) {
    // Usar el estado del tema desde el ThemeManager
    val isDarkTheme = rememberThemeState().value

    MaterialTheme(
        colorScheme = if (isDarkTheme) DarkColors else LightColors,
        content = content
    )
}