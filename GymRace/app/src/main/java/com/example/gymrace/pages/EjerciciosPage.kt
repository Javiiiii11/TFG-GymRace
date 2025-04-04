package com.example.gymrace

import android.content.res.XmlResourceParser
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.example.gymrace.ui.theme.GymRaceTheme
import org.xmlpull.v1.XmlPullParser

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

data class Step(
    val number: Int,
    val description: String
)

@Composable
fun EjerciciosPage(modifier: Modifier = Modifier) {
    Content(modifier)
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
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = searchQuery.value,
            onValueChange = { searchQuery.value = it },
            label = { Text("Buscar") },
            shape = RoundedCornerShape(8.dp),
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
                shape = RoundedCornerShape(8.dp),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier.menuAnchor()
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

@Composable
fun Content(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val (gifs, missingGifs) = remember { loadGifsFromXml(context.resources.getXml(R.xml.ejercicios), context) }
    val missingGifsText = remember { mutableStateOf(missingGifs.joinToString(", ")) }

    val searchQuery = remember { mutableStateOf("") }
    val selectedCategory = remember { mutableStateOf("Todos") }
    val categories = listOf("Todos") + gifs.map { it.category }.distinct()

    val filteredGifs = gifs.filter { gif ->
        (selectedCategory.value == "Todos" || gif.category == selectedCategory.value) &&
                (searchQuery.value.isEmpty() ||
                        gif.title.contains(searchQuery.value, ignoreCase = true) ||
                        gif.description.contains(searchQuery.value, ignoreCase = true))
    }

    Column(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Spacer(modifier = Modifier.height(32.dp))
        FilterBar(searchQuery = searchQuery, selectedCategory = selectedCategory, categories = categories)
        Spacer(modifier = Modifier.height(32.dp))

        if (missingGifsText.value.isNotEmpty()) {
            Text(
                text = "Missing GIFs: ${missingGifsText.value}",
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (filteredGifs.isEmpty()) {
            Text(
                text = "No se encontraron ejercicios",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        } else {
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
            Spacer(modifier = Modifier.height(68.dp))
        }
    }
}

@Composable
fun GifBox(gifData: GifData) {
    var showDetail by remember { mutableStateOf(false) }

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

    if (showDetail) {
        ExerciseDetailDialog(
            gifData = gifData,
            onDismiss = { showDetail = false }
        )
    }
}

@Composable
fun ExerciseDetailDialog(
    gifData: GifData,
    onDismiss: () -> Unit
) {
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
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}


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
    val steps = mutableListOf<Step>()
    val tips = mutableListOf<String>()
    var mainMuscle = ""
    var secondaryMuscles = ""
    var inSteps = false
    var inTips = false
    var currentStepNumber = 0
    var currentStepDesc = ""

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

@Preview(showBackground = true)
@Composable
fun ContentPreview() {
    GymRaceTheme {
        EjerciciosPage()
    }
}