package com.example.gymrace

import androidx.compose.ui.graphics.vector.ImageVector

// Cambia el color del icono y el texto no seleccionado
data class NavItem(
    val label : String,
    val icon : ImageVector,
    val badgeCount : Int,
)
