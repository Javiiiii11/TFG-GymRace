package com.example.gymrace.pages.dietas

import android.content.Intent
import androidx.compose.foundation.lazy.items
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Modelo de datos actualizado para representar una dieta completa
data class Dieta(
    val id: String = "",                                // ID único de la dieta en Firestore
    val nombre: String = "",                            // Nombre de la dieta
    val tipo: String = "",                              // Tipo de dieta (Proteica, Vegana, etc.)
    val objetivo: String = "",                          // Objetivo principal de la dieta
    val macronutrientes: Map<String, String> = mapOf(), // Distribución de macronutrientes
    val calorias: String = "",                          // Rango de calorías diarias
    val comidas: String = "",                           // Número y distribución de comidas
    val alimentosPermitidos: List<String> = listOf(),   // Lista de alimentos permitidos
    val alimentosProhibidos: List<String> = listOf(),   // Lista de alimentos prohibidos
    val ejemploMenu: Map<String, String> = mapOf(),     // Ejemplo de menú diario
    val suplementacion: List<String> = listOf(),        // Suplementos recomendados
    val consejos: List<String> = listOf(),              // Consejos adicionales
    val descripcion: String = "",                       // Descripción detallada de la dieta
    val estra: String = "",                             // Campo para estrategia o características adicionales
    val imagenUrl: String = "",                         // URL de la imagen de la dieta
    val enlaceWeb: String = "https://example.com/dietas" // Enlace web para más información
)


//Página principal que muestra la lista de dietas con modal de detalles
@Composable
fun DietasPage() {
    val context = LocalContext.current

    // Estado para el buscador
    var searchQuery by remember { mutableStateOf("") }

    // Estado para los filtros
    var showFilterDialog by remember { mutableStateOf(false) }  // Controla la visibilidad del diálogo de filtros
    var selectedFilter by remember { mutableStateOf("Todos") }  // Filtro actualmente seleccionado

    // Estado para las dietas
    var dietas by remember { mutableStateOf<List<Dieta>>(emptyList()) }  // Lista completa de dietas
    var dietasFiltradas by remember { mutableStateOf<List<Dieta>>(emptyList()) }  // Lista de dietas después de aplicar filtros
    var isLoading by remember { mutableStateOf(true) }  // Indica si está cargando datos
    var error by remember { mutableStateOf<String?>(null) }  // Almacena mensajes de error, si los hay

    // Lista dinámica de tipos de dietas que será poblada con los tipos únicos encontrados en la base de datos
    var tiposFiltros by remember { mutableStateOf<List<String>>(listOf("Todos")) }

    // Estado para el modal de detalles
    var selectedDieta by remember { mutableStateOf<Dieta?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    // Efecto que se ejecuta al iniciar la pantalla para cargar las dietas desde Firebase
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {  // Ejecutar en hilo de IO para operaciones de red
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("dietas").get().await()  // Obtener todas las dietas de Firestore
                val dietasList = snapshot.documents.map { doc ->
                    Dieta(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        tipo = doc.getString("tipo") ?: "",
                        objetivo = doc.getString("objetivo") ?: "",
                        macronutrientes = doc.get("macronutrientes") as? Map<String, String> ?: mapOf(),
                        calorias = doc.getString("calorias") ?: "",
                        comidas = doc.getString("comidas") ?: "",
                        alimentosPermitidos = (doc.get("alimentosPermitidos") as? List<*>)?.map { it.toString() } ?: listOf(),
                        alimentosProhibidos = (doc.get("alimentosProhibidos") as? List<*>)?.map { it.toString() } ?: listOf(),
                        ejemploMenu = doc.get("ejemploMenu") as? Map<String, String> ?: mapOf(),
                        suplementacion = (doc.get("suplementacion") as? List<*>)?.map { it.toString() } ?: listOf(),
                        consejos = (doc.get("consejos") as? List<*>)?.map { it.toString() } ?: listOf(),
                        descripcion = doc.getString("descripcion") ?: "",
                        estra = doc.getString("estra") ?: "",
                        imagenUrl = doc.getString("imagenUrl") ?: getImagenPorTipo(doc.getString("tipo") ?: ""),
                        enlaceWeb = doc.getString("enlaceWeb") ?: "https://example.com/dietas/${doc.id}"
                    )
                }
                dietas = dietasList
                dietasFiltradas = dietasList

                // Extraer todos los tipos de dietas únicos para los filtros
                val tiposUnicos = dietasList.map { it.tipo }.distinct().filter { it.isNotEmpty() }
                tiposFiltros = listOf("Todos") + tiposUnicos.sorted()

                isLoading = false  // Finalizar estado de carga
            }
        } catch (e: Exception) {
            error = "Error al cargar las dietas: ${e.message}"  // Capturar y mostrar errores
            isLoading = false
        }
    }

    // Efecto para filtrar las dietas cuando cambia el filtro o la búsqueda
    LaunchedEffect(searchQuery, selectedFilter, dietas) {
        dietasFiltradas = dietas.filter { dieta ->
            // Filtrar por tipo seleccionado y texto de búsqueda
            (selectedFilter == "Todos" || dieta.tipo == selectedFilter) &&
                    (dieta.nombre.contains(searchQuery, ignoreCase = true) ||
                            dieta.tipo.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar dietas...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar"
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showFilterDialog = true }) {  // Botón para mostrar el diálogo de filtros
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filtrar"
                        )
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Chip para mostrar el filtro activo (solo se muestra cuando hay un filtro diferente a "Todos")
            if (selectedFilter != "Todos") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = { selectedFilter = "Todos" },  // Al hacer clic, se elimina el filtro
                        label = { Text(selectedFilter) }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${dietasFiltradas.size} resultados",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Contenido principal - Maneja diferentes estados (cargando, error, sin resultados, lista de dietas)
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()  // Indicador de carga
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = error ?: "Error desconocido")  // Mensaje de error
                    }
                }
                dietasFiltradas.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No se encontraron dietas")  // Mensaje cuando no hay resultados
                    }
                }
                else -> {
                    LazyColumn(  // Lista optimizada para mostrar muchos elementos
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(dietasFiltradas) { dieta ->
                            DietaCard(
                                dieta = dieta,
                                onClick = {
                                    selectedDieta = dieta
                                    showDetailDialog = true
                                    Log.d("Dieta", "Dieta seleccionada: ${dieta.id}")
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(60.dp))  // Espacio al final para evitar que el contenido se oculte
                        }
                    }
                }
            }
        }
    }

    // Diálogo de filtro - Se muestra cuando showFilterDialog es true
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filtrar por tipo") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(tiposFiltros) { tipo ->
                        Row(
                            modifier = Modifier
//                                .padding(horizontal = 8.dp, vertical = 12.dp)
                                .clickable {
                                    selectedFilter = tipo
                                    showFilterDialog = false
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedFilter == tipo,  // Muestra seleccionado el filtro actual
                                onClick = {
                                    selectedFilter = tipo
                                    showFilterDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = tipo)
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
    // Diálogo modal de detalles de la dieta - Actualizado para mostrar todos los campos
// Diálogo modal de detalles de la dieta - Con título corregido para evitar que oculte el botón cerrar
    if (showDetailDialog && selectedDieta != null) {
        Dialog(
            onDismissRequest = { showDetailDialog = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp), // Altura fija para permitir desplazamiento
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()) // Permite desplazamiento vertical
                ) {
                    // Cabecera con título y botón de cerrar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Botón cerrar siempre a la derecha
                        IconButton(onClick = { showDetailDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar"
                            )
                        }
                    }

                    // Título en su propia fila para que ocupe todo el ancho disponible
                    Text(
                        text = selectedDieta?.nombre ?: "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Imagen principal
                    AsyncImage(
                        model = selectedDieta?.imagenUrl,
                        contentDescription = "Imagen de ${selectedDieta?.nombre}",
                        modifier = Modifier
                            .size(120.dp)
                            .padding(8.dp),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Información básica
                    Text(
                        text = "Tipo: ${selectedDieta?.tipo}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Objetivo
                    Text(
                        text = "Objetivo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedDieta?.objetivo ?: "No especificado",
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Distribución de Macronutrientes
                    Text(
                        text = "Distribución de Macronutrientes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedDieta?.macronutrientes?.isNotEmpty() == true) {
                        selectedDieta!!.macronutrientes.forEach { (macro, valor) ->
                            Text(
                                text = "• $macro: $valor",
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Text(
                            text = "No hay información disponible",
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Calorías
                    Text(
                        text = "Calorías diarias",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedDieta?.calorias ?: "No especificado",
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Comidas
                    Text(
                        text = "Horario de Comidas",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedDieta?.comidas ?: "No especificado",
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Alimentos Permitidos
                    Text(
                        text = "Alimentos Permitidos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedDieta?.alimentosPermitidos?.isNotEmpty() == true) {
                        selectedDieta!!.alimentosPermitidos.forEach { alimento ->
                            Text(
                                text = "• $alimento",
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Text(
                            text = "No hay información disponible",
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Alimentos Prohibidos
                    Text(
                        text = "Alimentos Prohibidos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedDieta?.alimentosProhibidos?.isNotEmpty() == true) {
                        selectedDieta!!.alimentosProhibidos.forEach { alimento ->
                            Text(
                                text = "• $alimento",
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Text(
                            text = "No hay información disponible",
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Ejemplo de Menú Diario
                    Text(
                        text = "Ejemplo de Menú Diario",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedDieta?.ejemploMenu?.isNotEmpty() == true) {
                        selectedDieta!!.ejemploMenu.forEach { (comida, plato) ->
                            Text(
                                text = "• $comida: $plato",
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Text(
                            text = "No hay ejemplo de menú disponible",
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Suplementación
                    Text(
                        text = "Suplementación",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedDieta?.suplementacion?.isNotEmpty() == true) {
                        selectedDieta!!.suplementacion.forEach { suplemento ->
                            Text(
                                text = "• $suplemento",
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Text(
                            text = "No hay suplementación recomendada",
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Consejos
                    Text(
                        text = "Consejos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedDieta?.consejos?.isNotEmpty() == true) {
                        selectedDieta!!.consejos.forEach { consejo ->
                            Text(
                                text = "• $consejo",
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Text(
                            text = "No hay consejos disponibles",
                            fontSize = 14.sp
                        )
                    }

                    // Descripción (si está disponible)
                    if (!selectedDieta?.descripcion.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Descripción",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = selectedDieta?.descripcion ?: "No hay descripción disponible.",
                            fontSize = 14.sp
                        )
                    }

                    // Estrategia (si está disponible)
                    if (!selectedDieta?.estra.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Estrategia",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = selectedDieta?.estra ?: "",
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Enlace a sitio web
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(selectedDieta?.enlaceWeb)
                                }
                                context.startActivity(intent)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Enlace externo",
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Más información en la web",
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }
        }
    }
}

/**
 * Componente que muestra una tarjeta con la información resumida de una dieta
 *
 * @param dieta La dieta a mostrar
 * @param onClick Función a ejecutar cuando se hace clic en la tarjeta
 */
@Composable
fun DietaCard(
    dieta: Dieta,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),  // Hace que toda la tarjeta sea clicable
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text(
                    text = dieta.nombre,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Tipo: ${dieta.tipo}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Mostrar el objetivo si está disponible
                if (dieta.objetivo.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Objetivo: ${dieta.objetivo}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AsyncImage(  // Carga imágenes de forma asíncrona con la biblioteca Coil
                model = dieta.imagenUrl,
                contentDescription = "Imagen de ${dieta.nombre}",
                modifier = Modifier
                    .padding(10.dp)
                    .size(80.dp),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Función auxiliar para obtener imágenes predeterminadas según el tipo de dieta
 * Se usa cuando no hay URL de imagen específica en Firestore
 *
 * @param tipo Tipo de dieta
 * @return URL de imagen predeterminada según el tipo
 */
private fun getImagenPorTipo(tipo: String): String {
    return when (tipo.lowercase()) {
//        "mediterránea" -> "https://cdn-icons-png.freepik.com/512/2902/2902208.png"
//        "proteica" -> "https://example.com/images/proteica.jpg"
//        "vegana" -> "https://example.com/images/vegana.jpg"
//        "vegetariana" -> "https://example.com/images/vegetariana.jpg"
//        "keto" -> "https://example.com/images/keto.jpg"
//        "paleo" -> "https://example.com/images/paleo.jpg"
        else -> "https://cdn-icons-png.flaticon.com/512/1410/1410596.png"
    }
}
