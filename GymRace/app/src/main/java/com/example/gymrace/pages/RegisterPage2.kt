package com.example.gymrace.pages

import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPage2(navController: NavController) {
    val contexto = LocalContext.current
    val scrollState = rememberScrollState()
    val alcanceCorrutina = rememberCoroutineScope()

    // Estado del formulario
    var peso by rememberSaveable { mutableStateOf("") }
    var altura by rememberSaveable { mutableStateOf("") }
    var año by rememberSaveable { mutableStateOf("") }
    var nombre by rememberSaveable { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    // Estado del menú desplegable
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

    var expandedDaysMenu by remember { mutableStateOf(false) }
    var selectedDays by rememberSaveable { mutableStateOf("Selecciona días") }
    val trainingDays = listOf("1", "2", "3", "4", "5", "6", "7")

    var expandedExperienceMenu by remember { mutableStateOf(false) }
    var selectedExperience by rememberSaveable { mutableStateOf("Selecciona nivel") }
    val experienceLevels = listOf(
        "Principiante (menos de 6 meses)",
        "Intermedio (6 meses - 2 años)",
        "Avanzado (más de 2 años)"
    )

    // Validación del formulario
    val isFormValid = peso.isNotBlank() &&
            altura.isNotBlank() &&
            nombre.isNotBlank() &&
            año.isNotBlank() &&
            selectedGoal != "Selecciona tu objetivo" &&
            selectedDays != "Selecciona días" &&
            selectedExperience != "Selecciona nivel"

    // Función para guardar datos del usuario
    fun saveUserData() {
        cargando = true
        alcanceCorrutina.launch {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val userId = currentUser.uid
                    val profileData = hashMapOf(
                        "nombre" to nombre,
                        "peso" to peso.toFloat(),
                        "altura" to altura.toFloat(),
                        "edad" to año.toInt(),
                        "objetivoFitness" to selectedGoal,
                        "diasEntrenamiento" to selectedDays.toInt(),
                        "nivelExperiencia" to selectedExperience
                    )

//                    Firebase.firestore.collection("perfilesUsuarios")
//                        .document(userId)
//                        .set(profileData)
//                        .await()

                    GLOBAL.guardarDatosRegistro(
                        userId, nombre, peso, altura, año, selectedGoal, selectedDays, selectedExperience
                    ) {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                        Toast.makeText(contexto, "Perfil guardado con éxito", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(contexto, "No se ha podido identificar al usuario", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(contexto, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                cargando = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título de la página
        Text(
            text = "Tu Perfil Físico",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campos de nombre, peso, altura y edad

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Nombre") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        OutlinedTextField(
            value = año,
            onValueChange = { año = it },
            label = { Text("Edad") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
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
            leadingIcon = { Icon(Icons.Default.MonitorWeight, contentDescription = "Peso") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Objetivo fitness
        Text(
            text = "Información de Entrenamiento",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
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

        Spacer(modifier = Modifier.height(32.dp))

        // Botón de guardar
        Button(
            onClick = { saveUserData() },
            modifier = Modifier.fillMaxWidth(),
            enabled = isFormValid && !cargando,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text(text = "Guardar y Continuar", color = Color.White)
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