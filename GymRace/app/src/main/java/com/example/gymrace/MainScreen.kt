package com.example.gymrace

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.gymrace.pages.desafios.ChallengeViewModel
import com.example.gymrace.pages.desafios.DesafiosPage
import com.example.gymrace.pages.dietas.DietasPage
import com.example.gymrace.ui.theme.ThemeManager
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.firebase.auth.FirebaseAuth
import com.example.gymrace.pages.inicio.HomePage
import np.com.bimalkafle.bottomnavigationdemo.pages.UserPage

// Modelo que representa cada ítem del menú inferior
data class BottomNavItem(val label: String, val route: String, val icon: ImageVector)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(parentNavController: NavHostController) {
    val context = LocalContext.current
    val navController = rememberAnimatedNavController()

    // Lista de pestañas para la barra de navegación inferior
    val items = listOf(
        BottomNavItem("Inicio", "home", Icons.Default.Home),
        BottomNavItem("Dietas", "dietas", ImageVector.vectorResource(R.drawable.dietas2)),
        BottomNavItem("Ejercicios", "ejercicios", ImageVector.vectorResource(R.drawable.fitness)),
        BottomNavItem("Desafíos", "desafios", ImageVector.vectorResource(R.drawable.fuego)),
        BottomNavItem("Perfíl", "user", Icons.Default.Person),
    )

    // Monitorea la ruta actual que está activa
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Mapa que asocia cada ruta con su índice en la lista (para las animaciones)
    val routeIndices = remember { mutableStateMapOf<String, Int>() }

    // Inicializa los índices una sola vez
    LaunchedEffect(true) {
        items.forEachIndexed { index, item ->
            routeIndices[item.route] = index
        }
    }

    // Guarda el índice del destino actual para decidir hacia dónde se desliza la animación
    var lastKnownRouteIndex by remember { mutableStateOf(0) }

    // Actualiza el índice cada vez que cambia la ruta
    currentRoute?.let { route ->
        routeIndices[route]?.let { index ->
            lastKnownRouteIndex = index
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                // Botones del menú inferior
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    // Limpia el stack hasta el inicio (evita múltiples instancias)
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true  // Previene que se creen copias de la misma pantalla
                                    restoreState = true     // Recupera el estado anterior de la pantalla
                                }
                            }
                        },
                        icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                        label = { Text(text = item.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // Controlador de navegación animada
        AnimatedNavHost(
            navController = navController,
            startDestination = "home",
            // Transición al navegar adelante
            enterTransition = {
                val fromIndex = routeIndices[initialState.destination.route] ?: 0
                val toIndex = routeIndices[targetState.destination.route] ?: 0

                if (toIndex > fromIndex) {
                    // Si se navega a una pestaña a la derecha, entra desde la derecha
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(300)
                    )
                } else {
                    // Si se navega a una pestaña a la izquierda, entra desde la izquierda
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(300)
                    )
                }
            },
            // Transición de salida
            exitTransition = {
                val fromIndex = routeIndices[initialState.destination.route] ?: 0
                val toIndex = routeIndices[targetState.destination.route] ?: 0

                if (toIndex > fromIndex) {
                    // Sale hacia la izquierda si se va a la derecha
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(300)
                    )
                } else {
                    // Sale hacia la derecha si se va a la izquierda
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(300)
                    )
                }
            },
            // Transición para volver atrás (back)
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300)
                )
            },
            modifier = Modifier.padding(innerPadding)
        ) {
            // Cada pestaña es un "composable" con su ruta

            composable("home") {
                HomePage(
                    onThemeChange = { ThemeManager.toggleTheme(context) },
                    navController = parentNavController  // Se usa el controlador global para salir de MainScreen
                )
            }

            composable("dietas") {
                DietasPage()
            }

            composable("ejercicios") {
                EjerciciosPage()
            }

            composable("desafios") {
                val viewModel = viewModel<ChallengeViewModel>()
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                DesafiosPage(viewModel = viewModel, userId = userId)
            }

            composable("user") {
                UserPage(
                    onThemeChange = { ThemeManager.toggleTheme(context) },
                    navController = parentNavController
                )
            }
        }
    }
}
