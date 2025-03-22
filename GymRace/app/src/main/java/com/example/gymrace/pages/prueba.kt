package com.example.gymrace.pages
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class prueba {

    @Composable
    fun prueba() {
        Column(
            modifier = Modifier.fillMaxSize().background(color =  MaterialTheme.colorScheme.primary)
        ) {
            Text("Prueba")
        }
    }
}