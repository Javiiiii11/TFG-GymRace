package np.com.bimalkafle.bottomnavigationdemo.pages

import android.content.Context
import com.example.gymrace.pages.RegisterPage
import com.google.firebase.auth.FirebaseAuth
import com.example.gymrace.pages.saveLoginState
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.util.Log
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.rememberNavController
import com.example.gymrace.R
import com.example.gymrace.pages.saveLoginState
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Clase para representar un usuario
data class User(
    val id: String,
    val nombre: String,
    val peso: String,
    val altura: String,
    val edad: String,
    val objetivoFitness: String,
    val nivelExperiencia: String
)

@Composable
fun UserPage(modifier: Modifier = Modifier, onThemeChange: () -> Unit,navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showThemeMenu by remember { mutableStateOf(false) }

    // Estado para el diálogo de configuración
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    // Estados para diálogos
    var showCommunityDialog by remember { mutableStateOf(false) }
    var showFriendsDialog by remember { mutableStateOf(false) }

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
    var isLoadingUsers by remember { mutableStateOf(false) }

    // Estado para saber si el usuario está registrado con Google
    var isGoogleUser by remember { mutableStateOf(false) }




    // Efecto para cargar los datos del usuario desde Firebase
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
        LaunchedEffect(key1 = showCommunityDialog) {
            isLoadingUsers = true
            loadAllUsers { users ->
                // Filtrar al usuario actual
                allUsers = users.filter { it.id != GLOBAL.id }
                isLoadingUsers = false
                Log.d("UserPage", "Usuarios cargados para el diálogo: ${allUsers.size}")
            }
        }

        UsersDialog(
            title = "Comunidad",
            users = allUsers,
            isLoading = isLoadingUsers,
            onDismiss = { showCommunityDialog = false },
            onUserAction = { userId ->
                // Si el usuario ya es amigo, se elimina; de lo contrario, se agrega
                if (friendsList.any { it.id == userId }) {
                    removeFriend(GLOBAL.id, userId) {
                        // Recargar la lista de amigos después de eliminar
                        loadFriendsList(GLOBAL.id) { friends ->
                            friendsList = friends
                        }
                    }
                } else {
                    addFriend(GLOBAL.id, userId) {
                        // Recargar la lista de amigos después de agregar
                        loadFriendsList(GLOBAL.id) { friends ->
                            friendsList = friends
                        }
                    }
                }
            },
            showAddButton = true,
            currentFriends = friendsList.map { it.id }
        )
    }

// Mostrar diálogo de amigos
    if (showFriendsDialog) {
        UsersDialog(
            title = "Mis Amigos",
            users = friendsList,
            isLoading = isLoadingUsers,
            onDismiss = { showFriendsDialog = false },
            onUserAction = { userId ->
                removeFriend(GLOBAL.id, userId) {
                    // Recargar la lista de amigos después de eliminar
                    loadFriendsList(GLOBAL.id) { friends ->
                        friendsList = friends
                    }
                }
            },
            showAddButton = false
        )
    }


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


            Box {
                IconButton(onClick = { showThemeMenu = !showThemeMenu }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                // Menú desplegable para cambiar tema y más funciones
                DropdownMenu(
                    expanded = showThemeMenu,
                    onDismissRequest = { showThemeMenu = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .width(200.dp)
                ) {
                    Text(
                        text = "Ajustes",
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
                                    imageVector = Icons.Outlined.DarkMode,
                                    contentDescription = "Cambiar tema",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Cambiar tema")
                            }
                        },
                        onClick = {
                            onThemeChange()
                            showThemeMenu = false
                        }
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
                            Log.d("Si", "Rutas del navController" + navController.currentBackStackEntry)
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true } // Borra todo el historial de navegación
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
                        }
                    )
                }

                // Diálogo de confirmación para eliminar la cuenta
                if (showDeleteAccountDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteAccountDialog = false },
                        title = { Text("Confirmación") },
                        text = { Text("¿Estás seguro de que deseas borrar tu cuenta? Esta acción no se puede deshacer.") },
                        confirmButton = {
                            TextButton(onClick = {
                                val user = FirebaseAuth.getInstance().currentUser
                                if (user != null) {
                                    Firebase.firestore.collection("usuarios").document(user.uid).delete()
                                    Firebase.firestore.collection("amigos").document(user.uid).delete()

                                    user.delete().addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Log.d("UserPage", "Cuenta eliminada")
                                            navController.navigate("login") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        } else {
                                            Log.d("UserPage", "Error al eliminar cuenta: ${task.exception?.message}")
                                        }
                                    }

                                    FirebaseAuth.getInstance().signOut()
                                    saveLoginState(context, false, "")
                                    clearFirestoreCache()
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
            }
        }

        // Indicador de carga
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Profile Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
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
                    Button(
                        onClick = { showFriendsDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text(
                            text = "Mis amigos",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Button(
                        onClick = { showCommunityDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = "Comunidad")
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
                InfoItem(
                    label = "Fecha de registro",
                    value = SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(Date())
                )
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
                        Log.d("UserPage", "Editando perfil")
                        navController.navigate("register2") {
                            popUpTo(0) { inclusive = false }
                            Log.d("UserPage", "Editando perfil")
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
                            Log.d("UserPage", "Cerrando sesión")
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

        Spacer(modifier = Modifier.height(75.dp))
    }
}

fun cerrarSesion(context: Context) {
    FirebaseAuth.getInstance().signOut()
    saveLoginState(context, false, "") // Guardar el estado de inicio de sesión
    clearFirestoreCache() // Limpiar la caché de Firestore

}

fun clearFirestoreCache() {
    FirebaseFirestore.getInstance().clearPersistence()
}



// Diálogo para mostrar usuarios (amigos o comunidad)
// Improved User Dialog to better handle loading states and empty lists
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
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Dialog header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$title (${users.size})",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

                // Dialog content
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Cargando usuarios...",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else if (users.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (showAddButton)
                                    "No hay usuarios disponibles"
                                else "No tienes amigos aún",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            if (showAddButton) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Parece que aún no hay otros usuarios en la aplicación",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Ve a 'Comunidad' para agregar amigos",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Debug text to show loaded data
                    Text(
                        text = "Usuarios cargados: ${users.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

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
                            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cerrar", color = MaterialTheme.colorScheme.onPrimary)
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
    isAlreadyFriend: Boolean = false
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
                modifier = Modifier.widthIn(max = 200.dp) // Limita ancho para no chocar con el botón
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
            onClick = onAction
        ) {
            Icon(
                imageVector = if (showAddButton) {
                    if (isAlreadyFriend) Icons.Default.PersonRemove else Icons.Default.PersonAdd
                } else {
                    Icons.Default.PersonRemove
                },
                contentDescription = if (showAddButton) {
                    if (isAlreadyFriend) "Eliminar amigo" else "Agregar amigo"
                } else {
                    "Eliminar amigo"
                },
                tint = if (showAddButton && !isAlreadyFriend) MaterialTheme.colorScheme.primary else Color.Red
            )
        }
    }
}


// Función para cargar todos los usuarios de la BD
// Improved function to load all users from the database
private fun loadAllUsers(callback: (List<User>) -> Unit) {
    val db = Firebase.firestore

    // Add logging to track progress
    println("Iniciando carga de usuarios desde Firestore")

    db.collection("usuarios")
        .get()
        .addOnSuccessListener { snapshot ->
            println("Query successful. Document count: ${snapshot.documents.size}")
            val usersList = mutableListOf<User>()

            for (doc in snapshot.documents) {
                try {
                    val userId = doc.id
                    val userName = doc.getString("nombre") ?: "Sin nombre"
                    val userWeight = doc.getString("peso") ?: ""
                    val userHeight = doc.getString("altura") ?: ""
                    val userAge = doc.getString("edad") ?: ""
                    val userGoal = doc.getString("objetivoFitness") ?: "No especificado"
                    val userExp = doc.getString("nivelExperiencia") ?: ""

                    println("Cargando usuario: $userName (ID: $userId)")

                    val user = User(
                        id = userId,
                        nombre = userName,
                        peso = userWeight,
                        altura = userHeight,
                        edad = userAge,
                        objetivoFitness = userGoal,
                        nivelExperiencia = userExp
                    )
                    usersList.add(user)
                } catch (e: Exception) {
                    println("Error al procesar documento: ${e.message}")
                }
            }

            println("Carga completada. Total usuarios: ${usersList.size}")
            callback(usersList)
        }
        .addOnFailureListener { e ->
            println("Error crítico al cargar usuarios: ${e.message}")
            callback(emptyList())
        }
}

// Función para cargar la lista de amigos del usuario
// Improved function to load friends list
private fun loadFriendsList(userId: String, callback: (List<User>) -> Unit) {
    val db = Firebase.firestore

    println("Iniciando carga de amigos para usuario: $userId")

    // First get friend IDs
    db.collection("amigos")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists() && document.get("listaAmigos") != null) {
                val friendIds = document.get("listaAmigos") as? List<String> ?: emptyList()
                println("IDs de amigos encontrados: ${friendIds.size}")

                if (friendIds.isEmpty()) {
                    println("No tiene amigos agregados")
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
                                    nivelExperiencia = userDoc.getString("nivelExperiencia") ?: ""
                                )
                                friendsList.add(user)
                            } else {
                                println("Documento de usuario no encontrado para amigo ID: $friendId")
                            }

                            loadedCount++
                            if (loadedCount >= friendIds.size) {
                                println("Carga de amigos completada. Total: ${friendsList.size}")
                                callback(friendsList)
                            }
                        }
                        .addOnFailureListener { e ->
                            println("Error al cargar datos de amigo $friendId: ${e.message}")
                            loadedCount++
                            if (loadedCount >= friendIds.size) {
                                callback(friendsList)
                            }
                        }
                }
            } else {
                println("No existe documento de amigos para el usuario o está vacío")
                callback(emptyList())
            }
        }
        .addOnFailureListener { e ->
            println("Error crítico al cargar amigos: ${e.message}")
            callback(emptyList())
        }
}
// Función para agregar un amigo
private fun addFriend(userId: String, friendId: String, callback: () -> Unit) {
    val db = Firebase.firestore

    // Verificar si ya existe un documento de amigos para el usuario
    db.collection("amigos")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                // Obtener la lista actual de amigos
                val currentFriends = document.get("listaAmigos") as? List<String> ?: emptyList()

                // Agregar el nuevo amigo solo si no está ya en la lista
                if (friendId !in currentFriends) {
                    val updatedFriends = currentFriends + friendId

                    // Actualizar el documento
                    db.collection("amigos")
                        .document(userId)
                        .update("listaAmigos", updatedFriends)
                        .addOnSuccessListener {
                            callback()
                        }
                        .addOnFailureListener { e ->
                            println("Error al agregar amigo: ${e.message}")
                            callback()
                        }
                } else {
                    // Ya está en la lista, simplemente devolver
                    callback()
                }
            } else {
                // Crear un nuevo documento para este usuario con su primer amigo
                val newFriendsList = mapOf("listaAmigos" to listOf(friendId))

                db.collection("amigos")
                    .document(userId)
                    .set(newFriendsList)
                    .addOnSuccessListener {
                        callback()
                    }
                    .addOnFailureListener { e ->
                        println("Error al crear lista de amigos: ${e.message}")
                        callback()
                    }
            }
        }
        .addOnFailureListener { e ->
            println("Error al verificar lista de amigos: ${e.message}")
            callback()
        }
}

// Función para eliminar un amigo
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
                        println("Error al eliminar amigo: ${e.message}")
                        callback()
                    }
            } else {
                callback()
            }
        }
        .addOnFailureListener { e ->
            println("Error al verificar lista de amigos: ${e.message}")
            callback()
        }
}



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

@Composable
fun ProfileSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Section header with expand/collapse
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

            // Section content
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}

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

// Objeto global para mantener datos del usuario en sesión
object GLOBAL {
    var id: String = ""
    var nombre: String = ""
    var peso: String = ""
    var altura: String = ""
    var edad: String = ""
    var objetivoFitness: String = ""
    var diasEntrenamientoPorSemana: String = ""
    var nivelExperiencia: String = ""
}