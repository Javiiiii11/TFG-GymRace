package com.example.gymrace

import android.content.res.XmlResourceParser
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.example.gymrace.ui.theme.GymRaceTheme
import org.xmlpull.v1.XmlPullParser

// Clase de datos que almacena información de cada GIF junto con su categoría, título y descripción
data class GifData(
    val category: String,    // Categoría muscular del ejercicio
    val title: String,       // Nombre del ejercicio
    val description: String, // Descripción del ejercicio
    val resource: Int        // Identificador del recurso del GIF
)

// Actividad principal que configura la UI con Compose
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Habilita la visualización edge-to-edge en la pantalla
        enableEdgeToEdge()
        setContent {
            GymRaceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Content(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    searchQuery: MutableState<String>,
    selectedCategory: MutableState<String>,
    categories: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery.value,
            onValueChange = { searchQuery.value = it },
            label = { Text("Buscar") },
            shape = RoundedCornerShape(8.dp), // Bordes redondeados para el buscador
            modifier = Modifier.weight(0.6f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(0.6f)
        ) {
            OutlinedTextField(
                value = selectedCategory.value,
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoría") },
                shape = RoundedCornerShape(8.dp), // Bordes redondeados para el filtro
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier.menuAnchor() // Asegura que el menú se ancle al TextField
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            selectedCategory.value = category
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// Composable principal que muestra la lista de ejercicios y las categorías filtradas
@Composable
fun Content(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Carga los GIFs desde XML; se obtiene la lista de GIFs y la lista de nombres de GIFs faltantes
    val (gifs, missingGifs) = remember { loadGifsFromXml(context.resources.getXml(R.xml.ejercicios), context) }
    val missingGifsText = remember { mutableStateOf(missingGifs.joinToString(", ")) }

    // Estados para búsqueda y categoría seleccionada
    val searchQuery = remember { mutableStateOf("") }
    val selectedCategory = remember { mutableStateOf("Todos") }
    // Lista de categorías: "Todos" + las categorías existentes
    val categories = listOf("Todos") + gifs.map { it.category }.distinct()

    // Filtra los ejercicios según la búsqueda y la categoría seleccionada
    val filteredGifs = gifs.filter { gif ->
        (selectedCategory.value == "Todos" || gif.category == selectedCategory.value) &&
                (searchQuery.value.isEmpty() ||
                        gif.title.contains(searchQuery.value, ignoreCase = true) ||
                        gif.description.contains(searchQuery.value, ignoreCase = true))
    }
    // Agrupa los ejercicios filtrados por categoría
    val gifsByCategory = filteredGifs.groupBy { it.category }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Fila superior con buscador y desplegable
        item {
            FilterBar(
                searchQuery = searchQuery,
                selectedCategory = selectedCategory,
                categories = categories
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        // Muestra una alerta si hay GIFs faltantes
        item {
            if (missingGifsText.value.isNotEmpty()) {
                Text(
                    text = "Missing GIFs: ${missingGifsText.value}",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
        // Si no hay ejercicios que cumplan con los filtros, muestra un mensaje
        if (filteredGifs.isEmpty()) {
            item {
                Text(
                    text = "No se encontraron ejercicios",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            // Itera por cada categoría y sus ejercicios filtrados
            for ((category, exercises) in gifsByCategory) {
                // Encabezado de la categoría
                item {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                // Lista de ejercicios de la categoría
                items(exercises) { gifData ->
                    GifBox(
                        title = gifData.title,
                        description = gifData.description,
                        gif = gifData.resource
                    )
                }
            }
        }
    }
}

// Composable que muestra un contenedor (Box) con la imagen del GIF, título y descripción
@Composable
fun GifBox(title: String, description: String, gif: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            GifImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                gif = gif
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Composable que carga y muestra el GIF usando la librería Coil
@Composable
fun GifImage(modifier: Modifier = Modifier, gif: Int) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()
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

/**
 * Función para cargar los GIFs desde un archivo XML.
 *
 * Lee la estructura XML:
 * <ejercicios>
 *     <categoria nombre="NombreCategoria">
 *         <ejercicio>
 *             <nombre>NombreEjercicio</nombre>
 *             <descripcion>Descripción</descripcion>
 *             <gif>@drawable/ejercicio_gif</gif>
 *         </ejercicio>
 *         ...
 *     </categoria>
 *     ...
 * </ejercicios>
 *
 * @return Un par donde el primer elemento es la lista de GifData y el segundo es la lista de GIFs faltantes.
 */
fun loadGifsFromXml(
    parser: XmlResourceParser,
    context: android.content.Context
): Pair<List<GifData>, List<String>> {
    val gifs = mutableListOf<GifData>()
    val missingGifs = mutableListOf<String>()

    var eventType = parser.eventType
    var currentTag: String? = null
    var currentCategory = ""
    var title = ""
    var description = ""
    var resource = 0

    while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
            XmlPullParser.START_TAG -> {
                currentTag = parser.name
                when (currentTag) {
                    "categoria" -> {
                        currentCategory = parser.getAttributeValue(null, "nombre") ?: ""
                    }
                    "ejercicio" -> {
                        title = ""
                        description = ""
                        resource = 0
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
                            if (resource == 0) {
                                missingGifs.add(text)
                            }
                        }
                    }
                }
            }
            XmlPullParser.END_TAG -> {
                if (parser.name == "ejercicio" && resource != 0) {
                    gifs.add(GifData(category = currentCategory, title = title, description = description, resource = resource))
                }
                currentTag = null
            }
        }
        eventType = parser.next()
    }
    return Pair(gifs, missingGifs)
}

@Preview(showBackground = true)
@Composable
fun ContentPreview() {
    GymRaceTheme {
        Content()
    }
}
