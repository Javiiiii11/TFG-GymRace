package np.com.bimalkafle.bottomnavigationdemo.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrace.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserPage(modifier: Modifier = Modifier, onThemeChange: () -> Unit) {
    val scrollState = rememberScrollState()
    var showThemeMenu by remember { mutableStateOf(false) }
    var isDarkTheme by rememberSaveable { mutableStateOf(false) }



    // Estados para datos del usuario
    var userName by remember { mutableStateOf("Cargando...") }
    var userWeight by remember { mutableStateOf("") }
    var userHeight by remember { mutableStateOf("") }
    var userAge by remember { mutableStateOf("") }
    var userFitnessGoal by remember { mutableStateOf("") }
    var userTrainingDays by remember { mutableStateOf("") }
    var userExperienceLevel by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Estado para saber si el usuario está registrado con Google
    var isGoogleUser by remember { mutableStateOf(false) }

    // Efecto para cargar los datos del usuario desde Firebase
    LaunchedEffect(key1 = Unit) {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // Comprobar si el usuario inició sesión con Google
                isGoogleUser = currentUser.providerData.any { it.providerId == "google.com" }

                val userDocument = Firebase.firestore
                    .collection("usuarios")
                    .document(currentUser.uid)
                    .get()
                    .await()

                if (userDocument.exists()) {
                    // Actualizar variables globales
                    GLOBAL.id = currentUser.uid
                    GLOBAL.nombre = userDocument.getString("nombre") ?: ""
                    GLOBAL.peso = userDocument.getString("peso") ?: ""
                    GLOBAL.altura = userDocument.getString("altura") ?: ""
                    GLOBAL.edad = userDocument.getString("edad") ?: ""
                    GLOBAL.objetivoFitness = userDocument.getString("objetivoFitness") ?: ""
                    GLOBAL.diasEntrenamientoPorSemana = userDocument.getString("diasEntrenamientoPorSemana") ?: ""
                    GLOBAL.nivelExperiencia = userDocument.getString("nivelExperiencia") ?: ""

                    // Actualizar estado local
                    userName = GLOBAL.nombre
                    userWeight = GLOBAL.peso
                    userHeight = GLOBAL.altura
                    userAge = GLOBAL.edad
                    userFitnessGoal = GLOBAL.objetivoFitness
                    userTrainingDays = GLOBAL.diasEntrenamientoPorSemana
                    userExperienceLevel = GLOBAL.nivelExperiencia
                }
            }
        } catch (e: Exception) {
            println("Error al cargar datos de usuario: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mi Perfil",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1E1E)
            )

            Box {
                IconButton(onClick = { showThemeMenu = !showThemeMenu }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración",
                        tint = Color(0xFF1E1E1E)
                    )
                }
                // Menú desplegable para cambiar tema
                DropdownMenu(
                    expanded = showThemeMenu,
                    onDismissRequest = { showThemeMenu = false },
                    modifier = Modifier
                        .background(Color.White)
                        .width(200.dp)
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
                ) {
                    Text(
                        text = "Ajustes",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Divider()
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.DarkMode,
                                    contentDescription = "Cambiar tema",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Cambiar tema")
                            }
                        },
                        onClick = {
                            onThemeChange()
                            showThemeMenu = false
                        }
                    )
                }
            }
        }

        // Indicador de carga
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp),
                color = Color(0xffff9241)
            )
        }

        // Profile Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Foto de perfil
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.profile_avatar),
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xffff9241), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                // Nombre del usuario
                Text(
                    text = userName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1E1E),
                    modifier = Modifier.padding(top = 8.dp)
                )
                // Fila de estadísticas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(count = userWeight, label = "Kg")
                    StatItem(count = userHeight, label = "cm")
                    StatItem(count = userAge, label = "años")
                }
                // Botones de acción: editar y compartir perfil
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { /* TODO: Acción para editar perfil */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text(text = "Editar perfil")
                    }
                    Button(
                        onClick = { /* TODO: Acción para compartir perfil */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = "Compartir")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sección de Información Personal
        ProfileSection(
            title = "Información personal",
            icon = Icons.Outlined.Person,
            content = {
                InfoItem(label = "Objetivo Fitness", value = userFitnessGoal)
                InfoItem(label = "Días por semana", value = userTrainingDays)
                InfoItem(label = "Experiencia", value = userExperienceLevel)
                InfoItem(
                    label = "Fecha de registro",
                    value = SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(Date())
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sección de Información de la cuenta
        ProfileSection(
            title = "Información de la cuenta",
            icon = Icons.Default.Settings,
            content = {
                // Botón para cambiar de cuenta

                Button(
                    onClick = {
                        // TODO: Implementar lógica para cambiar de cuenta
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xffff9241))
                ) {
                    Text(text = "Cambiar de cuenta", color = Color(0xffff9241))
                }

                // Botón para cerrar sesión

                Button(
                    onClick = {
                        // TODO: Implementar lógica para cerrar sesión
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xffff9241)),
                    border = BorderStroke(1.dp, Color(0xffff9241))
                ) {
                    Text(text = "Cerrar sesión", color = Color.White)
                }

            }
        )


        Spacer(modifier = Modifier.height(75.dp))
    }
}

@Composable
fun StatItem(count: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1E1E)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF757575)
        )
    }
}

@Composable
fun ProfileSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xffff9241),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E1E)
                    )
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Contraer" else "Expandir",
                        tint = Color(0xFF757575)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF757575)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E1E1E)
        )
    }
}

@Composable
fun ChipGroup(items: List<String>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                Chip(text = item)
            }
        }
    }
}

@Composable
fun Chip(text: String) {
    Surface(
        modifier = Modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE8EAF6)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color(0xffff9241),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
@Composable
fun MyApp() {
    var isDarkTheme by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    ) {
        UserPage(onThemeChange = { isDarkTheme = !isDarkTheme })
    }
}

// FlowRow implementation since it might not be directly available
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val horizontalGapPx = 0
        val verticalGapPx = 0

        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        val rowWidths = mutableListOf<Int>()

        var rowHeight = 0
        var rowWidth = 0
        var rowItems = mutableListOf<androidx.compose.ui.layout.Placeable>()

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)

            if (rowWidth + placeable.width <= constraints.maxWidth) {
                rowItems.add(placeable)
                rowWidth += placeable.width + horizontalGapPx
                rowHeight = maxOf(rowHeight, placeable.height)
            } else {
                rows.add(rowItems)
                rowWidths.add(rowWidth - horizontalGapPx)

                rowItems = mutableListOf(placeable)
                rowWidth = placeable.width + horizontalGapPx
                rowHeight = placeable.height
            }
        }

        if (rowItems.isNotEmpty()) {
            rows.add(rowItems)
            rowWidths.add(rowWidth - horizontalGapPx)
        }

        val width = rowWidths.maxOrNull() ?: 0
        val height = rows.sumOf { row -> row.maxOfOrNull { it.height } ?: 0 } + (verticalGapPx * (rows.size - 1))

        val rowY = mutableListOf<Int>()
        var y = 0

        rows.forEach { row ->
            rowY.add(y)
            y += row.maxOfOrNull { it.height } ?: 0
            y += verticalGapPx
        }

        layout(width, height) {
            rows.forEachIndexed { rowIndex, row ->
                var x = 0
                row.forEach { placeable ->
                    placeable.place(x, rowY[rowIndex])
                    x += placeable.width + horizontalGapPx
                }
            }
        }
    }
}