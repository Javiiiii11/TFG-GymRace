//package com.example.gymrace.pages
//
//import androidx.compose.animation.core.animateFloatAsState
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.size
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.alpha
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import coil.ImageLoader
//import coil.compose.AsyncImage
//import coil.decode.GifDecoder
//import coil.decode.ImageDecoderDecoder
//import coil.request.ImageRequest
//import com.example.gymrace.R
//import kotlinx.coroutines.delay
//import android.os.Build
//import android.util.Log
//import com.google.firebase.auth.FirebaseAuth
//
//// Composable para la pantalla de inicio (carga inicial)
//@Composable
//fun InitialScreen(navController: NavController) {
//    var startAnimation by remember { mutableStateOf(false) }
//    val alphaAnim by animateFloatAsState(
//        targetValue = if (startAnimation) 1f else 0f,
//        animationSpec = tween(durationMillis = 1000),
//        label = "splashAlpha"
//    )
//
//    LaunchedEffect(key1 = true) {
//        startAnimation = true // Inicia la animación
//        delay(3000) // Espera 3 segundos
//
//        val auth = FirebaseAuth.getInstance() // Obtiene la instancia de FirebaseAuth
//        val currentUser = auth.currentUser // Obtiene el usuario actual
//
//        currentUser?.reload() // Recarga el estado del usuario
//
//        // Verifica si el usuario está autenticado y si su email está verificado
//        Log.d("Firebase", "currentUser: ${currentUser?.email}")
//        Log.d("Firebase", "isEmailVerified: ${currentUser?.isEmailVerified}")
//
//        // Si el usuario está autenticado y su email está verificado, navega a la pantalla principal
//        if (currentUser != null) {
//            navController.navigate("main") {
//                Log.d("Navigation", "Navegando a home")
//                popUpTo("splash") { inclusive = true }
//            }
//        } else {
//            navController.navigate("login") {
//                Log.d("Navigation", "Navegando a login")
//                popUpTo("splash") { inclusive = true }
//            }
//        }
//    }
//
//    // Pantalla de carga
//    SplashContent(alpha = alphaAnim)
//}
//
//// Composable para el contenido de la pantalla de carga
//@Composable
//fun SplashContent(alpha: Float) {
//    val context = LocalContext.current
//
//    // Configuración del ImageLoader específico para GIFs
//    val imageLoader = ImageLoader.Builder(context)
//        .components {
//            if (Build.VERSION.SDK_INT >= 28) {
//                add(ImageDecoderDecoder.Factory())
//            } else {
//                add(GifDecoder.Factory())
//            }
//        }
//        .build()
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
////            .background(MaterialTheme.colorScheme.background), // Fondo
//            .background(Color.White), // Fondo
//        contentAlignment = Alignment.Center
//    ) {
//        AsyncImage(
//            model = ImageRequest.Builder(context)
//                .data(R.drawable.rotating) // GIF girando
////                .data(R.drawable.logo_girando) // GIF girando
//                .build(),
//            contentDescription = "Logo girando de GymRace",
//            imageLoader = imageLoader,
//            modifier = Modifier
//                .size(250.dp)
//                .alpha(alpha) // Aplicamos el efecto de aparición gradual solo al GIF
//        )
//    }
//}








package com.example.gymrace.pages.inicio

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.animation.*
import androidx.compose.ui.graphics.graphicsLayer
import com.google.firebase.auth.FirebaseAuth

// Composable para la pantalla de inicio (carga inicial)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InitialScreen(navController: NavController) {
    var startAnimation by remember { mutableStateOf(false) }
    var startExitAnimation by remember { mutableStateOf(false) }

    // Animación de aparición gradual
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation && !startExitAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "splashAlpha"
    )

    // Animación de escala para el efecto final
    val scaleAnim by animateFloatAsState(
        targetValue = if (startExitAnimation) 2f else 1f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "splashScale"
    )

    // Animación de rotación para añadir al GIF
    val rotationAnim by animateFloatAsState(
        targetValue = if (startExitAnimation) -360f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "splashRotation"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true // Inicia la animación de entrada
        delay(2500) // Espera mientras se muestra el GIF girando

        startExitAnimation = true // Inicia la animación de salida
        delay(600) // Espera que termine la animación de salida

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        try {
            currentUser?.reload()?.addOnCompleteListener {
                if (currentUser != null) {
                    navController.navigate("main") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Error al recargar usuario", e)
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }


        Log.d("Firebase", "currentUser: ${currentUser?.email}")
        Log.d("Firebase", "isEmailVerified: ${currentUser?.isEmailVerified}")

        if (currentUser != null) {
            navController.navigate("main") {
                Log.d("Navigation", "Navegando a home")
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                Log.d("Navigation", "Navegando a login")
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // Pantalla de carga con animaciones
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Capa de fondo que se desvanece
        AnimatedVisibility(
            visible = startExitAnimation,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.White.copy(alpha = 0.7f))
            )
        }

        // Logo con animaciones
        SplashContent(
            alpha = alphaAnim,
            scale = scaleAnim,
            rotation = rotationAnim
        )
    }
}

// Composable para el contenido de la pantalla de carga con más animaciones
@Composable
fun SplashContent(alpha: Float, scale: Float, rotation: Float) {
    val context = LocalContext.current

    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    // Escala relativa al tamaño de pantalla
    val fillFraction = 0.6f + (scale - 1f) * 0.4f // más grande al inicio

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(R.drawable.rotating)
                .build(),
            contentDescription = "Logo girando de GymRace",
            imageLoader = imageLoader,
            modifier = Modifier
                .fillMaxSize(fraction = fillFraction.coerceIn(0.3f, 1f))
                .alpha(alpha)
                .graphicsLayer {
                    rotationZ = rotation
                }
        )
    }
}
