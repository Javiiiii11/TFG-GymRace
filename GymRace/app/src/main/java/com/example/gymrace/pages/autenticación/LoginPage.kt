package com.example.gymrace.pages.autenticación

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gymrace.R
import com.example.gymrace.pages.GLOBAL
import com.example.gymrace.ui.theme.ThemeManager
import com.example.gymrace.ui.theme.rememberThemeState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Composable para la página de inicio de sesión
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPage(navController: NavController, onThemeChange: @Composable () -> Unit) {
    // Obtener el contexto de la actividad
    val context = LocalContext.current
    // Obtener el estado del tema (claro/oscuro)
    val isDarkTheme = rememberThemeState().value

    // Variables de estado para el formulario y datos del usuario
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("") }  // Variable para guardar el nombre del usuario

    // Variable para mostrar el diálogo de recuperación de contraseña
    var showPasswordResetDialog by remember { mutableStateOf(false) }
    // Variable para guardar el correo electrónico de recuperación
    var resetEmail by remember { mutableStateOf("") }

    // Configuración para GoogleSignIn
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    // Launcher para el intent de inicio de sesión con Google
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = true
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            // Obtener la cuenta de Google
            val account = task.getResult(ApiException::class.java)
            // Guardar el nombre del usuario en la variable userName
            userName = account.displayName ?: "Usuario"
            // Autenticar con Firebase usando la cuenta de Google
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            Firebase.auth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    isLoading = false
                    if (authTask.isSuccessful) {
                        // Obtener el id del usuario autenticado
                        val userId = Firebase.auth.currentUser?.uid.orEmpty()
                        // Verificar si el usuario es nuevo
                        val isNewUser = authTask.result?.additionalUserInfo?.isNewUser ?: false

                        if (isNewUser) {
                            // Llama a la función para crear el usuario en Firestore
                            GLOBAL.crearUsuarioEnFirestore(userId, userName) {
                                navController.navigate("register2") {
                                    Log.d("Navigation", "Navegando a la pantalla de registro")
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        } else {
                            // Si el usuario ya existe, te lleva a la pantalla principal
                            navController.navigate("main") {
                                Log.d("Navigation", "Navegando a la pantalla principal")
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    } else {
                        // Manejar el error de inicio de sesión
                        Log.e("Error", "Google sign in failed: ${authTask.exception?.message}")
                        loginError = authTask.exception?.message ?: "Error al iniciar sesión con Google"
                        Toast.makeText(context, loginError, Toast.LENGTH_LONG).show()
                    }
                }
        } catch (e: ApiException) {
            // Manejar el error de inicio de sesión
            isLoading = false
            Log.e("GoogleSignIn", "Google sign in failed with error code: ${e.statusCode}")
            when (e.statusCode) {
                GoogleSignInStatusCodes.DEVELOPER_ERROR -> {
                    // Manejar el error de configuración del cliente
                    Log.e("Error", "Developer error: ${e.message}")
                    loginError = "Error de configuración en Google Sign-In. Código: 10"
                }
                else -> {
                    // Manejar otros errores de Google Sign-In
                    Log.e("Error", "Google sign in failed: ${e.message}")
                    loginError = "Error al iniciar sesión con Google: ${e.statusCode}"
                }
            }
            // Mostrar mensaje de error
            Log.e("Error", loginError)
        }
    }

    // Función para iniciar sesión con correo/contraseña
    fun loginWithEmailAndPassword(email: String, password: String) {
        // Validar el correo electrónico y la contraseña
        isLoading = true
        loginError = ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Intentar iniciar sesión con Firebase
                Firebase.auth.signInWithEmailAndPassword(email, password).await()
                withContext(Dispatchers.Main) {
                    // Si el inicio de sesión es exitoso, guardar el estado
                    isLoading = false
                    saveLoginState(context, true, email)
                    Toast.makeText(context, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                    Log.d("Navigation", "Navegando a la pantalla principal")
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    loginError = when {
                        e.message?.contains("password") == true -> "Contraseña incorrecta"
                        e.message?.contains("no user record") == true -> {
                            Toast.makeText(context, "La cuenta no existe. Por favor regístrate.", Toast.LENGTH_LONG).show()
                            "No existe cuenta con este correo"
                        }
                        e.message?.contains("blocked") == true -> {
                            delay(30000) // Espera 30 segundos antes de permitir otro intento
                            "Demasiados intentos fallidos. Intenta más tarde"
                        }
                        e.message?.contains("Las credenciales de autenticación son incorrectas o están mal formadas") == true ->
                            "Las credenciales de autenticación son incorrectas o están mal formadas"
                        else -> e.message ?: "Error al iniciar sesión"
                    }
                    // Mostrar mensaje de error
                    Log.e("Error", loginError)
                    Toast.makeText(context, "Error al iniciar sesión", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Función para iniciar el proceso de registro con Google
    fun signInWithGoogle() {
        // Mostrar mensaje de carga
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }
    }

    // Función para enviar correo de recuperación de contraseña
    fun sendPasswordResetEmail(email: String) {
        // Validar el correo electrónico
        if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
            Toast.makeText(context, "Ingresa un correo electrónico válido", Toast.LENGTH_LONG).show()
            return
        }
        // Mostrar mensaje de carga
        Firebase.auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Se ha enviado un correo para restablecer tu contraseña", Toast.LENGTH_LONG).show()
                    showPasswordResetDialog = false
                } else {
                    Toast.makeText(context, "Error: ${task.exception?.message ?: "No se pudo enviar el correo"}", Toast.LENGTH_LONG).show()
                }
            }
    }

// Función para validar el formulario de inicio de sesión
fun validateForm(): String {
    email = email.trim() // Eliminar espacios en blanco al inicio y al final
    return when {
        email.isBlank() && password.isBlank() -> "Tienes que llenar todos los campos"
        email.isBlank() -> "El correo electrónico no puede estar vacío"
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "El correo electrónico no es válido"
        password.isBlank() -> "La contraseña no puede estar vacía"
        password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
        else -> "" // No hay errores de validación
    }
}

    //Contenido de la página de inicio de sesión
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Botón de cambio de tema en la esquina superior derecha
        IconButton(
            onClick = {
                ThemeManager.toggleTheme(context)
            },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = if (isDarkTheme) {
                    Icons.Default.LightMode // Icono para modo oscuro (cuando está en claro)
                } else {
                    Icons.Default.DarkMode // Icono para modo claro (cuando está en oscuro)
                },
                contentDescription = "Cambiar tema",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Título de la página
            Text(
                text = "Iniciar Sesión",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Campos de entrada para login con email/contraseña
            // Correo electrónico
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFFF9240),
                    unfocusedBorderColor = Color(0xff000000)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Contraseña
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = "Contraseña") },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFFF9240),
                    unfocusedBorderColor = Color(0xff000000)
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Enlace para recuperar contraseña
            Box(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = {
                        resetEmail = email // Pre-llenar con el email actual si existe
                        showPasswordResetDialog = true
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(text = "¿Olvidaste tu contraseña?", color = Color(0xFF1976D2))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Botón de inicio de sesión con email/contraseña
            Button(
                onClick = {
                    // Validar el formulario antes de iniciar sesión
                    val validationMessage = validateForm()
                    // Si hay un mensaje de validación, mostrarlo
                    if (validationMessage.isNotEmpty()) {
                        Toast.makeText(context, validationMessage, Toast.LENGTH_SHORT).show()
                    } else {
                        loginWithEmailAndPassword(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xffff9240))
            ) {
                // Mostrar un indicador de carga si isLoading es verdadero
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xff7c461d)
                    )
                } else {
                    Text(text = "Iniciar Sesión", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Separador para las opciones de inicio de sesión
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(
                    modifier = Modifier.weight(1f),
                    color = Color.Gray.copy(alpha = 0.5f)
                )
                Text(
                    text = " O CONTINÚA CON ",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Divider(
                    modifier = Modifier.weight(1f),
                    color = Color.Gray.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Botón de inicio de sesión con Google
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .clickable(enabled = !isLoading) { signInWithGoogle() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Icono de Google
                    Image(
                        painter = painterResource(id = R.drawable.logo_de_google),
                        contentDescription = "Logo de Google",
                        modifier = Modifier.size(24.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continuar con Google",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Enlace para registrarse
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¿No tienes cuenta? Regístrate",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        navController.navigate("register") {
                            // Limpiar la pila de navegación para evitar volver a la página de inicio de sesión
                            Log.d("Navigation", "Navegando a la pantalla de registro")
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
        }
    }

    // Diálogo para recuperación de contraseña
    if (showPasswordResetDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordResetDialog = false },
            title = { Text("Recuperar contraseña") },
            text = {
                Column {
                    Text("Introduce tu correo electrónico para recibir instrucciones de recuperación de contraseña:")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Correo electrónico") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFFFF9240),
                            unfocusedBorderColor = Color(0xff000000)
                        ),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            },
            confirmButton = {
                // Botón para enviar el correo de recuperación
                Button(
                    onClick = { sendPasswordResetEmail(resetEmail) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                // Botón para cancelar el diálogo
                TextButton(onClick = { showPasswordResetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Función para guardar el estado de inicio de sesión
fun saveLoginState(context: Context, isLoggedIn: Boolean, account: String) {
    // Guardar el estado de inicio de sesión en SharedPreferences
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putBoolean("isLoggedIn", isLoggedIn)
    editor.putString("account", account)
    editor.apply()
}