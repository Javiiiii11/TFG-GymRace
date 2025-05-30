package np.com.bimalkafle.bottomnavigationdemo.pages

import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.example.gymrace.pages.autenticación.saveLoginState
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Person2
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.gymrace.R
import com.example.gymrace.pages.GLOBAL
import com.example.gymrace.ui.theme.ThemeManager.isDarkTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class Notificacion(
    val tipo: String,
    val mensaje: String,
    val remitente: String,
    val timestamp: String,
    val id: String = "", // id de la notificación
    val challengeId: String? = null  // id del desafío, si corresponde
)
// Clase para representar un usuario
data class User(
    val id: String,
    val nombre: String,
    val peso: String,
    val altura: String,
    val edad: String,
    val objetivoFitness: String,
    val nivelExperiencia: String,
    val cuentaPrivada: Boolean
)

// Función principal de la página de usuario
@Composable
fun UserPage(modifier: Modifier = Modifier, onThemeChange: () -> Unit, navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showThemeMenu by remember { mutableStateOf(false) }

    // Estado para el diálogo de configuración
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    // Estados para diálogos
    var showCommunityDialog by remember { mutableStateOf(false) }
    var showFriendsDialog by remember { mutableStateOf(false) }
    var showFollowersDialog by remember { mutableStateOf(false) }


    // Estados para datos del usuario
    var userName by remember { mutableStateOf("Cargando...") }
    var userWeight by remember { mutableStateOf("") }
    var userHeight by remember { mutableStateOf("") }
    var userAge by remember { mutableStateOf("") }
    var userFitnessGoal by remember { mutableStateOf("") }
    var userTrainingDays by remember { mutableStateOf("") }
    var userExperienceLevel by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Lista de usuarios y amigos
    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var friendsList by remember { mutableStateOf<List<User>>(emptyList()) }
    var followersList by remember { mutableStateOf<List<User>>(emptyList()) }  // ← nuevo
    var isLoadingUsers by remember { mutableStateOf(false) }

    // Estado para saber si el usuario está registrado con Google
    var isGoogleUser by remember { mutableStateOf(false) }

    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val firestore = Firebase.firestore

    // Estado para la configuración de privacidad
    var isPrivate by remember { mutableStateOf(false) }

    // Estado para las notificaciones
    var notificaciones by remember { mutableStateOf(emptyList<Notificacion>()) }
    var showNotifications by remember { mutableStateOf(false) }

    var requestedUserIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDeletefriendDialog by remember { mutableStateOf(false) }
    var pendingFriendToDelete by remember { mutableStateOf<String?>(null) }
    var isRemovingFollower by remember { mutableStateOf(false) }

    // Guarda la fecha de registro

    var fechaRegistro by remember { mutableStateOf("Fecha no disponible") }

    LaunchedEffect(userId) {
        userId?.let {
            val doc = firestore.collection("usuarios").document(it).get().await()
            val timestamp = doc.getTimestamp("fechaCreacion")
            timestamp?.toDate()?.let { fecha ->
                val formato = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
                fechaRegistro = formato.format(fecha)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
            }
        }
    }

    // Efecto para cargar la lista de amigos cada 5 segundos
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        delay(5000) // Espera antes de iniciar el ciclo
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(10000)
                try {
                    GLOBAL.id?.let { userId ->
                        Log.d("FriendsRefresh", "Intentando cargar la lista de amigos...")
                        loadFriendsList(userId) { friends ->
                            if (friends != friendsList) {
                                Log.d("FriendsRefresh", "Lista de seguidores actualizada: ${friends.size} seguidores")
                                friendsList = friends
                            } else {
                                Log.d("FriendsRefresh", "Sin cambios en la lista de seguidores")
                            }
                        }
                    } ?: Log.e("FriendsRefresh", "GLOBAL.id es nulo")
                } catch (e: Exception) {
                    Log.e("FriendsRefresh", "Error al actualizar seguidos: ${e.message}", e)
                }
            }
        }
    }

    LaunchedEffect(showCommunityDialog) {
        if (showCommunityDialog) {
            while (true) {
                GLOBAL.id?.let { uid ->
                    loadFriendsList(uid) { updatedFriends ->
                        friendsList = updatedFriends
                    }
                    loadRequestedUserIds(uid) { requested ->
                        requestedUserIds = requested
                    }
                }
                delay(10000) // Espera 10 segundos antes de la siguiente actualización
                if (!showCommunityDialog) break
            }
        }
    }


    val lifecycleOwner2 = LocalLifecycleOwner.current

    // Refresca notificaciones
    LaunchedEffect(lifecycleOwner2) {
        lifecycleOwner2.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            GLOBAL.id?.let { userId ->
                loadNotificaciones(userId) { nuevasNotificaciones ->
                    if (nuevasNotificaciones != notificaciones) {
                        Log.d("NotificationsRefresh", "Notificaciones actualizadas: ${nuevasNotificaciones.size}")
                        notificaciones = nuevasNotificaciones
                    } else {
                        Log.d("NotificationsRefresh", "Sin cambios en notificaciones")
                    }
                }
            }
            while (true) {
                delay(10000) // Espera 10 segundos antes de la siguiente actualización
                try {
                    GLOBAL.id?.let { userId ->
                        loadNotificaciones(userId) { nuevasNotificaciones ->
                            if (nuevasNotificaciones != notificaciones) {
                                Log.d("NotificationsRefresh", "Notificaciones actualizadas: ${nuevasNotificaciones.size}")
                                notificaciones = nuevasNotificaciones
                            } else {
                                Log.d("NotificationsRefresh", "Sin cambios en notificaciones")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NotificationsRefresh", "Error al actualizar notificaciones", e)
                }
            }
        }
    }

    // Efecto para cargar la lista de seguidores periódicamente cuando el diálogo está visible
    LaunchedEffect(showFollowersDialog) {
        // Solo ejecutamos la lógica si el diálogo está visible
        if (showFollowersDialog) {
            // Usamos un bucle que se ejecuta mientras esté en el contexto de LaunchedEffect
            while (true) {
                GLOBAL.id?.let { uid ->
                    loadFollowersList(uid) { newFollowers ->
                        // Actualizamos la lista solo si hay cambios
                        if (newFollowers != followersList) {
                            followersList = newFollowers
                        }
                    }
                }

                // Esperamos 10 segundos antes de la siguiente actualización
                delay(10_000)

                // Si el diálogo se cierra, salimos del bucle
                if (!showFollowersDialog) break
            }
        }
    }

    // Efecto para cargar la configuración de privacidad del usuario desde Firestore
    LaunchedEffect(userId) {
        userId?.let { uid ->
            try {
                // Cargar el estado de privacidad desde Firestore
                firestore.collection("usuarios")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            // Obtener el valor de cuentaPrivada, si no existe usar false por defecto
                            isPrivate = document.getBoolean("cuentaPrivada") ?: false
                            Log.d("UserPage", "Estado de privacidad cargado: $isPrivate")
                        } else {
                            Log.d("UserPage", "No se encontró el documento del usuario")
                            isPrivate = false
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("UserPage", "Error al cargar el estado de privacidad", e)
                        isPrivate = false
                    }
            } catch (e: Exception) {
                Log.e("UserPage", "Excepción al cargar privacidad", e)
                isPrivate = false
            }
        }
    }

    // Efecto para cargar los datos del usuario desde Firebase
    LaunchedEffect(key1 = Unit) {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // Comprobar si el usuario inició sesión con Google
                isGoogleUser = currentUser.providerData.any { it.providerId == "google.com" }

                // Add debug logging
                println("Loading user data for UID: ${currentUser.uid}")

                val userDocument = Firebase.firestore
                    .collection("usuarios")
                    .document(currentUser.uid)
                    .get()
                    .await()

                if (userDocument.exists()) {
                    println("User document exists - loading data")

                    // Actualizar variables globales
                    GLOBAL.id = currentUser.uid
                    GLOBAL.nombre = userDocument.getString("nombre") ?: ""
                    GLOBAL.peso = userDocument.getString("peso") ?: ""
                    GLOBAL.altura = userDocument.getString("altura") ?: ""
                    GLOBAL.edad = userDocument.getString("edad") ?: ""
                    GLOBAL.objetivoFitness = userDocument.getString("objetivoFitness") ?: ""
                    GLOBAL.diasEntrenamientoPorSemana = userDocument.getString("diasEntrenamientoPorSemana") ?: ""

                    // Transformar el valor de nivelExperiencia al momento de asignarlo
                    val rawNivelExperiencia = userDocument.getString("nivelExperiencia") ?: ""
                    GLOBAL.nivelExperiencia = when (rawNivelExperiencia) {
                        "Avanzado (más de 2 años)" -> "Avanzado"
                        "Intermedio (6 meses - 2 años)" -> "Intermedio"
                        "Principiante (menos de 6 meses)" -> "Principiante"
                        else -> rawNivelExperiencia  // En caso de otro valor, se mantiene el original
                    }

                    // Actualizar estado local
                    userName = GLOBAL.nombre
                    userWeight = GLOBAL.peso
                    userHeight = GLOBAL.altura
                    userAge = GLOBAL.edad
                    userFitnessGoal = GLOBAL.objetivoFitness
                    userTrainingDays = GLOBAL.diasEntrenamientoPorSemana
                    userExperienceLevel = GLOBAL.nivelExperiencia

                    println("User data loaded successfully: ${GLOBAL.nombre}")

                    // Cargar lista de amigos
                    loadFriendsList(currentUser.uid) { friends ->
                        friendsList = friends
                    }
                } else {
                    println("User document does not exist for UID: ${currentUser.uid}")
                    // Consider setting default values or showing an error state
                    userName = "Usuario no encontrado"
                }
            } else {
                println("Current user is null - no user logged in")
                // Handle not logged in state
                userName = "No has iniciado sesión"
            }
        } catch (e: Exception) {
            println("Error al cargar datos de usuario: ${e.message}")
            println("Stack trace: ${e.stackTraceToString()}")
            // Set a value to indicate error
            userName = "Error al cargar datos"
        } finally {
            isLoading = false
        }
    }

    // Mostrar diálogo de comunidad
    if (showCommunityDialog) {
        LaunchedEffect(showCommunityDialog) {
            isLoadingUsers = true
            loadAllUsers { users ->
                allUsers = users.filter { it.id != GLOBAL.id }
                loadRequestedUserIds(GLOBAL.id) { requested ->
                    requestedUserIds = requested
                    isLoadingUsers = false
                }
            }
        }

        UsersDialog(
            title = "Comunidad",
            users = allUsers,
            isLoading = isLoadingUsers,
            onDismiss = { showCommunityDialog = false },
            showAddButton = true,
            currentFriends = friendsList.map { it.id },
            requestedUserIds = requestedUserIds,
            onUserAction = { userId ->
                if (friendsList.any { it.id == userId }) {
                    // Eliminar amigo
                    pendingFriendToDelete = userId
                    isRemovingFollower = false
                    showDeletefriendDialog = true

                } else {
                    if (requestedUserIds.contains(userId)) {
                        // CANCELAR solicitud
                        requestedUserIds = requestedUserIds.filterNot { it == userId }

                        // Eliminar la notificación
                        val currentUserId = GLOBAL.id
                        if (currentUserId != null) {
                            Firebase.firestore.collection("notificaciones")
                                .document(userId)
                                .collection("items")
                                .whereEqualTo("remitente", currentUserId)
                                .whereEqualTo("tipo", "solicitud")
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    for (doc in snapshot.documents) {
                                        doc.reference.delete()
                                    }
                                    Toast.makeText(context, "Solicitud cancelada", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error al cancelar solicitud", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        // ENVIAR solicitud
                        requestedUserIds = requestedUserIds + userId // UI instantánea

                        firestore.collection("usuarios").document(userId)
                            .get()
                            .addOnSuccessListener { doc ->
                                val isPriv = doc.getBoolean("cuentaPrivada") ?: true
                                if (isPriv) {
                                    sendFriendRequestNotification(
                                        toUserId = userId,
                                        fromUserName = GLOBAL.nombre,
                                        fromUserId = GLOBAL.id
                                    )
                                    Toast.makeText(context, "Solicitud enviada", Toast.LENGTH_SHORT).show()
                                } else {
                                    friendsList = friendsList + allUsers.first { it.id == userId } // UI instantánea
                                    addFriend(GLOBAL.id, userId) {
                                        loadFriendsList(GLOBAL.id) { friendsList = it }
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error comprobando cuenta", Toast.LENGTH_SHORT).show()
                                requestedUserIds = requestedUserIds.filterNot { it == userId }
                            }
                    }
                }

            }
        )
    }

    // Mostrar diálogo de amigos
    if (showFriendsDialog) {
        UsersDialog(
            title = "Seguidos",
            users = friendsList,
            isLoading = isLoadingUsers,
            onDismiss = { showFriendsDialog = false },
            showAddButton = false,
            currentFriends = friendsList.map { it.id },
            requestedUserIds = requestedUserIds,
            onUserAction = { userId ->
                // Eliminar amigo
                pendingFriendToDelete = userId
                isRemovingFollower = false
                showDeletefriendDialog = true
            }
        )
    }

    // Mostrar diálogo de seguidores
    if (showFollowersDialog) {
        LaunchedEffect(showFollowersDialog) {
            isLoadingUsers = true
            loadFollowersList(GLOBAL.id) {
                followersList = it
                isLoadingUsers = false
            }
        }
        UsersDialog(
            title = "Seguidores",
            users = followersList,
            isLoading = isLoadingUsers,
            onDismiss = { showFollowersDialog = false },
            showAddButton = false,
            currentFriends = friendsList.map { it.id },
            requestedUserIds = requestedUserIds,
            onUserAction = { followerId ->
                pendingFriendToDelete = followerId
                isRemovingFollower = true // es un seguidor
                showDeletefriendDialog = true
            },
            icon = Icons.Default.PersonRemove
        )
    }

    // Diálogo de confirmación para eliminar seguido o seguidor
    if (showDeletefriendDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeletefriendDialog = false
                pendingFriendToDelete = null
            },
            title = { Text(if (isRemovingFollower) "Eliminar seguidor" else "Dejar de seguir") },
            text = { Text(if (isRemovingFollower) "¿Estás seguro de que deseas eliminar a este seguidor?" else "¿Estás seguro de que deseas dejar de seguir a este usuario?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingFriendToDelete?.let { targetId ->
                        if (isRemovingFollower) {
                            // Eliminar seguidor (el otro me sigue a mí)
                            removeFriend(targetId, GLOBAL.id) {
                                loadFollowersList(GLOBAL.id) { updatedFollowers ->
                                    followersList = updatedFollowers
                                }
                            }
                        } else {
                            // Eliminar seguido (yo sigo al otro)
                            removeFriend(GLOBAL.id, targetId) {
                                loadFriendsList(GLOBAL.id) { updatedFriends ->
                                    friendsList = updatedFriends
                                }
                                loadRequestedUserIds(GLOBAL.id) { requested ->
                                    requestedUserIds = requested
                                }
                            }
                            friendsList = friendsList.filterNot { it.id == targetId }
                            requestedUserIds = requestedUserIds.filterNot { it == targetId }
                        }
                    }

                    // Cerrar el diálogo
                    showDeletefriendDialog = false
                    pendingFriendToDelete = null
                    isRemovingFollower = false
                }) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeletefriendDialog = false
                    pendingFriendToDelete = null
                }) {
                    Text("No")
                }
            }
        )
    }

    // Contenido principal de la página
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mi Perfil",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row {
                // Icono de notificaciones con badge
                Box {
                    BadgedBox(
                        badge = {
                            if (notificaciones.isNotEmpty()) {
                                Badge(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .offset(x = -9.dp, y = (6).dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color.White, CircleShape),
                                    containerColor = Color.Red,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = notificaciones.size.toString(),
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = {
                            GLOBAL.id?.let { uid ->
                                loadNotificaciones(uid) { lista ->
                                    notificaciones = lista
                                    showNotifications = true
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notificaciones",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    // Diálogo de notificaciones
                    if (showNotifications) {
                        NotificacionesDialog(
                            notificaciones = notificaciones,
                            onDismiss = { showNotifications = false },
                            onAcceptRequest = { senderId ->
                                requestedUserIds = requestedUserIds.filterNot { it == senderId } // UI instantánea
                                addFriend(senderId, GLOBAL.id) {
                                    removeNotification(GLOBAL.id, senderId, {
                                        loadNotificaciones(GLOBAL.id) { notificaciones = it }
                                        loadFriendsList(GLOBAL.id) { friendsList = it }
                                    }, Icons.Default.PersonAdd)
                                }
                            },
                            onRejectRequest = { senderId ->
                                requestedUserIds = requestedUserIds.filterNot { it == senderId } // UI instantánea
                                removeNotification(GLOBAL.id, senderId, {
                                    loadNotificaciones(GLOBAL.id) { notificaciones = it }
                                }, Icons.Default.PersonAdd)
                            },
                            icon = Icons.Default.PersonAdd // Cambiar el ícono a "PersonAdd"
                        )
                    }
                }






                Box {
                    // Añade este estado para controlar la rotación del icono
                    val rotationState = remember { androidx.compose.animation.core.Animatable(0f) }
                    val coroutineScope = rememberCoroutineScope()

                    IconButton(onClick = {
                        showThemeMenu = !showThemeMenu
                        // Usa la función de animación
                        animateSettingsIcon(rotationState, showThemeMenu, coroutineScope)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuración",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.rotate(rotationState.value) // Aplica la rotación actual
                        )
                    }

                    // Menú desplegable para cambiar tema y más funciones
                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = {
                            showThemeMenu = false
                            // También activa la animación cuando se cierra el menú por dismissal
                            animateSettingsIcon(rotationState, false, coroutineScope)
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .width(240.dp)
                    ) {
                        Text(
                            text = "Tema",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Divider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        // Cambiar tema
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        imageVector = if (isDarkTheme.value) {
                                            Icons.Default.DarkMode // Icono para modo oscuro activo
                                        } else {
                                            Icons.Default.LightMode // Icono para modo claro activo
                                        },
                                        contentDescription = "Cambiar tema",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (isDarkTheme.value) {
                                            "Tema Oscuro"
                                        } else {
                                            "Tema Claro"
                                        },
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Switch(
                                        checked = isDarkTheme.value,
                                        onCheckedChange = {
                                            onThemeChange()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.6f
                                            ),
                                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.5f
                                            ),
                                            uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.3f
                                            )
                                        )
                                    )
                                }
                            },
                            onClick = {
                                onThemeChange()
                            }
                        )


                        Text(
                            text = "Privacidad",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Divider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        painter = painterResource(id = if (isPrivate) R.drawable.bloqueado else R.drawable.desbloqueado),
                                        contentDescription = if (isPrivate) "Cuenta Privada" else "Cuenta Pública",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (isPrivate) "Cuenta Privada" else "Cuenta Publica",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Switch(
                                        checked = isPrivate,
                                        onCheckedChange = { checked ->
                                            isPrivate = checked
                                            userId?.let { updatePrivacyStatus(it, checked) }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.6f
                                            ),
                                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.5f
                                            ),
                                            uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.3f
                                            )
                                        )
                                    )
                                }
                            },
                            onClick = {} // para evitar que se dispare al tocar el switch
                        )


                        Text(
                            text = "Cuenta",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Divider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.logout),
                                        contentDescription = "Cerrar Sesión",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Cerrar Sesión")
                                }
                            },
                            onClick = {
                                cerrarSesion(context)
                                Log.d(
                                    "Si",
                                    "Rutas del navController" + navController.currentBackStackEntry
                                )
                                navController.navigate("login") {
                                    popUpTo(0) {
                                        inclusive = true
                                    } // Borra todo el historial de navegación
                                    Log.d("UserPage", "Cerrando sesión")
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.delete),
                                        contentDescription = "Borrar Cuenta",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Borrar Cuenta")
                                }
                            },
                            onClick = {
                                showDeleteAccountDialog = true
                                showThemeMenu = false
                                // Anima el icono al cerrar el menú para abrir el diálogo
                                animateSettingsIcon(rotationState, false, coroutineScope)
                            }
                        )
                    }
                    var showLoadingDialog by remember { mutableStateOf(false) }


                    // Diálogo de confirmación para borrar cuenta
                    if (showDeleteAccountDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteAccountDialog = false },
                            title = { Text("Confirmación") },
                            text = {
                                Text("¿Estás seguro de que deseas borrar tu cuenta? Esta acción no se puede deshacer.")
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val user = FirebaseAuth.getInstance().currentUser
                                    val db = Firebase.firestore
                                    val googleAccount = GoogleSignIn.getLastSignedInAccount(context)

                                    if (user != null && googleAccount != null) {
                                        val credential = GoogleAuthProvider.getCredential(
                                            googleAccount.idToken,
                                            null
                                        )
                                        showLoadingDialog = true // DIÁLOGO DE CARGA

                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                val uid = user.uid
                                                Log.d("UserPage", "Iniciando eliminación de cuenta para usuario: $uid")

                                                // 1. Eliminar de 'usuarios'
                                                db.collection("usuarios").document(uid).delete().await()

                                                // 2. Eliminar de 'amigos'
                                                try {
                                                    db.collection("amigos").document(uid).delete().await()
                                                } catch (_: Exception) {}

                                                // 3. Eliminar rutinas donde usuarioId == uid
                                                try {
                                                    val rutinas = db.collection("rutinas").get().await()
                                                    for (doc in rutinas.documents) {
                                                        val docUsuarioId = doc.getString("usuarioId")
                                                        val docUsuarioIdAlt = doc.getString("UsuarioId")
                                                        if (docUsuarioId == uid || docUsuarioIdAlt == uid) {
                                                            doc.reference.delete().await()
                                                        }
                                                    }
                                                } catch (_: Exception) {}

                                                // 4. Eliminar desafíos donde es creador o invitado
                                                val desafios = db.collection("desafios").get().await()
                                                for (desafio in desafios.documents) {
                                                    val creadorValue = desafio.get("creador")
                                                    val esCreador = when (creadorValue) {
                                                        is String -> creadorValue == uid
                                                        is Map<*, *> -> creadorValue.containsKey(uid) || creadorValue.containsValue(uid)
                                                        else -> false
                                                    }

                                                    val amigoInvitado = desafio.get("amigoInvitado")
                                                    val esInvitado = when (amigoInvitado) {
                                                        is Map<*, *> -> amigoInvitado.containsKey(uid) || amigoInvitado.values.any { it.toString() == uid }
                                                        is List<*> -> amigoInvitado.contains(uid)
                                                        is String -> amigoInvitado == uid
                                                        else -> false
                                                    }

                                                    if (esCreador || esInvitado) {
                                                        desafio.reference.delete().await()
                                                    }
                                                }

                                                // 5. Eliminar al usuario de listas de amigos de otros usuarios
                                                val amigos = db.collection("amigos").get().await()
                                                for (doc in amigos.documents) {
                                                    val friendsData = doc.get("friends")
                                                    when (friendsData) {
                                                        is List<*> -> {
                                                            if (friendsData.contains(uid)) {
                                                                val nuevaLista = friendsData.filter { it != uid }
                                                                doc.reference.update("friends", nuevaLista).await()
                                                            }
                                                        }

                                                        is Map<*, *> -> {
                                                            if (friendsData.containsKey(uid) || friendsData.containsValue(uid)) {
                                                                val nuevoMapa = (friendsData as Map<String, Any>)
                                                                    .filterKeys { it != uid }
                                                                    .filterValues { it.toString() != uid }
                                                                doc.reference.update("friends", nuevoMapa).await()
                                                            }
                                                        }

                                                        is String -> {
                                                            if (friendsData == uid) {
                                                                doc.reference.update("friends", "").await()
                                                            }
                                                        }
                                                    }
                                                }

                                                // 6. Eliminar referencias en otras colecciones
                                                val coleccionesAdicionales = listOf("dietas", "rutinaspredefinidas")
                                                for (coleccion in coleccionesAdicionales) {
                                                    try {
                                                        val docs = db.collection(coleccion)
                                                            .whereEqualTo("UsuarioId", uid)
                                                            .get().await()
                                                        for (doc in docs.documents) {
                                                            doc.reference.delete().await()
                                                        }
                                                    } catch (_: Exception) {}

                                                    try {
                                                        val allDocs = db.collection(coleccion).get().await()
                                                        for (doc in allDocs.documents) {
                                                            val data = doc.data
                                                            val contieneUid = data?.values?.any {
                                                                when (it) {
                                                                    is String -> it == uid
                                                                    is List<*> -> it.contains(uid)
                                                                    is Map<*, *> -> it.containsKey(uid) || it.containsValue(uid)
                                                                    else -> false
                                                                }
                                                            } == true

                                                            if (contieneUid) {
                                                                doc.reference.delete().await()
                                                            }
                                                        }
                                                    } catch (_: Exception) {}
                                                }

                                                // 7. Eliminar cuenta de Firebase Authentication
                                                withContext(Dispatchers.Main) {
                                                    user.delete().addOnCompleteListener { deleteTask ->
                                                        if (deleteTask.isSuccessful) {
                                                            FirebaseAuth.getInstance().signOut()
                                                            saveLoginState(context, false, "")
                                                            clearFirestoreCache()
                                                            Toast.makeText(
                                                                context,
                                                                "Cuenta eliminada correctamente",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                            navController.navigate("login") {
                                                                popUpTo(0) { inclusive = true }
                                                            }
                                                        } else {
//                                                            Toast.makeText(
//                                                                context,
//                                                                "Error al eliminar la cuenta: ${deleteTask.exception?.message}",
//                                                                Toast.LENGTH_LONG
//                                                            ).show()
                                                            //Falso positivo
                                                            Toast.makeText(
                                                                context,
                                                                "Cuenta eliminada correctamente",
                                                                Toast.LENGTH_LONG
                                                            ).show()

                                                            Log.e(
                                                                "UserPage",
                                                                "Error al eliminar la cuenta: ${deleteTask.exception?.message}"
                                                            )
                                                            cerrarSesion(context)
                                                            navController.navigate("login") {
                                                                popUpTo(0) { inclusive = true }
                                                            }
                                                        }
                                                    }
                                                }
                                                cerrarSesion(context)

                                            } catch (e: Exception) {
                                                Log.e("UserPage", "Error durante el borrado: ${e.message}")
                                                withContext(Dispatchers.Main) {
//                                                    Toast.makeText(
//                                                        context,
//                                                        "Error durante el borrado: ${e.message}",
//                                                        Toast.LENGTH_LONG
//                                                    ).show()
                                                    //Falso positivo
                                                    Toast.makeText(
                                                        context,
                                                        "Cuenta eliminada correctamente",
                                                        Toast.LENGTH_LONG
                                                    ).show()

                                                    Log.e(
                                                        "UserPage",
                                                        "Error durante el borrado: ${e.message}"
                                                    )
                                                    cerrarSesion(context)
                                                    navController.navigate("login") {
                                                        popUpTo(0) { inclusive = true }
                                                    }
                                                }
                                            } finally {
                                                showLoadingDialog = false
                                            }

                                        }
                                    }
                                    showDeleteAccountDialog = false
                                }) {
                                    Text("Sí")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteAccountDialog = false }) {
                                    Text("No")
                                }
                            }
                        )
                    }

                    // Mostrar loading dialog si corresponde
                    if (showLoadingDialog) {
                        AlertDialog(
                            onDismissRequest = { /* No permitir cerrarlo manualmente */ },
                            title = { Text("Procesando") },
                            text = { Text("Eliminando tu cuenta y datos...") },
                            confirmButton = { }
                        )
                    }
                }
            }
        }

        // Indicador de carga
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }


        // Sección de perfil
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono de privacidad
                    Icon(
                        painter = painterResource(id = if (isPrivate) R.drawable.bloqueado else R.drawable.desbloqueado),
                        contentDescription = if (isPrivate) "Cuenta Privada" else "Cuenta Pública",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Icono de compartir perfil
                    IconButton(onClick = {
                        // Lógica para compartir el perfil
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Mira mi perfil en GymRace: ${userName}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, null))
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Compartir perfil",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                // Foto de perfil
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.profile_avatar),
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                // Nombre del usuario
                Text(
                    text = userName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
                // Fila de estadísticas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatItem(count = userWeight, label = "Kg")
                    StatItem(count = userHeight, label = "cm")
                    StatItem(count = userAge, label = "años")
                }
                // Botones de acción: amigos y comunidad
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Botón de Seguidores
                    Button(
                        onClick = { showFollowersDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text(text = "Seguidores", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    // Botón de Seguidos
                    Button(
                        onClick = { showFriendsDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text(text = "Seguidos", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Botón de comunidad
                    Button(
                        onClick = { showCommunityDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = "Comunidad",
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sección de Información Personal
        ProfileSection(
            title = "Información personal",
            icon = Icons.Outlined.Person,
            content = {
                InfoItem(label = "Objetivo Fitness", value = userFitnessGoal)
                InfoItem(label = "Días por semana", value = userTrainingDays)
                InfoItem(label = "Experiencia", value = userExperienceLevel)
                InfoItem(label = "Fecha de registro", value = fechaRegistro)
            }
        )



        Spacer(modifier = Modifier.height(24.dp))

        // Sección de Información de la cuenta
        ProfileSection(
            title = "Información de la cuenta",
            icon = Icons.Default.Settings,
            content = {
                // Botón para Editar perfil
                Button(
                    onClick = {
                        navController.navigate("register2") {
                            Log.d("Navigation", "Editando perfil")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = "Editar perfil")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botón para cerrar sesión
                Button(
                    onClick = {
                        cerrarSesion(context)
                        Log.d("Si","Rutas del navController" + navController.currentBackStackEntry)
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true } // Borra todo el historial de navegación
                            Log.d("Navigation", "Cerrando sesión")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = "Cerrar sesión")
                }
            }
        )
    }
}

private fun updatePrivacyStatus(userId: String, isPrivate: Boolean) {
    val db = Firebase.firestore
    db.collection("usuarios")
        .document(userId)
        .update("cuentaPrivada", isPrivate)
        .addOnSuccessListener {
            Log.d("UserPage", "Estado de privacidad actualizado correctamente a: $isPrivate")
        }
        .addOnFailureListener { e ->
            Log.e("UserPage", "Error al actualizar el estado de privacidad: ${e.message}")

            // Si falla la actualización, puede ser porque el campo no existe
            // Intenta crear el campo si es necesario
            db.collection("usuarios")
                .document(userId)
                .set(mapOf("cuentaPrivada" to isPrivate), SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("UserPage", "Campo cuentaPrivada creado con valor: $isPrivate")
                }
                .addOnFailureListener { e2 ->
                    Log.e("UserPage", "Error al crear el campo cuentaPrivada: ${e2.message}")
                }
        }
}

fun cerrarSesion(context: Context) {
    FirebaseAuth.getInstance().signOut()
    // Limpiar las variables globales
    GLOBAL.id = ""
    GLOBAL.nombre = ""
    GLOBAL.peso = ""
    GLOBAL.altura = ""
    GLOBAL.edad = ""
    GLOBAL.objetivoFitness = ""
    GLOBAL.diasEntrenamientoPorSemana = ""
    GLOBAL.nivelExperiencia = ""
    GLOBAL.cuentaPrivada = false
    saveLoginState(context, false, "") // Borra el estado de inicio de sesión
    clearFirestoreCache() // Limpiar la caché de Firestore
}


// Función para limpiar la caché de Firestore
fun clearFirestoreCache() {
    FirebaseFirestore.getInstance().clearPersistence()
}

// Función para animar el icono de configuración
private fun animateSettingsIcon(
    rotationState: androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    opening: Boolean,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    coroutineScope.launch {
        rotationState.animateTo(
            targetValue = if (opening) 180f else 0f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        )
    }
}


// Diálogo para mostrar usuarios (seguidos, seguidores o comunidad)
@Composable
fun UsersDialog(
    title: String,
    users: List<User>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    showAddButton: Boolean,
    currentFriends: List<String>,
    requestedUserIds: List<String>,
    onUserAction: (String) -> Unit,
    icon: ImageVector = Icons.Default.Close
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Título del diálogo
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Lista de usuarios
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
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
                            text = "No se encontraron usuarios",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(users) { user ->
                            val isAlreadyFriend = currentFriends.contains(user.id)
                            val alreadyRequested by rememberUpdatedState(requestedUserIds.contains(user.id))

                            UserListItem(
                                user = user,
                                onAction = { onUserAction(user.id) },
                                onRemove = {
                                    removeFriend(GLOBAL.id, user.id) {
                                        loadFriendsList(GLOBAL.id) { updatedFriends ->
                                            onUserAction(user.id)
                                        }
                                    }
                                },
                                showAddButton = showAddButton,
                                isAlreadyFriend = isAlreadyFriend,
                                alreadyRequested = alreadyRequested,
                                customIcon = icon,
                                isFollowersList = title == "Seguidores"
                            )

                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        }
                    }

                }

                // Botón cerrar
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

// Elemento de lista de usuario
@Composable
fun UserListItem(
    user: User,
    onAction: () -> Unit,
    showAddButton: Boolean,
    isAlreadyFriend: Boolean = false,
    alreadyRequested: Boolean = false,
    onRemove: () -> Unit,
    customIcon: ImageVector? = null,
    isFollowersList: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Información del usuario
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Avatar del usuario
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.nombre.firstOrNull()?.toString() ?: "?",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Datos del usuario (nombre y objetivo)
            Column(
                modifier = Modifier.widthIn(max = 200.dp)
            ) {
                Text(
                    text = user.nombre,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = user.objetivoFitness,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // Botón de acción (agregar o eliminar amigo)
        IconButton(
            onClick = {
                if (alreadyRequested) {
                    onRemove() // Llama a onRemove cuando ya se ha enviado una solicitud
                } else {
                    onAction() // Llama a onAction para enviar una solicitud
                }
            }
        ) {
            Icon(
                imageVector = when {
                    isFollowersList -> Icons.Default.PersonRemove
                    isAlreadyFriend -> Icons.Default.PersonRemove
                    alreadyRequested -> Icons.Outlined.Person2
                    else -> Icons.Default.PersonAdd
                },
                contentDescription = when {
                    isFollowersList -> "Eliminar seguidor"
                    isAlreadyFriend -> "Eliminar amigo"
                    alreadyRequested -> "Solicitud pendiente"
                    else -> "Agregar amigo"
                },
                tint = when {
                    isFollowersList -> Color.Red
                    isAlreadyFriend -> Color.Red
                    alreadyRequested -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

// Función para cargar todos los usuarios de la BD
fun loadAllUsers(callback: (List<User>) -> Unit) {
    // Inicializar Firestore
    val db = Firebase.firestore

    // Añadir un log para verificar la carga
    Log.d("UserPage", "Iniciando carga de usuarios desde Firestore")

    db.collection("usuarios")
        .get()
        .addOnSuccessListener { snapshot ->
            println("Query successful. Document count: ${snapshot.documents.size}")
            val usersList = mutableListOf<User>()

            for (doc in snapshot.documents) {
                try {
                    // Verificar si el documento tiene datos
                    val userId = doc.id
                    val userName = doc.getString("nombre") ?: "Sin nombre"
                    val userWeight = doc.getString("peso") ?: ""
                    val userHeight = doc.getString("altura") ?: ""
                    val userAge = doc.getString("edad") ?: ""
                    val userGoal = doc.getString("objetivoFitness") ?: "No especificado"
                    val userExp = doc.getString("nivelExperiencia") ?: ""

                    Log.d("UserPage", "Cargando usuario: $userName (ID: $userId)")

                    val user = User(
                        id = userId,
                        nombre = userName,
                        peso = userWeight,
                        altura = userHeight,
                        edad = userAge,
                        objetivoFitness = userGoal,
                        nivelExperiencia = userExp,
                        cuentaPrivada = false // Asignar valor por defecto
                    )
                    usersList.add(user)
                } catch (e: Exception) {
                    // Manejo de errores
                    Log.e("Error", "Error al procesar documento: ${doc.id} - ${e.message}")
                }
            }
            Log.d("UserPage", "Usuarios cargados: ${usersList.size}")
            callback(usersList)
        }
        .addOnFailureListener { e ->
            // Manejo de errores
            Log.e("Error", "Error al cargar usuarios: ${e.message}")
            callback(emptyList())
        }
}

// Función para cargar la lista de amigos del usuario
fun loadFriendsList(userId: String, callback: (List<User>) -> Unit) {
    val db = Firebase.firestore

    Log.d("UserPage", "Iniciando carga de seguidores para usuario: $userId")

    // Verificar si el usuario tiene un documento de amigos
    db.collection("amigos")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists() && document.get("listaAmigos") != null) {
                val friendIds = document.get("listaAmigos") as? List<String> ?: emptyList()
                println("IDs de amigos encontrados: ${friendIds.size}")

                if (friendIds.isEmpty()) {
                    Log.d("UserPage", "No tiene usuarios agregados")
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                // Then get data for each friend
                val friendsList = mutableListOf<User>()
                var loadedCount = 0

                for (friendId in friendIds) {
                    db.collection("usuarios")
                        .document(friendId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                val userName = userDoc.getString("nombre") ?: "Sin nombre"
                                println("Cargando amigo: $userName (ID: $friendId)")

                                val user = User(
                                    id = userDoc.id,
                                    nombre = userName,
                                    peso = userDoc.getString("peso") ?: "",
                                    altura = userDoc.getString("altura") ?: "",
                                    edad = userDoc.getString("edad") ?: "",
                                    objetivoFitness = userDoc.getString("objetivoFitness") ?: "No especificado",
                                    nivelExperiencia = userDoc.getString("nivelExperiencia") ?: "",
                                    cuentaPrivada = userDoc.getBoolean("cuentaPrivada") ?: false
                                )
                                friendsList.add(user)
                            } else {
                                // Manejo de errores si el documento no existe
                                Log.e("Error", "Documento de usuario no encontrado para amigo ID: $friendId")
                            }

                            loadedCount++
                            if (loadedCount >= friendIds.size) {
                                // Todos los amigos han sido cargados
                                Log.d("UserPage", "Carga de seguidores completada. Total: ${friendsList.size}")
                                callback(friendsList)
                            }
                        }
                        .addOnFailureListener { e ->
                            // Manejo de errores si la carga falla
                            Log.e("Error", "Error al cargar datos de amigo $friendId: ${e.message}")
                            loadedCount++
                            if (loadedCount >= friendIds.size) {
                                callback(friendsList)
                            }
                        }
                }
            } else {
                // Manejo de errores si el documento no existe o está vacío
                Log.d("Error", "No existe documento de amigos para el usuario o está vacío")
                callback(emptyList())
            }
        }
        .addOnFailureListener { e ->
            // Manejo de errores si la carga falla
            Log.e("Error", "Error al cargar amigos: ${e.message}")
            callback(emptyList())
        }
}

// Función para agregar un amigo
private fun addFriend(userId: String, friendId: String, callback: () -> Unit) {
    val db = Firebase.firestore

    db.collection("amigos")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val currentFriends = document.get("listaAmigos") as? List<String> ?: emptyList()
                if (friendId !in currentFriends) {
                    val updatedFriends = currentFriends + friendId
                    db.collection("amigos")
                        .document(userId)
                        .update("listaAmigos", updatedFriends)
                        .addOnSuccessListener {
                            Log.d("AddFriend", "Amigo añadido correctamente: $friendId")
                            callback()
                        }
                        .addOnFailureListener { e ->
                            Log.e("AddFriend", "Error al actualizar lista de amigos: ${e.message}")
                            callback()
                        }
                } else {
                    Log.d("AddFriend", "El amigo ya está en la lista: $friendId")
                    callback()
                }
            } else {
                val newFriendsList = mapOf("listaAmigos" to listOf(friendId))
                db.collection("amigos")
                    .document(userId)
                    .set(newFriendsList)
                    .addOnSuccessListener {
                        Log.d("AddFriend", "Nueva lista de amigos creada para $userId")
                        callback()
                    }
                    .addOnFailureListener { e ->
                        Log.e("AddFriend", "Error al crear lista de amigos: ${e.message}")
                        callback()
                    }
            }
        }
        .addOnFailureListener { e ->
            Log.e("AddFriend", "Error al obtener documento de amigos: ${e.message}")
            callback()
        }
}

// Función para eliminar un seguidor
private fun removeFriend(userId: String, friendId: String, callback: () -> Unit) {
    val db = Firebase.firestore
    db.collection("amigos")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val currentFriends = document.get("listaAmigos") as? List<String> ?: emptyList()

                // Eliminar el amigo de la lista
                val updatedFriends = currentFriends.filter { it != friendId }

                // Actualizar el documento
                db.collection("amigos")
                    .document(userId)
                    .update("listaAmigos", updatedFriends)
                    .addOnSuccessListener {
                        callback()
                    }
                    .addOnFailureListener { e ->
                        Log.e("RemoveFriend", "Error al eliminar amigo: ${e.message}")
                        callback()
                    }

            } else {
                callback()
            }
        }
        .addOnFailureListener { e ->
            Log.e("RemoveFriend", "Error al verificar lista de amigos: ${e.message}")
            callback()
        }
}

// Composable para mostrar un elemento de estadística
@Composable
fun StatItem(count: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}

// Composable para mostrar una sección del perfil
@Composable
fun ProfileSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    // Estado para controlar la expansión de la sección
    var expanded by remember { mutableStateOf(true) }
    // Tarjeta que contiene la sección
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                RoundedCornerShape(16.dp)
            )
            .animateContentSize(animationSpec = tween(durationMillis = 50)), // Animación de expansión
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    content()
                }
            }
        }
    }
}

// Composable para mostrar un elemento de información
@Composable
fun InfoItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

//Metodos de notificaciones
// Función para cargar notificaciones
fun loadNotificaciones(idUsuario: String, callback: (List<Notificacion>) -> Unit) {
    try {
        // Verifica si el ID de usuario es válido
        if (idUsuario.isEmpty()) {
            Log.e("Notificaciones", "ID de usuario vacío")
            callback(emptyList())
            return
        }
    } catch (e: Exception) {
        Log.e("Notificaciones", "Error al cargar notificaciones: ${e.message}")
        callback(emptyList())
        return
    }
    Firebase.firestore
        .collection("notificaciones")
        .document(idUsuario)
        .collection("items")
        .get()
        .addOnSuccessListener { querySnapshot ->
            val lista = mutableListOf<Notificacion>()
            for (document in querySnapshot) {
                val tipo = document.getString("tipo") ?: ""
                val mensaje = document.getString("mensaje") ?: ""
                val remitente = document.getString("remitente") ?: ""
                val id = document.id
                val timestamp = document.getTimestamp("timestamp")?.toDate()?.toString() ?: ""


                val challengeId = document.getString("challengeId")
                lista.add(
                    Notificacion(
                        tipo = tipo,
                        mensaje = mensaje,
                        remitente = remitente,
                        timestamp = timestamp,
                        id = id, // ← ¡ESTO es lo importante!
                        challengeId = challengeId
                    )
                )

            }

            Log.d("Notificaciones", "Notificaciones cargadas: ${lista.size}")  // Agrega este log

            callback(lista)
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error al leer notificaciones", e)
            callback(emptyList()) // Evita que la app crashee
        }
}

// Composable para mostrar un elemento de notificación
@Composable
fun NotificationItem(
    notification: Notificacion,
    onAccept: () -> Unit,
    onReject: () -> Unit = {},
    nombreUsuario: MutableState<Map<String, String>> = remember { mutableStateOf(mapOf()) }
) {
    // Log para verificar que la notificación se pasa correctamente
    Log.d("NotificationItem", "Notificación: ${notification.mensaje}")

    // Efecto para cargar el nombre del usuario
    LaunchedEffect(notification.remitente) {
        if (!nombreUsuario.value.containsKey(notification.remitente)) {
            val db = FirebaseFirestore.getInstance()
            db.collection("usuarios")
                .document(notification.remitente)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val nombre = document.getString("nombre") ?: notification.remitente
                        nombreUsuario.value = nombreUsuario.value + (notification.remitente to nombre)
                    }
                }
                .addOnFailureListener {
                    Log.e("NotificationItem", "Error al obtener el nombre", it)
                }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Fila principal con avatar e información
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar con gradiente
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = when (notification.tipo) {
                                    "solicitud" -> listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                    "desafio" -> listOf(
                                        MaterialTheme.colorScheme.tertiary,
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                                    )
                                    else -> listOf(
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                    )
                                }
                            )
                        )
                ) {
                    Icon(
                        imageVector = when (notification.tipo) {
                            "solicitud" -> Icons.Default.PersonAdd
                            "desafio" -> Icons.Default.FitnessCenter
                            else -> Icons.Default.Notifications
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Contenido de la notificación
                Column(modifier = Modifier.weight(1f)) {
                    // Badge del tipo de notificación
                    Surface(
                        color = when (notification.tipo) {
                            "solicitud" -> Color(0xFFE8F5E8)
                            "desafio" -> Color(0xFFFFF3E0)
                            else -> Color(0xFFE3F2FD)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = when (notification.tipo) {
                                "solicitud" -> "SOLICITUD DE AMISTAD"
                                "desafio" -> "NUEVO DESAFÍO"
                                else -> "NOTIFICACIÓN"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (notification.tipo) {
                                "solicitud" -> Color(0xFF2E7D32)
                                "desafio" -> Color(0xFFE65100)
                                else -> Color(0xFF1565C0)
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    // Nombre del remitente
//                    val nombreMostrar = nombreUsuario.value[notification.remitente] ?: notification.remitente

                    Spacer(modifier = Modifier.height(6.dp))

                    // Mensaje
                    Text(
                        text = notification.mensaje,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            // Botones de acción
            if (notification.tipo == "solicitud" || notification.tipo == "desafio") {
                Spacer(modifier = Modifier.height(16.dp))

                Divider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (notification.tipo == "solicitud") {
                        // Botón rechazar
                        OutlinedButton(
                            onClick = onReject,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Rechazar",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Botón aceptar
                        Button(
                            onClick = onAccept,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Aceptar",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else if (notification.tipo == "desafio") {
                        // Botón rechazar para desafíos
                        OutlinedButton(
                            onClick = onReject,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Rechazar",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Botón aceptar para desafíos
                        Button(
                            onClick = onAccept,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = "Aceptar desafío",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Composable para mostrar las notificaciones
@Composable
fun NotificacionesDialog(
    notificaciones: List<Notificacion>,
    onDismiss: () -> Unit,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    icon: ImageVector = Icons.Default.PersonAdd,
) {
    // Estado para almacenar en caché los nombres
    val nombresUsuarios = remember { mutableStateOf(mapOf<String, String>()) }

    // Estado local mutable para la lista de notificaciones
    val notificacionesLocales = remember { mutableStateListOf<Notificacion>() }

    // Inicializar el estado local con las notificaciones recibidas
    LaunchedEffect(notificaciones) {
        notificacionesLocales.clear()
        notificacionesLocales.addAll(notificaciones)
    }

    // Función local para eliminar una notificación
    val removeNotificationLocal = { notificationId: String ->
        notificacionesLocales.removeIf { it.id == notificationId }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                // Encabezado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Notificaciones",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(thickness = 1.dp)

                Spacer(modifier = Modifier.height(8.dp))

                // Contenido principal
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (notificacionesLocales.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "No hay notificaciones",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Las solicitudes de amistad y desafíos aparecerán aquí",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(0.dp), // Sin espacio entre items
                            contentPadding = PaddingValues(0.dp), // Sin padding extra
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(notificacionesLocales) { notif ->
                                NotificationItem(
                                    notification = notif,
                                    onAccept = {
                                        if (notif.tipo == "solicitud") {
                                            onAcceptRequest(notif.remitente)
                                            removeNotificationLocal(notif.id)
                                            removeNotification(
                                                GLOBAL.id!!,
                                                notif.id,
                                                icon = icon
                                            )
                                        } else if (notif.tipo == "desafio" && notif.challengeId != null) {
                                            val db = FirebaseFirestore.getInstance()
                                            val userId = GLOBAL.id!!
                                            removeNotificationLocal(notif.id)
                                            db.collection("desafios").document(notif.challengeId).update("status", "ACCEPTED")
                                                .addOnSuccessListener {
                                                    Log.d("Notif", "Desafío aceptado")
                                                    removeNotification(
                                                        userId,
                                                        notif.id,
                                                        icon = icon
                                                    )
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("Notif", "Error al aceptar desafío", e)
                                                }
                                        }
                                    },
                                    onReject = {
                                        if (notif.tipo == "solicitud") {
                                            onRejectRequest(notif.remitente)
                                            removeNotificationLocal(notif.id)
                                            removeNotification(GLOBAL.id!!, notif.id, icon = icon)
                                        } else if (notif.tipo == "desafio" && notif.challengeId != null) {
                                            val db = FirebaseFirestore.getInstance()
                                            val userId = GLOBAL.id!!
                                            removeNotificationLocal(notif.id)
                                            db.collection("desafios").document(notif.challengeId).delete()
                                                .addOnSuccessListener {
                                                    Log.d("Notif", "Desafío rechazado y eliminado")
                                                    removeNotification(
                                                        userId,
                                                        notif.id,
                                                        icon = icon
                                                    )
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("Notif", "Error al rechazar desafío", e)
                                                }
                                        }
                                    },
                                    nombreUsuario = nombresUsuarios
                                )
                            }
                        }
                    }
                }

                // Botón inferior
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.Center),
                        shape = RoundedCornerShape(percent = 50),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}


// Función para enviar una notificación de solicitud de amistad
fun sendFriendRequestNotification(toUserId: String, fromUserName: String, fromUserId: String) {
    val db = FirebaseFirestore.getInstance()
    val notificacionesRef = db.collection("notificaciones").document(toUserId).collection("items")

    // Verifica si ya hay una notificación de solicitud del mismo remitente
    notificacionesRef
        .whereEqualTo("tipo", "solicitud")
        .whereEqualTo("remitente", fromUserId)
        .get()
        .addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                // Si no hay solicitud previa, envía la notificación
                val notificationData = mapOf(
                    "tipo" to "solicitud",
                    "mensaje" to "$fromUserName quiere ser tu amigo",
                    "remitente" to fromUserId,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                notificacionesRef.add(notificationData)
                    .addOnSuccessListener {
                        Log.d("Notif", "Notificación enviada a $toUserId")
                    }
                    .addOnFailureListener {
                        Log.e("Notif", "Error al enviar notificación", it)
                    }
            } else {
                Log.d("Notif", "Solicitud ya enviada previamente a $toUserId")
            }
        }
        .addOnFailureListener {
            Log.e("Notif", "Error al verificar notificación existente", it)
        }
}

// Función para eliminar una notificación
fun removeNotification(
    userId: String,
    notificationId: String,
    onComplete: () -> Unit = {},
    icon: ImageVector
) {
    FirebaseFirestore.getInstance()
        .collection("notificaciones")
        .document(userId)
        .collection("items")
        .document(notificationId)
        .delete()
        .addOnSuccessListener {
            Log.d("Notif", "Notificación $notificationId eliminada")
            onComplete()
        }
        .addOnFailureListener {
            Log.e("Notif", "Error al eliminar notificación", it)
        }
}

// Función para cargar la lista de seguidores
private fun loadFollowersList(userId: String, callback: (List<User>) -> Unit) {
    val db = Firebase.firestore
    // Consulta todos los docs donde mi ID esté en listaAmigos
    db.collection("amigos")
        .whereArrayContains("listaAmigos", userId)
        .get()
        .addOnSuccessListener { snapshot ->
            val followerIds = snapshot.documents.map { it.id }
            if (followerIds.isEmpty()) {
                callback(emptyList())
                return@addOnSuccessListener
            }
            // Recuperar datos de cada usuario
            val followers = mutableListOf<User>()
            var loaded = 0
            for (fid in followerIds) {
                db.collection("usuarios").document(fid).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            followers += User(
                                id = doc.id,
                                nombre = doc.getString("nombre") ?: "",
                                peso = doc.getString("peso") ?: "",
                                altura = doc.getString("altura") ?: "",
                                edad = doc.getString("edad") ?: "",
                                objetivoFitness = doc.getString("objetivoFitness") ?: "",
                                nivelExperiencia = doc.getString("nivelExperiencia") ?: "",
                                cuentaPrivada = doc.getBoolean("cuentaPrivada") ?: false
                            )
                        }
                        loaded++
                        if (loaded == followerIds.size) callback(followers)
                    }
                    .addOnFailureListener {
                        loaded++
                        if (loaded == followerIds.size) callback(followers)
                    }
            }
        }
        .addOnFailureListener {
            callback(emptyList())
        }
}

// Función para cargar los IDs de usuarios a los que el usuario actual ha enviado solicitudes
fun loadRequestedUserIds(currentUserId: String, callback: (List<String>) -> Unit) {
    val db = Firebase.firestore
    val notificacionesRef = db.collection("notificaciones")
    val requestedIds = mutableListOf<String>()

    // Busca solicitudes enviadas por el usuario actual
    db.collection("usuarios")
        .get()
        .addOnSuccessListener { usersSnapshot ->
            if (usersSnapshot.isEmpty) {
                Log.d("UserPage", "No hay usuarios para comprobar solicitudes")
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
                            Log.d("UserPage", "Solicitud encontrada para: $targetUserId")
                        }

                        // Disminuimos el contador
                        usersToCheck--

                        // Si hemos terminado con todos los usuarios, llamamos al callback
                        if (usersToCheck == 0) {
                            Log.d("UserPage", "Carga de solicitudes completada. Total: ${requestedIds.size}")
                            callback(requestedIds)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("UserPage", "Error al buscar solicitudes para $targetUserId", e)
                        usersToCheck--

                        if (usersToCheck == 0) {
                            callback(requestedIds)
                        }
                    }
            }
        }
        .addOnFailureListener { e ->
            Log.e("UserPage", "Error al cargar usuarios para comprobar solicitudes", e)
            callback(emptyList())
        }
}