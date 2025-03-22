package com.example.gymrace.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// Singleton para manejar el tema globalmente
object ThemeManager {
    // Estado mutable privado
    private val _isDarkTheme = mutableStateOf(false)

    // Estado público inmutable para lectura
    val isDarkTheme: State<Boolean> = _isDarkTheme

    // Función para inicializar desde SharedPreferences
    fun init(context: Context) {
        val sharedPreferences = context.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        _isDarkTheme.value = sharedPreferences.getBoolean("isDark", false)
    }

    // Función para cambiar el tema
    fun toggleTheme(context: Context) {
        _isDarkTheme.value = !_isDarkTheme.value
        // Guardar en SharedPreferences
        val sharedPreferences = context.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isDark", _isDarkTheme.value)
        editor.apply()
    }
}

// Composable para acceder al tema
@Composable
fun rememberThemeState(): State<Boolean> {
    val context = LocalContext.current
    // Inicializar si es necesario
    remember { ThemeManager.init(context) }
    return ThemeManager.isDarkTheme
}