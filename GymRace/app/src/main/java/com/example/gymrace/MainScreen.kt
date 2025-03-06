package com.example.gymrace

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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import np.com.bimalkafle.bottomnavigationdemo.pages.HomePage
import np.com.bimalkafle.bottomnavigationdemo.pages.UserPage


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
//    var showInitialScreen by remember { mutableStateOf(true) }
//
//    if (showInitialScreen) {
//        InitialScreen(onTimeout = { showInitialScreen = false })
//    } else {
        val navItemList = listOf(
            NavItem("Inicio", Icons.Default.Home, 0),
            NavItem("Ejercicios", ImageVector.vectorResource(id = R.drawable.fitness), 0),
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
                                        Badge() {
                                            Text(text = navItem.badgeCount.toString())
                                        }
                                }) {
                                    Icon(imageVector = navItem.icon, contentDescription = "Icon")
                                }
                            },
                            label = { Text(text = navItem.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            ContentScreen(modifier = Modifier.padding(innerPadding), selectedIndex)
        }
    }
//}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ContentScreen(modifier: Modifier = Modifier, selectedIndex : Int) {
    when(selectedIndex){
        0-> HomePage()
        1-> EjerciciosPage()
        2-> UserPage()
    }
}



















