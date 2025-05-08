package com.example.gymrace

import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import np.com.bimalkafle.bottomnavigationdemo.pages.User
import np.com.bimalkafle.bottomnavigationdemo.pages.UsersDialog

// Función para enviar solicitud de amistad si la cuenta es privada
fun sendFriendRequestNotification(toUserId: String, fromUserName: String, fromUserId: String) {
    val db = FirebaseFirestore.getInstance()
    val notificationData = mapOf(
        "tipo" to "solicitud",
        "mensaje" to "$fromUserName quiere ser tu amigo",
        "remitente" to fromUserId,
        "timestamp" to FieldValue.serverTimestamp()
    )
    db.collection("notificaciones")
        .document(toUserId)
        .collection("items")
        .add(notificationData)
        .addOnSuccessListener { Log.d("ListarRutinasAmigosPage", "Notificación enviada a $toUserId") }
        .addOnFailureListener { Log.e("ListarRutinasAmigosPage", "Error al enviar notificación", it) }
}

// Definimos la clase Usuario con campo objetivo fitness
data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val fotoPerfil: String = "",
    val email: String = "",
    val objetivoFitness: String = "Mantener forma física"
)

// Rutina compartida por amigo
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

data class RutinasPorUsuario(
    val usuario: Usuario,
    val rutinas: List<RutinaAmigo>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListarRutinasAmigosPage(navController: NavHostController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()

    var rutinasPorUsuario by remember { mutableStateOf<List<RutinasPorUsuario>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) } // Para carga inicial
    var isBackgroundLoading by remember { mutableStateOf(false) } // Para actualizaciones en segundo plano
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    var showCommunityDialog by remember { mutableStateOf(false) }
    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoadingUsers by remember { mutableStateOf(false) }
    var friendsList by remember { mutableStateOf<List<User>>(emptyList()) }
    var showFriendsListDialog by remember { mutableStateOf(false) }

    // Carga rutinas de amigos suspendida
    suspend fun cargarRutinasAmigos(initialLoad: Boolean = true) {
        // Solo activamos isLoading en la carga inicial
        if (initialLoad) {
            isLoading = true
        } else {
            isBackgroundLoading = true
        }

        try {
            val amigosDoc = db.collection("amigos").document(userId).get().await()
            val amigosIds = (amigosDoc.get("listaAmigos") as? List<String>) ?: emptyList()

            // Si no hay amigos, limpiamos la lista y terminamos
            if (amigosIds.isEmpty()) {
                rutinasPorUsuario = emptyList()
                isLoading = false
                isBackgroundLoading = false
                return
            }

            val usuariosMap = mutableMapOf<String, Usuario>()
            val batchSize = 10
            for (i in amigosIds.indices step batchSize) {
                val batch = amigosIds.subList(i, minOf(i + batchSize, amigosIds.size))
                val usuariosSnapshot = db.collection("usuarios")
                    .whereIn(FieldPath.documentId(), batch)
                    .get().await()
                usuariosSnapshot.documents.forEach { doc ->
                    usuariosMap[doc.id] = Usuario(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        fotoPerfil = doc.getString("fotoPerfil") ?: "",
                        email = doc.getString("email") ?: "",
                        objetivoFitness = doc.getString("objetivoFitness") ?: "Mantener forma física"
                    )
                }
            }

            val rutinasResult = mutableMapOf<String, MutableList<RutinaAmigo>>()
            for (i in amigosIds.indices step batchSize) {
                val batch = amigosIds.subList(i, minOf(i + batchSize, amigosIds.size))
                val rutinasSnapshot = db.collection("rutinas")
                    .whereIn("usuarioId", batch)
                    .whereEqualTo("compartirConAmigos", true)
                    .get().await()
                rutinasSnapshot.documents.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val uid = data["usuarioId"] as? String ?: return@forEach
                    usuariosMap[uid]?.let { usuario ->
                        val rutina = RutinaAmigo(
                            id = doc.id,
                            nombre = data["nombre"] as? String ?: "",
                            descripcion = data["descripcion"] as? String ?: "",
                            dificultad = data["dificultad"] as? String ?: "Medio",
                            ejercicios = (data["ejercicios"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            usuarioId = uid,
                            nombreUsuario = usuario.nombre,
                            compartirConAmigos = data["compartirConAmigos"] as? Boolean ?: false,
                            fechaCreacion = data["fechaCreacion"] as? Timestamp ?: Timestamp.now()
                        )
                        rutinasResult.getOrPut(uid) { mutableListOf() }.add(rutina)
                    }
                }
            }

            // Preparar el resultado
            val nuevoResultado = rutinasResult.mapNotNull { (uid, rutinas) ->
                usuariosMap[uid]?.let { user -> RutinasPorUsuario(user, rutinas.sortedByDescending { it.fechaCreacion }) }
            }.sortedBy { it.usuario.nombre }

            // Actualizar solo si hay cambios o es la carga inicial
            rutinasPorUsuario = nuevoResultado

        } catch (e: Exception) {
            Log.e("ListarRutinasAmigosPage", "Error: ${e.message}")
            // Solo mostrar error en la carga inicial
            if (initialLoad) {
                errorMessage = "Error al cargar rutinas: ${e.message}"
            }
        } finally {
            isLoading = false
            isBackgroundLoading = false
        }
    }

    // Efectos para diálogos
    LaunchedEffect(showCommunityDialog) {
        if (showCommunityDialog) {
            isLoadingUsers = true
            loadAllUsers { allUsers = it.filter { u -> u.id != userId }; isLoadingUsers = false }
            loadFriendsList(userId) { friendsList = it }
        }
    }
    LaunchedEffect(showFriendsListDialog) {
        if (showFriendsListDialog) loadFriendsList(userId) { friendsList = it }
    }
    LaunchedEffect(userId) { if (userId.isNotEmpty()) scope.launch { cargarRutinasAmigos(initialLoad = true) } }

    // Refresco periódico de usuarios, amigos y rutinas cada 5 segundos
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(5000)
                // Verificar que no haya una actualización en curso antes de iniciar otra
                if (!isBackgroundLoading) {
                    // Si el diálogo de comunidad está abierto, recargar usuarios y amigos
                    if (showCommunityDialog) {
                        loadAllUsers { allUsers = it.filter { u -> u.id != userId } }
                        loadFriendsList(userId) { friendsList = it }
                    }
                    // Siempre recargar amigos y rutinas en segundo plano
                    loadFriendsList(userId) { friendsList = it }
                    scope.launch { cargarRutinasAmigos(initialLoad = false) }
                    Log.d("ListarRutinasAmigosPage", "Refresco periódico ejecutado")
                } else {
                    Log.d("ListarRutinasAmigosPage", "Omitiendo refresco, hay una carga en progreso")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rutinas de Amigos") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("main") { popUpTo(0) { inclusive = true } } }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showFriendsListDialog = true }) {
                        Icon(Icons.Default.People, contentDescription = "Ver Amigos")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCommunityDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Buscar Comunidad")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "Error",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                rutinasPorUsuario.isEmpty() -> {
                    Text(
                        text = "No hay rutinas compartidas",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    rutinasPorUsuario.forEach { group ->
                        item { AmigoCabecera(group.usuario) }
                        items(group.rutinas) { RutinaAmigoCard(rutina = it) {
                            navController.navigate("rutinaDetalle/${it.id}")
                        } }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // La actualización en segundo plano ocurre sin indicador visual
        }
    }

    // Diálogo de comunidad
    if (showCommunityDialog) {
        UsersDialog(
            title = "Comunidad",
            users = allUsers,
            isLoading = isLoadingUsers,
            onDismiss = { showCommunityDialog = false },
            showAddButton = true,
            currentFriends = friendsList.map { it.id },
            onUserAction = { selectedId ->
                if (friendsList.any { it.id == selectedId }) {
                    scope.launch {
                        removeFriend(userId, selectedId) {
                            loadFriendsList(userId) { friendsList = it; successMessage = "Usuario eliminado de amigos"; scope.launch { cargarRutinasAmigos(initialLoad = false) } }
                        }
                    }
                } else {
                    db.collection("usuarios").document(selectedId).get()
                        .addOnSuccessListener { doc ->
                            val private = doc.getBoolean("cuentaPrivada") ?: true
                            if (private) {
                                sendFriendRequestNotification(selectedId, allUsers.find { u -> u.id == userId }?.nombre ?: "", userId)
                                Toast.makeText(context, "Solicitud enviada", Toast.LENGTH_SHORT).show()
                            } else {
                                scope.launch {
                                    addFriend(userId, selectedId) {
                                        loadFriendsList(userId) { friendsList = it; successMessage = "Usuario agregado a amigos"; scope.launch { cargarRutinasAmigos(initialLoad = false) } }
                                    }
                                }
                                Toast.makeText(context, "Amigo añadido", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { Toast.makeText(context, "Error comprobando privacidad", Toast.LENGTH_SHORT).show() }
                }
            }
        )
    }

    // Diálogo de amigos
    if (showFriendsListDialog) {
        UsersDialog(
            title = "Mis Amigos",
            users = friendsList,
            isLoading = isLoadingUsers,
            onDismiss = { showFriendsListDialog = false },
            onUserAction = { friendId ->
                scope.launch {
                    removeFriend(userId, friendId) { loadFriendsList(userId) { friendsList = it } }
                }
            },
            showAddButton = false
        )
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

// Funciones para agregar amigos
fun addFriend(userId: String, friendId: String, callback: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("amigos")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val currentFriends = document.get("listaAmigos") as? List<String> ?: emptyList()
                if (!currentFriends.contains(friendId)) {
                    val updatedFriends = currentFriends + friendId
                    db.collection("amigos")
                        .document(userId)
                        .update("listaAmigos", updatedFriends)
                        .addOnSuccessListener { callback() }
                        .addOnFailureListener { callback() }
                } else {
                    callback()
                }
            } else {
                // Crear un nuevo documento si no existe
                db.collection("amigos")
                    .document(userId)
                    .set(mapOf("listaAmigos" to listOf(friendId)))
                    .addOnSuccessListener { callback() }
                    .addOnFailureListener { callback() }
            }
        }
        .addOnFailureListener { callback() }
}

// Función para eliminar amigos
fun removeFriend(userId: String, friendId: String, callback: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("amigos")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val currentFriends = document.get("listaAmigos") as? List<String> ?: emptyList()
                val updatedFriends = currentFriends.filter { it != friendId }
                db.collection("amigos")
                    .document(userId)
                    .update("listaAmigos", updatedFriends)
                    .addOnSuccessListener { callback() }
                    .addOnFailureListener { callback() }
            } else {
                callback()
            }
        }
        .addOnFailureListener { callback() }
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
                Spacer(modifier = Modifier.width(8.dp))  // Espacio fijo entre el texto y el chip
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