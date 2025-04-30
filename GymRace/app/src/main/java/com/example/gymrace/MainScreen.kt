//package com.example.gymrace
//
//import android.os.Build
//import androidx.annotation.RequiresApi
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Home
//import androidx.compose.material.icons.filled.Person
//import androidx.compose.material3.Badge
//import androidx.compose.material3.BadgedBox
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.NavigationBar
//import androidx.compose.material3.NavigationBarItem
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableIntStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.res.vectorResource
//import np.com.bimalkafle.bottomnavigationdemo.pages.HomePage
//import np.com.bimalkafle.bottomnavigationdemo.pages.UserPage
//import androidx.compose.material3.NavigationBarItemDefaults
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavController
//import com.example.gymrace.pages.ChallengeViewModel
//import com.example.gymrace.pages.DesafiosPage
//import com.example.gymrace.pages.DietasPage
//import com.example.gymrace.ui.theme.ThemeManager
//import com.example.gymrace.ui.theme.rememberThemeState
//import com.google.firebase.auth.FirebaseAuth
//
//// Clase de datos para los elementos de navegación
//@RequiresApi(Build.VERSION_CODES.O)
//@Composable
//fun MainScreen(navController: NavController) {
//    // Recupera el contexto de la aplicación
//    val context = LocalContext.current
//    // Cambia el tema de la aplicación
//    val isDarkTheme = rememberThemeState().value
//
//    // Define la lista de elementos de navegación
//    val navItemList = listOf(
//        NavItem("Inicio", Icons.Default.Home, 0),
//        NavItem("Dietas", ImageVector.vectorResource(id = R.drawable.dietas2), 0),
//        NavItem("Ejercicios", ImageVector.vectorResource(id = R.drawable.fitness), 0),
//        NavItem("Desafios", ImageVector.vectorResource(id = R.drawable.fuego), 0),
//        NavItem("Perfíl", Icons.Default.Person, 0),
//    )
//    // Estado para el índice seleccionado
//    var selectedIndex by remember { mutableIntStateOf(0) }
//
//    // Cambia el color del icono y el texto seleccionado
//    Scaffold(
//        modifier = Modifier.fillMaxSize(),
//        bottomBar = {
//            NavigationBar {
//                navItemList.forEachIndexed { index, navItem ->
//                    NavigationBarItem(
//                        selected = selectedIndex == index,
//                        onClick = { selectedIndex = index },
//                        icon = {
//                            BadgedBox(badge = {
//                                if (navItem.badgeCount > 0)
//                                    Badge(
//                                        containerColor = MaterialTheme.colorScheme.surfaceVariant // Cambia el color del círculo aquí
//                                    ) {
//                                        Text(text = navItem.badgeCount.toString())
//                                    }
//                            }) {
//                                Icon(imageVector = navItem.icon, contentDescription = "Icon")
//                            }
//                        },
//                        label = { Text(
//                            text = navItem.label,
//                            fontSize = 11.sp // Adjust the font size here
//
//                        ) },
//                        colors = NavigationBarItemDefaults.colors(
//                            selectedIconColor = MaterialTheme.colorScheme.primary,
//                            selectedTextColor = MaterialTheme.colorScheme.primary
//                        )
//                    )
//                }
//            }
//        }
//    ) { innerPadding ->
//        ContentScreen(
//            modifier = Modifier.padding(innerPadding),
//            selectedIndex = selectedIndex,
//            onThemeChange = {
//                ThemeManager.toggleTheme(context)
//            },
//            navController = navController
//        )
//    }
//}
//
//// Clase de datos para los elementos de navegación
//@RequiresApi(Build.VERSION_CODES.O)
//@Composable
//fun ContentScreen(modifier: Modifier = Modifier, selectedIndex: Int, onThemeChange: () -> Unit, navController: NavController) {
//    when (selectedIndex) {
//        // Cambia el contenido de la pantalla según el índice seleccionado
//        0 -> HomePage(onThemeChange = onThemeChange, navController = navController)
//        1 -> DietasPage()
//        2 -> EjerciciosPage()
//        3 -> {
//            // Obtenemos una instancia del ViewModel
//            val viewModel = viewModel<ChallengeViewModel>()
//            // Obtenemos el ID del usuario actual
//            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
//            // Llamamos a DesafiosPage con los parámetros necesarios
//            DesafiosPage(viewModel = viewModel, userId = userId)
//        }
//        4 -> UserPage(onThemeChange = onThemeChange, navController = navController)
//    }
//}















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

data class BottomNavItem(val label: String, val route: String, val icon: ImageVector)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(parentNavController: NavHostController) {
    val context = LocalContext.current
    val navController = rememberAnimatedNavController()

    val items = listOf(
        BottomNavItem("Inicio", "home", Icons.Default.Home),
        BottomNavItem("Dietas", "dietas", ImageVector.vectorResource(R.drawable.dietas2)),
        BottomNavItem("Ejercicios", "ejercicios", ImageVector.vectorResource(R.drawable.fitness)),
        BottomNavItem("Desafíos", "desafios", ImageVector.vectorResource(R.drawable.fuego)),
        BottomNavItem("Perfíl", "user", Icons.Default.Person),
    )

    // Seguimiento del destino actual
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Mapa para rastrear la última ruta conocida
    val routeIndices = remember { mutableStateMapOf<String, Int>() }

    // Inicializar el mapa con los índices de las rutas
    LaunchedEffect(true) {
        items.forEachIndexed { index, item ->
            routeIndices[item.route] = index
        }
    }

    // Último índice conocido (por defecto, home = 0)
    var lastKnownRouteIndex by remember { mutableStateOf(0) }

    // Actualizar el último índice conocido cuando cambia la ruta
    currentRoute?.let { route ->
        routeIndices[route]?.let { index ->
            lastKnownRouteIndex = index
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
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
        AnimatedNavHost(
            navController = navController,
            startDestination = "home",
            enterTransition = {
                val fromIndex = routeIndices[initialState.destination.route] ?: 0
                val toIndex = routeIndices[targetState.destination.route] ?: 0

                if (toIndex > fromIndex) {
                    // Si vamos a una pestaña a la derecha, entrada desde la derecha
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(300)
                    )
                } else {
                    // Si vamos a una pestaña a la izquierda, entrada desde la izquierda
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(300)
                    )
                }
            },
            exitTransition = {
                val fromIndex = routeIndices[initialState.destination.route] ?: 0
                val toIndex = routeIndices[targetState.destination.route] ?: 0

                if (toIndex > fromIndex) {
                    // Si vamos a una pestaña a la derecha, salida hacia la izquierda
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(300)
                    )
                } else {
                    // Si vamos a una pestaña a la izquierda, salida hacia la derecha
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(300)
                    )
                }
            },
            popEnterTransition = {
                // Para navegación hacia atrás (pop) - entrada desde la izquierda
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                // Para navegación hacia atrás (pop) - salida hacia la derecha
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300)
                )
            },
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomePage(
                    onThemeChange = { ThemeManager.toggleTheme(context) },
                    navController = parentNavController  // Usar el controlador principal
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
                    navController = parentNavController  // Usar el controlador principal
                )
            }
        }
    }
}