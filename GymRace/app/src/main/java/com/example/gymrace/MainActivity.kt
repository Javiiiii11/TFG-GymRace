//package com.example.gymrace
//
//import android.content.res.XmlResourceParser
//import android.os.Build.VERSION.SDK_INT
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.*
//import androidx.compose.material3.DropdownMenuItem
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import coil.ImageLoader
//import coil.compose.rememberAsyncImagePainter
//import coil.decode.GifDecoder
//import coil.decode.ImageDecoderDecoder
//import coil.request.ImageRequest
//import coil.size.Size
//import com.example.gymrace.ui.theme.GymRaceTheme
//import org.xmlpull.v1.XmlPullParser
//import androidx.compose.foundation.lazy.grid.GridCells
//import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
//import androidx.compose.foundation.lazy.grid.items
//import androidx.compose.ui.draw.clip
//
//// Clase de datos que almacena información de cada GIF junto con su categoría, título y descripción
//data class GifData(
//    val category: String,    // Categoría muscular del ejercicio
//    val title: String,       // Nombre del ejercicio
//    val description: String, // Descripción del ejercicio (ya no se muestra en la UI)
//    val resource: Int        // Identificador del recurso del GIF
//)
//
//// Actividad principal que configura la UI con Compose
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        // Habilita la visualización edge-to-edge en la pantalla
//        enableEdgeToEdge()
//        setContent {
//            GymRaceTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Content(
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun FilterBar(
//    searchQuery: MutableState<String>,
//    selectedCategory: MutableState<String>,
//    categories: List<String>
//) {
//    var expanded by remember { mutableStateOf(false) }
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        OutlinedTextField(
//            value = searchQuery.value,
//            onValueChange = { searchQuery.value = it },
//            label = { Text("Buscar") },
//            shape = RoundedCornerShape(8.dp),
//            modifier = Modifier.weight(0.6f)
//        )
//        Spacer(modifier = Modifier.width(8.dp))
//        ExposedDropdownMenuBox(
//            expanded = expanded,
//            onExpandedChange = { expanded = !expanded },
//            modifier = Modifier.weight(0.6f)
//        ) {
//            OutlinedTextField(
//                value = selectedCategory.value,
//                onValueChange = {},
//                readOnly = true,
//                label = { Text("Categoría") },
//                shape = RoundedCornerShape(8.dp),
//                trailingIcon = {
//                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
//                },
//                modifier = Modifier.menuAnchor()
//            )
//            ExposedDropdownMenu(
//                expanded = expanded,
//                onDismissRequest = { expanded = false }
//            ) {
//                categories.forEach { category ->
//                    DropdownMenuItem(
//                        text = { Text(category) },
//                        onClick = {
//                            selectedCategory.value = category
//                            expanded = false
//                        }
//                    )
//                }
//            }
//        }
//    }
//}
//
//// Composable principal que muestra la lista de ejercicios y las categorías filtradas
//@Composable
//fun Content(modifier: Modifier = Modifier) {
//    val context = LocalContext.current
//    val (gifs, missingGifs) = remember { loadGifsFromXml(context.resources.getXml(R.xml.ejercicios), context) }
//    val missingGifsText = remember { mutableStateOf(missingGifs.joinToString(", ")) }
//
//    val searchQuery = remember { mutableStateOf("") }
//    val selectedCategory = remember { mutableStateOf("Todos") }
//    val categories = listOf("Todos") + gifs.map { it.category }.distinct()
//
//    val filteredGifs = gifs.filter { gif ->
//        (selectedCategory.value == "Todos" || gif.category == selectedCategory.value) &&
//                (searchQuery.value.isEmpty() ||
//                        gif.title.contains(searchQuery.value, ignoreCase = true) ||
//                        gif.description.contains(searchQuery.value, ignoreCase = true))
//    }
//
//    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
//        // Barra de búsqueda y selección de categoría
//        FilterBar(searchQuery = searchQuery, selectedCategory = selectedCategory, categories = categories)
//        Spacer(modifier = Modifier.height(16.dp))
//
//        if (missingGifsText.value.isNotEmpty()) {
//            Text(
//                text = "Missing GIFs: ${missingGifsText.value}",
//                color = Color.Red,
//                style = MaterialTheme.typography.bodyMedium,
//                modifier = Modifier.padding(bottom = 16.dp)
//            )
//        }
//
//        if (filteredGifs.isEmpty()) {
//            Text(
//                text = "No se encontraron ejercicios",
//                style = MaterialTheme.typography.bodyMedium,
//                modifier = Modifier.padding(16.dp)
//            )
//        } else {
//            LazyVerticalGrid(
//                columns = GridCells.Fixed(2),
//                modifier = Modifier.fillMaxSize(),
//                verticalArrangement = Arrangement.spacedBy(1.dp),
//                horizontalArrangement = Arrangement.spacedBy(1.dp)
//            ) {
//                items(filteredGifs) { gifData ->
//                    // Solo se pasan el título y el recurso del GIF, omitiendo la descripción
//                    GifBox(
//                        title = gifData.title,
//                        gif = gifData.resource
//                    )
//                }
//            }
//        }
//    }
//}
//
//// Composable que muestra un contenedor (Box) con la imagen del GIF pegada arriba y el título
//@Composable
//fun GifBox(title: String, gif: Int) {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(250.dp) // Tamaño de la caja
//            .padding(8.dp)
//            .clip(RoundedCornerShape(16.dp)) // Bordes redondeados
//            .background(Color.LightGray)
//            .padding(top = 16.dp, start = 0.dp, end = 0.dp) // Padding superior y laterales
//    ) {
//        Column(modifier = Modifier.fillMaxSize()) {
//            // Imagen en la parte superior
//            GifImage(
//                modifier = Modifier
//                    .clip(RoundedCornerShape(16.dp)) // Bordes redondeados
//                    .height(120.dp)
//                    .background(Color.LightGray),
//                gif = gif
//            )
//
//            // Título centrado con padding lateral
//            Box(
//                modifier = Modifier
//                    .fillMaxSize(),
//                contentAlignment = Alignment.Center
//
//            ) {
//                Text(
//                    text = title,
//                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
//                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)
//                )
//            }
//        }
//    }
//}
//
//
//
//// Composable que carga y muestra el GIF usando la librería Coil
//@Composable
//fun GifImage(modifier: Modifier = Modifier, gif: Int) {
//    val context = LocalContext.current
//    val imageLoader = ImageLoader.Builder(context)
//        .components {
//            if (SDK_INT >= 28) {
//                add(ImageDecoderDecoder.Factory())
//            } else {
//                add(GifDecoder.Factory())
//            }
//        }
//        .build()
//    Image(
//        painter = rememberAsyncImagePainter(
//            ImageRequest.Builder(context)
//                .data(data = gif)
//                .apply { size(Size.ORIGINAL) }
//                .build(),
//            imageLoader = imageLoader
//        ),
//        contentDescription = null,
//        modifier = modifier.fillMaxWidth()
//    )
//}
//
///**
// * Función para cargar los GIFs desde un archivo XML.
// *
// * Lee la estructura XML:
// * <ejercicios>
// *     <categoria nombre="NombreCategoria">
// *         <ejercicio>
// *             <nombre>NombreEjercicio</nombre>
// *             <descripcion>Descripción</descripcion>
// *             <gif>@drawable/ejercicio_gif</gif>
// *         </ejercicio>
// *         ...
// *     </categoria>
// *     ...
// * </ejercicios>
// *
// * @return Un par donde el primer elemento es la lista de GifData y el segundo es la lista de GIFs faltantes.
// */
//fun loadGifsFromXml(
//    parser: XmlResourceParser,
//    context: android.content.Context
//): Pair<List<GifData>, List<String>> {
//    val gifs = mutableListOf<GifData>()
//    val missingGifs = mutableListOf<String>()
//
//    var eventType = parser.eventType
//    var currentTag: String? = null
//    var currentCategory = ""
//    var title = ""
//    var description = ""
//    var resource = 0
//
//    while (eventType != XmlPullParser.END_DOCUMENT) {
//        when (eventType) {
//            XmlPullParser.START_TAG -> {
//                currentTag = parser.name
//                when (currentTag) {
//                    "categoria" -> {
//                        currentCategory = parser.getAttributeValue(null, "nombre") ?: ""
//                    }
//                    "ejercicio" -> {
//                        title = ""
//                        description = ""
//                        resource = 0
//                    }
//                }
//            }
//            XmlPullParser.TEXT -> {
//                val text = parser.text.trim()
//                if (currentTag != null && text.isNotEmpty()) {
//                    when (currentTag) {
//                        "nombre" -> title = text
//                        "descripcion" -> description = text
//                        "gif" -> {
//                            val resourceName = text.replace(".gif", "").replace("@drawable/", "").trim()
//                            resource = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
//                            if (resource == 0) {
//                                missingGifs.add(text)
//                            }
//                        }
//                    }
//                }
//            }
//            XmlPullParser.END_TAG -> {
//                if (parser.name == "ejercicio" && resource != 0) {
//                    gifs.add(GifData(category = currentCategory, title = title, description = description, resource = resource))
//                }
//                currentTag = null
//            }
//        }
//        eventType = parser.next()
//    }
//    return Pair(gifs, missingGifs)
//}
//
//@Preview(showBackground = true)
//@Composable
//fun ContentPreview() {
//    GymRaceTheme {
//        Content()
//    }
//}

































package com.example.gymrace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import com.example.gymrace.ui.theme.GymRaceTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymRaceTheme {
                MainScreen()
            }
        }
    }
}

