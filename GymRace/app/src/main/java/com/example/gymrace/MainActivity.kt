//package com.example.gymrace
//
//import android.os.Build
//import android.os.Bundle
//import android.util.Log
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.annotation.RequiresApi
//import androidx.compose.ui.platform.LocalContext
//import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
//import androidx.navigation.NavType
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//import androidx.navigation.navArgument
//import com.example.gymrace.pages.Dieta
//import com.example.gymrace.pages.DietasPage
//import com.example.gymrace.pages.InitialScreen
//import com.example.gymrace.pages.LoginPage
//import com.example.gymrace.pages.RegisterPage
//import com.example.gymrace.pages.RegisterPage2
//import com.example.gymrace.ui.theme.GymRaceTheme
//import com.example.gymrace.ui.theme.ThemeManager
//import com.google.firebase.FirebaseApp
//import com.google.firebase.firestore.FirebaseFirestore
//import np.com.bimalkafle.bottomnavigationdemo.pages.PredefinedRoutine
//
//// Clase principal de la aplicación
//@RequiresApi(Build.VERSION_CODES.O)
//class MainActivity : ComponentActivity() {
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        // Activar la SplashScreen para Android 12+
//        installSplashScreen()
//
//        super.onCreate(savedInstanceState)
//
//        // Inicializar Firebase (si no está inicializado)
//        FirebaseApp.initializeApp(this)
//
//        setContent {
//            GymRaceTheme {
//                val navController = rememberNavController()
//                NavHost(navController, startDestination = "splash") {
//
//                    // Composable para la pantalla de inicio (carga inicial)
//                    composable("splash") { InitialScreen(navController) }
//
//                    // Composable para la página de registro
//                    composable("register") {
//                        RegisterPage(
//                            navController = navController,
//                            onThemeChange = {
//                                // ThemeManager accederá al contexto desde dentro del RegisterPage
//                                ThemeManager.toggleTheme(LocalContext.current)
//                            }
//                        )
//                    }
//
//                    // Composable para la página principal
//                    composable("main") { MainScreen(navController) }
//
//                    // Composable para la página de inicio de sesión
//                    composable("login") {
//                        LoginPage(
//                            navController = navController,
//                            onThemeChange = {
//                                ThemeManager.toggleTheme(LocalContext.current)
//                            }
//                        )
//                    }
//                    // Composable para la página de crear usuario fittnes
//                    composable("register2") { RegisterPage2(navController) }
//
//                    // Composable para la página de detalles de rutina predefinida
//                    composable(
//                        "editar_rutina/{rutinaId}",
//                        arguments = listOf(navArgument("rutinaId") { type = NavType.StringType })
//                    ) { backStackEntry ->
//                        val rutinaId = backStackEntry.arguments?.getString("rutinaId")
//                        CrearRutinaPage(navController = navController, rutinaId = rutinaId)
//                    }
//
//                    // Composable para la página de crear rutina
//                    composable("crearRutina") {
//                        CrearRutinaPage(navController = navController)
//                    }
//
//                    // Composable para la página de listar rutinas del usuario
//                    composable("misRutinas") { ListarMisRutinasPage(navController) }
//
//                    // Composable para la página de listar rutinas de amigos
//                    composable("rutinasAmigos") { ListarRutinasAmigosPage(navController) }
//
//                    // Composable para las dietas
//                    composable("dietas") { DietasPage() }
//
//                    // Composable para ejecutar rutina
//                    composable(
//                        route = "ejecutar_rutina/{rutinaId}",
//                        arguments = listOf(navArgument("rutinaId") { type = NavType.StringType })
//                    ) { backStackEntry ->
//                        val rutinaId = backStackEntry.arguments?.getString("rutinaId") ?: ""
//                        EjecutarRutinaPage(navController = navController, rutinaId = rutinaId)
//                    }
//                }
//            }
//        }
//    }
//
//data class ExerciseDetail(
//        val name: String = "",
//        val repetitions: Int = 0,
//        val sets: Int = 0
//    )
//}
















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
                    composable("splash") {
                        InitialScreen(navController)
                    }

                    composable("register") {
                        RegisterPage(
                            navController = navController,
                            onThemeChange = {
                                ThemeManager.toggleTheme(LocalContext.current)
                            }
                        )
                    }

                    composable("main") {
                        MainScreen(navController)
                    }

                    composable("login") {
                        LoginPage(
                            navController = navController,
                            onThemeChange = {
                                ThemeManager.toggleTheme(LocalContext.current)
                            }
                        )
                    }

                    composable("register2") {
                        RegisterPage2(navController)
                    }

                    composable(
                        route = "editar_rutina/{rutinaId}",
                        arguments = listOf(navArgument("rutinaId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val rutinaId = backStackEntry.arguments?.getString("rutinaId")
                        CrearRutinaPage(navController = navController, rutinaId = rutinaId)
                    }

                    composable("crearRutina") {
                        CrearRutinaPage(navController = navController)
                    }

                    composable("misRutinas") {
                        ListarMisRutinasPage(navController)
                    }

                    composable("rutinasAmigos") {
                        ListarRutinasAmigosPage(navController)
                    }

                    composable("dietas") {
                        DietasPage()
                    }

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
