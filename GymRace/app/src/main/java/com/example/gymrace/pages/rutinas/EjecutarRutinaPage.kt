package com.example.gymrace

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjecutarRutinaPage(navController: NavHostController, rutinaId: String) {
    // Inicializa Firebase Firestore
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Inicializa el controlador de la corutina
//    val scope = rememberCoroutineScope()

    // Estados para la rutina
    var rutina by remember { mutableStateOf<Rutina?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Estados para la ejecución
    var currentExerciseIndex by remember { mutableStateOf(0) }
    var isExerciseCompleted by remember { mutableStateOf(false) }
    var isRutinaCompleted by remember { mutableStateOf(false) }

    // Estados para las series
    var currentSeries by remember { mutableStateOf(1) }
    var lastTimeUsed by remember { mutableStateOf(30) } // Guarda el último tiempo usado para resetear

    // Datos de ejercicios
    val (ejerciciosData, _) = remember {
        loadGifsFromXml(context.resources.getXml(R.xml.ejercicios), context)
    }

    // Referencia al ejercicio actual
    val currentExerciseName = rutina?.ejercicios?.getOrNull(currentExerciseIndex)
    val currentExerciseData = currentExerciseName?.let { nombre ->
        ejerciciosData.find { it.title == nombre }
    }

    // Temporizador de ejercicio
    var exerciseTimer by remember { mutableStateOf(30) } // 30 segundos por defecto
    var isTimerRunning by remember { mutableStateOf(false) }

    // Estado para controlar la visibilidad del popup de ajuste del temporizador
    var showTimerAdjustDialog by remember { mutableStateOf(false) }

    // Límites para el temporizador
    val minTime = 10 // tiempo mínimo en segundos
    val maxTime = 120 // tiempo máximo en segundos

    // Carga la rutina
    LaunchedEffect(rutinaId) {
        isLoading = true
        try {
            // Intentamos obtener la rutina desde la colección "rutinas"
            var document = db.collection("rutinas").document(rutinaId).get().await()

            // Si no existe en "rutinas", se prueba en "rutinaspredefinidas"
            if (!document.exists()) {
                document = db.collection("rutinaspredefinidas").document(rutinaId).get().await()
            }

            // Si se encontró la rutina en alguna de las colecciones
            if (document.exists()) {
                val data = document.data
                if (data != null) {
                    rutina = Rutina(
                        id = document.id,
                        nombre = data["nombre"] as? String ?: "",
                        descripcion = data["descripcion"] as? String ?: "",
                        dificultad = data["dificultad"] as? String ?: "Medio",
                        ejercicios = (data["ejercicios"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        usuarioId = data["usuarioId"] as? String ?: "",
                        fechaCreacion = data["fechaCreacion"] as? Timestamp ?: Timestamp.now()
                    )
                }
                isLoading = false
            } else {
                // Si no se encontró la rutina en ninguna colección
                errorMessage = "No se encontró la rutina"
                Log.d("Error", "Rutina no encontrada")
                isLoading = false
            }
        } catch (e: Exception) {
            // Manejo de errores al cargar la rutina
            errorMessage = "Error al cargar la rutina: ${e.message}"
            Log.e("Error", "Error al cargar la rutina: ${e.message}")
            isLoading = false
        }
    }

    // Efecto para el temporizador
    LaunchedEffect(isTimerRunning, currentExerciseIndex, currentSeries) {
        if (isTimerRunning) {
            while (exerciseTimer > 0) {
                delay(1000)
                exerciseTimer--
            }
            // Completar ejercicio cuando termina el temporizador
            isExerciseCompleted = true
            isTimerRunning = false
        }
    }

    // Diálogo de confirmación para salir
    var showExitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(rutina?.nombre ?: "Ejecutar Rutina") },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Progreso de la rutina y serie actual
                    if (rutina != null && !isLoading) {
                        Text(
                            "${currentExerciseIndex + 1}/${rutina!!.ejercicios.size} · Serie $currentSeries",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (errorMessage != null) {
                ErrorMessage(
                    message = errorMessage!!,
                    onRetry = {
                        errorMessage = null
                        isLoading = true
                        // Volver a cargar la rutina
                    }
                )
            } else if (isRutinaCompleted) {
                RutinaCompletada(
                    rutina = rutina!!,
                    onFinish = {
                        navController.navigate("main") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Barra de progreso para toda la rutina
                    LinearProgressIndicator(
                        progress = (currentExerciseIndex.toFloat() / rutina!!.ejercicios.size.toFloat()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp) // Altura reducida
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Nombre del ejercicio actual y número de serie
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentExerciseName ?: "Ejercicio",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }


                    Spacer(modifier = Modifier.height(32.dp))

                    // Visualización del ejercicio (GIF)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentExerciseData != null) {
                            GifImage(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                gif = currentExerciseData.resource
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Detalle del ejercicio actual
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            if (currentExerciseData != null) {
                                Text(
                                    text = "Músculo principal: ${currentExerciseData.mainMuscle}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Categoría: ${currentExerciseData.category}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(0.5f))

                    // Temporizador y botones pegados abajo
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Temporizador
                        if (!isExerciseCompleted) {
                            // Temporizador clickable para abrir el diálogo de ajuste
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable(enabled = !isTimerRunning) {
                                        if (!isTimerRunning) {
                                            showTimerAdjustDialog = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = exerciseTimer.toString(),
                                        fontSize = 40.sp,
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )

                                    // Indicador visual para ajustar el tiempo (solo visible si no está corriendo)
                                    if (!isTimerRunning) {
                                        Text(
                                            text = "Tocar para ajustar",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Botón de inicio/pausa
                            Button(
                                onClick = { isTimerRunning = !isTimerRunning },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isTimerRunning) "Pausar" else "Iniciar")
                            }
                        } else {
                            // Botones de acción cuando se completa un ejercicio
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Botón para otra serie
                                Button(
                                    onClick = {
                                        currentSeries++
                                        isExerciseCompleted = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Otra serie")
                                }

                                // Botón para siguiente ejercicio
                                Button(
                                    onClick = {
                                        if (currentExerciseIndex < rutina!!.ejercicios.size - 1) {
                                            currentExerciseIndex++
                                            currentSeries = 1 // Resetear series para el nuevo ejercicio
                                            isExerciseCompleted = false
                                            exerciseTimer = 30 // Resetear temporizador al valor por defecto
                                            lastTimeUsed = 30 // Resetear valor guardado también
                                        } else {
                                            isRutinaCompleted = true
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    if (currentExerciseIndex < rutina!!.ejercicios.size - 1) {
                                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Siguiente")
                                    } else {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Finalizar")
                                    }
                                }
                            }

                            // Información sobre series completadas
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currentSeries series completadas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo para ajustar el temporizador
    if (showTimerAdjustDialog) {
        AlertDialog(
            onDismissRequest = { showTimerAdjustDialog = false },
            title = { Text("Ajustar tiempo") },
            text = {
                Column(
                    modifier = Modifier.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Valor actual grande
                    Text(
                        text = "$exerciseTimer s",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Control de ajuste del temporizador
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botones de decremento
                        Row {
                            OutlinedIconButton(
                                onClick = { exerciseTimer = (exerciseTimer - 10).coerceAtLeast(minTime) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("-10", style = MaterialTheme.typography.labelMedium)
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            OutlinedIconButton(
                                onClick = { exerciseTimer = (exerciseTimer - 5).coerceAtLeast(minTime) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("-5", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Botones de incremento
                        Row {
                            OutlinedIconButton(
                                onClick = { exerciseTimer = (exerciseTimer + 5).coerceAtMost(maxTime) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+5", style = MaterialTheme.typography.labelMedium)
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            OutlinedIconButton(
                                onClick = { exerciseTimer = (exerciseTimer + 10).coerceAtMost(maxTime) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+10", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Slider para ajuste fino
                    Slider(
                        value = exerciseTimer.toFloat(),
                        onValueChange = { exerciseTimer = it.toInt() },
                        valueRange = minTime.toFloat()..maxTime.toFloat(),
                        steps = ((maxTime - minTime) / 5) - 1, // Pasos de 5 segundos
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Rango del slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$minTime s", style = MaterialTheme.typography.bodySmall)
                        Text("$maxTime s", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTimerAdjustDialog = false
                        lastTimeUsed = exerciseTimer // Guardar el tiempo seleccionado
                    }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Diálogo de confirmación para salir
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Salir de la rutina") },
            text = { Text("¿Estás seguro que deseas salir? Tu progreso no se guardará.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Salir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continuar")
                }
            }
        )
    }
}

// Composable para mostrar un mensaje de error
@Composable
fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}

// Composable para mostrar la pantalla de rutina completada
@Composable
fun RutinaCompletada(rutina: Rutina, onFinish: () -> Unit) {

    // Animación de escala infinita
    // Se utiliza para hacer que el icono de verificación crezca y decrezca
    val infiniteTransition = rememberInfiniteTransition()
    // Se define la animación de escala
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(100.dp * scale),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "¡Rutina Completada!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "¡Felicidades! Has completado \"${rutina.nombre}\"",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Completaste ${rutina.ejercicios.size} ejercicios",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Home, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Volver al inicio")
        }
    }
}

// Clase para iniciar una rutina desde un botón
class RutinaLauncher(
    private val navController: NavController,
    private val rutinaId: String
) {
    // Función para iniciar la rutina
    fun launch() {
        navController.navigate("ejecutar_rutina/$rutinaId")
    }

    // Composable que crea un botón para iniciar la rutina
    @Composable
    fun LaunchButton(
        modifier: Modifier = Modifier,
        text: String = "Iniciar Rutina",
        showIcon: Boolean = true
    ) {
        Button(
            onClick = { launch() },
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (showIcon) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(text)
        }
    }
}