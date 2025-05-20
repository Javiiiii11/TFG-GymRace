package com.example.gymrace

import android.util.Log
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import np.com.bimalkafle.bottomnavigationdemo.pages.User

// Definimos la clase Usuario con un nuevo campo para el objetivo fitness
data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val fotoPerfil: String = "",
    val email: String = "",
    val objetivoFitness: String = "Mantener forma física" // Añadido campo de objetivo fitness
)

// Ampliamos la definición de Rutina para incluir la opción de compartir
data class RutinaAmigo(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val dificultad: String = "Medio",
    val ejercicios: List<String> = emptyList(),
    val usuarioId: String = "",
    val nombreUsuario: String = "",
    val compartirConAmigos: Boolean = false,
    val fechaCreacion: Timestamp = Timestamp.now()
)

// Nueva clase para agrupar rutinas por usuario
data class RutinasPorUsuario(
    val usuario: Usuario,
    val rutinas: List<RutinaAmigo>
)

// Componente principal de la página
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListarRutinasAmigosPage(navController: NavHostController) {
    // Inicializamos Firebase Firestore y Firebase Auth
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()
    // Estados para manejar la carga de rutinas y errores
    var rutinasPorUsuario by remember { mutableStateOf<List<RutinasPorUsuario>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedRutina by remember { mutableStateOf<RutinaAmigo?>(null) }
    var isCopying by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage2 by remember { mutableStateOf<String?>(null) }

    // Estados para el diálogo de comunidad y usuarios
    var showCommunityDialog by remember { mutableStateOf(false) }
    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoadingUsers by remember { mutableStateOf(false) }
    var friendsList by remember { mutableStateOf<List<User>>(emptyList()) }
    var showFriendsListDialog by remember { mutableStateOf(false) }

    // Función suspendida para cargar las rutinas de amigos.
    suspend fun cargarRutinasAmigos() {
        isLoading = true
        try {
            // Obtener la lista de IDs de amigos del documento "amigos/{userId}"
            val amigosDoc = db.collection("amigos")
                .document(userId)
                .get()
                .await()
            val amigosIds = if (amigosDoc.exists() && amigosDoc.get("listaAmigos") != null) {
                amigosDoc.get("listaAmigos") as? List<String> ?: emptyList()
            } else {
                emptyList()
            }

            if (amigosIds.isEmpty()) {
                rutinasPorUsuario = emptyList()
                isLoading = false
                return
            }

            // Obtener información de los amigos (usuarios)
            val usuariosMap = mutableMapOf<String, Usuario>()
            val batchSize = 10
            for (i in amigosIds.indices step batchSize) {
                val batch = amigosIds.subList(i, minOf(i + batchSize, amigosIds.size))
                val usuariosSnapshot = db.collection("usuarios")
                    .whereIn(FieldPath.documentId(), batch)
                    .get()
                    .await()
                usuariosSnapshot.documents.forEach { doc ->
                    val usuario = Usuario(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        fotoPerfil = doc.getString("fotoPerfil") ?: "",
                        email = doc.getString("email") ?: "",
                        objetivoFitness = doc.getString("objetivoFitness") ?: "Mantener forma física"
                    )
                    usuariosMap[usuario.id] = usuario
                }
            }

            // Obtener rutinas compartidas por los amigos
            val rutinasResult = mutableMapOf<String, MutableList<RutinaAmigo>>()
            for (i in amigosIds.indices step batchSize) {
                val batch = amigosIds.subList(i, minOf(i + batchSize, amigosIds.size))
                val rutinasSnapshot = db.collection("rutinas")
                    .whereIn("usuarioId", batch)
                    .whereEqualTo("compartirConAmigos", true)
                    .get()
                    .await()
                rutinasSnapshot.documents.forEach { doc ->
                    val data = doc.data
                    if (data != null) {
                        val usuarioId = data["usuarioId"] as? String ?: ""
                        val usuario = usuariosMap[usuarioId]
                        if (usuario != null) {
                            val rutina = RutinaAmigo(
                                id = doc.id,
                                nombre = data["nombre"] as? String ?: "",
                                descripcion = data["descripcion"] as? String ?: "",
                                dificultad = data["dificultad"] as? String ?: "Medio",
                                ejercicios = (data["ejercicios"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                usuarioId = usuarioId,
                                nombreUsuario = usuario.nombre,
                                compartirConAmigos = data["compartirConAmigos"] as? Boolean ?: false,
                                fechaCreacion = data["fechaCreacion"] as? Timestamp ?: Timestamp.now()
                            )
                            if (!rutinasResult.containsKey(usuarioId)) {
                                rutinasResult[usuarioId] = mutableListOf()
                            }
                            rutinasResult[usuarioId]?.add(rutina)
                        }
                    }
                }
            }

            // Agrupar y ordenar las rutinas por usuario
            val resultList = mutableListOf<RutinasPorUsuario>()
            for ((usuarioId, rutinas) in rutinasResult) {
                val usuario = usuariosMap[usuarioId]
                if (usuario != null && rutinas.isNotEmpty()) {
                    val rutinasOrdenadas = rutinas.sortedByDescending { it.fechaCreacion }
                    resultList.add(RutinasPorUsuario(usuario, rutinasOrdenadas))
                }
            }
            rutinasPorUsuario = resultList.sortedBy { it.usuario.nombre }
            isLoading = false

        } catch (e: Exception) {
            Log.e("ListarRutinasAmigosPage", "Error al cargar rutinas: ${e.message}")
            errorMessage = "Error al cargar rutinas: ${e.message}"
            isLoading = false
        }
    }

    // Al abrir el diálogo de comunidad, cargar la lista de usuarios y amigos
    LaunchedEffect(showCommunityDialog) {
        if (showCommunityDialog) {
            isLoadingUsers = true
            loadAllUsers { users ->
                allUsers = users.filter { it.id != userId }
                isLoadingUsers = false
            }
            loadFriendsList(userId) { friends ->
                friendsList = friends
            }
        }
    }

    // Cargar lista de amigos cuando se abre el diálogo de amigos
    LaunchedEffect(showFriendsListDialog) {
        if (showFriendsListDialog) {
            loadFriendsList(userId) { friends ->
                friendsList = friends
            }
        }
    }

    // Cargar rutinas al iniciar
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            cargarRutinasAmigos()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rutinas de Amigos") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("main") {
                            popUpTo(0) { inclusive = true }
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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
                                cargarRutinasAmigos()
                            } catch (e: Exception) {
                                errorMessage = "Error al cargar rutinas: ${e.message}"
                            }
                        }
                    }) {
                        Text("Reintentar")
                    }
                }
            } else if (rutinasPorUsuario.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "No hay rutinas",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay rutinas compartidas",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tus amigos aún no han compartido rutinas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    rutinasPorUsuario.forEach { grupo ->
                        item { AmigoCabecera(usuario = grupo.usuario) }
                        items(grupo.rutinas) { rutina ->
                            RutinaAmigoCard(rutina = rutina, onRutinaClick = {
                                selectedRutina = rutina
                                showDetailDialog = true
                            })
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    // Diálogo para mostrar detalles de la rutina con opción para copiarla.
    if (showDetailDialog && selectedRutina != null) {
        val rutinaLocal = selectedRutina
        Dialog(
            onDismissRequest = {
                selectedRutina = null
                showDetailDialog = false
            },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnClickOutside = true,
                dismissOnBackPress = true
            )
        ) {
            rutinaLocal?.let { rutina ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        // Cabecera
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = rutina.nombre,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 48.dp)
                            )
                            IconButton(
                                modifier = Modifier.align(Alignment.TopEnd),
                                onClick = {
                                    selectedRutina = null
                                    showDetailDialog = false
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar")
                            }
                        }

                        // Creador
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rutina.nombreUsuario.firstOrNull()?.toString() ?: "?",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Creado por ${rutina.nombreUsuario}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AssistChip(
                            onClick = { },
                            label = { Text(rutina.dificultad) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (rutina.dificultad) {
                                        "Fácil" -> Icons.Default.Star
                                        "Difícil" -> Icons.Default.StarRate
                                        else -> Icons.Default.StarHalf
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )

                        // Mensajes de estado
                        AnimatedVisibility(
                            visible = isCopying || successMessage != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isCopying) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Copiando rutina a tu colección...",
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

                        // Mensaje de error
                        AnimatedVisibility(
                            visible = errorMessage2 != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage2 ?: "Error desconocido",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        LaunchedEffect(errorMessage2) {
                            if (errorMessage2 != null) {
                                delay(2000)
                                errorMessage2 = null
                            }
                        }

                        // Descripción y ejercicios
                        if (rutina.descripcion.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Descripción:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = rutina.descripcion,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ejercicios (${rutina.ejercicios.size}):",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            items(rutina.ejercicios) { ejercicio ->
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

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    navController.navigate("ejecutar_rutina/${rutina.id}")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Iniciar Rutina")
                            }

                            Button(
                                onClick = {
                                    isCopying = true
                                    scope.launch {
                                        try {
                                            val querySnapshot = db.collection("rutinas")
                                                .whereEqualTo("usuarioId", userId)
                                                .whereEqualTo("nombre", rutina.nombre)
                                                .get()
                                                .await()

                                            if (!querySnapshot.isEmpty) {
                                                isCopying = false
                                                errorMessage2 = "Ya tienes una rutina con este nombre"
                                                delay(2000)
                                                if (showDetailDialog) errorMessage2 = null
                                                return@launch
                                            }

                                            val nuevaRutina = hashMapOf(
                                                "nombre" to rutina.nombre,
                                                "descripcion" to "${rutina.descripcion}\n\nCopiada de: ${rutina.nombreUsuario}",
                                                "dificultad" to rutina.dificultad,
                                                "ejercicios" to rutina.ejercicios,
                                                "usuarioId" to userId,
                                                "compartirConAmigos" to false,
                                                "fechaCreacion" to Timestamp.now()
                                            )

                                            db.collection("rutinas").add(nuevaRutina).await()
                                            isCopying = false
                                            successMessage = "¡Rutina guardada en tu colección!"
                                            delay(2000)
                                            if (showDetailDialog) successMessage = null
                                        } catch (e: Exception) {
                                            isCopying = false
                                            errorMessage = "Error al copiar rutina: ${e.message}"
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Guardar en Mis Rutinas")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Función para cargar todos los usuarios de la base de datos
fun loadAllUsers(callback: (List<User>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("usuarios")
        .get()
        .addOnSuccessListener { snapshot ->
            val usersList = snapshot.documents.map { doc ->
                User(
                    id = doc.id,
                    nombre = doc.getString("nombre") ?: "",
                    objetivoFitness = doc.getString("objetivoFitness") ?: "",
                    peso = doc.getString("peso") ?: "",
                    altura = doc.getString("altura") ?: "",
                    edad = doc.getString("edad") ?: "",
                    nivelExperiencia = doc.getString("nivelExperiencia") ?: "",
                    cuentaPrivada = doc.getBoolean("cuentaPrivada") ?: false,
                )
            }
            callback(usersList)
        }
        .addOnFailureListener { e ->
            Log.e("loadAllUsers", "Error: ${e.message}")
            callback(emptyList())
        }
}

// Función para cargar la lista de amigos de un usuario
fun loadFriendsList(userId: String, callback: (List<User>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("amigos")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists() && document.get("listaAmigos") != null) {
                val friendIds = document.get("listaAmigos") as? List<String> ?: emptyList()
                if (friendIds.isEmpty()) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }
                val friendsList = mutableListOf<User>()
                var loadedCount = 0
                friendIds.forEach { friendId ->
                    db.collection("usuarios")
                        .document(friendId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                friendsList.add(
                                    User(
                                        id = userDoc.id,
                                        nombre = userDoc.getString("nombre") ?: "",
                                        objetivoFitness = userDoc.getString("objetivoFitness") ?: "",
                                        peso = userDoc.getString("peso") ?: "",
                                        altura = userDoc.getString("altura") ?: "",
                                        edad = userDoc.getString("edad") ?: "",
                                        nivelExperiencia = userDoc.getString("nivelExperiencia") ?: "",
                                        cuentaPrivada = userDoc.getBoolean("cuentaPrivada") ?: false,
                                    )
                                )
                            }
                            loadedCount++
                            if (loadedCount >= friendIds.size) {
                                callback(friendsList)
                            }
                        }
                        .addOnFailureListener {
                            loadedCount++
                            if (loadedCount >= friendIds.size) {
                                callback(friendsList)
                            }
                        }
                }
            } else {
                callback(emptyList())
            }
        }
        .addOnFailureListener { e ->
            Log.e("loadFriendsList", "Error: ${e.message}")
            callback(emptyList())
        }
}

// Componente para mostrar la cabecera del amigo
@Composable
fun AmigoCabecera(usuario: Usuario) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = usuario.nombre.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = usuario.nombre,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Añadir objetivos fitness debajo del nombre
        if (usuario.objetivoFitness.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(start = 52.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Objetivo fitness",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = usuario.objetivoFitness,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

// Componente para mostrar la tarjeta de rutina de un amigo
@Composable
fun RutinaAmigoCard(
    rutina: RutinaAmigo,
    onRutinaClick: () -> Unit
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
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rutina.nombreUsuario.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)  // Asigna peso para controlar el espacio
                ) {
                    Text(
                        text = rutina.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "por ${rutina.nombreUsuario}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(
                    onClick = { },
                    label = { Text(rutina.dificultad) },
                    leadingIcon = {
                        Icon(
                            imageVector = when(rutina.dificultad) {
                                "Fácil" -> Icons.Default.Star
                                "Difícil" -> Icons.Default.StarRate
                                else -> Icons.Default.StarHalf
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            if (rutina.descripcion.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = rutina.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}