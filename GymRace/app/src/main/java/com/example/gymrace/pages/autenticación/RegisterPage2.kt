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


// Composable para la página de registro 2 / edición de perfil
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPage2(navController: NavController) {
    val contexto = LocalContext.current
    val alcanceCorrutina = rememberCoroutineScope()

    // Determinar si es un nuevo usuario o actualización de perfil
    val esNuevoUsuario = GLOBAL.nombre.isBlank()

    // Campos autocompletados si existen datos en GLOBAL
    var nombre by rememberSaveable { mutableStateOf(GLOBAL.nombre.takeIf { it.isNotBlank() } ?: "") }
    var año by rememberSaveable { mutableStateOf(GLOBAL.edad.takeIf { it.isNotBlank() } ?: "") }
    var altura by rememberSaveable { mutableStateOf(GLOBAL.altura.takeIf { it.isNotBlank() } ?: "") }
    var peso by rememberSaveable { mutableStateOf(GLOBAL.peso.takeIf { it.isNotBlank() } ?: "") }

    // Dropdown Objetivo
    var expandedGoalMenu by remember { mutableStateOf(false) }
    var selectedGoal by rememberSaveable { mutableStateOf(
        GLOBAL.objetivoFitness.takeIf { it.isNotBlank() } ?: "Selecciona tu objetivo"
    ) }
    val goals = listOf(
        "Perder peso", "Ganar masa muscular", "Mejorar resistencia",
        "Mejorar flexibilidad", "Mejorar salud general", "Preparación para competición"
    )

    // Dropdown Días
    var expandedDaysMenu by remember { mutableStateOf(false) }
    var selectedDays by rememberSaveable { mutableStateOf(
        GLOBAL.diasEntrenamientoPorSemana.takeIf { it.isNotBlank() } ?: "Selecciona días"
    ) }
    val trainingDays = (1..7).map { it.toString() }

    // Dropdown Experiencia
    var expandedExperienceMenu by remember { mutableStateOf(false) }
    var selectedExperience by rememberSaveable { mutableStateOf(
        GLOBAL.nivelExperiencia.takeIf { it.isNotBlank() } ?: "Selecciona nivel"
    ) }
    val experienceLevels = listOf(
        "Principiante (menos de 6 meses)",
        "Intermedio (6 meses - 2 años)",
        "Avanzado (más de 2 años)"
    )

    var cargando by remember { mutableStateOf(false) }

    fun validateForm(): String = when {
        nombre.isBlank() -> "Por favor, ingresa tu apodo."
        nombre.length < 3 -> "El apodo debe tener al menos 3 caracteres."
        nombre.length > 20 -> "El apodo no puede tener más de 20 caracteres."
        año.isBlank() -> "Por favor, ingresa tu edad."
        año.toInt() > 120 -> "La edad debe de ser una edad realista."
        !año.all { it.isDigit() } -> "La edad debe ser un número."
        año.toInt() < 0 -> "La edad no puede ser negativa."
        altura.isBlank() -> "Por favor, ingresa tu altura."
        !altura.all { it.isDigit() } -> "La altura debe ser un número."
        altura.toInt() < 0 -> "La altura no puede ser negativa."
        altura.toInt() > 300 -> "La altura debe de ser una altura realista."
        peso.isBlank() -> "Por favor, ingresa tu peso."
        !peso.all { it.isDigit() } -> "El peso debe ser un número."
        peso.toInt() < 0 -> "El peso no puede ser negativo."
        peso.toInt() > 500 -> "El peso debe de ser un peso realista."
        selectedGoal == "Selecciona tu objetivo" -> "Por favor, selecciona tu objetivo fitness."
        selectedDays == "Selecciona días" -> "Por favor, selecciona los días de entrenamiento."
        selectedExperience == "Selecciona nivel" -> "Por favor, selecciona tu nivel de experiencia."
        else -> ""
    }

    fun saveUserData() {
        cargando = true
        alcanceCorrutina.launch {
            try {
                // Actualizar GLOBAL localmente
                GLOBAL.nombre = nombre
                GLOBAL.edad = año
                GLOBAL.altura = altura
                GLOBAL.peso = peso
                GLOBAL.objetivoFitness = selectedGoal
                GLOBAL.diasEntrenamientoPorSemana = selectedDays
                GLOBAL.nivelExperiencia = selectedExperience

                FirebaseAuth.getInstance().currentUser?.let { user ->
                    GLOBAL.guardarDatosRegistro(
                        user.uid,
                        nombre,
                        peso,
                        altura,
                        año,
                        selectedGoal,
                        selectedDays,
                        selectedExperience
                    ) {
                        // Si es un nuevo usuario, navegar a main
                        // Si es actualización de perfil, volver a la página anterior
                        if (esNuevoUsuario) {
                            navController.navigate("main") {
                                popUpTo("login") { inclusive = true }
                            }
                            Toast.makeText(contexto, "Registro completo", Toast.LENGTH_SHORT).show()
                        } else {
                            navController.popBackStack()
                            Toast.makeText(contexto, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                        }
                    }
                } ?: Toast.makeText(contexto, "No se ha podido identificar al usuario", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("Firestore", "Error al guardar datos del usuario: ${e.message}")
            } finally {
                cargando = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Tu Perfil Físico",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))

        // Apodo
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Apodo") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFFFF9240),
                unfocusedBorderColor = Color(0xFF000000)
            )
        )
        Spacer(Modifier.height(8.dp))

        // Edad
        OutlinedTextField(
            value = año,
            onValueChange = { año = it },
            label = { Text("Edad") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFFFF9240),
                unfocusedBorderColor = Color(0xFF000000)
            )
        )
        Spacer(Modifier.height(8.dp))

        // Altura
        OutlinedTextField(
            value = altura,
            onValueChange = { altura = it },
            label = { Text("Altura (cm)") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Height, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFFFF9240),
                unfocusedBorderColor = Color(0xFF000000)
            )
        )
        Spacer(Modifier.height(8.dp))

        // Peso
        OutlinedTextField(
            value = peso,
            onValueChange = { peso = it },
            label = { Text("Peso (kg)") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.MonitorWeight, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFFFF9240),
                unfocusedBorderColor = Color(0xFF000000)
            )
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Información de Entrenamiento",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(8.dp))

        // Objetivo select
        ExposedDropdownMenuBox(
            expanded = expandedGoalMenu,
            onExpandedChange = { expandedGoalMenu = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = selectedGoal,
                onValueChange = {},
                label = { Text("Objetivo Fitness") },
                leadingIcon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedGoalMenu) },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFFF9240),
                    unfocusedBorderColor = Color(0xFF000000)
                )
            )
            ExposedDropdownMenu(
                expanded = expandedGoalMenu,
                onDismissRequest = { expandedGoalMenu = false }
            ) {
                goals.forEach { goal ->
                    DropdownMenuItem(
                        text = { Text(goal) },
                        onClick = { selectedGoal = goal; expandedGoalMenu = false }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Días select
        ExposedDropdownMenuBox(
            expanded = expandedDaysMenu,
            onExpandedChange = { expandedDaysMenu = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = selectedDays,
                onValueChange = {},
                label = { Text("Días/semana") },
                leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedDaysMenu) },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFFF9240),
                    unfocusedBorderColor = Color(0xFF000000)
                )
            )
            ExposedDropdownMenu(
                expanded = expandedDaysMenu,
                onDismissRequest = { expandedDaysMenu = false }
            ) {
                trainingDays.forEach { day ->
                    DropdownMenuItem(
                        text = { Text(day) },
                        onClick = { selectedDays = day; expandedDaysMenu = false }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Experiencia select
        ExposedDropdownMenuBox(
            expanded = expandedExperienceMenu,
            onExpandedChange = { expandedExperienceMenu = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = selectedExperience,
                onValueChange = {},
                label = { Text("Nivel de experiencia") },
                leadingIcon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedExperienceMenu) },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFFF9240),
                    unfocusedBorderColor = Color(0xFF000000)
                )
            )
            ExposedDropdownMenu(
                expanded = expandedExperienceMenu,
                onDismissRequest = { expandedExperienceMenu = false }
            ) {
                experienceLevels.forEach { level ->
                    DropdownMenuItem(
                        text = { Text(level) },
                        onClick = { selectedExperience = level; expandedExperienceMenu = false }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Botón Guardar/Actualizar
        Button(
            onClick = {
                validateForm().also { msg ->
                    if (msg.isNotEmpty()) Toast.makeText(contexto, msg, Toast.LENGTH_SHORT).show()
                    else saveUserData()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !cargando,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9240))
        ) {
            if (cargando) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF7C461D))
            else Text(text = if (esNuevoUsuario) "Guardar y Continuar" else "Actualizar Perfil", color = Color.White)
        }

        Spacer(Modifier.height(8.dp))

        // Botón Cancelar/Continuar sin guardar
        Button(
            onClick = {
                if (esNuevoUsuario) {
                    // Si es un nuevo usuario, ir a main
                    navController.navigate("main") { popUpTo("login") { inclusive = true } }
                } else {
                    // Si es una actualización, volver a la página anterior
                    navController.popBackStack()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9240))
        ) {
            if (cargando) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF7C461D))
            else Text(text = if (esNuevoUsuario) "Continuar sin perfil" else "Cancelar", color = Color.White)
        }

        Spacer(Modifier.height(16.dp))

        // Información opcional
        Text(
            text = "Esta información nos ayudará a personalizar tu experiencia",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}