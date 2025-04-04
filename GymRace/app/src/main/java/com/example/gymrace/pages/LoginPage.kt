package com.example.gymrace.pages

import android.app.Activity
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPage(navController: NavController) {
    val context = LocalContext.current

    // Variables de estado para el formulario y datos del usuario
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("") }  // Variable para guardar el nombre del usuario

    // Variables para el diálogo de recuperación de contraseña
    var showPasswordResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    // Validación del formulario
    val isFormValid = email.isNotBlank() &&
            email.contains("@") &&
            email.contains(".") &&
            password.isNotBlank()

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
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        } else {
                            navController.navigate("main") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    } else {
                        loginError = authTask.exception?.message ?: "Error al iniciar sesión con Google"
                        Toast.makeText(context, loginError, Toast.LENGTH_LONG).show()
                    }
                }
        } catch (e: ApiException) {
            isLoading = false
            Log.e("GoogleSignIn", "Google sign in failed with error code: ${e.statusCode}")
            when (e.statusCode) {
                GoogleSignInStatusCodes.DEVELOPER_ERROR -> {
                    loginError = "Error de configuración en Google Sign-In. Código: 10"
                }
                else -> {
                    loginError = "Error al iniciar sesión con Google: ${e.statusCode}"
                }
            }
            Toast.makeText(context, loginError, Toast.LENGTH_LONG).show()
        }
    }

    // Función para iniciar sesión con correo/contraseña
    fun loginWithEmailAndPassword(email: String, password: String) {
        isLoading = true
        loginError = ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Firebase.auth.signInWithEmailAndPassword(email, password).await()
                withContext(Dispatchers.Main) {
                    isLoading = false
                    saveLoginState(context, true, email)
                    Toast.makeText(context, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
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
                        e.message?.contains("The supplied auth credential is incorrect, malformed or has expired") == true ->
                            "Las credenciales de autenticación son incorrectas o están mal formadas"
                        else -> e.message ?: "Error al iniciar sesión"
                    }
                    Toast.makeText(context, loginError, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Función para iniciar el proceso de registro con Google
    fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }
    }

    // Función para enviar correo de recuperación de contraseña
    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
            Toast.makeText(context, "Ingresa un correo electrónico válido", Toast.LENGTH_LONG).show()
            return
        }

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

    fun validateForm(): String {
        return when {
            email.isBlank() && password.isBlank() -> "Tienes que llenar todos los campos"
            email.isBlank() -> "El correo electrónico no puede estar vacío"
            !email.contains("@") || !email.contains(".") -> "El correo electrónico no es válido"
            email.count { it == '@' } > 1 -> "El correo electrónico no es válido"
            email.count { it == '.' } > 1 -> "El correo electrónico no es válido"
            password.isBlank() -> "La contraseña no puede estar vacía"
            password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
            password.length > 20 -> "La contraseña no puede tener más de 20 caracteres"
            else -> "" // Todo está bien
        }
    }

    // UI con Jetpack Compose
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                val validationMessage = validateForm()
                if (validationMessage.isNotEmpty()) {
                    Toast.makeText(context, validationMessage, Toast.LENGTH_SHORT).show()
                } else {
                    loginWithEmailAndPassword(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xffff9240))
        ) {
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
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
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
                Button(
                    onClick = { sendPasswordResetEmail(resetEmail) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordResetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Función para guardar el estado de inicio de sesión
fun saveLoginState(context: Context, isLoggedIn: Boolean, account: String) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putBoolean("isLoggedIn", isLoggedIn)
    editor.putString("account", account)
    editor.apply()
}

// Función para obtener el estado de inicio de sesión
fun getLoginState(context: Context): Pair<Boolean, String?> {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
    val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
    val account = sharedPreferences.getString("account", null)
    Log.d("LoginState", "isLoggedIn: $isLoggedIn, account: $account")
    return Pair(isLoggedIn, account)
}
