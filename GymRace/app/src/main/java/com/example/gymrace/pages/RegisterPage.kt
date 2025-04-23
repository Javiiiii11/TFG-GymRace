package com.example.gymrace.pages

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.VisualTransformation
import androidx.navigation.NavController
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.example.gymrace.R
import com.example.gymrace.pages.GLOBAL.Companion.crearUsuarioEnFirestore
import com.example.gymrace.ui.theme.ThemeManager
import com.example.gymrace.ui.theme.rememberThemeState
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPage(navController: NavController, onThemeChange: @Composable () -> Unit) {
    val context = LocalContext.current
    val isDarkTheme = rememberThemeState().value // Obtener el estado actual del tema

    // Variables de estado para el formulario
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var registrationError by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Validación del formulario
    val isFormValid = name.isNotBlank() &&
            email.isNotBlank() &&
            email.contains("@") &&
            email.contains(".") &&
            password.isNotBlank() &&
            password.length >= 6 &&
            password == confirmPassword

    // Configuración para GoogleSignIn
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("706050795925-23ac30bccl18pvgg70c5jucc1ug7p0vr.apps.googleusercontent.com")
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
            // Autenticar con Firebase usando la cuenta de Google
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            Firebase.auth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val user = Firebase.auth.currentUser
                        val uid = user?.uid
                        if (uid != null) {
                            // Verificar si el usuario ya existe en Firestore
                            Firebase.firestore.collection("usuarios").document(uid)
                                .get()
                                .addOnSuccessListener { document ->
                                    isLoading = false
                                    if (document.exists()) {
                                        // Usuario existente, navegar a main
                                        navController.navigate("main") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        // Usuario nuevo: primero se crea en Firestore usando la cuenta de Google,
                                        // luego se navega a register2
                                        crearUsuarioEnFirestore(uid, account.displayName ?: "") {
                                            navController.navigate("register2?uid=$uid&nombre=${account.displayName}") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    Log.e("Firestore", "Error al verificar usuario en Firestore", e)
                                    Toast.makeText(context, "Error al verificar usuario", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            isLoading = false
                            Toast.makeText(context, "Error: UID de usuario no disponible", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        isLoading = false
                        registrationError = authTask.exception?.message ?: "Error al iniciar sesión con Google"
                        Toast.makeText(context, registrationError, Toast.LENGTH_LONG).show()
                    }
                }
        } catch (e: ApiException) {
            isLoading = false
            Log.e("GoogleSignIn", "Google sign in failed with error code: ${e.statusCode}")
            when (e.statusCode) {
                GoogleSignInStatusCodes.DEVELOPER_ERROR -> {
                    registrationError = "Error de configuración en Google Sign-In. Código: 10"
                    Log.e("GoogleSignIn", "Developer error - SHA-1 fingerprint or package name mismatch")
                }
                else -> {
                    registrationError = "Error al iniciar sesión con Google: ${e.statusCode}"
                }
            }
            Toast.makeText(context, registrationError, Toast.LENGTH_LONG).show()
        }
    }

    // Función para iniciar el proceso de registro con Google
    fun signInWithGoogle() {
        // Forzar el diálogo de selección de cuenta cerrando la sesión actual
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }
    }

    // Función para registrar con correo/contraseña
    fun registerUser(name: String, email: String, password: String, param: (Boolean) -> Unit) {
        isLoading = true
        registrationError = ""

        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    Firebase.auth.currentUser?.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                    )?.addOnCompleteListener {
                        // Llamamos al callback indicando éxito
                        param(true)
                        // Navegación después de actualizar perfil
                        navController.navigate("register2") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Error desconocido"
                    registrationError = errorMessage
                    param(false)
                    if (errorMessage.contains("The email address is already in use")) {
                        Toast.makeText(context, "Este email ya está en uso", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, registrationError, Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    fun validateForm(): String {
        return when {
            name.isBlank() && email.isBlank() && password.isBlank() && confirmPassword.isBlank() -> "Todos los campos son obligatorios"
            name.isBlank() -> "El nombre no puede estar vacío"
            name.length < 3 -> "El nombre debe tener al menos 3 caracteres"
            name.length > 20 -> "El nombre no puede tener más de 20 caracteres"
            name.any { it.isDigit() } -> "El nombre no puede contener números"
            email.isBlank() -> "El correo electrónico no puede estar vacío"
            !email.contains("@") || !email.contains(".") -> "El correo electrónico no es válido"
            email.count { it == '@' } > 1 -> "El correo electrónico no es válido"
            email.count { it == '.' } > 1 -> "El correo electrónico no es válido"
            password.isBlank() -> "La contraseña no puede estar vacía"
            password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
            password.length > 20 -> "La contraseña no puede tener más de 20 caracteres"
            confirmPassword.isBlank() -> "La confirmación de contraseña no puede estar vacía"
            confirmPassword.length < 6 -> "La confirmación de contraseña debe tener al menos 6 caracteres"
            confirmPassword.length > 20 -> "La confirmación de contraseña no puede tener más de 20 caracteres"
            password != confirmPassword -> "Las contraseñas no coinciden"
            else -> "" // Todo está bien
        }
    }

    fun checkIfEmailExists(email: String, callback: (Boolean) -> Unit) {
        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val existingMethods = task.result?.signInMethods
                    callback(existingMethods?.isNotEmpty() == true)
                } else {
                    Log.e("Registro", "Error al verificar el correo: ${task.exception?.message}")
                    callback(false)
                }
            }
    }

    // UI con Jetpack Compose
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
            verticalArrangement = Arrangement.Center,
        ) {
            // Título de la página
            Text(
                text = "Crear Cuenta",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Campo para nombre
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre completo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFFF9240),
                    unfocusedBorderColor = Color(0xff000000)
                ),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Nombre") }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Campo para email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
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
            Spacer(modifier = Modifier.height(8.dp))

            // Campo para contraseña
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

            // Campo para confirmar contraseña
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmar contraseña") },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = "Confirmar contraseña") },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFFF9240),
                    unfocusedBorderColor = Color(0xff000000)
                ),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (confirmPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Botón de registro con email/contraseña
            Button(
                onClick = {
                    val validationMessage = validateForm()
                    if (validationMessage.isNotEmpty()) {
                        Toast.makeText(context, validationMessage, Toast.LENGTH_SHORT).show()
                    } else {
                        checkIfEmailExists(email) { emailExists ->
                            if (emailExists) {
                                Toast.makeText(context, "Este correo electrónico ya está en uso.", Toast.LENGTH_LONG).show()
                            } else {
                                Log.d("Firestore", "Intentando registrar usuario con email y contraseña")
                                CoroutineScope(Dispatchers.IO).launch {
                                    GLOBAL.name = name
                                    GLOBAL.email = email
                                    GLOBAL.password = password

                                    registerUser(name, email, password) { isSuccess ->
                                        if (isSuccess) {
                                            FirebaseAuth.getInstance().currentUser?.reload()
                                            val userId = Firebase.auth.currentUser?.uid
                                            if (!userId.isNullOrEmpty()) {
                                                crearUsuarioEnFirestore(userId, name) {
                                                    Log.d("Firestore", "✅ Usuario creado en Firestore correctamente.")
                                                }
                                            } else {
                                                Log.e("Firestore", "⚠ Usuario no autenticado después del registro.")
                                            }
                                        } else {
                                            Log.e("Registro", "❌ Error en el registro de usuario.")
                                        }
                                    }
                                }
                            }
                        }
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
                    Text(text = "Registrarse", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Separador para las opciones de registro
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

            // Botón de registro con Google
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surface)
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

            // Enlace a la pantalla de login
            Text(
                text = "¿Ya tienes una cuenta? Inicia sesión",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
    }
}