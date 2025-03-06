package np.com.bimalkafle.bottomnavigationdemo.pages

import android.os.Build
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrace.R
import com.example.gymrace.pages.Exercise
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomePage(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Espaciado inicial
        item {
            Spacer(modifier = Modifier.height(64.dp))
            TitleSection()
            CalendarSection()
        }

        // Rutinas predefinidas
        val predefinedExercises = listOf(
            Exercise("Biceps", R.drawable.rbiceps),
            Exercise("Abdomen", R.drawable.rabdomen),
            Exercise("Pecho", R.drawable.rpecho),
            Exercise("Espalda", R.drawable.respalda),
            Exercise("Piernas", R.drawable.rcuadriceps),
            Exercise("Gluteos", R.drawable.rgluteos)
        )
        item {
            RoutineSection("Rutinas predefinidas", predefinedExercises)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color(0x001976d2)),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = "Más opciones",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Espaciado adicional al final
        item {
            Spacer(modifier = Modifier.height(95.dp))
        }
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
    Spacer(modifier = Modifier.height(16.dp))
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarSection() {
    ImprovedCalendar(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(450.dp),
        onDateSelected = { day -> println("Día seleccionado: $day") }
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun RoutineSection(title: String, exercises: List<Exercise>) {
    Text(text = title, fontSize = 18.sp, color = Color.Gray)
    Spacer(modifier = Modifier.height(10.dp))
    LazyRow {
        items(exercises.size) { index ->
            RoutineCard(exercises[index])
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
}


@Composable
fun RoutineCard(exercise: Exercise) {
    Button(
        onClick = { },
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
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
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
    Text(text = "Crea tu rutina personalizada", fontSize = 18.sp, color = Color.Gray)
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
        CalendarHeader(currentYearMonth, { currentYearMonth = currentYearMonth.minusMonths(1) }, { currentYearMonth = currentYearMonth.plusMonths(1) })
        CalendarGrid(daysInMonth, firstDayOfMonth, isCurrentMonth, today, onDateSelected)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarHeader(currentYearMonth: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onPrevious) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev") }
        Text(text = "${currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "ES"))} ${currentYearMonth.year}", fontSize = 18.sp)
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
                    .padding(4.dp)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(if (isToday) Color(0xFFE91E63) else Color.Transparent)
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings Page",
            fontSize = 40.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}
