package com.example.gymrace

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearRutinaPage(
    navController: NavHostController,
    rutinaId: String? = null
) {
    // Inicializar Firebase Firestore
    val db = FirebaseFirestore.getInstance()
    // Obtener el ID del usuario actual
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    // Obtener el contexto de la aplicación
    val context = LocalContext.current
    // Inicializar la corutina
    val scope = rememberCoroutineScope()

    // Estado para determinar si estamos en modo edición
    var isEditMode by remember { mutableStateOf(rutinaId != null) }
    // Estado para manejar la carga de datos
    var isLoading by remember { mutableStateOf(true) }
    // Estados para mostrar mensajes de error y éxito
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    // Estados para mostrar diálogos de error y éxito
    var showErrorDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Estados para los campos del formulario
    var rutinaNombre by remember { mutableStateOf(TextFieldValue()) }
    var rutinaDescripcion by remember { mutableStateOf(TextFieldValue()) }
    var selectedDificultad by remember { mutableStateOf("Medio") }
    val dificultades = listOf("Fácil", "Medio", "Difícil")
    val ejerciciosSeleccionados = remember { mutableStateListOf<String>() }
    var compartirConAmigos by remember { mutableStateOf(false) }

    // Estado para la barra de búsqueda
    var buscarEjercicio by remember { mutableStateOf(TextFieldValue()) }
    // Estado para mostrar el diálogo de ejercicios
    var showEjerciciosDialog by remember { mutableStateOf(false) }
    // Estado para la categoría seleccionada
    var selectedCategory by remember { mutableStateOf("Todos") }
    // Estado para mostrar los detalles del ejercicio
    var selectedExerciseDetail by remember { mutableStateOf<GifData?>(null) }

    // Cargar ejercicios desde XML
    val (ejerciciosData, _) = remember {
        loadGifsFromXml(context.resources.getXml(R.xml.ejercicios), context)
    }

    // Lista de categorías
    val categories = listOf("Todos") + ejerciciosData.map { it.category }.distinct()

    // Filtrar ejercicios según búsqueda y categoría
    val ejerciciosFiltrados = ejerciciosData.filter {
        (selectedCategory == "Todos" || it.category == selectedCategory) &&
                it.title.contains(buscarEjercicio.text, ignoreCase = true)
    }.map { it.title }

    // Efecto para cargar la rutina si estamos en modo edición
    LaunchedEffect(rutinaId) {
        if (rutinaId != null) {
            isLoading = true
            try {
                // Obtener la rutina de Firestore
                val document = db.collection("rutinas").document(rutinaId).get().await()
                if (document.exists()) {
                    val data = document.data
                    if (data != null) {
                        // Actualizar estados con los datos de la rutina
                        rutinaNombre = TextFieldValue(data["nombre"] as? String ?: "")
                        rutinaDescripcion = TextFieldValue(data["descripcion"] as? String ?: "")
                        selectedDificultad = data["dificultad"] as? String ?: "Medio"
                        compartirConAmigos = data["compartirConAmigos"] as? Boolean ?: false

                        // Cargar ejercicios seleccionados
                        val ejercicios = (data["ejercicios"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        ejerciciosSeleccionados.clear()
                        ejerciciosSeleccionados.addAll(ejercicios)

                        // Verificar que la rutina pertenece al usuario actual
                        val rutinaUserId = data["usuarioId"] as? String ?: ""
                        if (rutinaUserId != userId) {
                            errorMessage = "No tienes permiso para editar esta rutina"
                            showErrorDialog = true
                            isLoading = false
                            return@LaunchedEffect
                        }
                    }
                } else {
                    // La rutina no existe
                    errorMessage = "La rutina no existe"
                    Log.e("Error", "La rutina no existe")
                    showErrorDialog = true
                }
                isLoading = false
            } catch (e: Exception) {
                // Manejar errores al cargar la rutina
                errorMessage = "Error al cargar la rutina: ${e.message}"
                Log.e("Error", "Error al cargar la rutina: ${e.message}")
                showErrorDialog = true
                isLoading = false
            }
        } else {
            // No estamos en modo edición, solo desactivamos el indicador de carga
            isLoading = false
        }
    }
    // Pantalla principal
    Scaffold(
        topBar = {
            TopAppBar(
                // Título de la barra superior
                title = { Text(if (isEditMode) "Editar Rutina" else "Crear Nueva Rutina") },
                navigationIcon = {
                    IconButton(onClick = {
                        val previousBackStackEntry = navController.previousBackStackEntry
                        if (previousBackStackEntry != null && previousBackStackEntry.destination.route != "main") {
                            navController.popBackStack()
                        } else {
                            navController.navigate("main") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
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
                // Pantalla de carga
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isEditMode) "Cargando rutina..." else "Preparando...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // Contenido principal
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    // Título de la página
                    OutlinedTextField(
                        value = rutinaNombre,
                        onValueChange = { rutinaNombre = it },
                        label = { Text("Nombre de la Rutina*") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = rutinaNombre.text.isBlank() && errorMessage.isNotEmpty()
                    )
                    // Mensaje de error si el nombre está vacío
                    if (rutinaNombre.text.isBlank() && errorMessage.isNotEmpty()) {
                        Text(
                            text = "El nombre es obligatorio",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Descripción de la rutina
                    OutlinedTextField(
                        value = rutinaDescripcion,
                        onValueChange = { rutinaDescripcion = it },
                        label = { Text("Descripción (Opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Filtro de dificultad con chips
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Dificultad",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                dificultades.forEach { dificultad ->
                                    FilterChip(
                                        selected = dificultad == selectedDificultad,
                                        onClick = { selectedDificultad = dificultad },
                                        label = {
                                            Text(
                                                dificultad,
                                                color = if (dificultad == selectedDificultad)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    LocalContentColor.current
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Checkbox para compartir con amigos
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = compartirConAmigos,
                                onCheckedChange = { compartirConAmigos = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Visible para amigos",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Título de la sección de ejercicios
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Ejercicios",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = { showEjerciciosDialog = true },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir ejercicio")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Añadir")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Lista de ejercicios seleccionados
                    if (ejerciciosSeleccionados.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No hay ejercicios seleccionados",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(ejerciciosSeleccionados) { ejercicio ->
                                ExerciseCard(
                                    ejercicio = ejercicio,
                                    ejerciciosData = ejerciciosData,
                                    ejerciciosSeleccionados = ejerciciosSeleccionados,
                                    onShowExerciseDetail = { nombreEjercicio ->
                                        showExerciseDetail(nombreEjercicio, ejerciciosData) {
                                            selectedExerciseDetail = it
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón para guardar o actualizar
                    Button(
                        onClick = {
                            // Validar campos antes de guardar
                            if (rutinaNombre.text.isBlank()) {
                                errorMessage = "Por favor, introduzca un nombre para la rutina"
                                showErrorDialog = true
                                return@Button
                            }

                            if (ejerciciosSeleccionados.isEmpty()) {
                                errorMessage = "Por favor, añada al menos un ejercicio"
                                showErrorDialog = true
                                return@Button
                            }

                            isLoading = true

                            val rutina = hashMapOf(
                                "nombre" to rutinaNombre.text,
                                "descripcion" to rutinaDescripcion.text,
                                "dificultad" to selectedDificultad,
                                "ejercicios" to ejerciciosSeleccionados.toList(),
                                "usuarioId" to userId,
                                "compartirConAmigos" to compartirConAmigos
                            )

                            // Si estamos en modo edición, actualizamos la fecha de modificación
                            // Si no, establecemos la fecha de creación
                            if (isEditMode) {
                                rutina["fechaModificacion"] = Timestamp.now()
                            } else {
                                rutina["fechaCreacion"] = Timestamp.now()
                            }

                            scope.launch {
                                try {
                                    if (isEditMode && rutinaId != null) {
                                        // Actualizar rutina existente
                                        db.collection("rutinas").document(rutinaId)
                                            .update(rutina as Map<String, Any>)
                                            .await()

                                        isLoading = false
                                        successMessage = "Rutina actualizada correctamente"
                                        showSuccessDialog = true
                                    } else {
                                        // Crear nueva rutina
                                        db.collection("rutinas").add(rutina)
                                            .await()

                                        isLoading = false
                                        successMessage = "Rutina creada correctamente"
                                        showSuccessDialog = true
                                    }
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = "Error: ${e.message}"
                                    showErrorDialog = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            // Mostrar un indicador de carga en el botón
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isEditMode) "Actualizar Rutina" else "Guardar Rutina")
                    }
                }
            }
        }
    }

    // Dialog para añadir ejercicios
    if (showEjerciciosDialog) {
        Dialog(
            onDismissRequest = { showEjerciciosDialog = false }
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Título y botón para cerrar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Añadir Ejercicios",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = { showEjerciciosDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Barra de búsqueda
                    OutlinedTextField(
                        value = buscarEjercicio,
                        onValueChange = { buscarEjercicio = it },
                        label = { Text("Buscar ejercicio") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filtro por categoría con chips horizontales
                    Text(
                        "Categorías:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Fila de chips para categorías
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp)
                    ) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = category == selectedCategory,
                                onClick = { selectedCategory = category },
                                label = { Text(category) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Lista de ejercicios filtrados con miniaturas
                    if (ejerciciosFiltrados.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No se encontraron ejercicios",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            items(ejerciciosFiltrados.size) { index ->
                                val ejercicioNombre = ejerciciosFiltrados[index]
                                val ejercicio = ejerciciosData.find { it.title == ejercicioNombre }

                                if (ejercicio != null) {
                                    ExercisePreviewItem(
                                        gifData = ejercicio,
                                        isSelected = ejerciciosSeleccionados.contains(ejercicioNombre),
                                        onToggleSelect = { selected ->
                                            if (selected) {
                                                if (!ejerciciosSeleccionados.contains(ejercicioNombre)) {
                                                    ejerciciosSeleccionados.add(ejercicioNombre)
                                                }
                                            } else {
                                                ejerciciosSeleccionados.remove(ejercicioNombre)
                                            }
                                        },
                                        onViewDetail = {
                                            showExerciseDetail(ejercicioNombre, ejerciciosData) {
                                                selectedExerciseDetail = it
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones de acción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            showEjerciciosDialog = false
                            buscarEjercicio = TextFieldValue("")
                            selectedCategory = "Todos"
                        }) {
                            Text("Cancelar")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                showEjerciciosDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Confirmar (${ejerciciosSeleccionados.size})")
                        }
                    }
                }
            }
        }
    }

    // Dialog de error
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = { showErrorDialog = false }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Dialog de éxito
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                if (isEditMode) {
                    navController.popBackStack()
                } else {
                    navController.navigate("main") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            },
            title = { Text("Éxito") },
            text = { Text(successMessage) },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    if (isEditMode) {
                        navController.popBackStack()
                    } else {
                        navController.navigate("main") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Dialog para detalles del ejercicio
    if (selectedExerciseDetail != null) {
        ExerciseDetailDialog(
            gifData = selectedExerciseDetail!!,
            onDismiss = { selectedExerciseDetail = null }
        )
    }
}

// Función para buscar y mostrar detalles de un ejercicio
fun showExerciseDetail(
    ejercicioNombre: String,
    ejercicios: List<GifData>,
    setSelectedExerciseDetail: (GifData?) -> Unit
) {
    // Buscar el ejercicio en la lista de ejercicios
    val ejercicio = ejercicios.find { it.title == ejercicioNombre }
    // Si se encuentra el ejercicio, mostrar sus detalles
    if (ejercicio != null) {
        setSelectedExerciseDetail(ejercicio)
    }
}

// Componente para mostrar la tarjeta de un ejercicio seleccionado con el texto truncado
@Composable
fun ExerciseCard(
    ejercicio: String,
    ejerciciosData: List<GifData>,
    ejerciciosSeleccionados: MutableList<String>,
    onShowExerciseDetail: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onShowExerciseDetail(ejercicio) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // El texto se trunca a una sola línea con puntos suspensivos
            Text(
                text = ejercicio,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row {
                // Botones para ver detalles y eliminar el ejercicio
                IconButton(onClick = { onShowExerciseDetail(ejercicio) }) {
                    Icon(
                        imageVector = Icons.Default.RemoveRedEye,
                        contentDescription = "Ver detalles",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { ejerciciosSeleccionados.remove(ejercicio) }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Componente para mostrar la vista previa de un ejercicio en el diálogo de búsqueda
@Composable
fun ExercisePreviewItem(
    gifData: GifData,
    isSelected: Boolean,
    onToggleSelect: (Boolean) -> Unit,
    onViewDetail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onViewDetail() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniatura del GIF (asegúrate de tener implementado GifImage)
            GifImage(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                gif = gifData.resource
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Título del ejercicio
                Text(
                    text = gifData.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                // Descripción del ejercicio
                Text(
                    text = "Músculo: ${gifData.mainMuscle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = onToggleSelect
            )
        }
    }
}