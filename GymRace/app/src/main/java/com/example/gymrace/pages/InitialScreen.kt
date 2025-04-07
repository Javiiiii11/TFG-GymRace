package com.example.gymrace.pages

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.gymrace.R
import kotlinx.coroutines.delay
import android.os.Build
import android.util.Log
import androidx.compose.material3.MaterialTheme
import com.google.firebase.auth.FirebaseAuth

@Composable
fun InitialScreen(navController: NavController) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "splashAlpha"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true // Inicia la animación
        delay(3000) // Espera 3 segundos

        val auth = FirebaseAuth.getInstance() // Obtiene la instancia de FirebaseAuth
        val currentUser = auth.currentUser // Obtiene el usuario actual

        currentUser?.reload() // Recarga el estado del usuario

        // Verifica si el usuario está autenticado y si su email está verificado
        Log.d("Firebase", "currentUser: ${currentUser?.email}")
        Log.d("Firebase", "isEmailVerified: ${currentUser?.isEmailVerified}")

        // Si el usuario está autenticado y su email está verificado, navega a la pantalla principal
        if (currentUser != null /* && currentUser.isEmailVerified */) {
            // Si quieres verificar el email, descomenta la condición anterior
            navController.navigate("main") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // Pantalla de carga
    SplashContent(alpha = alphaAnim)
}


@Composable
fun SplashContent(alpha: Float) {
    val context = LocalContext.current

    // Configuración del ImageLoader específico para GIFs
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    Box(
        modifier = Modifier
            .fillMaxSize()
//            .background(MaterialTheme.colorScheme.background), // Fondo
            .background(Color.White), // Fondo
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(R.drawable.rotating) // GIF girando
//                .data(R.drawable.logo_girando) // GIF girando
                .build(),
            contentDescription = "Logo girando de GymRace",
            imageLoader = imageLoader,
            modifier = Modifier
                .size(250.dp)
                .alpha(alpha) // Aplicamos el efecto de aparición gradual solo al GIF
        )
    }
}