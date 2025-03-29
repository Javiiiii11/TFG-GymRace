package com.example.gymrace.pages

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

// Modelos de datos
data class Challenge(
    val id: String = "",
    val name: String,
    val description: String,
    val creatorId: String,
    val participantId: String,
    val status: String // PENDING, ACCEPTED, COMPLETED
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
            // Accedemos al documento de amigos del usuario
            val amigosDocSnapshot = firestore.collection("amigos")
                .document(userId)
                .get()
                .await()

            // Obtenemos el arreglo "listamigos" que contiene los IDs de los amigos
            val friendsIds = amigosDocSnapshot.get("listaAmigos") as? List<String> ?: emptyList()
            val friendsList = mutableListOf<Friend>()
            Log.d("ChallengeViewModel", "Friends IDs: $friendsIds")
            Log.d("ChallengeViewModel", "Friends IDs size: ${friendsIds.size}")

            // Por cada ID, recuperamos la información del amigo desde la colección "usuarios"
            for (friendId in friendsIds) {
                val friendUserSnapshot = firestore.collection("usuarios")
                    .document(friendId)
                    .get()
                    .await()

                if (friendUserSnapshot.exists()) {
                    val friendName = friendUserSnapshot.getString("nombre") ?: "Usuario"

                    friendsList.add(
                        Friend(
                            id = friendId,
                            name = friendName,
                        )
                    )
                }
            }

            // Actualizamos el StateFlow con la lista de amigos obtenida
            _userFriends.value = friendsList

        } catch (e: Exception) {
            Log.e("ChallengeViewModel", "Error loading friends: ${e.message}")
        }
    }



    suspend fun loadFriendsChallenges(userId: String) {
        try {
            val challengesSnapshot = firestore.collection("desafios")
                .whereEqualTo("idCreador", userId)
                .get()
                .await()

            val participantChallengesSnapshot = firestore.collection("desafios")
                .whereEqualTo("amigoInvitado.id", userId)
                .get()
                .await()

            val allChallenges = mutableListOf<Challenge>()

            challengesSnapshot.documents.mapNotNullTo(allChallenges) { document ->
                Challenge(
                    id = document.id,
                    name = document.getString("titulo") ?: "",
                    description = document.getString("descripcion") ?: "",
                    creatorId = document.getString("idCreador") ?: "",
                    participantId = document.get("amigoInvitado.id") as? String ?: "",
                    status = document.getString("status") ?: "PENDING"
                )
            }

            participantChallengesSnapshot.documents.mapNotNullTo(allChallenges) { document ->
                Challenge(
                    id = document.id,
                    name = document.getString("titulo") ?: "",
                    description = document.getString("descripcion") ?: "",
                    creatorId = document.getString("idCreador") ?: "",
                    participantId = document.get("amigoInvitado.id") as? String ?: "",
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
            // Generamos un ID único si aún no lo tiene
            val challengeData = if (challenge.id.isBlank()) {
                challenge.copy(id = UUID.randomUUID().toString())
            } else {
                challenge
            }

            // Estructura de datos para la colección "desafios"
            val desafioData = mapOf(
                "idCreador" to challengeData.creatorId,
                "titulo" to challengeData.name,
                "descripcion" to challengeData.description,
                "amigoInvitado" to mapOf(
                    "id" to challengeData.participantId
                ),
                "status" to challengeData.status
            )

            // Guardamos en la colección "desafios" usando el ID generado
            firestore.collection("desafios")
                .document(challengeData.id)
                .set(desafioData)
                .await()

            // Actualizamos la lista de desafíos (si la función loadFriendsChallenges utiliza la colección "desafios", asegúrate de modificarla)
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

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedChallengeName by remember { mutableStateOf("") }
    var selectedChallengeDescription by remember { mutableStateOf("") }
    var selectedFriendId by remember { mutableStateOf("") }
    var selectedFriendName by remember { mutableStateOf("Seleccionar amigo") }


    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Desafíos") },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Crear Desafío")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (challenges.isEmpty()) {
                EmptyState()
            } else {
                ChallengesList(challenges = challenges, viewModel = viewModel, userId = userId)
            }

            // Dialog for creating challenges
            if (showCreateDialog) {
                CreateChallengeDialog(
                    friends = friends,
                    selectedFriendName = selectedFriendName,
                    onDismiss = { showCreateDialog = false },
                    onChallengeName = { selectedChallengeName = it },
                    onChallengeDescription = { selectedChallengeDescription = it },
                    onFriendSelected = { friendId, friendName ->
                        selectedFriendId = friendId
                        selectedFriendName = friendName
                    },
                    onCreateChallenge = {
                        coroutineScope.launch {
                            viewModel.createChallenge(
                                Challenge(
                                    name = selectedChallengeName,
                                    description = selectedChallengeDescription,
                                    creatorId = userId,
                                    participantId = selectedFriendId,
                                    status = "PENDING"
                                )
                            )
                            showCreateDialog = false
                            // Reseteamos los estados
                            selectedChallengeName = ""
                            selectedChallengeDescription = ""
                            selectedFriendId = ""
                            selectedFriendName = "Seleccionar amigo"
                        }
                    }
                )
            }

        }
    }

    // Load data when the page is first composed
    LaunchedEffect(userId) {
        viewModel.loadFriends(userId)
        viewModel.loadFriendsChallenges(userId)
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
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
    }
}

@Composable
fun ChallengesList(
    challenges: List<Challenge>,
    viewModel: ChallengeViewModel,
    userId: String
) {
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(challenges) { challenge ->
            ChallengeItem(
                challenge = challenge,
                onAccept = {
                    scope.launch {
                        viewModel.updateChallengeStatus(challenge.id, "ACCEPTED", userId)
                    }
                },
                onComplete = {
                    scope.launch {
                        viewModel.updateChallengeStatus(challenge.id, "COMPLETED", userId)
                    }
                }
            )
        }
    }
}

@Composable
fun ChallengeItem(
    challenge: Challenge,
    onAccept: () -> Unit,
    onComplete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = challenge.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = challenge.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(status = challenge.status)
                when (challenge.status) {
                    "PENDING" -> OutlinedButton(onClick = onAccept) {
                        Text("Aceptar")
                    }
                    "ACCEPTED" -> OutlinedButton(onClick = onComplete) {
                        Text("Completar")
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (color, text) = when (status) {
        "PENDING" -> Pair(MaterialTheme.colorScheme.tertiary, "Pendiente")
        "ACCEPTED" -> Pair(MaterialTheme.colorScheme.primary, "En progreso")
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
    friends: List<Friend>,
    selectedFriendName: String,
    onDismiss: () -> Unit,
    onChallengeName: (String) -> Unit,
    onChallengeDescription: (String) -> Unit,
    onFriendSelected: (String, String) -> Unit, // Recibe id y nombre
    onCreateChallenge: () -> Unit
) {
    var challengeName by remember { mutableStateOf("") }
    var challengeDescription by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear nuevo desafío") },
        text = {
            Column {
                OutlinedTextField(
                    value = challengeName,
                    onValueChange = {
                        challengeName = it
                        onChallengeName(it)
                    },
                    label = { Text("Nombre del desafío") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                OutlinedTextField(
                    value = challengeDescription,
                    onValueChange = {
                        challengeDescription = it
                        onChallengeDescription(it)
                    },
                    label = { Text("Descripción") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedFriendName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        friends.forEach { friend ->
                            DropdownMenuItem(
                                text = { Text(friend.name) },
                                onClick = {
                                    onFriendSelected(friend.id, friend.name)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCreateChallenge,
                enabled = challengeName.isNotBlank() && challengeDescription.isNotBlank() && selectedFriendName != "Seleccionar amigo"
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
}
