package com.example.gymrace

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymrace.pages.InitialScreen
import com.example.gymrace.pages.LoginPage
import com.example.gymrace.pages.RegisterPage
import com.example.gymrace.pages.RegisterPage2
import com.example.gymrace.ui.theme.GymRaceTheme

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Activar la SplashScreen para Android 12+
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContent {
            GymRaceTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "splash") {
                    composable("splash") { InitialScreen(navController) }
                    composable("register") { RegisterPage(navController) }
                    composable("main") { MainScreen(navController) }
                    composable("login") { LoginPage(navController) }
                    composable("register2") { RegisterPage2(navController) }
                }
            }
        }
    }
}


//arreglar registro 2 y 1








//bueno

//@RequiresApi(Build.VERSION_CODES.O)
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            GymRaceTheme {
//                val navController = rememberNavController()
//                NavHost(navController, startDestination = "login") {
//                    composable("register") { RegisterPage(navController) }
//                    composable("main") { MainScreen(navController) }
//                    composable("login") { LoginPage(navController) }
//                    composable("register2") { RegisterPage2(navController) }
//
//                    //pasar el navcontroller a todas las pantallas
//                    //has olvidado la contraseña
//                }
//            }
//        }
//    }
//}


