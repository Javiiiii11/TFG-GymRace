package com.example.gymrace

// Importaciones necesarias para la aplicación y Compose
import android.content.res.XmlResourceParser
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
            // Aplica el tema personalizado GymRaceTheme
            GymRaceTheme {
                // Scaffold para estructura básica de pantalla
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Llama al contenido principal con padding interno
                    Content(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Composable principal que muestra la lista de ejercicios y las categorías
@Composable
fun Content(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Carga los GIFs desde XML; se obtiene la lista de GIFs y la lista de nombres de GIFs faltantes
    val (gifs, missingGifs) = remember { loadGifsFromXml(context.resources.getXml(R.xml.ejercicios), context) }
    // Estado que contiene los nombres de los GIFs faltantes, separados por coma
    val missingGifsText = remember { mutableStateOf(missingGifs.joinToString(", ")) }

    // Agrupa los ejercicios por categoría muscular
    val gifsByCategory = gifs.groupBy { it.category }

    // Lista perezosa para mostrar el contenido
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
        // Itera por cada categoría y sus ejercicios
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
            // Muestra la imagen del GIF
            GifImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                gif = gif
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Título del ejercicio
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Descripción del ejercicio
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
    // Configura el ImageLoader para soportar GIFs según la versión de Android
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()
    // Usa rememberAsyncImagePainter para cargar el GIF desde el recurso
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

    // Recorre el XML hasta el final del documento
    while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
            XmlPullParser.START_TAG -> {
                currentTag = parser.name
                when (currentTag) {
                    "categoria" -> {
                        // Obtiene el atributo "nombre" para la categoría
                        currentCategory = parser.getAttributeValue(null, "nombre") ?: ""
                    }
                    "ejercicio" -> {
                        // Reinicia los valores para cada ejercicio
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
                            // Elimina la extensión ".gif" y el prefijo "@drawable/" para obtener el nombre del recurso
                            val resourceName = text.replace(".gif", "").replace("@drawable/", "").trim()
                            resource = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
                            // Si no se encuentra el recurso, se agrega a la lista de faltantes
                            if (resource == 0) {
                                missingGifs.add(text)
                            }
                        }
                    }
                }
            }
            XmlPullParser.END_TAG -> {
                // Al finalizar un ejercicio, si el recurso es válido se agrega a la lista
                if (parser.name == "ejercicio") {
                    if (resource != 0) {
                        gifs.add(GifData(category = currentCategory, title = title, description = description, resource = resource))
                    }
                }
                currentTag = null
            }
        }
        eventType = parser.next()
    }
    // Retorna un par con la lista de GIFs y la lista de GIFs faltantes
    return Pair(gifs, missingGifs)
}

// Función de previsualización para el composable Content
@Preview(showBackground = true)
@Composable
fun ContentPreview() {
    GymRaceTheme {
        Content()
    }
}
