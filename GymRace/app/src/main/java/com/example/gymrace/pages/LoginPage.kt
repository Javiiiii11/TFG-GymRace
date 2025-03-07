package com.example.gymrace.pages

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.gymrace.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import android.util.Log

@Composable
fun LoginPage(navController: NavController) {
    val context = LocalContext.current

    // Variables de estado para el formulario
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

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
            // Autenticar con Firebase usando la cuenta de Google
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Firebase.auth.signInWithCredential(credential).await()
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        Toast.makeText(context, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        loginError = e.message ?: "Error al iniciar sesión con Google"
                        Toast.makeText(context, loginError, Toast.LENGTH_LONG).show()
                        Log.e("GoogleSignIn", "Error: ${e.message}")
                    }
                }
            }
        } catch (e: ApiException) {
            isLoading = false
            Log.e("GoogleSignIn", "Google sign in failed with error code: ${e.statusCode}")
            loginError = "Error al iniciar sesión con Google: ${e.statusCode}"
            if (e.statusCode == 10) {
                loginError = "Error de configuración en Google Sign-In. Verifica SHA-1 y configuración."
                Log.e("GoogleSignIn", "Developer error - SHA-1 fingerprint or package name mismatch")
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
                        e.message?.contains("no user record") == true -> "No existe cuenta con este correo"
                        e.message?.contains("blocked") == true -> "Demasiados intentos fallidos. Intenta más tarde"
                        else -> e.message ?: "Error al iniciar sesión"
                    }
                    Toast.makeText(context, loginError, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Función para iniciar el proceso de registro con Google
    fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    // UI con Jetpack Compose
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título de la página
        Text(
            text = "Iniciar Sesión",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
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
                onClick = { navController.navigate("password_recovery") },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(text = "¿Olvidaste tu contraseña?", color = Color(0xFF1976D2))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón de inicio de sesión con email/contraseña
        Button(
            onClick = { loginWithEmailAndPassword(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = isFormValid && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
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
                // Imagen del logo de Google
                Image(
                    painter = painterResource(id = R.drawable.logo_de_google), // Usar el mismo recurso que en RegisterPage
                    contentDescription = "Logo de Google",
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continuar con Google",
                    color = Color.Black,
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
            // Enlace a la pantalla de registro
            Text(
                text = "¿No tienes cuenta? Regístrate",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    navController.navigate("register") {
                        // Limpia la pila de navegación para que el usuario no pueda volver atrás
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
    }
}