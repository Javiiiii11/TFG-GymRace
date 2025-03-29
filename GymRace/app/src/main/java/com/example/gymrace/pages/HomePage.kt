package np.com.bimalkafle.bottomnavigationdemo.pages

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.gymrace.MainActivity
import com.example.gymrace.R
import com.example.gymrace.RutinaLauncher
import com.example.gymrace.pages.Exercise
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
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
            LoadPredefinedRoutinesFromFirestore(navController)

        }

        // Sección de rutina personalizada
        item {
            CustomRoutineSection(navController)
        }

        // Sección de más opciones
        item {
            Masoptions()
        }

        // Espaciado adicional al final
        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}




// Rutinas Predefinidas

// Firestore Data Classes
data class PredefinedRoutine(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageName: String = "",
    val exercises: List<MainActivity.ExerciseDetail> = emptyList()
)

@Composable
fun PredefinedRoutineCard(
    routine: PredefinedRoutine,
    navController: NavController
) {
    var showDialog by remember { mutableStateOf(false) }

    // Obtención del recurso de imagen basado en el nombre
    val imageResourceId = when (routine.imageName) {
        "rbiceps" -> R.drawable.rbiceps
        "rabdomen" -> R.drawable.rabdomen
        "rpecho" -> R.drawable.rpecho
        "respalda" -> R.drawable.respalda
        "rcuadriceps" -> R.drawable.rcuadriceps
        "rgluteos" -> R.drawable.rgluteos
        else -> R.drawable.default_image
    }

    Card(
        modifier = Modifier
            .width(300.dp)
            .height(250.dp)
            .clickable { showDialog = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF303030)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Imagen de fondo
            Image(
                painter = painterResource(id = imageResourceId),
                contentDescription = routine.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Capa oscura
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
            )
            // Contenido: título y botón (este último se puede omitir, ya que el diálogo se abre al hacer clic en la tarjeta)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = routine.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { showDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Iniciar Rutina"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iniciar Rutina")
                }
            }
        }
    }

    if (showDialog) {
        PredefinedRoutineDetailDialog(
            routine = routine,
            navController = navController,
            onDismissRequest = { showDialog = false }
        )
    }
}



@Composable
fun PredefinedRoutinesSection(
    routines: List<PredefinedRoutine>,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Rutinas Predefinidas",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
        ) {
            items(routines.size) { index ->
                PredefinedRoutineCard(
                    routine = routines[index],
                    navController = navController
                )
            }
        }
    }
}


@Composable
fun LoadPredefinedRoutinesFromFirestore(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val routines = remember { mutableStateListOf<PredefinedRoutine>() }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(true) {
        try {
            val result = db.collection("rutinaspredefinidas")
                .get()
                .await()

            routines.clear()
            for (document in result.documents) {
                val routine = document.toObject(PredefinedRoutine::class.java)?.copy(id = document.id)
                routine?.let { routines.add(it) }
            }
            isLoading.value = false
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error getting routines", e)
            isLoading.value = false
        }
    }

    if (isLoading.value) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        PredefinedRoutinesSection(routines, navController)
    }
}


@Composable
fun PredefinedRoutineDetailDialog(
    routine: PredefinedRoutine,
    navController: NavController,
    onDismissRequest: () -> Unit
) {
    // Utiliza la clase RutinaLauncher que ya tienes para iniciar la rutina.
    // Nota: Asegúrate de que la ruta utilizada en RutinaLauncher ("ejecutar_rutina/$rutinaId")
    // sea la misma que espera EjecutarRutinaPage en tu NavHost.
    val rutinaLauncher = remember { RutinaLauncher(navController, routine.id) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = routine.title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Muestra la descripción de la rutina
                Text(text = routine.description)
                Spacer(modifier = Modifier.height(8.dp))
                // Lista de ejercicios (nombre, repeticiones y series)
                if (routine.exercises.isNotEmpty()) {
                    Text(
                        text = "Ejercicios a realizar:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    routine.exercises.forEach { exercise ->
                        Row() {
                            Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = null)

                            Text(
                                text = " ${exercise.name}",
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            // Al presionar este botón se lanza la rutina usando tu RutinaLauncher
            rutinaLauncher.LaunchButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Iniciar Rutina"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cerrar")
            }
        }
    )
}































































// Rutinas por  Personalizadas

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
                            popUpTo("main") {
                                inclusive = false
                            } // Borra todo el historial de navegación
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
                        launchSingleTop = true
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
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
        days.forEach { Text( it, fontWeight = FontWeight.Bold)}
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
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(4.dp)

        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = buildAnnotatedString {
                        append("Unete a nuestra comunidad en ")
                        pushStringAnnotation(tag = "URL", annotation = "https://discord.gg/GwKP9ghQSg")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.secondary, textDecoration = TextDecoration.Underline)) {
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
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.secondary, textDecoration = TextDecoration.Underline)) {
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