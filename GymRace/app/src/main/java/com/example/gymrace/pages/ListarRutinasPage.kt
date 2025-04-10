package com.example.gymrace

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Rutina(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val dificultad: String = "Medio",
    val ejercicios: List<String> = emptyList(),
    val usuarioId: String = "",
    val fechaCreacion: Timestamp = Timestamp.now()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListarMisRutinasPage(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()

    var rutinas by remember { mutableStateOf<List<Rutina>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var rutinaToDelete by remember { mutableStateOf<Rutina?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedRutina by remember { mutableStateOf<Rutina?>(null) }

    // Cargar rutinas del usuario
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            isLoading = true
            try {
                // Consulta simplificada que solo filtra por usuarioId sin ordenar
                val snapshot = db.collection("rutinas")
                    .whereEqualTo("usuarioId", userId)
                    .get()
                    .await()

                // Mapear los resultados y luego ordenarlos localmente
                rutinas = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { data ->
                        Rutina(
                            id = doc.id,
                            nombre = data["nombre"] as? String ?: "",
                            descripcion = data["descripcion"] as? String ?: "",
                            dificultad = data["dificultad"] as? String ?: "Medio",
                            ejercicios = (data["ejercicios"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            usuarioId = data["usuarioId"] as? String ?: "",
                            fechaCreacion = data["fechaCreacion"] as? Timestamp ?: Timestamp.now()
                        )
                    }
                }.sortedByDescending { it.fechaCreacion } // Ordenar localmente por fecha

                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Error al cargar rutinas: ${e.message}"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Rutinas") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("main") {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("crearRutina")
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Crear Rutina")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("crearRutina") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Rutina")
            }
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
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage ?: "Error desconocido",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        errorMessage = null
                        isLoading = true
                        scope.launch {
                            try {
                                val snapshot = db.collection("rutinas")
                                    .whereEqualTo("usuarioId", userId)
                                    .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                                    .get()
                                    .await()

                                rutinas = snapshot.documents.mapNotNull { doc ->
                                    doc.data?.let { data ->
                                        Rutina(
                                            id = doc.id,
                                            nombre = data["nombre"] as? String ?: "",
                                            descripcion = data["descripcion"] as? String ?: "",
                                            dificultad = data["dificultad"] as? String ?: "Medio",
                                            ejercicios = (data["ejercicios"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                            usuarioId = data["usuarioId"] as? String ?: "",
                                            fechaCreacion = data["fechaCreacion"] as? Timestamp ?: Timestamp.now()
                                        )
                                    }
                                }
                                isLoading = false
                            } catch (e: Exception) {
                                errorMessage = "Error al cargar rutinas: ${e.message}"
                                isLoading = false
                            }
                        }
                    }) {
                        Text("Reintentar")
                    }
                }
            } else if (rutinas.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = "No hay rutinas",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tienes rutinas creadas",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Crea tu primera rutina con el botón +",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        navController.navigate("crearRutina")
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Crear Rutina")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
// Dentro de LazyColumn en ListarMisRutinasPage:
                    items(rutinas) { rutina ->
                        RutinaCard(
                            rutina = rutina,
                            onRutinaClick = {
                                selectedRutina = rutina
                                showDetailDialog = true
                            },
                            onDeleteClick = {
                                rutinaToDelete = rutina
                                showDeleteDialog = true
                            },
                            onPlayClick = {
                                // Navegar a la pantalla de ejecución de rutina
                                navController.navigate("ejecutar_rutina/${rutina.id}")
                            },
                            onEditClick = {
                                // Navegar a la pantalla de edición con el ID de la rutina
                                navController.navigate("editar_rutina/${rutina.id}")
                            }
                        )
                    }
                }
            }
        }
    }

    // Diálogo para confirmar eliminación
    if (showDeleteDialog && rutinaToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                rutinaToDelete = null
            },
            title = { Text("Eliminar Rutina") },
            text = { Text("¿Estás seguro de que deseas eliminar la rutina '${rutinaToDelete?.nombre}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                db.collection("rutinas").document(rutinaToDelete!!.id).delete().await()
                                rutinas = rutinas.filter { it.id != rutinaToDelete!!.id }
                                showDeleteDialog = false
                                rutinaToDelete = null
                            } catch (e: Exception) {
                                errorMessage = "Error al eliminar: ${e.message}"
                                showDeleteDialog = false
                                rutinaToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    rutinaToDelete = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para mostrar detalles de la rutina con opción para cambiar dificultad
    if (showDetailDialog && selectedRutina != null) {
        // Lista de dificultades disponibles
        val dificultades = listOf("Fácil", "Medio", "Difícil")
        // Estado para controlar si se muestra o no el menú de dificultad
        var showDifficultyMenu by remember { mutableStateOf(false) }
        // Estado para la rutina actual que se está editando
        var currentRutina by remember { mutableStateOf(selectedRutina!!) }
        // Estado para mostrar indicador de guardado
        var isSaving by remember { mutableStateOf(false) }
        // Estado para mensajes de éxito
        var successMessage by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = {
            showDetailDialog = false
            selectedRutina = null
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentRutina.nombre,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            showDetailDialog = false
                            selectedRutina = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mostrar chip de dificultad con opción de cambio
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Dificultad: ",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            AssistChip(
                                onClick = { showDifficultyMenu = true },
                                label = { Text(currentRutina.dificultad) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when(currentRutina.dificultad) {
                                            "Fácil" -> Icons.Default.Star
                                            "Difícil" -> Icons.Default.StarRate
                                            else -> Icons.Default.StarHalf
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Cambiar dificultad",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }

                        // Menú desplegable para cambiar dificultad
                        DropdownMenu(
                            expanded = showDifficultyMenu,
                            onDismissRequest = { showDifficultyMenu = false },
                            modifier = Modifier
                                .width(200.dp)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            dificultades.forEach { dificultad ->
                                DropdownMenuItem(
                                    text = { Text(dificultad) },
                                    onClick = {
                                        // Solo actualizar si la dificultad cambió
                                        if (currentRutina.dificultad != dificultad) {
                                            isSaving = true
                                            scope.launch {
                                                try {
                                                    // Actualizar en Firestore
                                                    db.collection("rutinas")
                                                        .document(currentRutina.id)
                                                        .update("dificultad", dificultad)
                                                        .await()

                                                    // Actualizar la rutina actual
                                                    val updatedRutina = currentRutina.copy(dificultad = dificultad)
                                                    currentRutina = updatedRutina

                                                    // Actualizar la lista de rutinas
                                                    rutinas = rutinas.map {
                                                        if (it.id == updatedRutina.id) updatedRutina else it
                                                    }

                                                    successMessage = "Dificultad actualizada"
                                                    isSaving = false

                                                    // Auto-cerrar mensaje de éxito después de 2 segundos
                                                    scope.launch {
                                                        delay(2000)
                                                        successMessage = null
                                                    }
                                                } catch (e: Exception) {
                                                    errorMessage = "Error al actualizar: ${e.message}"
                                                    isSaving = false
                                                }
                                            }
                                        }
                                        showDifficultyMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when(dificultad) {
                                                "Fácil" -> Icons.Default.Star
                                                "Difícil" -> Icons.Default.StarRate
                                                else -> Icons.Default.StarHalf
                                            },
                                            contentDescription = null,
                                            tint = if (dificultad == currentRutina.dificultad)
                                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    trailingIcon = {
                                        if (dificultad == currentRutina.dificultad) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Seleccionado",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Mostrar indicador de guardado o mensaje de éxito
                    AnimatedVisibility(
                        visible = isSaving || successMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Guardando cambios...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else if (successMessage != null) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = successMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }

                    if (currentRutina.descripcion.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Descripción:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentRutina.descripcion,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Ejercicios (${currentRutina.ejercicios.size}):",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(currentRutina.ejercicios) { ejercicio ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = ejercicio,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                // Navegar a la pantalla de edición con el ID de la rutina
                                navController.navigate("editar_rutina/${currentRutina.id}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                        Spacer(modifier = Modifier.width(15.dp))
                        Button(
                            onClick = {
                                // Navegar a ejecutar_rutina con el ID de la rutina seleccionada
                                if (selectedRutina != null) {
                                    val rutinaId = currentRutina.id
                                    navController.navigate("ejecutar_rutina/$rutinaId")
                                    Log.d("ListarMisRutinasPage", "Ejecutar rutina: $rutinaId")

                                    // Limpiar después de la navegación
                                    showDetailDialog = false
                                    selectedRutina = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Iniciar Rutina")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RutinaCard(
    rutina: Rutina,
    onRutinaClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPlayClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onRutinaClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .height(140.dp) // Establece una altura fija para todas las tarjetas
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rutina.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Mostrar la cantidad de ejercicios y dificultad
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${rutina.ejercicios.size} ejercicios",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Indicador de dificultad
                        val dificultadColor = when(rutina.dificultad) {
                            "Fácil" -> Color(0xFF4CAF50)  // Verde
                            "Medio" -> Color(0xFFFFC107)  // Amarillo
                            "Difícil" -> Color(0xFFF44336)  // Rojo
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Surface(
                            modifier = Modifier
                                .padding(end = 4.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = dificultadColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = rutina.dificultad,
                                style = MaterialTheme.typography.bodySmall,
                                color = dificultadColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Fila de botones de acción
                Row {
                    // Botón de editar
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Rutina",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Botón de play para iniciar la rutina
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Iniciar Rutina",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Siempre mostrar el espacio para la descripción
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .height(40.dp) // Altura fija para la descripción
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (rutina.descripcion.isNotEmpty()) rutina.descripcion else "Sin descripción",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (rutina.descripcion.isNotEmpty())
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) // Color más claro para el placeholder
                )
            }

            // Espaciador para empujar el botón eliminar hacia abajo
            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eliminar")
                }
            }
        }
    }
}
