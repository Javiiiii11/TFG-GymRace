package com.example.gymrace.pages.desafios

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import coil.compose.rememberImagePainter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymrace.GifData
import com.example.gymrace.R
import com.example.gymrace.loadAllUsers
import com.example.gymrace.loadGifsFromXml
import com.example.gymrace.pages.GLOBAL
import com.example.gymrace.showExerciseDetail
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import np.com.bimalkafle.bottomnavigationdemo.pages.User
import np.com.bimalkafle.bottomnavigationdemo.pages.UsersDialog
import np.com.bimalkafle.bottomnavigationdemo.pages.sendFriendRequestNotification
import java.util.UUID

// Modelos de datos
data class Challenge(
    val id: String = "",
    val name: String,
    val description: String,
    val creatorId: String,
    val creatorName: String, // ← AÑADIDO
    val participantId: String,
    val exercise: String,
    val repetitions: Int,
    val creatorProgress: Int = 0,
    val participantProgress: Int = 0,
    val status: String
)


// Modelo de datos para amigos
data class Friend(
    val id: String,
    val name: String,
    val profilePicUrl: String? = null
)

// ViewModel para Desafíos
class ChallengeViewModel : ViewModel() {
    // Inicializa Firebase Firestore
    private val firestore = FirebaseFirestore.getInstance()
    // StateFlow para almacenar la lista de amigos y desafíos
    private val _userFriends = MutableStateFlow<List<Friend>>(emptyList())
    // Exposición de la lista de amigos como StateFlow
    val userFriends: StateFlow<List<Friend>> = _userFriends
    // StateFlow para almacenar la lista de desafíos
    private val _friendsChallenges = MutableStateFlow<List<Challenge>>(emptyList())
    // Exposición de la lista de desafíos como StateFlow
    val friendsChallenges: StateFlow<List<Challenge>> = _friendsChallenges
    // Función para cargar amigos de un usuario
    suspend fun loadFriends(userId: String) {
        try {
            // Obtiene la lista de amigos del usuario
            val amigosDocSnapshot = firestore.collection("amigos")
                .document(userId)
                .get()
                .await()
            // Verifica si el documento existe
            val friendsIds = amigosDocSnapshot.get("listaAmigos") as? List<String> ?: emptyList()
            // Crea una lista mutable para almacenar los amigos
            val friendsList = mutableListOf<Friend>()
            Log.d("ChallengeViewModel", "Friends IDs: $friendsIds")
            // Itera sobre los IDs de amigos y obtiene sus datos
            for (friendId in friendsIds) {
                val friendUserSnapshot = firestore.collection("usuarios")
                    .document(friendId)
                    .get()
                    .await()
                // Verifica si el documento del amigo existe
                if (friendUserSnapshot.exists()) {
                    val friendName = friendUserSnapshot.getString("nombre") ?: "Usuario"
                    friendsList.add(Friend(id = friendId, name = friendName))
                }
            }
            _userFriends.value = friendsList
        } catch (e: Exception) {
            // Manejo de errores al cargar amigos
            Log.e("ChallengeViewModel", "Error al cargar los usuarios: ${e.message}")
        }
    }
    // Función para cargar desafíos de amigos
    suspend fun loadFriendsChallenges(userId: String) {
        try {
            // Obtiene la lista de desafíos creados por el usuario
            val challengesSnapshot = firestore.collection("desafios")
                .whereEqualTo("creador.id", userId)
                .get()
                .await()
            // Obtiene la lista de desafíos en los que el usuario es participante
            val participantChallengesSnapshot = firestore.collection("desafios")
                .whereEqualTo("amigoInvitado.id", userId)
                .get()
                .await()
            // Crea una lista mutable para almacenar todos los desafíos
            val allChallenges = mutableListOf<Challenge>()
            // Mapea los documentos de desafíos a objetos Challenge
            challengesSnapshot.documents.mapNotNullTo(allChallenges) { document ->
                // Extrae los datos del documento y maneja posibles errores
                val creatorProgress = (document.get("creador.progreso") as? Long)?.toInt() ?: 0
                // Maneja el progreso del participante
                val participantProgress = (document.get("amigoInvitado.progreso") as? Long)?.toInt() ?: 0
                // Crea un objeto Challenge a partir de los datos del documento
                Challenge(
                    id = document.id,
                    name = document.getString("titulo") ?: "",
                    description = document.getString("descripcion") ?: "",
                    creatorId = document.getString("creador.id") ?: "",
                    creatorName = document.getString("creador.nombre") ?: "",
                    participantId = document.getString("amigoInvitado.id") ?: "",
                    exercise = document.getString("ejercicio") ?: "",
                    repetitions = (document.getLong("repeticiones") ?: 0).toInt(),
                    creatorProgress = creatorProgress,
                    participantProgress = participantProgress,
                    status = document.getString("status") ?: "PENDING"
                )
            }
            // Mapea los documentos de desafíos en los que el usuario es participante
            participantChallengesSnapshot.documents.mapNotNullTo(allChallenges) { document ->
                // Extrae los datos del documento y maneja posibles errores
                val creatorProgress = (document.get("creador.progreso") as? Long)?.toInt() ?: 0
                // Maneja el progreso del creador
                val participantProgress = (document.get("amigoInvitado.progreso") as? Long)?.toInt() ?: 0
                // Crea un objeto Challenge a partir de los datos del documento
                Challenge(
                    id = document.id,
                    name = document.getString("titulo") ?: "",
                    description = document.getString("descripcion") ?: "",
                    creatorId = document.getString("creador.id") ?: "",
                    creatorName = document.getString("creador.nombre") ?: "",
                    participantId = document.getString("amigoInvitado.id") ?: "",
                    exercise = document.getString("ejercicio") ?: "",
                    repetitions = (document.getLong("repeticiones") ?: 0).toInt(),
                    creatorProgress = creatorProgress,
                    participantProgress = participantProgress,
                    status = document.getString("status") ?: "PENDING"
                )
            }

            _friendsChallenges.value = allChallenges
        } catch (e: Exception) {
            // Manejo de errores al cargar desafíos
            Log.e("Error", "Error al cargar los desafios de los usuarios: ${e.message}")
        }
    }
    // Función para crear un nuevo desafío
    suspend fun createChallenge(challenge: Challenge) {
        try {
            val firestore = FirebaseFirestore.getInstance()

            // Si el challenge no tiene ID, le generamos uno nuevo
            val challengeData = if (challenge.id.isBlank()) {
                challenge.copy(id = UUID.randomUUID().toString())
            } else challenge

            // Preparamos los datos del desafío para Firestore
            val desafioData = mapOf(
                "creador" to mapOf("id" to challengeData.creatorId, "progreso" to 0),
                "amigoInvitado" to mapOf("id" to challengeData.participantId, "progreso" to 0),
                "titulo" to challengeData.name,
                "descripcion" to challengeData.description,
                "ejercicio" to challengeData.exercise,
                "repeticiones" to challengeData.repetitions,
                "status" to challengeData.status
            )

            // Guardamos en la colección 'desafios'
            firestore.collection("desafios")
                .document(challengeData.id)
                .set(desafioData)
                .await()

            // Recargamos la lista de desafíos
            loadFriendsChallenges(challenge.creatorId)

            // Creamos notificación
            val notificationData = mapOf(
                "tipo" to "desafio",
                "mensaje" to "${challenge.creatorName} te ha retado a un desafío",
                "remitente" to challenge.creatorId,
                "challengeId" to challengeData.id,
                "timestamp" to FieldValue.serverTimestamp()
            )

            firestore.collection("notificaciones")
                .document(challenge.participantId)
                .collection("items")
                .add(notificationData)
                .addOnSuccessListener {
                    Log.d("ChallengeNotification", "Notificación de desafío enviada con éxito")
                }
                .addOnFailureListener {
                    Log.e("ChallengeNotification", "Error al enviar la notificación de desafío", it)
                }

        } catch (e: Exception) {
            Log.e("Error", "Error al crear el desafio: ${e.message}")
        }
    }

    // Función para actualizar el estado de un desafío
    suspend fun updateChallengeStatus(challengeId: String, newStatus: String, userId: String) {
        try {
            // Verifica si el nuevo estado es "COMPLETED" y actualiza el progreso
            firestore.collection("desafios")
                .document(challengeId)
                .update("status", newStatus)
                .await()
            loadFriendsChallenges(userId)
        } catch (e: Exception) {
            // Manejo de errores al actualizar el estado del desafío
            Log.e("Error", "Error al actualizar el estado: ${e.message}")
        }
    }

    // Función para actualizar el progreso de un desafío
    suspend fun updateProgress(challengeId: String, userId: String, newProgress: Int) {
        try {
            // Obtiene el documento del desafío
            val challengeDoc = firestore.collection("desafios")
                .document(challengeId)
                .get()
                .await()
            // Verifica si el documento existe
            val creatorId = challengeDoc.get("creador.id") as? String
            // Verifica si el creador es el usuario actual
            val participantId = challengeDoc.get("amigoInvitado.id") as? String
            // Verifica si el participante es el usuario actual
            val updateField = when (userId) {
                creatorId -> "creador.progreso"
                participantId -> "amigoInvitado.progreso"
                else -> throw Exception("Usuario no es parte de este desafío")
            }
            // Actualiza el progreso del desafío
            firestore.collection("desafios")
                .document(challengeId)
                .update(updateField, newProgress)
                .await()
            // Verifica si el progreso alcanzó el objetivo
            val repetitions = (challengeDoc.getLong("repeticiones") ?: 0).toInt()
            // Si el progreso es igual o mayor al objetivo, actualiza el estado a "COMPLETED"
            if (newProgress >= repetitions) {
                firestore.collection("desafios")
                    .document(challengeId)
                    .update("status", "COMPLETED")
                    .await()
            }
            // Recarga la lista de desafíos
            loadFriendsChallenges(userId)
        } catch (e: Exception) {
            // Manejo de errores al actualizar el progreso
            Log.e("Error", "Error al actualizar el desafio: ${e.message}")
        }
    }



    // Permite eliminar un desafío:
    // - Si está en progreso: solo el creador puede eliminarlo.
    // - Si está completado: cualquiera de los 2 puede eliminarlo.
    suspend fun deleteChallenge(challengeId: String, userId: String, challengeStatus: String, creatorId: String) {
        try {
            // Verifica si el desafío está en progreso y si el usuario es el creador
            if (challengeStatus != "COMPLETED" && userId != creatorId) {
                throw Exception("Solo el creador puede eliminar el desafío en progreso")
            }
            // Elimina el desafío de Firestore
            firestore.collection("desafios")
                .document(challengeId)
                .delete()
                .await()
            loadFriendsChallenges(userId)
        } catch (e: Exception) {
            // Manejo de errores al eliminar el desafío
            Log.e("Error", "Error al eliminar el desafio: ${e.message}")
        }
    }
}


// Pantalla de Desafíos
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesafiosPage(
    viewModel: ChallengeViewModel = viewModel(),
    userId: String
) {

    suspend fun cargarUsuarioEnGlobal(): Boolean {
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val userDocument = Firebase.firestore
                    .collection("usuarios")
                    .document(currentUser.uid)
                    .get()
                    .await()

                if (userDocument.exists()) {
                    GLOBAL.id = currentUser.uid
                    GLOBAL.nombre = userDocument.getString("nombre") ?: ""
                    GLOBAL.peso = userDocument.getString("peso") ?: ""
                    GLOBAL.altura = userDocument.getString("altura") ?: ""
                    GLOBAL.edad = userDocument.getString("edad") ?: ""
                    GLOBAL.objetivoFitness = userDocument.getString("objetivoFitness") ?: ""
                    GLOBAL.diasEntrenamientoPorSemana = userDocument.getString("diasEntrenamientoPorSemana") ?: ""

                    val rawNivelExperiencia = userDocument.getString("nivelExperiencia") ?: ""
                    GLOBAL.nivelExperiencia = when (rawNivelExperiencia) {
                        "Avanzado (más de 2 años)" -> "Avanzado"
                        "Intermedio (6 meses - 2 años)" -> "Intermedio"
                        "Principiante (menos de 6 meses)" -> "Principiante"
                        else -> rawNivelExperiencia
                    }

                    true
                } else {
                    Log.w("UsuarioGlobal", "Documento de usuario no existe")
                    false
                }
            } else {
                Log.w("UsuarioGlobal", "Usuario no logueado")
                false
            }
        } catch (e: Exception) {
            Log.e("UsuarioGlobal", "Error al cargar usuario: ${e.message}")
            false
        }
    }


    // Recupera el contexto de la aplicación
    val challenges by viewModel.friendsChallenges.collectAsState(initial = emptyList())
    // Recupera la lista de amigos del ViewModel
    val friends by viewModel.userFriends.collectAsState(initial = emptyList())
    // Inicializa la base de datos Firestore
    val coroutineScope = rememberCoroutineScope()

    // Estados para diálogos
    // Estado para el diálogo de creación de desafío
    var showCreateDialog by remember { mutableStateOf(false) }
    // Estado para el diálogo de progreso
    var showProgressDialog by remember { mutableStateOf(false) }
    // Estado para el desafío seleccionado
    var selectedChallenge by remember { mutableStateOf<Challenge?>(null) }
    // Estado para el progreso del desafío
    var newProgress by remember { mutableStateOf("0") }
    // Estado para el diálogo de confirmación de eliminación
    var showDeleteChallengeDialog by remember { mutableStateOf(false) }

    // Para refrescar cada 5 segundos
    LaunchedEffect(userId) {
        val ok = cargarUsuarioEnGlobal()
        if (ok) {
            Log.d("UsuarioGlobal", "Datos cargados correctamente: ${GLOBAL.nombre}")
        } else {
            Log.e("UsuarioGlobal", "Error cargando datos")
        }

        while (true) {
            viewModel.loadFriendsChallenges(userId)
            delay(5000)
        }
    }

    // Pantalla principal
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Desafíos",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground) },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Crear Desafío")
                    }
                }
            )
        },
        floatingActionButton = {
            val scope = rememberCoroutineScope()
            val scale = remember { Animatable(1f) }

            FloatingActionButton(
                onClick = {
                    scope.launch {
                        scale.animateTo(0.85f, animationSpec = tween(100))
                        scale.animateTo(1f, animationSpec = tween(100))
                        showCreateDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.scale(scale.value)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Rutina")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = 8.dp)
                .scrollable(
                    state = rememberScrollState(),
                    orientation = Orientation.Vertical
                )
        ) {
            if (challenges.isEmpty()) {
                EmptyState(onCreateDesafio = { showCreateDialog = true })
            }else {
                ChallengesList(
                    challenges = challenges,
                    userId = userId,
                    onAccept = { challenge ->
                        coroutineScope.launch {
                            viewModel.updateChallengeStatus(challenge.id, "ACCEPTED", userId)
                        }
                        //borrar la notificacion
                        val firestore = FirebaseFirestore.getInstance()
                        firestore.collection("notificaciones")
                            .document(userId)
                            .collection("items")
                            .whereEqualTo("challengeId", challenge.id)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                for (document in querySnapshot.documents) {
                                    document.reference.delete()
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("DesafiosPage", "Error al eliminar la notificación: ${e.message}")
                            }
                    },
                    onUpdateProgress = { challenge ->
                        selectedChallenge = challenge
                        showProgressDialog = true
                        val currentProgress = if (userId == challenge.creatorId) {
                            challenge.creatorProgress
                        } else {
                            challenge.participantProgress
                        }
                        newProgress = currentProgress.toString()
                    },
                    onDeleteChallenge = { challenge ->
                        selectedChallenge = challenge
                        showDeleteChallengeDialog = true
                    }
                )
            }


            if (showDeleteChallengeDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteChallengeDialog = false },
                    title = { Text("Eliminar desafío") },
                    text = { Text("¿Estás seguro de que deseas eliminar este desafío?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    selectedChallenge?.let { challenge ->
                                        viewModel.deleteChallenge(challenge.id, userId, challenge.status, challenge.creatorId)
                                    }
                                    showDeleteChallengeDialog = false
                                }
                            }
                        ) {
                            Text("Eliminar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteChallengeDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            if (showProgressDialog && selectedChallenge != null) {
                val challenge = selectedChallenge!!
                val currentProgress = if (userId == challenge.creatorId) {
                    challenge.creatorProgress
                } else {
                    challenge.participantProgress
                }
                AlertDialog(
                    onDismissRequest = {
                        showProgressDialog = false
                        selectedChallenge = null
                    },
                    title = { Text("Actualizar progreso") },
                    text = {
                        Column {
                            Text("Desafío: ${challenge.name}")
                            Text("Ejercicio: ${challenge.exercise}")
                            Text("Objetivo: ${challenge.repetitions} repeticiones")
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Progreso actual: $currentProgress")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newProgress,
                                onValueChange = { newProgress = it.filter { char -> char.isDigit() } },
                                label = { Text("Nuevo progreso") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                // Validar el nuevo progreso
                                coroutineScope.launch {
                                    val progress = newProgress.toIntOrNull() ?: 0
                                    viewModel.updateProgress(challenge.id, userId, progress)
                                    showProgressDialog = false
                                    selectedChallenge = null
                                }
                            }
                        ) {
                            Text("Guardar")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                // Cerrar el diálogo sin guardar cambios
                                showProgressDialog = false
                                selectedChallenge = null
                            }
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            if (showCreateDialog) {
                CreateChallengeDialog(
                    currentUserId = userId,
                    friends = friends,
                    onDismiss = { showCreateDialog = false },
                    onCreateChallenge = { challenge ->
                        coroutineScope.launch {
                            viewModel.createChallenge(challenge)
                            showCreateDialog = false
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(userId) {
        viewModel.loadFriends(userId)
        viewModel.loadFriendsChallenges(userId)
    }
}

// Composable para el estado vacío (si no hay desafíos)
@Composable
fun EmptyState(onCreateDesafio: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Imagen central
        Icon(
//            imageVector = ImageVector.vectorResource(id = R.drawable.fuego)
            painter = painterResource(id = R.drawable.fuego2),
            contentDescription = null,
            modifier = Modifier.size(120.dp), // 80 dp
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No tienes desafíos disponibles",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Crea un desafío con el botón + o invita usuarios",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        // Botón para crear desafío
        Button(onClick = onCreateDesafio) {
            Icon(ImageVector.vectorResource(id = R.drawable.fuego), contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crear desafío")
        }
    }
}

// Composable para la lista de desafíos
@Composable
fun ChallengesList(
    challenges: List<Challenge>,
    userId: String,
    onAccept: (Challenge) -> Unit,
    onUpdateProgress: (Challenge) -> Unit,
    onDeleteChallenge: (Challenge) -> Unit
) {
    // Lista de desafíos
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Itera sobre la lista de desafíos y crea un ChallengeItem para cada uno
        items(challenges) { challenge ->
            ChallengeItem(
                challenge = challenge,
                userId = userId,
                onAccept = { onAccept(challenge) },
                onUpdateProgress = { onUpdateProgress(challenge) },
                onDeleteChallenge = { onDeleteChallenge(challenge) }
            )
        }
    }
}

// Composable para cada desafío
@Composable
fun ChallengeItem(
    challenge: Challenge,
    userId: String,
    onAccept: () -> Unit,
    onUpdateProgress: () -> Unit,
    onDeleteChallenge: () -> Unit
) {
    // Determina si el usuario es el creador del desafío
    val isCreator = userId == challenge.creatorId
    // Determina el progreso del creador y del participante
    val myProgress = if (isCreator) challenge.creatorProgress else challenge.participantProgress
    val otherProgress = if (isCreator) challenge.participantProgress else challenge.creatorProgress
    // Determina el nombre del otro participante
    val otherName = if (isCreator) "Invitado" else "Creador"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Contenido del desafío
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = challenge.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = challenge.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Ejercicio: ${challenge.exercise}", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = "Objetivo: ${challenge.repetitions} repeticiones", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Tu progreso: $myProgress / ${challenge.repetitions}", fontSize = 14.sp)
            LinearProgressIndicator(
                progress = myProgress.toFloat() / challenge.repetitions,
                modifier = Modifier.fillMaxWidth().height(8.dp).padding(vertical = 2.dp)
            )
            Text(text = "Progreso del $otherName: $otherProgress / ${challenge.repetitions}", fontSize = 14.sp)
            LinearProgressIndicator(
                progress = otherProgress.toFloat() / challenge.repetitions,
                modifier = Modifier.fillMaxWidth().height(8.dp).padding(vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(status = challenge.status)
                if (challenge.status == "PENDING" && !isCreator) {
                    Row {
                        IconButton(
                            onClick = onAccept,
                            modifier = Modifier
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Aceptar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(
                            onClick = { onDeleteChallenge() },
                            modifier = Modifier
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Rechazar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else if (challenge.status == "ACCEPTED" || challenge.status == "IN_PROGRESS") {
                    Button(onClick = onUpdateProgress, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Progreso")
                    }
                }
            }
            // Mostrar opciones de eliminación y ganador según el estado:
            if (challenge.status == "COMPLETED") {
                val winner = when {
                    challenge.creatorProgress > challenge.participantProgress -> "Creador"
                    challenge.participantProgress > challenge.creatorProgress -> "Invitado"
                    else -> "Empate"
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Ganador: $winner", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                // Ambos pueden eliminar cuando esté completado
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onDeleteChallenge) {
                    Text("Eliminar desafío")
                }
            } else if (challenge.status != "COMPLETED" && isCreator) {
                // Durante el progreso, solo el creador puede eliminar
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onDeleteChallenge) {
                    Text("Eliminar desafío")
                }
            }
        }
    }
}

// Composable para el chip de estado
@Composable
fun StatusChip(status: String) {
    // Determina el color y el texto del chip según el estado
    val (color, text) = when (status) {
        "PENDING" -> Pair(MaterialTheme.colorScheme.tertiary, "Pendiente de aceptar")
        "ACCEPTED", "IN_PROGRESS" -> Pair(MaterialTheme.colorScheme.primary, "En progreso")
        "COMPLETED" -> Pair(MaterialTheme.colorScheme.secondary, "Completado")
        else -> Pair(MaterialTheme.colorScheme.error, "Desconocido")
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Composable para el diálogo de creación de desafío
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChallengeDialog(
    currentUserId: String,
    friends: List<Friend>,
    onDismiss: () -> Unit,
    onCreateChallenge: (Challenge) -> Unit
) {
    // Variables de estado para los campos del formulario
    var challengeName by remember { mutableStateOf("") }
    var challengeDescription by remember { mutableStateOf("") }
    var selectedFriendId by remember { mutableStateOf("") }
    var selectedFriendName by remember { mutableStateOf("Seleccionar usuario") }
    var selectedExercise by remember { mutableStateOf("") }
    var repetitions by remember { mutableStateOf("") }
    // Estado para mostrar el menú desplegable de amigos
    var expandedFriends by remember { mutableStateOf(false) }
    // Estado para mostrar el diálogo de selección mejorada de ejercicio
    var showEjerciciosDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Crear nuevo desafío",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar"
                        )
                    }
                }
                Divider()
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = challengeName,
                    onValueChange = { challengeName = it },
                    label = { Text("Nombre del desafío") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                OutlinedTextField(
                    value = challengeDescription,
                    onValueChange = { challengeDescription = it },
                    label = { Text("Descripción") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                // Selección de amigo
                ExposedDropdownMenuBox(
                    expanded = expandedFriends,
                    onExpandedChange = { expandedFriends = !expandedFriends }
                ) {
                    OutlinedTextField(
                        value = selectedFriendName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Usuarios") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFriends)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .padding(vertical = 8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedFriends,
                        onDismissRequest = { expandedFriends = false }
                    ) {
                        friends.forEach { friend ->
                            DropdownMenuItem(
                                text = { Text(friend.name) },
                                onClick = {
                                    selectedFriendId = friend.id
                                    selectedFriendName = friend.name
                                    expandedFriends = false
                                }
                            )
                        }
                    }
                }
                // Selección de ejercicio con estado visual más claro
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Ejercicio",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEjerciciosDialog = true },
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (selectedExercise.isNotBlank()) selectedExercise else "Seleccionar ejercicio",
                                color = if (selectedExercise.isBlank())
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Buscar ejercicio",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = repetitions,
                    onValueChange = { repetitions = it.filter { char -> char.isDigit() } },
                    label = { Text("Repeticiones") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reps = repetitions.toIntOrNull() ?: 0
                    onCreateChallenge(
                        Challenge(
                            name = challengeName,
                            description = challengeDescription,
                            creatorId = currentUserId,
                            creatorName = GLOBAL.nombre, // ← NUEVO
                            participantId = selectedFriendId,
                            exercise = selectedExercise,
                            repetitions = reps,
                            status = "PENDING"
                        )
                    )
                },
                enabled = challengeName.isNotBlank() &&
                        challengeDescription.isNotBlank() &&
                        selectedFriendId.isNotBlank() &&
                        selectedExercise.isNotBlank() &&
                        repetitions.isNotBlank() &&
                        (repetitions.toIntOrNull() ?: 0) > 0
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )


    // Diálogo de selección mejorada de ejercicio
    if (showEjerciciosDialog) {
        EjerciciosDialog(
            onDismissRequest = { showEjerciciosDialog = false },
            onEjercicioSelected = { ejercicio ->
                selectedExercise = ejercicio
                showEjerciciosDialog = false
            }
        )
    }

}

// Función para mostrar el diálogo de selección de ejercicios
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjerciciosDialog(
    onDismissRequest: () -> Unit,
    onEjercicioSelected: (String) -> Unit
) {
    // Variables de estado para la búsqueda y la categoría seleccionada
    var buscarEjercicio by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }
    // Variables de estado para la lista de ejercicios y el detalle del ejercicio
    val context = LocalContext.current
    val (ejerciciosData, _) = remember {
        loadGifsFromXml(context.resources.getXml(R.xml.ejercicios), context)
    }
    // Cargar los datos de los ejercicios desde el XML
    val categories = listOf("Todos") + ejerciciosData.map { it.category }.distinct()
    val ejerciciosSeleccionados = remember { mutableStateListOf<GifData>() }
    var selectedExerciseDetail by remember { mutableStateOf<GifData?>(null) }

    val ejerciciosFiltrados = ejerciciosData.filter {
        it.title.contains(buscarEjercicio, ignoreCase = true) &&
                (selectedCategory == "Todos" || it.category == selectedCategory)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
        properties = DialogProperties(dismissOnClickOutside = true),
        content = {
            Card(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Contenido del diálogo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Añadir Ejercicios",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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

                    Text(
                        "Categorías:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
                                val ejercicio = ejerciciosFiltrados[index]

                                ExercisePreviewItem(
                                    gifData = ejercicio,
                                    isSelected = ejerciciosSeleccionados.contains(ejercicio),
                                    onToggleSelect = { selected ->
                                        if (selected) {
                                            ejerciciosSeleccionados.clear()
                                            ejerciciosSeleccionados.add(ejercicio)
                                        } else {
                                            ejerciciosSeleccionados.remove(ejercicio)
                                        }
                                    },
                                    onViewDetail = {
                                        showExerciseDetail(
                                            ejercicio.title,
                                            ejerciciosData
                                        ) { selectedExerciseDetail = it }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            onDismissRequest()
                            buscarEjercicio = ""
                            selectedCategory = "Todos"
                        }) {
                            Text("Cancelar")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (ejerciciosSeleccionados.isNotEmpty()) {
                                    onEjercicioSelected(ejerciciosSeleccionados.first().title)
                                }
                                onDismissRequest()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = ejerciciosSeleccionados.isNotEmpty()
                        ) {
//                            Text("Confirmar (${ejerciciosSeleccionados.size})")
                            Text("Confirmar")
                        }
                    }
                }
            }
        }
    )
}


// Composable para mostrar un ejercicio en la lista
@Composable
fun ExercisePreviewItem(
    gifData: GifData,
    isSelected: Boolean,
    onToggleSelect: (Boolean) -> Unit,
    onViewDetail: () -> Unit
) {
    // Tarjeta que muestra el ejercicio
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable { onToggleSelect(!isSelected) },
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        // Contenido de la tarjeta
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberImagePainter(data = gifData.resource),
                contentDescription = "GIF de ${gifData.title}",
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .animateContentSize() // Animación para el contenido

            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gifData.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Músculo: ${gifData.mainMuscle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect(it) }
            )
        }
    }
}


// Función para cargar los IDs de usuarios a los que el usuario actual ha enviado solicitudes
private fun loadRequestedUserIds(currentUserId: String, callback: (List<String>) -> Unit) {
    val db = Firebase.firestore
    val notificacionesRef = db.collection("notificaciones")
    val requestedIds = mutableListOf<String>()

    // Busca solicitudes enviadas por el usuario actual
    db.collection("usuarios")
        .get()
        .addOnSuccessListener { usersSnapshot ->
            if (usersSnapshot.isEmpty) {
                Log.d("DesafiosPage", "No hay usuarios para comprobar solicitudes")
                callback(emptyList())
                return@addOnSuccessListener
            }

            // Contador para saber cuándo hemos terminado con todas las consultas
            var usersToCheck = usersSnapshot.size()

            for (userDoc in usersSnapshot.documents) {
                val targetUserId = userDoc.id

                // Evitamos verificar solicitudes al propio usuario
                if (targetUserId == currentUserId) {
                    usersToCheck--
                    if (usersToCheck == 0) {
                        callback(requestedIds)
                    }
                    continue
                }

                // Busca en las notificaciones del usuario objetivo
                notificacionesRef.document(targetUserId)
                    .collection("items")
                    .whereEqualTo("tipo", "solicitud")
                    .whereEqualTo("remitente", currentUserId)
                    .get()
                    .addOnSuccessListener { notifSnapshot ->
                        if (!notifSnapshot.isEmpty) {
                            // Si hay solicitudes del usuario actual a este usuario, añadimos su ID
                            requestedIds.add(targetUserId)
                            Log.d("DesafiosPage", "Solicitud encontrada para: $targetUserId")
                        }

                        // Disminuimos el contador
                        usersToCheck--

                        // Si hemos terminado con todos los usuarios, llamamos al callback
                        if (usersToCheck == 0) {
                            Log.d("DesafiosPage", "Carga de solicitudes completada. Total: ${requestedIds.size}")
                            callback(requestedIds)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("DesafiosPage", "Error al buscar solicitudes para $targetUserId", e)
                        usersToCheck--

                        if (usersToCheck == 0) {
                            callback(requestedIds)
                        }
                    }
            }
        }
        .addOnFailureListener { e ->
            Log.e("DesafiosPage", "Error al cargar usuarios para comprobar solicitudes", e)
            callback(emptyList())
        }
}