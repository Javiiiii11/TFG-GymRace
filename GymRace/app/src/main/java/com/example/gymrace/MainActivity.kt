package com.example.gymrace

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymrace.pages.RegisterPage
import com.example.gymrace.ui.theme.GymRaceTheme

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymRaceTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "register") {
                    composable("register") { RegisterPage(navController) }
                    composable("main") { MainScreen() }
                }
            }
        }
    }
}