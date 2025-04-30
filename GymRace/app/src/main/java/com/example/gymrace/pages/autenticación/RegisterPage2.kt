package com.example.gymrace.pages.autenticación

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gymrace.pages.GLOBAL
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch


//Composable para la pagina de registro 2 (fittness profile)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPage2(navController: NavController) {
    // Variables de estado
    val contexto = LocalContext.current
    val alcanceCorrutina = rememberCoroutineScope()

    // Estado del formulario
    var peso by rememberSaveable { mutableStateOf("") }
    var altura by rememberSaveable { mutableStateOf("") }
    var año by rememberSaveable { mutableStateOf("") }
    var nombre by rememberSaveable { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    // Estado del menú desplegable de objetivo fitness
    var expandedGoalMenu by remember { mutableStateOf(false) }
    var selectedGoal by rememberSaveable { mutableStateOf("Selecciona tu objetivo") }
    val goals = listOf(
        "Perder peso",
        "Ganar masa muscular",
        "Mejorar resistencia",
        "Mejorar flexibilidad",
        "Mejorar salud general",
        "Preparación para competición"
    )
    // Estado del menú desplegable de días de entrenamiento
    var expandedDaysMenu by remember { mutableStateOf(false) }
    var selectedDays by rememberSaveable { mutableStateOf("Selecciona días") }
    val trainingDays = listOf("1", "2", "3", "4", "5", "6", "7")

    // Estado del menú desplegable de nivel de experiencia
    var expandedExperienceMenu by remember { mutableStateOf(false) }
    var selectedExperience by rememberSaveable { mutableStateOf("Selecciona nivel") }
    val experienceLevels = listOf(
        "Principiante (menos de 6 meses)",
        "Intermedio (6 meses - 2 años)",
        "Avanzado (más de 2 años)"
    )

    // Función para guardar datos del usuario
    fun saveUserData() {
        cargando = true
        alcanceCorrutina.launch {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val userId = currentUser.uid
                    GLOBAL.guardarDatosRegistro(
                        userId,
                        nombre,
                        peso,
                        altura,
                        año,
                        selectedGoal,
                        selectedDays,
                        selectedExperience
                    ) {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                        Toast.makeText(contexto, "Perfil fitnes actualizado", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(contexto, "No se ha podido identificar al usuario", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
//                Toast.makeText(contexto, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Firestore", "Error al guardar datos del usuario: ${e.message}")
            } finally {
                cargando = false
            }
        }
    }

    // validación del formulario
    fun validateForm(): String {
        return when {

            nombre.isBlank() -> "Por favor, ingresa tu apodo."
            nombre.length < 3 -> "El apodo debe tener al menos 3 caracteres."
            nombre.length > 20 -> "El apodo no puede tener más de 20 caracteres."

            año.isBlank() -> "Por favor, ingresa tu edad."
            !año.all { it.isDigit() } -> "La edad debe ser un número."
            año.toInt() < 0 -> "La edad no puede ser negativa."
            año.toInt() == 0 -> "La edad no puede ser cero."

            altura.isBlank() -> "Por favor, ingresa tu altura."
            !altura.all { it.isDigit() } -> "La altura debe ser un número."
            altura.toFloat() < 0 -> "La altura no puede ser negativa."
            altura.toFloat() == 0f -> "La altura no puede ser cero."

            peso.isBlank() -> "Por favor, ingresa tu peso."
            !peso.all { it.isDigit() } -> "El peso debe ser un número."
            peso.toFloat() < 0 -> "El peso no puede ser negativo."
            peso.toFloat() == 0f -> "El peso no puede ser cero."


            selectedGoal.isBlank() -> "Por favor, selecciona tu objetivo fitness."
            selectedGoal.isEmpty() -> "Por favor, selecciona tu objetivo fitness."

            selectedDays.isBlank() -> "Por favor, selecciona los días de entrenamiento."
            selectedDays.isEmpty() -> "Por favor, selecciona los días de entrenamiento."

            selectedExperience.isBlank() -> "Por favor, selecciona tu nivel de experiencia."
            selectedExperience.isEmpty() -> "Por favor, selecciona tu nivel de experiencia."

            selectedGoal == "Selecciona tu objetivo" -> "Por favor, selecciona tu objetivo fitness."
            selectedDays == "Selecciona días" -> "Por favor, selecciona los días de entrenamiento."
            selectedExperience == "Selecciona nivel" -> "Por favor, selecciona tu nivel de experiencia."

            else -> "" // Todo está bien
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
//            .verticalScroll(scrollState) // Descomentar si se necesita desplazamiento
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center

    ) {
        // Título de la página
        Text(
            text = "Tu Perfil Físico",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = (MaterialTheme.colorScheme.primary) // Color del texto

        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campos de nombre, peso, altura y edad

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Apodo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFFFF9240), // Color cuando está enfocado
                unfocusedBorderColor = Color(0xFF000000) // Color cuando no está enfocado
            ),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Apodo") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        OutlinedTextField(
            value = año,
            onValueChange = { año = it },
            label = { Text("Edad") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFFFF9240), // Color cuando está enfocado
                unfocusedBorderColor = Color(0xff000000) // Color cuando no está enfocado
            ),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Edad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = altura,
            onValueChange = { altura = it },
            label = { Text("Altura (cm)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFFFF9240), // Color cuando está enfocado
                unfocusedBorderColor = Color(0xff000000) // Color cuando no está enfocado
            ),
            leadingIcon = { Icon(Icons.Default.Height, contentDescription = "Altura") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = peso,
            onValueChange = { peso = it },
            label = { Text("Peso (kg)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFFFF9240), // Color cuando está enfocado
                unfocusedBorderColor = Color(0xff000000) // Color cuando no está enfocado
            ),
            leadingIcon = { Icon(Icons.Default.MonitorWeight, contentDescription = "Peso") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Objetivo fitness
        Text(
            text = "Información de Entrenamiento",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = (MaterialTheme.colorScheme.onBackground),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expandedGoalMenu,
            onExpandedChange = { expandedGoalMenu = it }
        ) {
            TextField(
                value = selectedGoal,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGoalMenu) },
                leadingIcon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Objetivo") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFFF9240), // Color cuando está enfocado
                    unfocusedBorderColor = Color(0xff000000) // Color cuando no está enfocado
                ),
                label = { Text("Objetivo Fitness") }
            )

            ExposedDropdownMenu(
                expanded = expandedGoalMenu,
                onDismissRequest = { expandedGoalMenu = false }
            ) {
                goals.forEach { goal: String ->
                    DropdownMenuItem(
                        text = { Text(text = goal) },
                        onClick = {
                            selectedGoal = goal
                            expandedGoalMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Días de entrenamiento
        ExposedDropdownMenuBox(
            expanded = expandedDaysMenu,
            onExpandedChange = { expandedDaysMenu = it }
        ) {
            TextField(
                value = selectedDays,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDaysMenu) },
                leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = "Días") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFFF9240), // Color cuando está enfocado
                    unfocusedBorderColor = Color(0xff000000) // Color cuando no está enfocado
                ),
                label = { Text("Días de entrenamiento por semana") }
            )

            ExposedDropdownMenu(
                expanded = expandedDaysMenu,
                onDismissRequest = { expandedDaysMenu = false }
            ) {
                trainingDays.forEach { day: String ->
                    DropdownMenuItem(
                        text = { Text(text = day) },
                        onClick = {
                            selectedDays = day
                            expandedDaysMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Nivel de experiencia
        ExposedDropdownMenuBox(
            expanded = expandedExperienceMenu,
            onExpandedChange = { expandedExperienceMenu = it }
        ) {
            TextField(
                value = selectedExperience,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedExperienceMenu) },
                leadingIcon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Experiencia") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFFF9240), // Color cuando está enfocado
                    unfocusedBorderColor = Color(0xff000000) // Color cuando no está enfocado
                ),
                label = { Text("Nivel de experiencia") }
            )

            ExposedDropdownMenu(
                expanded = expandedExperienceMenu,
                onDismissRequest = { expandedExperienceMenu = false }
            ) {
                experienceLevels.forEach { level: String ->
                    DropdownMenuItem(
                        text = { Text(text = level) },
                        onClick = {
                            selectedExperience = level
                            expandedExperienceMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón de guardar

        Button(
            onClick = {
                val validationMessage = validateForm()
                if (validationMessage.isNotEmpty()) {
                    Toast.makeText(contexto, validationMessage, Toast.LENGTH_SHORT).show()
                } else {
                    saveUserData()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !cargando, // Se deshabilita solo si está cargando, no por validación
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xffff9240))
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xff7c461d)
                )
            } else {
                Text(text = "Guardar y Continuar", color = Color.White)
            }
        }
        //Continuar sin guardar datos
        Button(
            onClick = {
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth(),
//            enabled = isFormValid && !cargando,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xffff9240))
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xff7c461d)
                )
            } else {
                Text(text = "Continuar sin guardar", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Información opcional
        Text(
            text = "Esta información nos ayudará a personalizar tu experiencia",
            color = Color.Gray,
            fontSize = 14.sp,
        )
    }
}