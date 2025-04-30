package com.example.gymrace

import android.content.res.XmlResourceParser
import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.example.gymrace.ui.theme.GymRaceTheme
import org.xmlpull.v1.XmlPullParser

// Informacion de los ejercicios
data class GifData(
    val category: String,
    val title: String,
    val description: String,
    val resource: Int,
    val steps: List<Step> = emptyList(),
    val tips: List<String> = emptyList(),
    val mainMuscle: String = "",
    val secondaryMuscles: String = ""
)

// Informacion de los pasos
data class Step(
    val number: Int,
    val description: String
)

// Pagina de ejercicios
@Composable
fun EjerciciosPage(modifier: Modifier = Modifier) {
    Content(modifier)
}

// Composable principal
@Composable
fun Content(modifier: Modifier = Modifier) {
    // Obtenemos el contexto de la aplicación
    val context = LocalContext.current
    // Cargamos los GIFs desde el XML
    val (gifs, missingGifs) = remember { loadGifsFromXml(context.resources.getXml(R.xml.ejercicios), context) }
    // Lista de GIFs que faltan
    val missingGifsText = remember { mutableStateOf(missingGifs.joinToString(", ")) }
    // Estado para la búsqueda y la categoría seleccionada
    val searchQuery = remember { mutableStateOf("") }
    // Estado para la categoría seleccionada
    val selectedCategory = remember { mutableStateOf("Todos") }
    // Estado para mostrar el diálogo de filtros
    var showFilterDialog by remember { mutableStateOf(false) }
    // Lista de categorías
    val categories = listOf("Todos") + gifs.map { it.category }.distinct().sorted()
    // Estado para los GIFs filtrados
    val filteredGifs = gifs.filter { gif ->
        (selectedCategory.value == "Todos" || gif.category == selectedCategory.value) &&
                (searchQuery.value.isEmpty() ||
                        gif.title.contains(searchQuery.value, ignoreCase = true) ||
                        gif.description.contains(searchQuery.value, ignoreCase = true))
    }

    Column(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)) {

        // Barra de búsqueda mejorada con estilo similar a la página de dietas
        OutlinedTextField(
            value = searchQuery.value,
            onValueChange = { searchQuery.value = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar ejercicios...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar"
                )
            },
            trailingIcon = {
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filtrar"
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Chip para mostrar el filtro activo (solo se muestra cuando hay un filtro diferente a "Todos")
        if (selectedCategory.value != "Todos") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = { selectedCategory.value = "Todos" },  // Al hacer clic, se elimina el filtro
                    label = { Text(selectedCategory.value) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${filteredGifs.size} resultados",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Mensaje de error si faltan GIFs
        if (missingGifsText.value.isNotEmpty()) {
            Text(
                text = "Missing GIFs: ${missingGifsText.value}",
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Mensaje de error si no se encuentran GIFs
        if (filteredGifs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se encontraron ejercicios",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            // Grid de GIFs
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(filteredGifs) { gifData ->
                    GifBox(gifData = gifData)
                }
            }
        }
    }

    // Diálogo de filtro por categoría con LazyColumn para permitir scroll
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filtrar por categoría") },
            text = {
                // Usando LazyColumn para permitir scroll en la lista de categorías
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    items(categories) { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCategory.value = category
                                    showFilterDialog = false
                                },
//                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCategory.value == category,
                                onClick = {
                                    selectedCategory.value = category
                                    showFilterDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = category)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

// Composable para mostrar cada GIF
@Composable
fun GifBox(gifData: GifData) {
    // Estado para mostrar el detalle del GIF
    var showDetail by remember { mutableStateOf(false) }
    // Contenedor del GIF
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { showDetail = true }
            .padding(top = 16.dp, start = 0.dp, end = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GifImage(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .height(120.dp),
                gif = gifData.resource
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = gifData.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                )
            }
        }
    }
    // Mostrar el detalle del GIF al hacer clic
    if (showDetail) {
        ExerciseDetailDialog(
            gifData = gifData,
            onDismiss = { showDetail = false }
        )
    }
}

// Composable para mostrar el detalle del GIF
@Composable
fun ExerciseDetailDialog(
    gifData: GifData,
    onDismiss: () -> Unit
) {
    // Mostrar el diálogo con los detalles del GIF
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 0.dp, end = 0.dp, bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Close button
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                // Title
                Text(
                    text = gifData.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Large GIF image
                GifImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    gif = gifData.resource
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = "Descripción:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = gifData.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                // Steps
                if (gifData.steps.isNotEmpty()) {
                    Text(
                        text = "Pasos:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    gifData.steps.forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${step.number}.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                            )
                            Text(
                                text = step.description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(16.dp))
                // Tips
                if (gifData.tips.isNotEmpty()) {
                    Text(
                        text = "Consejos:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    gifData.tips.forEach { tip ->
                        Text(
                            text = "• $tip",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                // Muscles

                if (gifData.mainMuscle.isNotEmpty() || gifData.secondaryMuscles.isNotEmpty()) {
                    Text(
                        text = "Músculos trabajados:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Principal: ",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = gifData.mainMuscle,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (gifData.secondaryMuscles.isNotEmpty()) {
                        Text(
                            text = "Secundarios: ",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = gifData.secondaryMuscles,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// Composable para mostrar el GIF
@Composable
fun GifImage(modifier: Modifier = Modifier, gif: Int) {
    // Obtenemos el contexto de la aplicación
    val context = LocalContext.current
    // Configuración del ImageLoader para manejar GIFs
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()
    // Cargamos el GIF usando Coil
    Image(
        painter = rememberAsyncImagePainter(
            ImageRequest.Builder(context)
                .data(data = gif)
                .apply { size(Size.ORIGINAL) }
                .build(),
            imageLoader = imageLoader
        ),
        contentDescription = null,
        modifier = modifier.fillMaxWidth()
    )
}

// Función para cargar los GIFs desde el XML
fun loadGifsFromXml(
    parser: XmlResourceParser,
    context: android.content.Context
): Pair<List<GifData>, List<String>> {
    // Lista para almacenar los GIFs
    val gifs = mutableListOf<GifData>()
    // Lista para almacenar los GIFs que faltan
    val missingGifs = mutableListOf<String>()

    // Variables para almacenar los datos del GIF actual
    var eventType = parser.eventType
    var currentTag: String? = null
    var currentCategory = ""
    var title = ""
    var description = ""
    var resource = 0
    val steps = mutableListOf<Step>()
    val tips = mutableListOf<String>()
    var mainMuscle = ""
    var secondaryMuscles = ""
    var inSteps = false
    var inTips = false
    var currentStepNumber = 0
    var currentStepDesc = ""

    // Iteramos a través del XML
    while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
            XmlPullParser.START_TAG -> {
                currentTag = parser.name
                when (currentTag) {
                    "categoria" -> currentCategory = parser.getAttributeValue(null, "nombre") ?: ""
                    "ejercicio" -> {
                        title = ""
                        description = ""
                        resource = 0
                        steps.clear()
                        tips.clear()
                        mainMuscle = ""
                        secondaryMuscles = ""
                    }
                    "pasos" -> inSteps = true
                    "consejo" -> inTips = true
                    "paso" -> {
                        currentStepNumber = parser.getAttributeValue(null, "numero")?.toIntOrNull() ?: 0
                        currentStepDesc = ""
                    }
                }
            }
            XmlPullParser.TEXT -> {
                val text = parser.text.trim()
                if (currentTag != null && text.isNotEmpty()) {
                    when (currentTag) {
                        "nombre" -> title = text
                        "descripcion" -> description = text
                        "gif" -> {
                            val resourceName = text.replace(".gif", "").replace("@drawable/", "").trim()
                            resource = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
                            if (resource == 0) missingGifs.add(text)
                        }
                        "paso" -> if (inSteps) currentStepDesc = text
                        "item" -> if (inTips) tips.add(text)
                        "principal" -> mainMuscle = text
                        "secundario" -> secondaryMuscles = text
                    }
                }
            }
            XmlPullParser.END_TAG -> {
                when (parser.name) {
                    "ejercicio" -> {
                        if (resource != 0) {
                            gifs.add(GifData(
                                category = currentCategory,
                                title = title,
                                description = description,
                                resource = resource,
                                steps = steps.toList(),
                                tips = tips.toList(),
                                mainMuscle = mainMuscle,
                                secondaryMuscles = secondaryMuscles
                            ))
                        }
                    }
                    "pasos" -> inSteps = false
                    "consejo" -> inTips = false
                    "paso" -> if (currentStepNumber > 0 && currentStepDesc.isNotEmpty()) {
                        steps.add(Step(currentStepNumber, currentStepDesc))
                    }
                }
                if (parser.name == currentTag) currentTag = null
            }
        }
        eventType = parser.next()
    }
    return Pair(gifs, missingGifs)
}

// Preview de la página de ejercicios
@Preview(showBackground = true)
@Composable
fun ContentPreview() {
    GymRaceTheme {
        EjerciciosPage()
    }
}