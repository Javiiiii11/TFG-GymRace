package com.example.gymrace

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.gymrace.pages.autenticación.LoginPage
import com.example.gymrace.pages.autenticación.RegisterPage
import com.example.gymrace.pages.autenticación.RegisterPage2
import com.example.gymrace.pages.dietas.DietasPage
import com.example.gymrace.pages.inicio.InitialScreen
import com.example.gymrace.ui.theme.GymRaceTheme
import com.example.gymrace.ui.theme.ThemeManager
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.firebase.FirebaseApp

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            GymRaceTheme {
                val navController = rememberAnimatedNavController()

                AnimatedNavHost(
                    navController = navController,
                    startDestination = "splash",
                    enterTransition = { fadeIn(animationSpec = tween(400)) },
                    exitTransition = { fadeOut(animationSpec = tween(400)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(400)) },
                    popExitTransition = { fadeOut(animationSpec = tween(400)) }
                ) {
                    // Pantalla de carga
                    composable("splash") {
                        InitialScreen(navController)
                    }
                    // Pantalla de registro
                    composable("register") {
                        RegisterPage(
                            navController = navController,
                            onThemeChange = {
                                ThemeManager.toggleTheme(LocalContext.current)
                            }
                        )
                    }
                    // Pantalla principal
                    composable("main") {
                        MainScreen(navController)
                    }
                    // Pantalla de inicio de sesión
                    composable("login") {
                        LoginPage(
                            navController = navController,
                            onThemeChange = {
                                ThemeManager.toggleTheme(LocalContext.current)
                            }
                        )
                    }
                    // Pantalla de registro 2 (registro fitness)
                    composable("register2") {
                        RegisterPage2(navController)
                    }
                    // Pantalla de editar rutina
                    composable(
                        route = "editar_rutina/{rutinaId}",
                        arguments = listOf(navArgument("rutinaId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val rutinaId = backStackEntry.arguments?.getString("rutinaId")
                        CrearRutinaPage(navController = navController, rutinaId = rutinaId)
                    }
                    // Pantalla de crear rutina
                    composable("crearRutina") {
                        CrearRutinaPage(navController = navController)
                    }
                    // Pantalla de mis rutinas
                    composable("misRutinas") {
                        ListarMisRutinasPage(navController)
                    }
                    // Pantalla de rutinas de amigos
                    composable("rutinasAmigos") {
                        ListarRutinasAmigosPage(navController)
                    }
                    // Pantalla de dietas
                    composable("dietas") {
                        DietasPage()
                    }
                    // Pantalla de ejecutar rutina
                    composable(
                        route = "ejecutar_rutina/{rutinaId}",
                        arguments = listOf(navArgument("rutinaId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val rutinaId = backStackEntry.arguments?.getString("rutinaId") ?: ""
                        EjecutarRutinaPage(navController = navController, rutinaId = rutinaId)
                    }
                }
            }
        }
    }
    data class ExerciseDetail(
        val name: String = "",
        val repetitions: Int = 0,
        val sets: Int = 0
    )
}
