package com.example.gymrace.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Modelo de datos para la dieta
data class Dieta(
    val id: String = "",
    val nombre: String = "",
    val tipo: String = "",
    val descripcion: String = "",
    val imagenUrl: String = ""
)

@Composable
fun DietasPage(
    navigateToDetail: (String) -> Unit = {}
) {
    // Estado para el buscador
    var searchQuery by remember { mutableStateOf("") }

    // Estado para los filtros
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Todos") }
    val tiposFiltros = remember { listOf("Todos", "Proteica", "Vegana", "Vegetariana", "Keto", "Paleo") }

    // Estado para las dietas
    var dietas by remember { mutableStateOf<List<Dieta>>(emptyList()) }
    var dietasFiltradas by remember { mutableStateOf<List<Dieta>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Efecto para cargar las dietas desde Firebase
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("dietas").get().await()
                val dietasList = snapshot.documents.map { doc ->
                    Dieta(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        tipo = doc.getString("tipo") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        imagenUrl = doc.getString("imagenUrl") ?: getImagenPorTipo(doc.getString("tipo") ?: "")
                    )
                }
                dietas = dietasList
                dietasFiltradas = dietasList
                isLoading = false
            }
        } catch (e: Exception) {
            error = "Error al cargar las dietas: ${e.message}"
            isLoading = false
        }
    }

    // Efecto para filtrar las dietas cuando cambia el filtro o la búsqueda
    LaunchedEffect(searchQuery, selectedFilter, dietas) {
        dietasFiltradas = dietas.filter { dieta ->
            (selectedFilter == "Todos" || dieta.tipo == selectedFilter) &&
                    (dieta.nombre.contains(searchQuery, ignoreCase = true) ||
                            dieta.tipo.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filtrar"
                    )
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Chip para mostrar el filtro activo
        if (selectedFilter != "Todos") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = { selectedFilter = "Todos" },
                    label = { Text(selectedFilter) },
//                    trailingIcon = {
//                        Icon(
//                            Icons.Default.FilterList,
//                            contentDescription = "Eliminar filtro"
//                        )
//                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${dietasFiltradas.size} resultados",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Contenido principal
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = error ?: "Error desconocido")
                }
            }
            dietasFiltradas.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No se encontraron dietas")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dietasFiltradas) { dieta ->
                        DietaCard(
                            dieta = dieta,
                            onClick = { navigateToDetail(dieta.id) }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de filtro
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filtrar por tipo") },
            text = {
                Column {
                    tiposFiltros.forEach { tipo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFilter = tipo
                                    showFilterDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedFilter == tipo,
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
}

@Composable
fun DietaCard(
    dieta: Dieta,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            }

            AsyncImage(
                model = dieta.imagenUrl,
                contentDescription = "Imagen de ${dieta.nombre}",
                modifier = Modifier
                    .size(100.dp),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun DietaDetailPage(dietaId: String) {
    var dieta by remember { mutableStateOf<Dieta?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(dietaId) {
        try {
            withContext(Dispatchers.IO) {
                val db = FirebaseFirestore.getInstance()
                val doc = db.collection("dietas").document(dietaId).get().await()
                if (doc.exists()) {
                    dieta = Dieta(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        tipo = doc.getString("tipo") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        imagenUrl = doc.getString("imagenUrl") ?: getImagenPorTipo(doc.getString("tipo") ?: "")
                    )
                } else {
                    error = "No se encontró la dieta"
                }
                isLoading = false
            }
        } catch (e: Exception) {
            error = "Error al cargar la dieta: ${e.message}"
            isLoading = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> CircularProgressIndicator()
            error != null -> Text(text = error ?: "Error desconocido")
            dieta != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    AsyncImage(
                        model = dieta?.imagenUrl,
                        contentDescription = "Imagen de ${dieta?.nombre}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = dieta?.nombre ?: "",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tipo: ${dieta?.tipo}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = dieta?.descripcion ?: "",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// Función auxiliar para obtener imágenes predeterminadas según el tipo de dieta
private fun getImagenPorTipo(tipo: String): String {
    return when (tipo.lowercase()) {
        "proteica" -> "https://example.com/images/proteica.jpg"
        "vegana" -> "https://example.com/images/vegana.jpg"
        "vegetariana" -> "https://example.com/images/vegetariana.jpg"
        "keto" -> "https://example.com/images/keto.jpg"
        "paleo" -> "https://example.com/images/paleo.jpg"
        else -> "https://example.com/images/default.jpg"
    }
}