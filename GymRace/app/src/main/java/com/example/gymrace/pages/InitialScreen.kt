package com.example.gymrace.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun InitialScreen(onNavigate: () -> Unit) {
    // Display your initial screen content here
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome to Gym Race", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        // Button to navigate to the main screen
        Button(onClick = onNavigate) {
            Text(text = "Go to Home")
        }
    }
}