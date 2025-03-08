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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
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
fun HomePage(modifier: Modifier = Modifier) {
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
        val dailyExercises = listOf(
            Exercise("Piernas", R.drawable.rbiceps),
            Exercise("Espalda", R.drawable.rbiceps)
        )
        item {
            RoutineSection("Por día", dailyExercises)
        }

        // Sección de rutina personalizada
        item {
            CustomRoutineSection()
        }

        // Caja para Settings Page (si es una sección adicional)
        item {
            Masoptions()
        }

        // Espaciado adicional al final
        item {
            Spacer(modifier = Modifier.height(95.dp))
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
        color = Color.Black
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
    Spacer(modifier = Modifier.height(5.dp))
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )

    Spacer(modifier = Modifier.height(10.dp))
    LazyRow (
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ){
        items(exercises.size) { index ->
            // Aquí se define el contenido del diálogo personalizado para cada tarjeta.
            RoutineCard(exercises[index]) { exercise, dismiss ->
                CustomDataDialog(
                    title = exercise.name,
                    imageId = exercise.imageId,
                    dataBelow = {
                        // Ejemplo de contenido personalizado debajo de la imagen:
                        LazyColumn {
                            item {
                                Text(
                                    text = "Información de la rutina",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = informacionRutina(exercise.name),
                                    fontSize = 16.sp,
                                    color = Color.Black,
                                    modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 16.dp)
                                )
                                Row {
                                    Text(
                                        text = "Ejercicio 1: " + ejercicio1rutina(exercise.name),
                                        fontSize = 16.sp,
                                        color = Color.Black,
                                        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 16.dp)
                                    )
                                    Image(
                                        //imagen del metodo de ejercicio
                                        painter = painterResource(id = R.drawable.deco4), //mal
                                        contentDescription = "Imagen músculo",
                                        modifier = Modifier
                                            .size(150.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .padding(0.dp, 0.dp, 0.dp, 16.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = dismiss,

                                        modifier = Modifier
                                            .padding(16.dp)
                                            .height(50.dp)
                                            .width(200.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(Color(0xff505050)),

                                    ) {
                                        Text(text = "Cerrar", color = Color.White, fontSize = 20.sp)
                                    }
                                }
                            }
                        }
                    },
                    onDismiss = dismiss
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
}

fun ejercicio1rutina(rutina: String): String {
    if (rutina == "Brazos") {
        return "Rutina de bíceps: 3 series de 10 repeticiones"
    } else if (rutina == "Abdomen") {
        return "Rutina de abdomen: 4 series de 15 repeticiones"
    } else if (rutina == "Pecho") {
        return "Rutina de pecho: 3 series de 12 repeticiones"
    } else if (rutina == "Espalda") {
        return "Rutina de espalda: 4 series de 10 repeticiones"
    } else if (rutina == "Piernas") {
        return "Rutina de piernas: 3 series de 12 repeticiones"
    } else if (rutina == "Gluteos") {
        return "Rutina de glúteos: 4 series de 15 repeticiones"
    }
    return "Información no disponible"
}

fun informacionRutina(rutina: String): String {
    if (rutina == "Brazos") {
        return "Rutina de bíceps: 3 series de 10 repeticiones"
    } else if (rutina == "Abdomen") {
        return "Rutina de abdomen: 4 series de 15 repeticiones"
    } else if (rutina == "Pecho") {
        return "Rutina de pecho: 3 series de 12 repeticiones"
    } else if (rutina == "Espalda") {
        return "Rutina de espalda: 4 series de 10 repeticiones"
    } else if (rutina == "Piernas") {
        return "Rutina de piernas: 3 series de 12 repeticiones"
    } else if (rutina == "Gluteos") {
        return "Rutina de glúteos: 4 series de 15 repeticiones"
    }
    return "Información no disponible"
}

// Diálogo personalizado para mostrar información adicional de la rutina
@Composable
fun CustomDataDialog(
    title: String,
    imageId: Int,
    dataBelow: @Composable () -> Unit, // Bloque composable para el contenido extra
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Título del diálogo (rutina)
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // Imagen principal de la rutina
                Image(
                    painter = painterResource(id = imageId),
                    contentDescription = "Imagen rutina",
                    modifier = Modifier
                        .size(300.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Contenido personalizado debajo de la imagen
                dataBelow()
            }
        }
    }
}

// Modificación de RoutineCard para usar el diálogo personalizado
@Composable
fun RoutineCard(
    exercise: Exercise,
    dialogContent: @Composable (Exercise, () -> Unit) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        dialogContent(exercise, { showDialog = false })
    }

    Button(
        onClick = { showDialog = true },
        modifier = Modifier
            .padding(16.dp)
            .height(200.dp)
            .width(250.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(Color(0xff505050))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = exercise.name,
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            )
            Image(
                painter = painterResource(id = exercise.imageId),
                contentDescription = "Imagen músculo",
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.BottomEnd),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun CustomRoutineSection() {
    Text(
        text = "Crea tu rutina personalizada",
        fontSize = 18.sp,
        color = Color.Black,
        fontWeight = FontWeight.Bold,
    )
    Button(
        onClick = { },
        modifier = Modifier
            .padding(16.dp)
            .height(200.dp)
            .width(200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(Color(0xff505050))
    ) {
        Text(text = "+", color = Color.White, fontSize = 35.sp)
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
            .fillMaxSize()
            .padding(0.dp)
            .background(Color(0x001976d2)),
    ) {
        Text(
            text = "Más opciones",
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            modifier = Modifier.padding(16.dp)
        )
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
                .padding(16.dp)
                .clickable { uriHandler.openUri("https://discord.gg/GwKP9ghQSg") }
        )
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
                .padding(16.dp)
                .clickable { uriHandler.openUri("https://www.ejemplo.com") }
        )
        Image(
            painter = painterResource(id = R.drawable.deco2),
            contentDescription = "Imagen abajo inicio",
            contentScale = ContentScale.Crop, // O utiliza ContentScale.Fit según el efecto deseado
            modifier = Modifier
                .padding(0.dp)
                .fillMaxWidth()
                .wrapContentHeight()
        )
    }
}
