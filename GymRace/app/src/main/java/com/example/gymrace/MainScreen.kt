package com.example.gymrace

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import np.com.bimalkafle.bottomnavigationdemo.pages.HomePage
import np.com.bimalkafle.bottomnavigationdemo.pages.UserPage
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.gymrace.pages.ChallengeViewModel
import com.example.gymrace.pages.DesafiosPage
import com.example.gymrace.pages.DietasPage
import com.example.gymrace.pages.getLoginState
import com.example.gymrace.ui.theme.ThemeManager
import com.example.gymrace.ui.theme.rememberThemeState
import com.google.firebase.auth.FirebaseAuth

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current
    val isDarkTheme = rememberThemeState().value

    // Ya no necesitas MaterialTheme aquí porque ahora está en GymRaceTheme

    val navItemList = listOf(
        NavItem("Inicio", Icons.Default.Home, 0),
        NavItem("Dietas", ImageVector.vectorResource(id = R.drawable.dietas2), 0),
        NavItem("Ejercicios", ImageVector.vectorResource(id = R.drawable.fitness), 0),
        NavItem("Desafios", ImageVector.vectorResource(id = R.drawable.fuego), 0),
        NavItem("Perfíl", Icons.Default.Person, 0),
    )

    var selectedIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                navItemList.forEachIndexed { index, navItem ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        icon = {
                            BadgedBox(badge = {
                                if (navItem.badgeCount > 0)
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant // Cambia el color del círculo aquí
                                    ) {
                                        Text(text = navItem.badgeCount.toString())
                                    }
                            }) {
                                Icon(imageVector = navItem.icon, contentDescription = "Icon")
                            }
                        },
                        label = { Text(
                            text = navItem.label,
                            fontSize = 11.sp // Adjust the font size here

                        ) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        ContentScreen(
            modifier = Modifier.padding(innerPadding),
            selectedIndex = selectedIndex,
            onThemeChange = {
                ThemeManager.toggleTheme(context)
            },
            navController = navController
        )
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ContentScreen(modifier: Modifier = Modifier, selectedIndex: Int, onThemeChange: () -> Unit, navController: NavController) {
    when (selectedIndex) {
        0 -> HomePage(onThemeChange = onThemeChange, navController = navController)
        1 -> DietasPage()
        2 -> EjerciciosPage()
        3 -> {
            // Obtenemos una instancia del ViewModel
            val viewModel = viewModel<ChallengeViewModel>()
            // Obtenemos el ID del usuario actual
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            // Llamamos a DesafiosPage con los parámetros necesarios
            DesafiosPage(viewModel = viewModel, userId = userId)
        }
        4 -> UserPage(onThemeChange = onThemeChange, navController = navController)
    }
}