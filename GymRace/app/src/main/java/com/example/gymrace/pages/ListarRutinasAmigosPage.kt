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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import np.com.bimalkafle.bottomnavigationdemo.pages.User
import np.com.bimalkafle.bottomnavigationdemo.pages.UsersDialog

data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val fotoPerfil: String = "",
    val email: String = "",
    val objetivoFitness: String = "Mantener forma física" // Añadido campo de objetivo fitness
)

data class Amigo(
    val id: String = "",
    val usuarioId: String = "",
    val amigoId: String = "",
    val fechaAgregado: Timestamp = Timestamp.now()
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

// ----- Funciones Auxiliares para la Comunidad -----
// Estas funciones utilizan Firebase y deben contar con la configuración de tu proyecto.
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
                    nivelExperiencia = doc.getString("nivelExperiencia") ?: ""
                )
            }
            callback(usersList)
        }
        .addOnFailureListener { e ->
            Log.e("loadAllUsers", "Error: ${e.message}")
            callback(emptyList())
        }
}

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
                                        nivelExperiencia = userDoc.getString("nivelExperiencia") ?: ""
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

// ----- Diálogo de Usuarios (Comunidad) -----
// Se reutiliza la lógica de tu otro archivo (UserPage.kt).
@Composable
fun UsersDialog(
    title: String,
    users: List<User>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onUserAction: (String) -> Unit,
    showAddButton: Boolean,
    currentFriends: List<String> = emptyList()
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$title (${users.size})",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                Divider()
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (users.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (showAddButton)
                                "No hay usuarios disponibles"
                            else "No tienes amigos aún",
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        items(users) { user ->
                            UserListItem(
                                user = user,
                                onAction = { onUserAction(user.id) },
                                showAddButton = showAddButton,
                                isAlreadyFriend = currentFriends.contains(user.id)
                            )
                            Divider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
fun UserListItem(
    user: User,
    onAction: () -> Unit,
    showAddButton: Boolean,
    isAlreadyFriend: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.nombre.firstOrNull()?.toString() ?: "?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = user.nombre,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (user.objetivoFitness.isNotEmpty()) {
                    Text(
                        text = user.objetivoFitness,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
        IconButton(
            onClick = onAction
        ) {
            Icon(
                imageVector = if (showAddButton) {
                    if (isAlreadyFriend) Icons.Default.PersonRemove else Icons.Default.PersonAdd
                } else Icons.Default.PersonRemove,
                contentDescription = if (showAddButton) {
                    if (isAlreadyFriend) "Eliminar amigo" else "Agregar amigo"
                } else "Eliminar amigo",
                tint = if (showAddButton && !isAlreadyFriend) MaterialTheme.colorScheme.primary else Color.Red
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListarRutinasAmigosPage(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()

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

    // Al abrir el diálogo de comunidad, cargar la lista de usuarios y amigos
    LaunchedEffect(showCommunityDialog) {
        if (showCommunityDialog) {
            isLoadingUsers = true
            loadAllUsers { users ->
                // Filtramos para excluir al usuario actual
                allUsers = users.filter { it.id != userId }
                isLoadingUsers = false
                Log.d("ListarRutinasAmigosPage", "Usuarios cargados para comunidad: ${allUsers.size}")
            }
            loadFriendsList(userId) { friends ->
                friendsList = friends
            }
        }
    }

    // También cargar la lista de amigos cuando se abre el diálogo de amigos
    LaunchedEffect(showFriendsListDialog) {
        if (showFriendsListDialog) {
            loadFriendsList(userId) { friends ->
                friendsList = friends
            }
        }
    }
    // Cargar rutinas de amigos agrupadas por usuario
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            Log.d("ListarRutinasAmigosPage", "Iniciando carga para el usuario: $userId")
            isLoading = true
            try {
                // Paso 1: Obtener la lista de IDs de amigos del documento "amigos/{userId}" mediante el campo "listaAmigos"
                val amigosDoc = db.collection("amigos")
                    .document(userId)
                    .get()
                    .await()
                val amigosIds = if (amigosDoc.exists() && amigosDoc.get("listaAmigos") != null) {
                    amigosDoc.get("listaAmigos") as? List<String> ?: emptyList()
                } else {
                    emptyList()
                }
                Log.d("ListarRutinasAmigosPage", "IDs de amigos obtenidos: $amigosIds")

                if (amigosIds.isEmpty()) {
                    // No hay amigos, mostrar lista vacía
                    rutinasPorUsuario = emptyList()
                    isLoading = false
                    return@LaunchedEffect
                }

                // Paso 2: Obtener información de todos los usuarios amigos
                val usuariosMap = mutableMapOf<String, Usuario>()
                val batchSize = 10
                for (i in amigosIds.indices step batchSize) {
                    val batch = amigosIds.subList(i, minOf(i + batchSize, amigosIds.size))
                    Log.d("ListarRutinasAmigosPage", "Consultando usuarios para batch: $batch")
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
                            objetivoFitness = doc.getString("objetivoFitness") ?: "Mantener forma física" // Obtener objetivo fitness
                        )
                        usuariosMap[usuario.id] = usuario
                        Log.d("ListarRutinasAmigosPage", "Usuario agregado: ${usuario.id} - ${usuario.nombre}")
                    }
                }
                Log.d("ListarRutinasAmigosPage", "Total de usuarios amigos obtenidos: ${usuariosMap.size}")

                // Paso 3: Obtener rutinas compartidas por los amigos
                val rutinasResult = mutableMapOf<String, MutableList<RutinaAmigo>>()

                for (i in amigosIds.indices step batchSize) {
                    val batch = amigosIds.subList(i, minOf(i + batchSize, amigosIds.size))
                    Log.d("ListarRutinasAmigosPage", "Consultando rutinas para batch de amigos: $batch")
                    val rutinasSnapshot = db.collection("rutinas")
                        .whereIn("usuarioId", batch)
                        .whereEqualTo("compartirConAmigos", true)
                        .get()
                        .await()

                    Log.d("ListarRutinasAmigosPage", "Rutinas encontradas en este batch: ${rutinasSnapshot.documents.size}")

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

                                // Agrupamos las rutinas por usuario
                                if (!rutinasResult.containsKey(usuarioId)) {
                                    rutinasResult[usuarioId] = mutableListOf()
                                }
                                rutinasResult[usuarioId]?.add(rutina)

                                Log.d("ListarRutinasAmigosPage", "Rutina agregada: ${rutina.nombre} de ${rutina.nombreUsuario}")
                            }
                        }
                    }
                }

                // Convertimos el mapa a una lista de RutinasPorUsuario, solo incluimos usuarios con al menos una rutina
                val resultList = mutableListOf<RutinasPorUsuario>()
                for ((usuarioId, rutinas) in rutinasResult) {
                    val usuario = usuariosMap[usuarioId]
                    if (usuario != null && rutinas.isNotEmpty()) {
                        // Ordenar las rutinas del usuario por fecha (más recientes primero)
                        val rutinasOrdenadas = rutinas.sortedByDescending { it.fechaCreacion }
                        resultList.add(RutinasPorUsuario(usuario, rutinasOrdenadas))
                    }
                }

                // Ordenamos la lista de RutinasPorUsuario alfabéticamente por nombre de usuario
                rutinasPorUsuario = resultList.sortedBy { it.usuario.nombre }

                Log.d("ListarRutinasAmigosPage", "Total de amigos con rutinas: ${rutinasPorUsuario.size}")
                isLoading = false

            } catch (e: Exception) {
                Log.e("ListarRutinasAmigosPage", "Error al cargar rutinas: ${e.message}")
                errorMessage = "Error al cargar rutinas: ${e.message}"
                isLoading = false
            }
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
                },
                actions = {
                    IconButton(onClick = { showFriendsListDialog = true }) {
                        Icon(Icons.Default.People, contentDescription = "Ver Amigos")
                    }
                }
            )
        },
        floatingActionButton = {
            // Botón de comunidad en la parte inferior derecha
            FloatingActionButton(
                onClick = {  showCommunityDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.PersonAdd,
                    contentDescription = "Abrir comunidad"
                )
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
                                // Aquí se podría reejecutar la lógica de carga (idealmente extraerla en una función)
                            } catch (e: Exception) {
                                errorMessage = "Error al cargar rutinas: ${e.message}"
                            } finally {
                                isLoading = false
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
                    Button(onClick = {
                        showCommunityDialog = true
                    }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buscar Amigos")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    rutinasPorUsuario.forEach { grupo ->
                        // Sección de título con el nombre del usuario
                        item {
                            AmigoCabecera(usuario = grupo.usuario)
                        }

                        // Rutinas del usuario
                        items(grupo.rutinas) { rutina ->
                            RutinaAmigoCard(
                                rutina = rutina,
                                onRutinaClick = {
                                    selectedRutina = rutina
                                    showDetailDialog = true
                                }
                            )
                        }

                        // Espacio entre secciones de usuarios
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

    // Diálogo para mostrar detalles de la rutina con opción para copiarla
    if (showDetailDialog && selectedRutina != null) {
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
                            text = selectedRutina!!.nombre,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            showDetailDialog = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    // Información del creador
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
                                text = selectedRutina!!.nombreUsuario.firstOrNull()?.toString() ?: "?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Creado por ${selectedRutina!!.nombreUsuario}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    AssistChip(
                        onClick = { },
                        label = { Text(selectedRutina!!.dificultad) },
                        leadingIcon = {
                            Icon(
                                imageVector = when(selectedRutina!!.dificultad) {
                                    "Fácil" -> Icons.Default.Star
                                    "Difícil" -> Icons.Default.StarRate
                                    else -> Icons.Default.StarHalf
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

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

                    //Espacio para el mensaje de error
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

                        // Iniciar corrutina para eliminar el mensaje de error después de un tiempo
                        LaunchedEffect(errorMessage2) {
                            if (errorMessage2 != null) {
                                scope.launch {
                                    delay(2000) // Esperar 2 segundos
                                    errorMessage2 = null
                                }
                            }
                        }
                    }

                    if (selectedRutina!!.descripcion.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Descripción:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedRutina!!.descripcion,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Ejercicios (${selectedRutina!!.ejercicios.size}):",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(selectedRutina!!.ejercicios) { ejercicio ->
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

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Button(
                            onClick = {
                                selectedRutina?.let {
                                    val rutinaId = it.id
                                    Log.d("ListarMisRutinasPage", "Ejecutar rutina: $rutinaId")
                                    navController.navigate("ejecutar_rutina/$rutinaId")
                                }
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
                                        val rutinaAmigo = selectedRutina!!
                                        Log.d("ListarRutinasAmigosPage", "Verificando si ya existe rutina: ${rutinaAmigo.nombre}")

                                        // Verifica si ya existe una rutina con ese nombre para el usuario
                                        val querySnapshot = db.collection("rutinas")
                                            .whereEqualTo("usuarioId", userId)
                                            .whereEqualTo("nombre", rutinaAmigo.nombre)
                                            .get()
                                            .await()

                                        if (!querySnapshot.isEmpty) {
                                            isCopying = false
                                            errorMessage2 = "Ya tienes una rutina con este nombre"
                                            Log.w("ListarRutinasAmigosPage", "Rutina duplicada: ${rutinaAmigo.nombre}")
                                            scope.launch {
                                                delay(2000)
                                                if (showDetailDialog) {
                                                    errorMessage2 = null
                                                }
                                            }
                                            return@launch
                                        }

                                        // Crear rutina si no existe
                                        val nuevaRutina = hashMapOf(
                                            "nombre" to rutinaAmigo.nombre,
                                            "descripcion" to "${rutinaAmigo.descripcion}\n\nCopiada de: ${rutinaAmigo.nombreUsuario}",
                                            "dificultad" to rutinaAmigo.dificultad,
                                            "ejercicios" to rutinaAmigo.ejercicios,
                                            "usuarioId" to userId,
                                            "compartirConAmigos" to false,
                                            "fechaCreacion" to Timestamp.now()
                                        )

                                        db.collection("rutinas")
                                            .add(nuevaRutina)
                                            .await()

                                        isCopying = false
                                        successMessage = "¡Rutina guardada en tu colección!"
                                        Log.d("ListarRutinasAmigosPage", "Rutina copiada exitosamente")

                                        scope.launch {
                                            delay(2000)
                                            if (showDetailDialog) {
                                                successMessage = null
                                            }
                                        }
                                    } catch (e: Exception) {
                                        isCopying = false
                                        errorMessage = "Error al copiar la rutina: ${e.message}"
                                        Log.e("ListarRutinasAmigosPage", "Error al copiar rutina: ${e.message}")
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
    // Diálogo de comunidad (buscar usuarios)
    if (showCommunityDialog) {
        UsersDialog(
            title = "Comunidad GymRace",
            users = allUsers,
            isLoading = isLoadingUsers,
            onDismiss = { showCommunityDialog = false },
            onUserAction = { friendId ->
                // Verificar si ya es amigo
                val friendIds = friendsList.map { it.id }
                if (friendIds.contains(friendId)) {
                    // Eliminar amigo
                    scope.launch {
                        removeFriend(userId, friendId) {
                            // Actualizar lista de amigos
                            loadFriendsList(userId) { friends ->
                                friendsList = friends
                                successMessage = "Usuario eliminado de amigos"
                            }
                        }
                    }
                } else {
                    // Agregar amigo
                    scope.launch {
                        addFriend(userId, friendId) {
                            // Actualizar lista de amigos
                            loadFriendsList(userId) { friends ->
                                friendsList = friends
                                successMessage = "Usuario agregado a amigos"
                            }
                        }
                    }
                }
            },
            showAddButton = true,
            currentFriends = friendsList.map { it.id }
        )
    }

    // Diálogo de amigos (ver amigos actuales)
    if (showFriendsListDialog) {
        UsersDialog(
            title = "Mis Amigos",
            users = friendsList,
            isLoading = isLoadingUsers,
            onDismiss = { showFriendsListDialog = false },
            onUserAction = { friendId ->
                // Eliminar amigo
                scope.launch {
                    removeFriend(userId, friendId) {
                        // Actualizar lista de amigos
                        loadFriendsList(userId) { friends ->
                            friendsList = friends
                            successMessage = "Usuario eliminado de amigos"
                        }
                    }
                }
            },
            showAddButton = false
        )
    }
}

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
                Column {
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
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
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