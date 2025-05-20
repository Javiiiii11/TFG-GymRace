package com.example.gymrace

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

// Composable para ejecutar una rutina
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjecutarRutinaPage(navController: NavHostController, rutinaId: String) {
    // Inicializa Firebase Firestore
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Estados para la rutina
    var rutina by remember { mutableStateOf<Rutina?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Estados para la ejecución
    var currentExerciseIndex by remember { mutableStateOf(0) }
    var isExerciseCompleted by remember { mutableStateOf(false) }
    var isRutinaCompleted by remember { mutableStateOf(false) }

    // Estado para el modo (ejercicio o descanso)
    var isRestMode by remember { mutableStateOf(false) }
    var isRestBetweenSeries by remember { mutableStateOf(false) }

    // Estados para las series
    var currentSeries by remember { mutableStateOf(1) }

    // Valores guardados para los temporizadores
    var lastExerciseTimeUsed by remember { mutableStateOf(30) } // Último tiempo de ejercicio usado
    var lastRestTimeUsed by remember { mutableStateOf(60) } // Último tiempo de descanso usado

    // Temporizadores
    var exerciseTimer by remember { mutableStateOf(30) } // 30 segundos por defecto para ejercicio
    var restTimer by remember { mutableStateOf(60) } // 60 segundos por defecto para descanso
    var isTimerRunning by remember { mutableStateOf(false) }

    // Datos de ejercicios
    val (ejerciciosData, _) = remember {
        loadGifsFromXml(context.resources.getXml(R.xml.ejercicios), context)
    }

    // Referencia al ejercicio actual
    val currentExerciseName = rutina?.ejercicios?.getOrNull(currentExerciseIndex)
    val currentExerciseData = currentExerciseName?.let { nombre ->
        ejerciciosData.find { it.title == nombre }
    }

    // Siguiente ejercicio (para mostrar en la vista de descanso)
    val nextExerciseName = when {
        isRestMode && isRestBetweenSeries -> rutina?.ejercicios?.getOrNull(currentExerciseIndex) // mostrar mismo ejercicio
        else -> rutina?.ejercicios?.getOrNull(currentExerciseIndex + 1) // mostrar siguiente ejercicio
    }

    // Datos del siguiente ejercicio
    val nextExerciseData = nextExerciseName?.let { nombre ->
        ejerciciosData.find { it.title == nombre }
    }


    // Estado para controlar la visibilidad del popup de ajuste del temporizador
    var showTimerAdjustDialog by remember { mutableStateOf(false) }

    // Límites para los temporizadores
    val minExerciseTime = 10 // tiempo mínimo en segundos para ejercicio
    val maxExerciseTime = 120 // tiempo máximo en segundos para ejercicio
    val minRestTime = 15 // tiempo mínimo en segundos para descanso
    val maxRestTime = 180 // tiempo máximo en segundos para descanso

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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Chip para indicar el modo actual (ejercicio o descanso)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isRestMode)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isRestMode) Icons.Default.Coffee else Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = if (isRestMode)
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isRestMode) "DESCANSO" else "EJERCICIO",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isRestMode)
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Contenido principal - Cambia según el modo (ejercicio o descanso)
                    AnimatedVisibility(
                        visible = !isRestMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        // MODO EJERCICIO
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Nombre del ejercicio actual
                            Text(
                                text = currentExerciseName ?: "Ejercicio",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(15.dp))

                            // Visualización del ejercicio (GIF)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(270.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentExerciseData != null) {
                                    GifImage(
                                        modifier = Modifier
                                            .size(230.dp)
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
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isRestMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Vista previa
                                if (nextExerciseName != null && nextExerciseData != null) {
                                    Text(
                                        text = "A continuación:",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = nextExerciseName,
                                        style = MaterialTheme.typography.headlineSmall,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2, // Limitar líneas para que no crezca tanto
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(260.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        GifImage(
                                            modifier = Modifier
                                                .size(230.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            gif = nextExerciseData.resource
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(240.dp)
                                            .background(
                                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            modifier = Modifier.size(130.dp),
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "¡Último descanso antes de terminar!",
                                        style = MaterialTheme.typography.headlineSmall,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }


                    Spacer(modifier = Modifier.weight(0.5f))

                    // Temporizador y botones pegados abajo
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Temporizador (igual para ejercicio y descanso, solo cambia el color)
                        if (!isExerciseCompleted || isRestMode) {
                            // Temporizador clickable para abrir el diálogo de ajuste
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isRestMode)
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        else MaterialTheme.colorScheme.primaryContainer
                                    )
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
                                        text = (if (isRestMode) restTimer else exerciseTimer).toString(),
                                        fontSize = 40.sp,
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = if (isRestMode)
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        else MaterialTheme.colorScheme.onPrimaryContainer
                                    )

                                    // Indicador visual para ajustar el tiempo (solo visible si no está corriendo)
                                    if (!isTimerRunning) {
                                        Text(
                                            text = "Tocar para ajustar",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isRestMode)
                                                MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                            else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
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
                                    .height(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRestMode)
                                        MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isTimerRunning) "Pausar" else "Iniciar")
                            }

                            // Botón para saltar el tiempo (solo visible si estamos en descanso)
                            if (isRestMode) {
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                onClick = {
                                    if (isRestBetweenSeries) {
                                        currentSeries++
                                        isRestMode = false
                                        isRestBetweenSeries = false
                                        isTimerRunning = false
                                        exerciseTimer = lastExerciseTimeUsed
                                    } else {
                                        currentExerciseIndex++
                                        currentSeries = 1
                                        isRestMode = false
                                        isTimerRunning = false
                                        exerciseTimer = lastExerciseTimeUsed
                                    }

                                },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                ) {
                                    Text("Saltar descanso")
                                }
                            }
                        }

                        // Botones de acción cuando se completa un ejercicio y no estamos en modo descanso
                        if (isExerciseCompleted && !isRestMode) {
                            // Botones de acción cuando se completa un ejercicio
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Botón para otra serie
                                Button(
                                    onClick = {
                                        isRestMode = true
                                        isRestBetweenSeries = true // << Activamos flag de descanso entre series
                                        restTimer = lastRestTimeUsed
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

                                // Botón para descanso/siguiente ejercicio
                                Button(
                                    onClick = {
                                        if (currentExerciseIndex < rutina!!.ejercicios.size - 1) {
                                            // Si no es el último ejercicio, ofrecemos descanso
                                            isRestMode = true
                                            restTimer = lastRestTimeUsed
                                            isExerciseCompleted = false
                                        } else {
                                            // Si es el último ejercicio, completamos la rutina
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
                                        Icon(Icons.Default.Coffee, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Descansar")
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

                        // Si estamos en modo descanso y es tiempo de pasar al siguiente ejercicio
                        if (isRestMode && restTimer <= 0) {
                            Button(
                                onClick = {
                                    if (isRestBetweenSeries) {
                                        // Es descanso entre series → NO cambiamos de ejercicio
                                        currentSeries++
                                        isRestMode = false
                                        isRestBetweenSeries = false
                                        exerciseTimer = lastExerciseTimeUsed
                                    } else {
                                        // Es descanso normal entre ejercicios → pasamos al siguiente
                                        currentExerciseIndex++
                                        currentSeries = 1
                                        isRestMode = false
                                        exerciseTimer = lastExerciseTimeUsed
                                    }

                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Siguiente ejercicio")
                            }
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
            title = { Text(if (isRestMode) "Ajustar tiempo de descanso" else "Ajustar tiempo de ejercicio") },
            text = {
                Column(
                    modifier = Modifier.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Valor actual grande
                    Text(
                        text = "${if (isRestMode) restTimer else exerciseTimer} s",
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
                                onClick = {
                                    if (isRestMode) {
                                        restTimer = (restTimer - 10).coerceAtLeast(minRestTime)
                                    } else {
                                        exerciseTimer = (exerciseTimer - 10).coerceAtLeast(minExerciseTime)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("-10", style = MaterialTheme.typography.labelMedium)
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            OutlinedIconButton(
                                onClick = {
                                    if (isRestMode) {
                                        restTimer = (restTimer - 5).coerceAtLeast(minRestTime)
                                    } else {
                                        exerciseTimer = (exerciseTimer - 5).coerceAtLeast(minExerciseTime)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("-5", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Botones de incremento
                        Row {
                            OutlinedIconButton(
                                onClick = {
                                    if (isRestMode) {
                                        restTimer = (restTimer + 5).coerceAtMost(maxRestTime)
                                    } else {
                                        exerciseTimer = (exerciseTimer + 5).coerceAtMost(maxExerciseTime)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+5", style = MaterialTheme.typography.labelMedium)
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            OutlinedIconButton(
                                onClick = {
                                    if (isRestMode) {
                                        restTimer = (restTimer + 10).coerceAtMost(maxRestTime)
                                    } else {
                                        exerciseTimer = (exerciseTimer + 10).coerceAtMost(maxExerciseTime)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+10", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Slider para ajuste fino
                    Slider(
                        value = if (isRestMode) restTimer.toFloat() else exerciseTimer.toFloat(),
                        onValueChange = { value ->
                            if (isRestMode) {
                                restTimer = value.toInt()
                            } else {
                                exerciseTimer = value.toInt()
                            }
                        },
                        valueRange = if (isRestMode) minRestTime.toFloat()..maxRestTime.toFloat()
                        else minExerciseTime.toFloat()..maxExerciseTime.toFloat(),
                        steps = ((if (isRestMode) maxRestTime - minRestTime else maxExerciseTime - minExerciseTime) / 5) - 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${if (isRestMode) minRestTime else minExerciseTime} s", style = MaterialTheme.typography.bodySmall)
                        Text("${if (isRestMode) maxRestTime else maxExerciseTime} s", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTimerAdjustDialog = false
                        if (isRestMode) {
                            lastRestTimeUsed = restTimer
                        } else {
                            lastExerciseTimeUsed = exerciseTimer
                        }
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
    // Efecto para el temporizador
    LaunchedEffect(key1 = isTimerRunning, key2 = isRestMode) {
        if (isTimerRunning) {
            while (if (isRestMode) restTimer > 0 else exerciseTimer > 0) {
                delay(1000)
                if (isRestMode) {
                    restTimer -= 1
                } else {
                    exerciseTimer -= 1
                }
            }

            isTimerRunning = false

            if (isRestMode) {
                if (isRestBetweenSeries) {
                    // DESCANSO entre series: NO pasar al siguiente ejercicio
                    currentSeries++
                    isRestMode = false
                    isRestBetweenSeries = false
                    exerciseTimer = lastExerciseTimeUsed
                    isExerciseCompleted = false
                } else {
                    // DESCANSO entre ejercicios: PASAR al siguiente ejercicio
                    currentExerciseIndex++
                    currentSeries = 1
                    isRestMode = false
                    exerciseTimer = lastExerciseTimeUsed
                    isExerciseCompleted = false

                    if (currentExerciseIndex >= (rutina?.ejercicios?.size ?: 0)) {
                        isRutinaCompleted = true
                    }
                }
            } else {
                isExerciseCompleted = true
            }
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

// Composable para mostrar la rutina completada
@Composable
fun RutinaCompletada(rutina: Rutina, onFinish: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
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
