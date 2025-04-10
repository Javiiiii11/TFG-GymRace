package com.example.gymrace.pages

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import coil.compose.rememberImagePainter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import coil.compose.rememberImagePainter
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import com.example.gymrace.GifData
import com.example.gymrace.R
import com.example.gymrace.addFriend
import com.example.gymrace.loadAllUsers
import com.example.gymrace.loadFriendsList
import com.example.gymrace.loadGifsFromXml
import com.example.gymrace.removeFriend
import com.example.gymrace.showExerciseDetail
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import np.com.bimalkafle.bottomnavigationdemo.pages.User
import np.com.bimalkafle.bottomnavigationdemo.pages.UsersDialog
import java.util.UUID

// Modelos de datos
data class Challenge(
    val id: String = "",
    val name: String,
    val description: String,
    val creatorId: String,
    val participantId: String,
    val exercise: String,
    val repetitions: Int,
    val creatorProgress: Int = 0,
    val participantProgress: Int = 0,
    val status: String // PENDING, ACCEPTED, IN_PROGRESS, COMPLETED
)

data class Friend(
    val id: String,
    val name: String,
    val profilePicUrl: String? = null
)

// ViewModel para Desafíos
class ChallengeViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _userFriends = MutableStateFlow<List<Friend>>(emptyList())
    val userFriends: StateFlow<List<Friend>> = _userFriends

    private val _friendsChallenges = MutableStateFlow<List<Challenge>>(emptyList())
    val friendsChallenges: StateFlow<List<Challenge>> = _friendsChallenges

    suspend fun loadFriends(userId: String) {
        try {
            val amigosDocSnapshot = firestore.collection("amigos")
                .document(userId)
                .get()
                .await()

            val friendsIds = amigosDocSnapshot.get("listaAmigos") as? List<String> ?: emptyList()
            val friendsList = mutableListOf<Friend>()
            Log.d("ChallengeViewModel", "Friends IDs: $friendsIds")

            for (friendId in friendsIds) {
                val friendUserSnapshot = firestore.collection("usuarios")
                    .document(friendId)
                    .get()
                    .await()

                if (friendUserSnapshot.exists()) {
                    val friendName = friendUserSnapshot.getString("nombre") ?: "Usuario"
                    friendsList.add(Friend(id = friendId, name = friendName))
                }
            }
            _userFriends.value = friendsList
        } catch (e: Exception) {
            Log.e("ChallengeViewModel", "Error loading friends: ${e.message}")
        }
    }

    suspend fun loadFriendsChallenges(userId: String) {
        try {
            val challengesSnapshot = firestore.collection("desafios")
                .whereEqualTo("creador.id", userId)
                .get()
                .await()

            val participantChallengesSnapshot = firestore.collection("desafios")
                .whereEqualTo("amigoInvitado.id", userId)
                .get()
                .await()

            val allChallenges = mutableListOf<Challenge>()

            challengesSnapshot.documents.mapNotNullTo(allChallenges) { document ->
                val creatorProgress = (document.get("creador.progreso") as? Long)?.toInt() ?: 0
                val participantProgress = (document.get("amigoInvitado.progreso") as? Long)?.toInt() ?: 0
                Challenge(
                    id = document.id,
                    name = document.getString("titulo") ?: "",
                    description = document.getString("descripcion") ?: "",
                    creatorId = document.getString("creador.id") ?: "",
                    participantId = document.getString("amigoInvitado.id") ?: "",
                    exercise = document.getString("ejercicio") ?: "",
                    repetitions = (document.getLong("repeticiones") ?: 0).toInt(),
                    creatorProgress = creatorProgress,
                    participantProgress = participantProgress,
                    status = document.getString("status") ?: "PENDING"
                )
            }

            participantChallengesSnapshot.documents.mapNotNullTo(allChallenges) { document ->
                val creatorProgress = (document.get("creador.progreso") as? Long)?.toInt() ?: 0
                val participantProgress = (document.get("amigoInvitado.progreso") as? Long)?.toInt() ?: 0
                Challenge(
                    id = document.id,
                    name = document.getString("titulo") ?: "",
                    description = document.getString("descripcion") ?: "",
                    creatorId = document.getString("creador.id") ?: "",
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
            Log.e("ChallengeViewModel", "Error loading challenges: ${e.message}")
        }
    }

    suspend fun createChallenge(challenge: Challenge) {
        try {
            val challengeData = if (challenge.id.isBlank()) {
                challenge.copy(id = UUID.randomUUID().toString())
            } else challenge

            val desafioData = mapOf(
                "creador" to mapOf("id" to challengeData.creatorId, "progreso" to 0),
                "amigoInvitado" to mapOf("id" to challengeData.participantId, "progreso" to 0),
                "titulo" to challengeData.name,
                "descripcion" to challengeData.description,
                "ejercicio" to challengeData.exercise,
                "repeticiones" to challengeData.repetitions,
                "status" to challengeData.status
            )

            firestore.collection("desafios")
                .document(challengeData.id)
                .set(desafioData)
                .await()

            loadFriendsChallenges(challenge.creatorId)
        } catch (e: Exception) {
            Log.e("ChallengeViewModel", "Error creating challenge: ${e.message}")
        }
    }

    suspend fun updateChallengeStatus(challengeId: String, newStatus: String, userId: String) {
        try {
            firestore.collection("desafios")
                .document(challengeId)
                .update("status", newStatus)
                .await()
            loadFriendsChallenges(userId)
        } catch (e: Exception) {
            Log.e("ChallengeViewModel", "Error updating challenge: ${e.message}")
        }
    }

    suspend fun updateProgress(challengeId: String, userId: String, newProgress: Int) {
        try {
            val challengeDoc = firestore.collection("desafios")
                .document(challengeId)
                .get()
                .await()

            val creatorId = challengeDoc.get("creador.id") as? String
            val participantId = challengeDoc.get("amigoInvitado.id") as? String

            val updateField = when (userId) {
                creatorId -> "creador.progreso"
                participantId -> "amigoInvitado.progreso"
                else -> throw Exception("Usuario no es parte de este desafío")
            }

            firestore.collection("desafios")
                .document(challengeId)
                .update(updateField, newProgress)
                .await()

            val repetitions = (challengeDoc.getLong("repeticiones") ?: 0).toInt()
            if (newProgress >= repetitions) {
                firestore.collection("desafios")
                    .document(challengeId)
                    .update("status", "COMPLETED")
                    .await()
            }
            loadFriendsChallenges(userId)
        } catch (e: Exception) {
            Log.e("ChallengeViewModel", "Error updating progress: ${e.message}")
        }
    }

    // Permite eliminar un desafío:
    // - Si está en progreso: solo el creador puede eliminarlo.
    // - Si está completado: cualquiera puede eliminarlo.
    suspend fun deleteChallenge(challengeId: String, userId: String, challengeStatus: String, creatorId: String) {
        try {
            if (challengeStatus != "COMPLETED" && userId != creatorId) {
                throw Exception("Solo el creador puede eliminar el desafío en progreso")
            }
            firestore.collection("desafios")
                .document(challengeId)
                .delete()
                .await()
            loadFriendsChallenges(userId)
        } catch (e: Exception) {
            Log.e("ChallengeViewModel", "Error deleting challenge: ${e.message}")
        }
    }
}

// Pantalla de Desafíos
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesafiosPage(
    viewModel: ChallengeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    userId: String
) {
    val challenges by viewModel.friendsChallenges.collectAsState(initial = emptyList())
    val friends by viewModel.userFriends.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var selectedChallenge by remember { mutableStateOf<Challenge?>(null) }
    var newProgress by remember { mutableStateOf("0") }

    // Estados para diálogos
    var showCommunityDialog by remember { mutableStateOf(false) }

    // Estados para el diálogo de comunidad y usuarios
    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoadingUsers by remember { mutableStateOf(false) }
    var friendsList by remember { mutableStateOf<List<User>>(emptyList()) }
    var showFriendsListDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()


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
    // Para refrescar cada 5 segundos
    LaunchedEffect(userId) {
        while (true) {
            viewModel.loadFriendsChallenges(userId)
            delay(5000)
        }
    }

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
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 80.dp, end = 8.dp)
                    .navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Desafío")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = 75.dp)
                .scrollable(
                    state = rememberScrollState(),
                    orientation = Orientation.Vertical
                )
        ) {
            if (challenges.isEmpty()) {
                EmptyState(onBuscarAmigos = { showCommunityDialog = true })
            }else {
                ChallengesList(
                    challenges = challenges,
                    userId = userId,
                    onAccept = { challenge ->
                        coroutineScope.launch {
                            viewModel.updateChallengeStatus(challenge.id, "ACCEPTED", userId)
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
                        coroutineScope.launch {
                            viewModel.deleteChallenge(challenge.id, userId, challenge.status, challenge.creatorId)
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
}

@Composable
fun EmptyState(onBuscarAmigos: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.fuego), // imagen central
            contentDescription = null,
            modifier = Modifier.size(80.dp),
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
            text = "Crea un desafío con el botón + o agrega amigos",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBuscarAmigos) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Buscar Amigos")
        }
    }
}


@Composable
fun ChallengesList(
    challenges: List<Challenge>,
    userId: String,
    onAccept: (Challenge) -> Unit,
    onUpdateProgress: (Challenge) -> Unit,
    onDeleteChallenge: (Challenge) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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

@Composable
fun ChallengeItem(
    challenge: Challenge,
    userId: String,
    onAccept: () -> Unit,
    onUpdateProgress: () -> Unit,
    onDeleteChallenge: () -> Unit
) {
    val isCreator = userId == challenge.creatorId
    val myProgress = if (isCreator) challenge.creatorProgress else challenge.participantProgress
    val otherProgress = if (isCreator) challenge.participantProgress else challenge.creatorProgress
    val otherName = if (isCreator) "Amigo" else "Creador"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
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
                    OutlinedButton(onClick = onAccept) { Text("Aceptar") }
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
                    challenge.participantProgress > challenge.creatorProgress -> "Amigo"
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

@Composable
fun StatusChip(status: String) {
    val (color, text) = when (status) {
        "PENDING" -> Pair(MaterialTheme.colorScheme.tertiary, "Pendiente")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChallengeDialog(
    currentUserId: String,
    friends: List<Friend>,
    onDismiss: () -> Unit,
    onCreateChallenge: (Challenge) -> Unit
) {
    var challengeName by remember { mutableStateOf("") }
    var challengeDescription by remember { mutableStateOf("") }
    var selectedFriendId by remember { mutableStateOf("") }
    var selectedFriendName by remember { mutableStateOf("Seleccionar amigo") }
    var selectedExercise by remember { mutableStateOf("") }
    var repetitions by remember { mutableStateOf("") }

    var expandedFriends by remember { mutableStateOf(false) }
    // Estado para mostrar el diálogo de selección mejorada de ejercicio
    var showEjerciciosDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear nuevo desafío") },
        text = {
            Column {
                OutlinedTextField(
                    value = challengeName,
                    onValueChange = { challengeName = it },
                    label = { Text("Nombre del desafío") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                OutlinedTextField(
                    value = challengeDescription,
                    onValueChange = { challengeDescription = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
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
                        label = { Text("Amigo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFriends) },
                        modifier = Modifier.fillMaxWidth().menuAnchor().padding(vertical = 8.dp)
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
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
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
                                color = if (selectedExercise.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
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
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
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
            TextButton(onClick = onDismiss) { Text("Cancelar") }
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

// Ejemplo de diálogo de ejercicios (basado en tu código de "Añadir Ejercicios")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjerciciosDialog(
    onDismissRequest: () -> Unit,
    onEjercicioSelected: (String) -> Unit
) {
    var buscarEjercicio by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }

    val context = LocalContext.current
    val (ejerciciosData, _) = remember {
        loadGifsFromXml(context.resources.getXml(R.xml.ejercicios), context)
    }

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


// Datos de ejemplo para cada ejercicio (ajusta según tu modelo real)
//data class ExerciseData(
//    val title: String,
//    val category: String = "Todos"
//)

// Chip de filtro para categorías
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
            .clip(MaterialTheme.shapes.small)
            .clickable { onToggleSelect(!isSelected) },
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
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
