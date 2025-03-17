package np.com.bimalkafle.bottomnavigationdemo.pages

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.gymrace.R
import com.example.gymrace.pages.Exercise
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomePage(modifier: Modifier = Modifier, navController: NavController, onThemeChange : () -> Unit) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Espaciado inicial
        item {
            Spacer(modifier = Modifier.height(64.dp))
            TitleSection()
            CalendarSection()
        }

        // Cargar rutinas predefinidas desde Firestore
        item {
            LoadPredefinedExercisesFromFirestore()
        }

        // Rutinas por día
//        val dailyExercises = listOf(
//            Exercise("Piernas", R.drawable.rbiceps),
//            Exercise("Espalda", R.drawable.rbiceps)
//        )
//        item {
//            RoutineSection("Por día", dailyExercises)
//        }

        // Sección de rutina personalizada
        item {
            CustomRoutineSection(navController)
        }

        // Caja para Settings Page (si es una sección adicional)
        item {
            Masoptions()
        }

        // Espaciado adicional al final
        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

data class Exercise(
    val name: String,
    val imageId: Int
)

data class ExerciseFromFirestore(
    val title: String = "",
    val imageName: String = ""
)

@Composable
fun LoadPredefinedExercisesFromFirestore() {
    val db = FirebaseFirestore.getInstance()
    val exercises = remember { mutableStateListOf<Exercise>() }
    val isLoading = remember { mutableStateOf(true) }

    // Obtén las rutinas desde Firestore
    LaunchedEffect(true) {
        db.collection("rutinaspredefinidas")
            .get()
            .addOnSuccessListener { result ->
                exercises.clear()
                for (document in result) {
                    try {
                        val exercise = document.toObject<ExerciseFromFirestore>()
                        val imageId = when (exercise.imageName) {
                            "rbiceps" -> R.drawable.rbiceps
                            "rabdomen" -> R.drawable.rabdomen
                            "rpecho" -> R.drawable.rpecho
                            "respalda" -> R.drawable.respalda
                            "rcuadriceps" -> R.drawable.rcuadriceps
                            "rgluteos" -> R.drawable.rgluteos
                            else -> R.drawable.default_image // Imagen por defecto
                        }
                        exercises.add(Exercise(exercise.title, imageId))
                    } catch (e: Exception) {
                        Log.e("FirestoreError", "Error parsing document: ${document.id}", e)
                    }
                }
                isLoading.value = false
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Error getting documents", e)
                isLoading.value = false
            }
    }

    if (isLoading.value) {
        // Aquí puedes mostrar un indicador de carga
        CircularProgressIndicator()
    } else {
        // Mostrar las rutinas predefinidas una vez que se carguen
        RoutineSection("Rutinas predefinidas", exercises)
    }
}

@Composable
fun TitleSection() {
    Text(
        text = "Bienvenido a Gym Race",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(0.dp))
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarSection() {
    val today = remember { LocalDate.now() }
    Column {
        Box(
            modifier = Modifier
                .padding(0.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ImprovedCalendar(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .zIndex(1f)
                    .padding(top = 55.dp)
                    .height(380.dp),
                onDateSelected = { day -> println("Día seleccionado: $day") }
            )
            Image(
                painter = painterResource(id = R.drawable.deco4),
                contentDescription = "Imagen calendario",
                modifier = Modifier
                    .size(350.dp)
                    .padding(0.dp)
                    .zIndex(0f)
                    .align(Alignment.Center),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun RoutineSection(title: String, exercises: List<Exercise>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
        ) {
            items(exercises.size) { index ->
                RoutineCard(exercises[index]) { exercise, dismiss ->
                    CustomDataDialog(
                        title = exercise.name,
                        imageId = exercise.imageId,
                        dataBelow = {
                            ExerciseContent(exercise.name)
                        },
                        onDismiss = dismiss
                    )
                }
            }
        }
    }
}



fun informacionRutina(rutina: String): String {
    if (rutina == "Brazo") {
        return "Rutina completa para brazos enfocada en bíceps, tríceps y antebrazos"
    } else if (rutina == "Abdomen") {
        return "Rutina intensiva para fortalecer y definir el core"
    } else if (rutina == "Pecho") {
        return "Rutina para desarrollar masa muscular en el pecho"
    } else if (rutina == "Espalda") {
        return "Rutina de espalda para mejorar fuerza y postura"
    } else if (rutina == "Piernas") {
        return "Rutina completa de piernas para todos los grupos musculares"
    } else if (rutina == "Gluteos") {
        return "Rutina especializada para tonificar y fortalecer glúteos"
    }
    return "Información no disponible"
}




// Diálogo personalizado mejorado para mostrar información de la rutina
@Composable
fun CustomDataDialog(
    title: String,
    imageId: Int,
    dataBelow: @Composable () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Botón de cerrar en la esquina superior izquierda
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(48.dp)
                    .background(Color(0xFFF0F0F0), shape = CircleShape)
                    .zIndex(10f)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Cerrar",
                    tint = Color(0xFF505050),
                    modifier = Modifier.size(32.dp)
                )
            }

            // Contenido principal
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 0.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título del ejercicio centrado
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF303030),
                    modifier = Modifier
                        .padding(top = 48.dp, bottom = 16.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Imagen del ejercicio más pequeña y centrada
                Card(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = imageId),
                        contentDescription = "Imagen de $title",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contenido de información del ejercicio
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color(0xFFF8F8F8)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        dataBelow()
                    }
                }
            }
        }
    }
}

// Contenido personalizado para mostrar dentro del diálogo
@Composable
fun ExerciseContent(exerciseName: String) {
    // Get routine details based on the exercise name
    val routineDetails = getRoutineDetails(exerciseName)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Sección de información general
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Información de la rutina",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF303030)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = informacionRutina(exerciseName),
                    fontSize = 16.sp,
                    color = Color(0xFF505050)
                )
            }
        }

        // Sección de ejercicios
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Updated to separate exercise name and details
            item {
                val (exerciseName1, exerciseDetails1) = separateNameAndDetails(routineDetails.exercise1.description)
                EjercicioCard(
                    numero = 1,
                    nombre = exerciseName1,
                    descripcion = exerciseDetails1,
                    imagenId = routineDetails.exercise1.imageId
                )
            }
            item {
                val (exerciseName2, exerciseDetails2) = separateNameAndDetails(routineDetails.exercise2.description)
                EjercicioCard(
                    numero = 2,
                    nombre = exerciseName2,
                    descripcion = exerciseDetails2,
                    imagenId = routineDetails.exercise2.imageId
                )
            }
            item {
                val (exerciseName3, exerciseDetails3) = separateNameAndDetails(routineDetails.exercise3.description)
                EjercicioCard(
                    numero = 3,
                    nombre = exerciseName3,
                    descripcion = exerciseDetails3,
                    imagenId = routineDetails.exercise3.imageId
                )
            }
            item {
                val (exerciseName4, exerciseDetails4) = separateNameAndDetails(routineDetails.exercise4.description)
                EjercicioCard(
                    numero = 4,
                    nombre = exerciseName4,
                    descripcion = exerciseDetails4,
                    imagenId = routineDetails.exercise4.imageId
                )
            }
            item {
                val (exerciseName5, exerciseDetails5) = separateNameAndDetails(routineDetails.exercise5.description)
                EjercicioCard(
                    numero = 5,
                    nombre = exerciseName5,
                    descripcion = exerciseDetails5,
                    imagenId = routineDetails.exercise5.imageId
                )
            }
        }
    }
}

fun separateNameAndDetails(description: String): Pair<String, String> {
    return if (description.contains(":")) {
        val parts = description.split(":", limit = 2)
        val name = parts[0].trim()
        val details = parts[1].trim()
        Pair(name, details)
    } else {
        Pair("Ejercicio", description)  // Fallback if description doesn't follow expected format
    }
}


data class ExerciseDetail(
    val description: String = "Información no disponible",
    val imageId: Int = R.drawable.default_image
)

data class RoutineDetails(
    val exercise1: ExerciseDetail = ExerciseDetail(),
    val exercise2: ExerciseDetail = ExerciseDetail(),
    val exercise3: ExerciseDetail = ExerciseDetail(),
    val exercise4: ExerciseDetail = ExerciseDetail(),
    val exercise5: ExerciseDetail = ExerciseDetail()
)



fun getRoutineDetails(routineName: String): RoutineDetails {
    return when (routineName) {
        "Brazo" -> RoutineDetails(
            exercise1 = ExerciseDetail("Curl de bíceps con mancuernas: 3 series de 10 repeticiones", R.drawable.default_image),
            exercise2 = ExerciseDetail("Curl de tríceps con polea: 3 series de 10 repeticiones", R.drawable.default_image),
            exercise3 = ExerciseDetail("Extensiones de antebrazo: 3 series de 10 repeticiones", R.drawable.default_image),
            exercise4 = ExerciseDetail("Press de hombros: 3 series de 10 repeticiones", R.drawable.default_image),
            exercise5 = ExerciseDetail("Curl martillo: 3 series de 10 repeticiones", R.drawable.default_image)
        )
        "Abdomen" -> RoutineDetails(
            exercise1 = ExerciseDetail("Crunches: 4 series de 15 repeticiones", R.drawable.default_image),
            exercise2 = ExerciseDetail("Plancha lateral: 4 series de 30 segundos", R.drawable.default_image),
            exercise3 = ExerciseDetail("Elevaciones de piernas: 4 series de 15 repeticiones", R.drawable.default_image),
            exercise4 = ExerciseDetail("Russian twist: 4 series de 20 repeticiones", R.drawable.default_image),
            exercise5 = ExerciseDetail("Plancha: 4 series de 45 segundos", R.drawable.default_image)
        )
        "Pecho" -> RoutineDetails(
            exercise1 = ExerciseDetail("Press banca: 3 series de 12 repeticiones", R.drawable.default_image),
            exercise2 = ExerciseDetail("Aperturas con mancuernas: 3 series de 12 repeticiones", R.drawable.default_image),
            exercise3 = ExerciseDetail("Fondos en paralelas: 3 series de 12 repeticiones", R.drawable.default_image),
            exercise4 = ExerciseDetail("Press inclinado: 3 series de 12 repeticiones", R.drawable.default_image),
            exercise5 = ExerciseDetail("Pullover: 3 series de 12 repeticiones", R.drawable.default_image)
        )
        "Espalda" -> RoutineDetails(
            exercise1 = ExerciseDetail("Remo con barra: 4 series de 10 repeticiones", R.drawable.default_image),
            exercise2 = ExerciseDetail("Dominadas: 4 series de 8 repeticiones", R.drawable.default_image),
            exercise3 = ExerciseDetail("Remo con mancuerna: 4 series de 10 repeticiones", R.drawable.default_image),
            exercise4 = ExerciseDetail("Jalón al pecho: 4 series de 10 repeticiones", R.drawable.default_image),
            exercise5 = ExerciseDetail("Peso muerto: 4 series de 8 repeticiones", R.drawable.default_image)
        )
        "Piernas" -> RoutineDetails(
            exercise1 = ExerciseDetail("Sentadillas: 3 series de 12 repeticiones", R.drawable.default_image),
            exercise2 = ExerciseDetail("Prensa de piernas: 3 series de 12 repeticiones", R.drawable.default_image),
            exercise3 = ExerciseDetail("Extensión de cuádriceps: 3 series de 12 repeticiones", R.drawable.default_image),
            exercise4 = ExerciseDetail("Curl de isquiotibiales: 3 series de 12 repeticiones", R.drawable.default_image),
            exercise5 = ExerciseDetail("Elevación de gemelos: 3 series de 15 repeticiones", R.drawable.default_image)
        )
        "Gluteos" -> RoutineDetails(
            exercise1 = ExerciseDetail("Hip thrust: 4 series de 15 repeticiones", R.drawable.default_image),
            exercise2 = ExerciseDetail("Sentadilla sumo: 4 series de 15 repeticiones", R.drawable.default_image),
            exercise3 = ExerciseDetail("Patada de glúteo: 4 series de 15 repeticiones", R.drawable.default_image),
            exercise4 = ExerciseDetail("Elevaciones de cadera: 4 series de 15 repeticiones", R.drawable.default_image),
            exercise5 = ExerciseDetail("Peso muerto rumano: 4 series de 12 repeticiones", R.drawable.default_image)
        )
        else -> RoutineDetails()
    }
}



// Tarjeta individual para cada ejercicio dentro de la rutina
@Composable
fun EjercicioCard(numero: Int, nombre: String, descripcion: String, imagenId: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Número de ejercicio
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF505050), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = numero.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información del ejercicio
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF303030)
                )

                Text(
                    text = descripcion,
                    fontSize = 14.sp,
                    color = Color(0xFF505050)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Imagen del ejercicio
            Image(
                painter = painterResource(id = imagenId),
                contentDescription = "Imagen ejercicio",
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

// Tarjetas de rutina mejoradas
@Composable
fun RoutineCard(
    exercise: Exercise,
    dialogContent: @Composable (Exercise, () -> Unit) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        dialogContent(exercise, { showDialog = false })
    }

    Card(
        modifier = Modifier
            .width(250.dp)
            .height(200.dp)
            .clickable { showDialog = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF303030)
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Imagen de fondo ocupando todo el espacio
            Image(
                painter = painterResource(id = exercise.imageId),
                contentDescription = "Imagen músculo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Overlay oscuro semi-transparente
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
            )

            // Título con sombra simulada usando múltiples textos superpuestos
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(50.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Texto sombra
                Text(
                    text = exercise.name,
                    color = Color.Black,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(1.dp, 1.dp),


                )

                // Texto principal
                Text(
                    text = exercise.name,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Botón en la parte inferior
//            Box(
//                modifier = Modifier
//                    .align(Alignment.BottomEnd)
//                    .padding(16.dp)
//                    .clip(RoundedCornerShape(8.dp))
//                    .background(Color(0xFFFF5722))
//                    .padding(horizontal = 12.dp, vertical = 8.dp)
//            ) {
//                Text(
//                    text = "Comenzar",
//                    color = Color.White,
//                    fontWeight = FontWeight.Bold,
//                    fontSize = 14.sp
//                )
//            }
        }
    }
}


@Composable
fun CustomRoutineSection(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Rutinas Personalizadas",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Mis Rutinas
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
                    .padding(end = 8.dp)
                    .clickable {
                        Log.d("Navigation", "Navegando a mis rutinas")
                        navController.navigate("misRutinas")
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
//                    Icon(
//                        painter = painterResource(id = R.drawable.default_image),
//                        contentDescription = "Mis Rutinas",
//                        modifier = Modifier.size(64.dp),
//                        tint = Color.White
//                    )
                    Image(
                        painter = painterResource(id = R.drawable.default_image),
                        contentDescription = "Mis Rutinas",
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mis Rutinas",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Rutinas de Amigos
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
                    .padding(start = 8.dp)
                    .clickable {
                        Log.d("Navigation", "Navegando a la página de crear rutina")
                        navController.navigate("misRutinas") {
                            popUpTo(0) { inclusive = true } // Borra todo el historial de navegación
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
//                    Icon(
//                        painter = painterResource(id = R.drawable.default_image),
//                        contentDescription = "Rutinas de Amigos",
//                        modifier = Modifier.size(64.dp),
//                        tint = Color.White
//                    )
                    Image(
                        painter = painterResource(id = R.drawable.default_image),
                        contentDescription = "Rutinas de Amigos",
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rutinas de Amigos",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Crear Rutina (mantiene la funcionalidad existente)
        Card(
            modifier = Modifier
                .size(180.dp)
                .clickable {
                    Log.d("Navigation", "Navegando a la página de crear rutina")
                    navController.navigate("crearRutina") {
                        popUpTo(0) { inclusive = true } // Borra todo el historial de navegación
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFF5722)
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "+",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        text = "Crear Rutina",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ImprovedCalendar(modifier: Modifier, onDateSelected: (Int) -> Unit) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now() }
    val isCurrentMonth = currentYearMonth.year == today.year && currentYearMonth.month == today.month
    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstDayOfMonth = currentYearMonth.atDay(1).dayOfWeek.value
    Column(modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CalendarHeader(
            currentYearMonth,
            onPrevious = { currentYearMonth = currentYearMonth.minusMonths(1) },
            onNext = { currentYearMonth = currentYearMonth.plusMonths(1) }
        )
        CalendarGrid(daysInMonth, firstDayOfMonth, isCurrentMonth, today, onDateSelected)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarHeader(currentYearMonth: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onPrevious) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev") }
        Text(
            text = "${currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "ES"))} ${currentYearMonth.year}",
            fontSize = 18.sp
        )
        IconButton(onClick = onNext) { Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next") }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarGrid(daysInMonth: Int, firstDayOfMonth: Int, isCurrentMonth: Boolean, today: LocalDate, onDateSelected: (Int) -> Unit) {
    val days = listOf("L", "M", "X", "J", "V", "S", "D")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { Text(it, fontWeight = FontWeight.Bold) }
    }
    LazyVerticalGrid(columns = GridCells.Fixed(7)) {
        items(firstDayOfMonth - 1) { Box(modifier = Modifier.aspectRatio(1f)) }
        items(daysInMonth) { day ->
            val dayNumber = day + 1
            val isToday = isCurrentMonth && dayNumber == today.dayOfMonth
            Box(
                modifier = Modifier
                    .padding(0.dp)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(if (isToday) Color(0xff753c12) else Color.Transparent)
                    .clickable { onDateSelected(dayNumber) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "$dayNumber", color = if (isToday) Color.White else Color.Black)
            }
        }
    }
}

@Composable
fun Masoptions() {
    val uriHandler = LocalUriHandler.current
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
//            .padding(horizontal = 16.dp, vertical = 16.dp)
            .background(Color(0x001976d2)),
    ) {
        Text(
            text = "Más opciones",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = buildAnnotatedString {
                        append("Unete a nuestra comunidad en ")
                        pushStringAnnotation(tag = "URL", annotation = "https://discord.gg/GwKP9ghQSg")
                        withStyle(style = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)) {
                            append("discord")
                        }
                        pop()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://discord.gg/GwKP9ghQSg") },
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = buildAnnotatedString {
                        append("Visita nuestra ")
                        pushStringAnnotation(tag = "URL", annotation = "https://www.ejemplo.com")
                        withStyle(style = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)) {
                            append("página web")
                        }
                        pop()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://www.ejemplo.com") },
                    fontSize = 16.sp
                )
            }
        }

        Image(
            painter = painterResource(id = R.drawable.deco2),
            contentDescription = "Imagen abajo inicio",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(0.dp)
                .fillMaxWidth()
                .wrapContentHeight()
        )
    }
}