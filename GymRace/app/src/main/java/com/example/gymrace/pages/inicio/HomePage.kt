package com.example.gymrace.pages.inicio

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.gymrace.MainActivity
import com.example.gymrace.R
import com.example.gymrace.RutinaLauncher
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun HomePage(modifier: Modifier = Modifier, navController: NavController, onThemeChange : () -> Unit) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Bienvenida
        item {
            Spacer(modifier = Modifier.height(64.dp))
            TitleSection()
            CalendarSection()
        }

        // Cargar rutinas predefinidas desde Firestore
        try {
            item {
                LoadPredefinedRoutinesFromFirestore(navController)
            }
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error loading routines", e)
        }
//        item {
//            LoadPredefinedRoutinesFromFirestore(navController)
//        }

        // Sección de rutina personalizada
        item {
            CustomRoutineSection(navController)
        }

        // Sección de más opciones
        item {
            Masoptions()
        }

        // Espaciado adicional al final
//        item {
//            Spacer(modifier = Modifier.height(60.dp))
//        }
    }
}


// Rutinas Predefinidas
// Esta clase representa una rutina predefinida
data class PredefinedRoutine(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageName: String = "",
    val exercises: List<MainActivity.ExerciseDetail> = emptyList()
)

// Esta función muestra una tarjeta para cada rutina predefinida
@Composable
fun PredefinedRoutineCard(
    routine: PredefinedRoutine,
    navController: NavController
) {
    // Estado para mostrar el diálogo
    var showDialog by remember { mutableStateOf(false) }

    // Estado para el lanzador de rutina
    val rutinaLauncher = remember { RutinaLauncher(navController, routine.id) }

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

    // Tarjeta de rutina predefinida
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
            // Contenido: título y botón
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
                    onClick = { rutinaLauncher.launch() },
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
    // Diálogo de detalles de la rutina
    if (showDialog) {
        PredefinedRoutineDetailDialog(
            routine = routine,
            navController = navController,
            onDismissRequest = { showDialog = false }
        )
    }
}


// Esta función muestra la sección de rutinas predefinidas
@Composable
fun PredefinedRoutinesSection(
    routines: List<PredefinedRoutine>,
    navController: NavController
) {
    // Si no hay rutinas, muestra un mensaje
    if (routines.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay rutinas predefinidas disponibles.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        return
    }
    // Título de la sección
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

// Esta función carga las rutinas predefinidas desde Firestore
@Composable
fun LoadPredefinedRoutinesFromFirestore(navController: NavController) {
    // Inicializa Firestore
    val db = FirebaseFirestore.getInstance()
    // Lista mutable para almacenar las rutinas
    val routines = remember { mutableStateListOf<PredefinedRoutine>() }
    // Estado para mostrar el indicador de carga
    val isLoading = remember { mutableStateOf(true) }
    // Efecto para cargar las rutinas al iniciar
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
    // Muestra un indicador de carga mientras se obtienen los datos
    if (isLoading.value) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator() // Indicador de carga
        }
    } else {
        PredefinedRoutinesSection(routines, navController) // Muestra las rutinas cargadas
    }
}

// Esta función muestra un diálogo con los detalles de la rutina predefinida
@Composable
fun PredefinedRoutineDetailDialog(
    routine: PredefinedRoutine,
    navController: NavController,
    onDismissRequest: () -> Unit
) {
    // Estado para el lanzador de rutina
    val rutinaLauncher = remember { RutinaLauncher(navController, routine.id) }

    // Diálogo de detalles
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = routine.title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = { // Contenido del diálogo
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
                    // Muestra cada ejercicio
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
        // Botón de confirmación
        confirmButton = {
            // Al presionar este botón se lanza la rutina usando tu RutinaLauncher
            rutinaLauncher.LaunchButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Iniciar Rutina"
            )
        },
        // Botón de cancelar
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cerrar")
            }
        }
    )
}


// Esta función muestra la sección de rutinas personalizadas
@Composable
fun CustomRoutineSection(navController: NavController) {
    // Título de la sección
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
    // Tarjetas para las rutinas personalizadas
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
                        // Navega a la página de mis rutinas
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
                        // Navega a la página de rutinas de amigos
                        Log.d("Navigation", "Navegando a rutinas de amigos")
                        navController.navigate("rutinasAmigos") {
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

        // Crear Rutina
        Card(
            modifier = Modifier
                .size(180.dp)
                .clickable {
                    // Navega a la página de crear rutina
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

// Esta función muestra el título de la aplicación
@Composable
fun TitleSection() {
    // Título de la aplicación
    Text(
        text = "Bienvenido a Gym Race",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(0.dp))
}

// Esta función muestra el calendario
@Composable
fun CalendarSection() {
    Column {
        Box(
            modifier = Modifier
                .padding(0.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Calendario
            ImprovedCalendar(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .zIndex(1f)
                    .padding(top = 55.dp)
                    .height(380.dp),
                onDateSelected = { day -> println("Día seleccionado: $day") }
            )
            // Imagen decorativa del calendario
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

// Clase para manejar las operaciones de calendario
class CalendarUtil {
    companion object {

        // Obtener el número de días en un mes
        fun getDaysInMonth(year: Int, month: Int): Int {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        // Obtener el primer día de la semana del mes
        fun getFirstDayOfMonth(year: Int, month: Int): Int {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            var dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
            if (dayOfWeek < 0) dayOfWeek += 7
            return dayOfWeek + 1
        }

        // Obtener el nombre del mes
        fun getMonthName(month: Int, locale: Locale = Locale("es", "ES")): String {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.MONTH, month)
            return SimpleDateFormat("MMMM", locale).format(calendar.time).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(locale) else it.toString()
            }
        }
    }
}

// Esta clase representa un mes y año
data class YearMonthCompat(val year: Int, val month: Int) {
    fun minusMonths(months: Int): YearMonthCompat {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.add(Calendar.MONTH, -months)
        return YearMonthCompat(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
    }

    fun plusMonths(months: Int): YearMonthCompat {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.add(Calendar.MONTH, months)
        return YearMonthCompat(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
    }

    fun lengthOfMonth(): Int {
        return CalendarUtil.getDaysInMonth(year, month)
    }

    companion object {
        fun now(): YearMonthCompat {
            val calendar = Calendar.getInstance()
            return YearMonthCompat(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
        }
    }
}

// Esta función muestra el calendario mejorado
@Composable
fun ImprovedCalendar(modifier: Modifier, onDateSelected: (Int) -> Unit) {
    // Estado para el mes y año actuales
    var currentYearMonth by remember { mutableStateOf(YearMonthCompat.now()) }

    // Obtener la fecha actual
    val calendar = remember { Calendar.getInstance() }
    val todayYear = remember { calendar.get(Calendar.YEAR) }
    val todayMonth = remember { calendar.get(Calendar.MONTH) }
    val todayDay = remember { calendar.get(Calendar.DAY_OF_MONTH) }

    // Verificar si es el mes actual
    val isCurrentMonth = currentYearMonth.year == todayYear && currentYearMonth.month == todayMonth

    // Obtener el número de días en el mes actual
    val daysInMonth = currentYearMonth.lengthOfMonth()

    // Obtener el primer día de la semana del mes actual
    val firstDayOfMonth = CalendarUtil.getFirstDayOfMonth(currentYearMonth.year, currentYearMonth.month)

    Column(modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Encabezado del calendario
        CalendarHeader(
            currentYearMonth,
            onPrevious = { currentYearMonth = currentYearMonth.minusMonths(1) },
            onNext = { currentYearMonth = currentYearMonth.plusMonths(1) }
        )
        // Rejilla del calendario
        CalendarGrid(daysInMonth, firstDayOfMonth, isCurrentMonth, todayDay, onDateSelected)
    }
}

// Esta función muestra el encabezado del calendario
@Composable
fun CalendarHeader(currentYearMonth: YearMonthCompat, onPrevious: () -> Unit, onNext: () -> Unit) {
    // Encabezado del calendario
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        // Botones para retroceder al mes anterior
        IconButton(onClick = onPrevious) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev") }
        // Título del mes y año
        Text(
            text = "${CalendarUtil.getMonthName(currentYearMonth.month)} ${currentYearMonth.year}",
            fontSize = 18.sp,
            modifier = Modifier
                .padding(0.dp)
                .wrapContentHeight()
                .align(Alignment.CenterVertically),
        )
        // Botón para avanzar al siguiente mes
        IconButton(onClick = onNext) { Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next") }
    }
}

// Esta función muestra la rejilla del calendario
@Composable
fun CalendarGrid(daysInMonth: Int, firstDayOfMonth: Int, isCurrentMonth: Boolean, todayDay: Int, onDateSelected: (Int) -> Unit) {
    // Días de la semana
    val days = listOf("L", "M", "X", "J", "V", "S", "D")
    // Encabezado de los días de la semana
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { day ->
            Text(day, fontWeight = FontWeight.Bold)
        }
    }
    // Rejilla del calendario
    LazyVerticalGrid(columns = GridCells.Fixed(7)) {
        // Espacios en blanco para los días antes del primer día del mes
        items(firstDayOfMonth - 1) { Box(modifier = Modifier.aspectRatio(1f)) }
        items(daysInMonth) { day ->
            // Espacio para cada día
            val dayNumber = day + 1
            // Verifica si el día es hoy
            val isToday = isCurrentMonth && dayNumber == todayDay
            // Cuadro para cada día
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
// Esta función muestra la sección de más opciones
@Composable
fun Masoptions() {
    // Manejo de URI y contexto
    val uriHandler = LocalUriHandler.current
    // Contexto de la aplicación
    val context = LocalContext.current
    // Sección de más opciones
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x001976d2)),
    ) {
        Text(
            text = "Más opciones",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        )
        // Tarjeta para compartir la aplicación
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
                // Texto para compartir la aplicación
                Text(
                    text = buildAnnotatedString {
                        append("Comparte nuestra app ")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.secondary, textDecoration = TextDecoration.Underline)) {
                            append("con tus amigos")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "¡Mira esta app!")
                                putExtra(Intent.EXTRA_TEXT, "¡Descarga esta fantástica aplicación! https://www.gymrace.sytes.net")
//                                putExtra(Intent.EXTRA_TEXT, "¡Descarga esta fantástica aplicación! https://play.google.com/store/apps/details?id=com.tuempresa.tuapp")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir usando"))
                        },
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Texto para visitar la página web
                Text(
                    text = buildAnnotatedString {
                        append("Visita nuestra ")
                        pushStringAnnotation(tag = "URL", annotation = "https://www.gymrace.sytes.net")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.secondary, textDecoration = TextDecoration.Underline)) {
                            append("página web")
                        }
                        pop()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://www.gymrace.sytes.net") },
                    fontSize = 16.sp
                )
            }
        }
        // Fondo decorativo
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