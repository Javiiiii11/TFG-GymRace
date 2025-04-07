package com.example.gymrace

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gymrace.pages.Dieta
import com.example.gymrace.pages.DietasPage
import com.example.gymrace.pages.InitialScreen
import com.example.gymrace.pages.LoginPage
import com.example.gymrace.pages.RegisterPage
import com.example.gymrace.pages.RegisterPage2
import com.example.gymrace.ui.theme.GymRaceTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import np.com.bimalkafle.bottomnavigationdemo.pages.PredefinedRoutine



@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Activar la SplashScreen para Android 12+
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Inicializar Firebase (si no está inicializado)
        FirebaseApp.initializeApp(this)

        // Crear datos de prueba para dietas (descomentar para ejecutar UNA vez, luego volver a comentar)
//         crearDatosDePrueba()

        // Crear datos de prueba para rutinas predefinidas (descomentar para ejecutar UNA vez, luego volver a comentar)
//        createSampleRoutines()


        setContent {
            GymRaceTheme {  // Cambiado de MaterialTheme a GymRaceTheme
                val navController = rememberNavController()
                NavHost(navController, startDestination = "splash") {
                    composable("splash") { InitialScreen(navController) }
                    composable("register") { RegisterPage(navController) }
                    composable("main") { MainScreen(navController) }
                    composable("login") { LoginPage(navController) }
                    composable("register2") { RegisterPage2(navController) }
                    composable("crearRutina") { CrearRutinaPage(navController) }
                    composable("misRutinas") { ListarMisRutinasPage(navController) }
                    composable("rutinasAmigos") { ListarRutinasAmigosPage(navController) }
                    composable("dietas") { DietasPage() }  // Añadir ruta para la página de dietas
                    // Actualizado: Ruta para ejecutar una rutina con ID
                    composable(
                        route = "ejecutar_rutina/{rutinaId}",
                        arguments = listOf(navArgument("rutinaId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val rutinaId = backStackEntry.arguments?.getString("rutinaId") ?: ""
                        EjecutarRutinaPage(navController = navController, rutinaId = rutinaId)
                    }
                }
            }
        }
    }




    data class ExerciseDetail(
        val name: String = "",
        val repetitions: Int = 0,
        val sets: Int = 0
    )

    fun createSampleRoutines() {
        // Obtén la instancia de Firestore
        val db = FirebaseFirestore.getInstance()

        // Lista de rutinas de ejemplo con ejercicios incluidos
        val routines = listOf(
            PredefinedRoutine(
                title = "Rutina de Piernas",
                description = "Fortalece y tonifica tus piernas con ejercicios específicos.",
                imageName = "rcuadriceps",
                exercises = listOf(
                    ExerciseDetail(name = "Sentadillas", repetitions = 12, sets = 3),
                    ExerciseDetail(name = "Prensa de Piernas", repetitions = 10, sets = 3)
                )
            ),
            PredefinedRoutine(
                title = "Rutina de Brazos",
                description = "Ejercicios para desarrollar bíceps y tríceps.",
                imageName = "rbiceps",
                exercises = listOf(
                    ExerciseDetail(name = "Curl de Bíceps", repetitions = 12, sets = 3),
                    ExerciseDetail(name = "Extensión de Tríceps", repetitions = 12, sets = 3)
                )
            ),
            PredefinedRoutine(
                title = "Rutina de Abdomen",
                description = "Trabaja tu core con ejercicios que tonifican el abdomen.",
                imageName = "rabdomen",
                exercises = listOf(
                    ExerciseDetail(name = "Crunches", repetitions = 20, sets = 3),
                    ExerciseDetail(name = "Plancha", repetitions = 1, sets = 3) // Puedes interpretar la plancha como duración en cada set
                )
            ),
            PredefinedRoutine(
                title = "Rutina de Espalda",
                description = "Fortalece y mejora la postura con ejercicios para la espalda.",
                imageName = "respalda",
                exercises = listOf(
                    ExerciseDetail(name = "Remo con Barra", repetitions = 12, sets = 3),
                    ExerciseDetail(name = "Peso Muerto", repetitions = 10, sets = 3)
                )
            ),
            PredefinedRoutine(
                title = "Rutina de Pecho",
                description = "Ejercicios para desarrollar la fuerza y el tamaño del pecho.",
                imageName = "rpecho",
                exercises = listOf(
                    ExerciseDetail(name = "Press de Banca", repetitions = 10, sets = 3),
                    ExerciseDetail(name = "Flexiones", repetitions = 15, sets = 3)
                )
            ),
            //gluteos
            PredefinedRoutine(
                title = "Rutina de Glúteos",
                description = "Ejercicios para fortalecer y tonificar los glúteos.",
                imageName = "rgluteos",
                exercises = listOf(
                    ExerciseDetail(name = "Elevación de Cadera", repetitions = 15, sets = 3),
                    ExerciseDetail(name = "Zancadas", repetitions = 12, sets = 3)
                )
            ),
        )

        // Agrega cada rutina a Firestore
        routines.forEach { routine ->
            db.collection("rutinaspredefinidas").add(routine)
                .addOnSuccessListener { documentReference ->
                    Log.d("Firestore", "${routine.title} creada con ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error al crear ${routine.title}", e)
                }
        }
    }


    private fun crearDatosDePrueba() {
        val db = FirebaseFirestore.getInstance()

        // Lista de 25 dietas de ejemplo
        val dietas = listOf(
            // 1. Dieta Proteica para Definición
            Dieta(
                nombre = "Dieta Proteica para Definición",
                tipo = "Proteica",
                objetivo = "Definición muscular y pérdida de grasa",
                macronutrientes = mapOf("Proteínas" to "40%", "Carbohidratos" to "30%", "Grasas" to "30%"),
                calorias = "1800-2200 kcal",
                comidas = "5 comidas: desayuno, media mañana, almuerzo, merienda y cena",
                alimentosPermitidos = listOf("Pollo", "Pavo", "Pescado blanco", "Huevos", "Tofu", "Quinoa", "Espinacas"),
                alimentosProhibidos = listOf("Azúcares refinados", "Harinas refinadas", "Alcohol", "Frituras"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Tortilla de claras con espinacas y avena",
                    "Media mañana" to "Yogur griego con frutos rojos",
                    "Almuerzo" to "Pechuga de pollo con arroz integral y verduras",
                    "Merienda" to "Batido de proteínas con plátano",
                    "Cena" to "Pescado a la plancha con ensalada"
                ),
                suplementacion = listOf("Proteína de suero", "BCAA", "Creatina"),
                consejos = listOf("Hidrátate bien", "Distribuye la proteína a lo largo del día"),
                descripcion = "Dieta alta en proteínas para ayudar a mantener la masa muscular mientras se reduce el porcentaje de grasa.",
                estra = "Déficit calórico moderado con ingesta alta de proteínas.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/1046/1046874.png",
                enlaceWeb = "https://www.fitnessrevolucionario.com/dieta-proteica/"
            ),
            // 2. Dieta Cetogénica
            Dieta(
                nombre = "Dieta Cetogénica",
                tipo = "Keto",
                objetivo = "Pérdida de peso y control metabólico",
                macronutrientes = mapOf("Proteínas" to "20%", "Carbohidratos" to "5%", "Grasas" to "75%"),
                calorias = "1600-2000 kcal",
                comidas = "3-4 comidas: desayuno, almuerzo, cena y opcional merienda",
                alimentosPermitidos = listOf("Carnes", "Huevos", "Aguacate", "Aceite de coco", "Verduras de hoja verde"),
                alimentosProhibidos = listOf("Azúcares", "Cereales", "Legumbres", "Frutas con alto contenido de azúcar"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Huevos revueltos con aguacate y tocino",
                    "Almuerzo" to "Ensalada de pollo con aceite de oliva y nueces",
                    "Merienda" to "Queso y aceitunas",
                    "Cena" to "Salmón a la plancha con espárragos"
                ),
                suplementacion = listOf("Electrolitos", "Magnesio", "MCT Oil"),
                consejos = listOf("Asegura suficiente ingesta de electrolitos", "Adáptate a la fase de cetosis gradualmente"),
                descripcion = "Alimentación baja en carbohidratos y alta en grasas que induce la cetosis para favorecer la quema de grasa.",
                estra = "Restringe los carbohidratos a niveles muy bajos para lograr cetosis.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/2252/2252076.png",
                enlaceWeb = "https://www.dietdoctor.com/es/cetogenica"
            ),
            // 3. Dieta Vegana Equilibrada
            Dieta(
                nombre = "Dieta Vegana Equilibrada",
                tipo = "Vegana",
                objetivo = "Nutrición completa basada en plantas",
                macronutrientes = mapOf("Proteínas" to "20%", "Carbohidratos" to "55%", "Grasas" to "25%"),
                calorias = "1800-2400 kcal",
                comidas = "4-5 comidas a lo largo del día",
                alimentosPermitidos = listOf("Legumbres", "Tofu", "Tempeh", "Frutas", "Verduras", "Semillas"),
                alimentosProhibidos = listOf("Productos de origen animal", "Miel", "Gelatina"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Porridge de avena con plátano y nueces",
                    "Media mañana" to "Smoothie verde con espinacas",
                    "Almuerzo" to "Bowl de quinoa con garbanzos y aguacate",
                    "Merienda" to "Hummus con palitos de zanahoria",
                    "Cena" to "Curry de lentejas con arroz integral"
                ),
                suplementacion = listOf("Vitamina B12", "Vitamina D", "Omega 3 (algas)"),
                consejos = listOf("Combina diferentes fuentes de proteína vegetal", "Suplementa con B12"),
                descripcion = "Dieta basada en alimentos vegetales que cubre todas las necesidades nutricionales con la debida planificación.",
                estra = "Planifica bien las combinaciones de alimentos para asegurar un perfil aminoacídico completo.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/2515/2515183.png",
                enlaceWeb = "https://www.vegansociety.com/go-vegan/nutrition"
            ),
            // 4. Dieta Mediterránea
            Dieta(
                nombre = "Dieta Mediterránea",
                tipo = "Mediterránea",
                objetivo = "Salud cardiovascular y longevidad",
                macronutrientes = mapOf("Proteínas" to "15-20%", "Carbohidratos" to "50-55%", "Grasas" to "30-35%"),
                calorias = "1800-2500 kcal",
                comidas = "3 comidas principales y 1-2 tentempiés",
                alimentosPermitidos = listOf("Aceite de oliva", "Frutas", "Verduras", "Legumbres", "Pescado", "Frutos secos"),
                alimentosProhibidos = listOf("Azúcares refinados", "Harinas refinadas", "Carnes procesadas"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Yogur griego con miel y nueces",
                    "Media mañana" to "Fruta de temporada y almendras",
                    "Almuerzo" to "Ensalada griega con pan integral",
                    "Merienda" to "Hummus con crudités",
                    "Cena" to "Pescado al horno con verduras asadas"
                ),
                suplementacion = listOf("Generalmente no necesaria"),
                consejos = listOf("Usa aceite de oliva virgen extra", "Consume pescado 2-3 veces por semana"),
                descripcion = "Dieta tradicional rica en alimentos frescos y naturales, ideal para la salud cardiovascular.",
                estra = "Enfocada en grasas saludables y alimentos de temporada.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/2902/2902208.png",
                enlaceWeb = "https://dietamediterranea.com/"
            ),
            // 5. Dieta Paleo
            Dieta(
                nombre = "Dieta Paleo",
                tipo = "Paleo",
                objetivo = "Salud metabólica y reducción de inflamación",
                macronutrientes = mapOf("Proteínas" to "25-35%", "Carbohidratos" to "20-40%", "Grasas" to "30-40%"),
                calorias = "1800-2500 kcal",
                comidas = "3 comidas principales, sin picoteo entre horas",
                alimentosPermitidos = listOf("Carnes de pasto", "Pescados salvajes", "Huevos", "Frutas", "Verduras", "Frutos secos"),
                alimentosProhibidos = listOf("Cereales", "Legumbres", "Lácteos", "Azúcares refinados"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Huevos con aguacate y espinacas",
                    "Almuerzo" to "Ensalada de carne con verduras y nueces",
                    "Cena" to "Pescado a la plancha con brócoli y batata"
                ),
                suplementacion = listOf("Magnesio", "Vitamina D"),
                consejos = listOf("Prioriza alimentos frescos y sin procesar", "Evita los alimentos modernos que puedan inflamar"),
                descripcion = "Basada en la alimentación de nuestros ancestros, eliminando alimentos procesados.",
                estra = "Busca alimentos enteros y naturales para favorecer la digestión.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/1886/1886036.png",
                enlaceWeb = "https://www.marksdailyapple.com/"
            ),
            // 6. Dieta para Hipertrofia
            Dieta(
                nombre = "Dieta para Hipertrofia",
                tipo = "Proteica",
                objetivo = "Aumento de masa muscular",
                macronutrientes = mapOf("Proteínas" to "35%", "Carbohidratos" to "50%", "Grasas" to "15%"),
                calorias = "2500-3000 kcal",
                comidas = "6 comidas distribuidas a lo largo del día",
                alimentosPermitidos = listOf("Carne magra", "Pescado", "Huevos", "Arroz integral", "Pasta integral", "Frutas", "Verduras"),
                alimentosProhibidos = listOf("Comida rápida", "Dulces", "Bebidas azucaradas"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Avena con claras y fruta",
                    "Media mañana" to "Batido de proteínas con plátano",
                    "Almuerzo" to "Carne magra con pasta integral y vegetales",
                    "Merienda" to "Yogur griego con frutos secos",
                    "Cena" to "Pescado con quinoa y ensalada",
                    "Snack nocturno" to "Requesón con miel"
                ),
                suplementacion = listOf("Proteína de suero", "Creatina"),
                consejos = listOf("Consume proteína en cada comida", "Ajusta calorías según entrenamiento"),
                descripcion = "Diseñada para favorecer el crecimiento muscular a través de una alta ingesta calórica y proteica.",
                estra = "Distribuye las calorías a lo largo del día para maximizar la síntesis proteica.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/1234/1234567.png",
                enlaceWeb = "https://www.bodybuilding.com/"
            ),
            // 7. Dieta Baja en FODMAP
            Dieta(
                nombre = "Dieta Baja en FODMAP",
                tipo = "Digestiva",
                objetivo = "Mejorar síntomas digestivos",
                macronutrientes = mapOf("Proteínas" to "25%", "Carbohidratos" to "50%", "Grasas" to "25%"),
                calorias = "1700-2100 kcal",
                comidas = "3 comidas principales y 2 snacks",
                alimentosPermitidos = listOf("Arroz", "Pollo", "Zanahoria", "Pepino", "Calabacín", "Frutas bajas en FODMAP"),
                alimentosProhibidos = listOf("Cebolla", "Ajo", "Legumbres", "Manzanas", "Peras"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Tostadas de pan sin gluten con aguacate",
                    "Media mañana" to "Plátano pequeño",
                    "Almuerzo" to "Pechuga de pollo con arroz y zanahorias",
                    "Merienda" to "Yogur sin lactosa con arándanos",
                    "Cena" to "Pescado al horno con calabacín"
                ),
                suplementacion = listOf("Probióticos"),
                consejos = listOf("Identifica tus disparadores alimenticios", "Consulta a un especialista en nutrición"),
                descripcion = "Ayuda a reducir molestias digestivas mediante la restricción de ciertos carbohidratos fermentables.",
                estra = "Elimina temporalmente alimentos altos en FODMAP para mejorar la digestión.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/3344/3344556.png",
                enlaceWeb = "https://www.digestivehealth.com/"
            ),
            // 8. Dieta Detox
            Dieta(
                nombre = "Dieta Detox",
                tipo = "Detox",
                objetivo = "Eliminación de toxinas y reinicio metabólico",
                macronutrientes = mapOf("Proteínas" to "20%", "Carbohidratos" to "60%", "Grasas" to "20%"),
                calorias = "1500-1800 kcal",
                comidas = "3 comidas principales y 2 snacks",
                alimentosPermitidos = listOf("Frutas", "Verduras", "Jugos naturales", "Infusiones", "Frutos secos"),
                alimentosProhibidos = listOf("Alcohol", "Comida procesada", "Azúcares refinados"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Smoothie verde con espinacas y manzana",
                    "Media mañana" to "Ensalada de frutas",
                    "Almuerzo" to "Ensalada grande con quinoa y aguacate",
                    "Merienda" to "Zumo de remolacha y zanahoria",
                    "Cena" to "Sopa de verduras y legumbres"
                ),
                suplementacion = listOf("Té verde"),
                consejos = listOf("Hidrátate constantemente", "Consume alimentos orgánicos"),
                descripcion = "Una dieta temporal para limpiar el organismo y mejorar el bienestar general.",
                estra = "Incorpora alimentos naturales para favorecer la eliminación de toxinas.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/4455/4455667.png",
                enlaceWeb = "https://www.detoxlife.com/"
            ),
            // 9. Dieta Antiinflamatoria
            Dieta(
                nombre = "Dieta Antiinflamatoria",
                tipo = "Saludable",
                objetivo = "Reducir la inflamación crónica",
                macronutrientes = mapOf("Proteínas" to "25%", "Carbohidratos" to "45%", "Grasas" to "30%"),
                calorias = "1700-2200 kcal",
                comidas = "4 comidas principales y 1 snack",
                alimentosPermitidos = listOf("Pescado azul", "Nueces", "Aceite de oliva", "Frutas y verduras"),
                alimentosProhibidos = listOf("Azúcares refinados", "Alimentos fritos", "Carnes procesadas"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Avena con frutos rojos y almendras",
                    "Almuerzo" to "Ensalada de salmón con espinacas",
                    "Merienda" to "Yogur natural con miel",
                    "Cena" to "Pollo a la plancha con verduras al vapor"
                ),
                suplementacion = listOf("Omega 3", "Cúrcuma"),
                consejos = listOf("Incluye especias antiinflamatorias", "Reduce el consumo de alimentos ultraprocesados"),
                descripcion = "Enfocada en alimentos que combaten la inflamación, ideal para personas con procesos inflamatorios crónicos.",
                estra = "Combina grasas saludables y antioxidantes para contrarrestar la inflamación.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/5566/5566778.png",
                enlaceWeb = "https://www.antiinflammatorydiet.com/"
            ),
            // 10. Dieta Flexitariana
            Dieta(
                nombre = "Dieta Flexitariana",
                tipo = "Semi-Vegana",
                objetivo = "Reducción de carne sin eliminarla por completo",
                macronutrientes = mapOf("Proteínas" to "30%", "Carbohidratos" to "50%", "Grasas" to "20%"),
                calorias = "1900-2300 kcal",
                comidas = "3 comidas principales y 2 snacks",
                alimentosPermitidos = listOf("Vegetales", "Legumbres", "Carne magra", "Pescado", "Frutos secos"),
                alimentosProhibidos = listOf("Carnes rojas en exceso", "Alimentos procesados"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Tostadas integrales con aguacate y huevo pochado",
                    "Almuerzo" to "Ensalada de lentejas con verduras",
                    "Merienda" to "Fruta fresca y yogur",
                    "Cena" to "Filete de pescado con quinoa y brócoli"
                ),
                suplementacion = listOf("Vitamina D"),
                consejos = listOf("Introduce carne de forma moderada", "Prioriza alimentos frescos"),
                descripcion = "Una dieta semi-vegetariana que permite flexibilidad en la ingesta de proteínas animales.",
                estra = "Equilibra las fuentes vegetales y animales para una nutrición completa.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/6677/6677889.png",
                enlaceWeb = "https://www.flexihealth.com/"
            ),
            // 11. Dieta DASH
            Dieta(
                nombre = "Dieta DASH",
                tipo = "Cardiovascular",
                objetivo = "Control de la presión arterial",
                macronutrientes = mapOf("Proteínas" to "20%", "Carbohidratos" to "55%", "Grasas" to "25%"),
                calorias = "1800-2200 kcal",
                comidas = "3 comidas principales y 2 snacks",
                alimentosPermitidos = listOf("Frutas", "Verduras", "Granos enteros", "Lácteos bajos en grasa", "Proteínas magras"),
                alimentosProhibidos = listOf("Sodio en exceso", "Alimentos procesados", "Bebidas azucaradas"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Cereal integral con leche descremada y fruta",
                    "Almuerzo" to "Sándwich integral de pavo con ensalada",
                    "Merienda" to "Zumo natural de naranja",
                    "Cena" to "Pescado al horno con vegetales y arroz integral"
                ),
                suplementacion = listOf("Potasio (según indicación)"),
                consejos = listOf("Reduce el sodio", "Aumenta la ingesta de potasio y fibra"),
                descripcion = "Dieta diseñada para ayudar a reducir la presión arterial y mejorar la salud cardiovascular.",
                estra = "Enfocada en alimentos frescos y bajos en sal.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/7788/7788990.png",
                enlaceWeb = "https://www.dashdiet.org/"
            ),
            // 12. Dieta de Volumen
            Dieta(
                nombre = "Dieta de Volumen",
                tipo = "Hipercalórica",
                objetivo = "Ganar masa muscular y peso",
                macronutrientes = mapOf("Proteínas" to "30%", "Carbohidratos" to "55%", "Grasas" to "15%"),
                calorias = "3000-3500 kcal",
                comidas = "5-6 comidas durante el día",
                alimentosPermitidos = listOf("Carne magra", "Arroz", "Pasta integral", "Frutas", "Verduras", "Frutos secos"),
                alimentosProhibidos = listOf("Comida chatarra", "Bebidas azucaradas"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Pan integral con huevo y aguacate",
                    "Media mañana" to "Batido proteico con avena",
                    "Almuerzo" to "Pollo con pasta y vegetales",
                    "Merienda" to "Yogur con frutas y nueces",
                    "Cena" to "Carne asada con arroz y ensalada",
                    "Snack nocturno" to "Requesón con miel"
                ),
                suplementacion = listOf("Proteína de suero", "Creatina"),
                consejos = listOf("Aumenta gradualmente las calorías", "Entrena de forma intensa y consistente"),
                descripcion = "Una dieta hipercalórica enfocada en aumentar la masa muscular mediante una elevada ingesta calórica.",
                estra = "Incrementa la ingesta calórica con alimentos nutritivos.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/8899/8899001.png",
                enlaceWeb = "https://www.gainmuscle.com/"
            ),
            // 13. Dieta para Deportistas
            Dieta(
                nombre = "Dieta para Deportistas",
                tipo = "Energética",
                objetivo = "Optimizar rendimiento y recuperación",
                macronutrientes = mapOf("Proteínas" to "25%", "Carbohidratos" to "55%", "Grasas" to "20%"),
                calorias = "2200-2800 kcal",
                comidas = "4-5 comidas incluyendo pre y post entrenamiento",
                alimentosPermitidos = listOf("Proteínas magras", "Carbohidratos complejos", "Frutas", "Verduras", "Bebidas isotónicas"),
                alimentosProhibidos = listOf("Comida procesada", "Bebidas azucaradas"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Tostadas integrales con aguacate y huevo",
                    "Media mañana" to "Batido de frutas y avena",
                    "Almuerzo" to "Pechuga de pollo con quinoa y ensalada",
                    "Merienda" to "Yogur con frutos secos",
                    "Cena" to "Pescado con batata y verduras"
                ),
                suplementacion = listOf("BCAA", "Electrolitos"),
                consejos = listOf("Optimiza tiempos de comida alrededor del entrenamiento", "Mantén buena hidratación"),
                descripcion = "Plan nutricional enfocado en mejorar el rendimiento deportivo y acelerar la recuperación.",
                estra = "Sincroniza la ingesta de nutrientes con los entrenamientos.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/9900/9900111.png",
                enlaceWeb = "https://www.sportsnutrition.com/"
            ),
            // 14. Dieta Baja en Calorías
            Dieta(
                nombre = "Dieta Baja en Calorías",
                tipo = "Hipocalórica",
                objetivo = "Pérdida de peso gradual",
                macronutrientes = mapOf("Proteínas" to "30%", "Carbohidratos" to "50%", "Grasas" to "20%"),
                calorias = "1200-1500 kcal",
                comidas = "3 comidas principales y 1 snack",
                alimentosPermitidos = listOf("Verduras", "Frutas", "Proteínas magras", "Lácteos bajos en grasa"),
                alimentosProhibidos = listOf("Frituras", "Dulces", "Comida procesada"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Batido de proteínas con fruta y espinacas",
                    "Almuerzo" to "Ensalada de pollo con vegetales variados",
                    "Merienda" to "Manzana con yogur",
                    "Cena" to "Pescado a la plancha con verduras al vapor"
                ),
                suplementacion = listOf("Multivitamínico"),
                consejos = listOf("Controla las porciones", "Realiza ejercicio moderado"),
                descripcion = "Una dieta hipocalórica para promover la pérdida de peso de forma saludable.",
                estra = "Reducción controlada de calorías sin sacrificar nutrientes esenciales.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/1100/1100112.png",
                enlaceWeb = "https://www.lowcaloriediet.com/"
            ),
            // 15. Dieta sin Gluten
            Dieta(
                nombre = "Dieta sin Gluten",
                tipo = "Alergénica",
                objetivo = "Mejorar la digestión en celíacos y sensibles",
                macronutrientes = mapOf("Proteínas" to "25%", "Carbohidratos" to "50%", "Grasas" to "25%"),
                calorias = "1700-2100 kcal",
                comidas = "3 comidas principales y 2 snacks",
                alimentosPermitidos = listOf("Arroz", "Quinoa", "Verduras", "Frutas", "Carnes magras"),
                alimentosProhibidos = listOf("Trigo", "Cebada", "Centeno"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Gachas de avena sin gluten con fruta",
                    "Almuerzo" to "Ensalada de quinoa con pollo y verduras",
                    "Merienda" to "Fruta fresca",
                    "Cena" to "Pescado al horno con arroz y ensalada"
                ),
                suplementacion = listOf("Vitamina D"),
                consejos = listOf("Verifica que todos los productos sean certificados sin gluten", "Lee bien las etiquetas"),
                descripcion = "Diseñada para personas con intolerancia al gluten, con alimentos naturalmente libres de este componente.",
                estra = "Selecciona productos certificados y frescos.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/1212/1212333.png",
                enlaceWeb = "https://www.glutenfreelife.com/"
            ),
            // 16. Dieta sin Lactosa
            Dieta(
                nombre = "Dieta sin Lactosa",
                tipo = "Alergénica",
                objetivo = "Aliviar problemas digestivos por intolerancia",
                macronutrientes = mapOf("Proteínas" to "25%", "Carbohidratos" to "50%", "Grasas" to "25%"),
                calorias = "1800-2200 kcal",
                comidas = "3 comidas principales y 2 snacks",
                alimentosPermitidos = listOf("Bebidas vegetales", "Tofu", "Carnes", "Pescados", "Huevos", "Frutas"),
                alimentosProhibidos = listOf("Leche", "Queso", "Yogur", "Helados", "Mantequilla"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Avena con bebida de almendras y plátano",
                    "Media mañana" to "Frutos secos y fruta",
                    "Almuerzo" to "Pollo asado con patatas y verduras",
                    "Merienda" to "Hummus con crudités",
                    "Cena" to "Tortilla de verduras con ensalada"
                ),
                suplementacion = listOf("Calcio", "Vitamina D"),
                consejos = listOf("Lee bien las etiquetas", "Busca alternativas enriquecidas con calcio"),
                descripcion = "Plan alimenticio para personas con intolerancia a la lactosa que elimina los lácteos convencionales.",
                estra = "Sustituye los lácteos por alternativas vegetales.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/3344/3344445.png",
                enlaceWeb = "https://www.lactosefree.org/"
            ),
// 17. Dieta de Definición Avanzada
            Dieta(
                nombre = "Dieta de Definición Avanzada",
                tipo = "Cíclica",
                objetivo = "Máxima definición muscular para competición",
                macronutrientes = mapOf("Proteínas" to "45%", "Carbohidratos" to "20%", "Grasas" to "35%"),
                calorias = "1500-1800 kcal",
                comidas = "6 comidas repartidas a lo largo del día",
                alimentosPermitidos = listOf("Pechuga de pollo", "Claras de huevo", "Pescado blanco", "Vegetales de hoja verde", "Frutos secos", "Aceite de coco"),
                alimentosProhibidos = listOf("Azúcares", "Harinas refinadas", "Lácteos", "Alcohol", "Alimentos procesados"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Claras de huevo con espinacas",
                    "Media mañana" to "Batido de proteínas con agua",
                    "Almuerzo" to "Pechuga de pollo con espárragos",
                    "Merienda" to "Atún natural con pepino",
                    "Cena" to "Merluza a la plancha con brócoli",
                    "Pre-cama" to "Caseína con agua"
                ),
                suplementacion = listOf("Proteína de suero", "Caseína", "Carnitina", "CLA", "Quemadores"),
                consejos = listOf("Ajusta los carbohidratos según la proximidad a la competición", "Mantén alta hidratación", "Controla el sodio"),
                descripcion = "Plan nutricional muy estricto para atletas que buscan la máxima definición muscular para competiciones.",
                estra = "Ciclos de carbohidratos según días de entrenamiento y descanso.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/2548/2548846.png",
                enlaceWeb = "https://www.competitionprep.com/"
            ),

// 18. Dieta Pescetariana
            Dieta(
                nombre = "Dieta Pescetariana",
                tipo = "Semi-vegetariana",
                objetivo = "Alimentación equilibrada excluyendo carnes",
                macronutrientes = mapOf("Proteínas" to "25%", "Carbohidratos" to "50%", "Grasas" to "25%"),
                calorias = "1800-2200 kcal",
                comidas = "3 comidas principales y 2 snacks",
                alimentosPermitidos = listOf("Pescados", "Mariscos", "Huevos", "Lácteos", "Legumbres", "Frutas", "Verduras"),
                alimentosProhibidos = listOf("Carnes rojas", "Aves", "Embutidos"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Tostadas con aguacate y huevo",
                    "Media mañana" to "Yogur con frutos rojos",
                    "Almuerzo" to "Ensalada de atún con garbanzos",
                    "Merienda" to "Fruta y frutos secos",
                    "Cena" to "Salmón al horno con verduras"
                ),
                suplementacion = listOf("Omega 3", "Vitamina B12"),
                consejos = listOf("Varía los tipos de pescado", "Incluye algas para obtener yodo"),
                descripcion = "Dieta que excluye carnes pero incluye pescados y mariscos como fuente principal de proteína animal.",
                estra = "Combinación de beneficios vegetarianos con los nutrientes del pescado.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/3187/3187550.png",
                enlaceWeb = "https://www.pescetarian.com/"
            ),

// 19. Dieta Alcalina
            Dieta(
                nombre = "Dieta Alcalina",
                tipo = "Equilibrio ácido-base",
                objetivo = "Reducir acidez corporal y mejorar salud general",
                macronutrientes = mapOf("Proteínas" to "15%", "Carbohidratos" to "70%", "Grasas" to "15%"),
                calorias = "1700-2000 kcal",
                comidas = "3 comidas principales y 2 snacks",
                alimentosPermitidos = listOf("Vegetales verdes", "Frutas", "Semillas", "Legumbres", "Aceite de oliva"),
                alimentosProhibidos = listOf("Carnes rojas", "Lácteos", "Azúcares refinados", "Cafeína", "Alcohol"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Smoothie verde con espinacas y aguacate",
                    "Media mañana" to "Manzana con almendras",
                    "Almuerzo" to "Ensalada de quinoa con verduras",
                    "Merienda" to "Zumo verde de apio y pepino",
                    "Cena" to "Sopa de verduras con tofu"
                ),
                suplementacion = listOf("Clorofila", "Spirulina"),
                consejos = listOf("Bebe agua con limón en ayunas", "Reduce alimentos acidificantes"),
                descripcion = "Dieta basada en alimentos alcalinos para equilibrar el pH del organismo y reducir la inflamación.",
                estra = "Prioriza vegetales y reduce proteínas animales.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/4445/4445551.png",
                enlaceWeb = "https://www.alkalinelife.com/"
            ),

// 20. Dieta Diabética
            Dieta(
                nombre = "Dieta Diabética",
                tipo = "Controlada en carbohidratos",
                objetivo = "Controlar niveles de glucosa en sangre",
                macronutrientes = mapOf("Proteínas" to "25%", "Carbohidratos" to "45%", "Grasas" to "30%"),
                calorias = "1600-2000 kcal",
                comidas = "5-6 comidas pequeñas a lo largo del día",
                alimentosPermitidos = listOf("Verduras no feculentas", "Proteínas magras", "Grasas saludables", "Carbohidratos complejos"),
                alimentosProhibidos = listOf("Azúcares simples", "Harinas refinadas", "Bebidas azucaradas", "Alcohol"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Avena con chía y frutos rojos",
                    "Media mañana" to "Yogur natural con nueces",
                    "Almuerzo" to "Pechuga de pollo con verduras y quinoa",
                    "Merienda" to "Manzana con queso fresco",
                    "Cena" to "Pescado al horno con ensalada",
                    "Pre-cama" to "Kéfir natural"
                ),
                suplementacion = listOf("Cromo", "Magnesio"),
                consejos = listOf("Controla las porciones de carbohidratos", "Mide regularmente tus niveles de glucosa"),
                descripcion = "Plan alimenticio para personas con diabetes que ayuda a mantener estables los niveles de azúcar en sangre.",
                estra = "Distribución regular de carbohidratos a lo largo del día.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/3429/3429438.png",
                enlaceWeb = "https://www.diabeticdiet.org/"
            ),

// 21. Dieta FODMAP
            Dieta(
                nombre = "Dieta Baja en FODMAP",
                tipo = "Digestiva",
                objetivo = "Reducir síntomas de SII y problemas digestivos",
                macronutrientes = mapOf("Proteínas" to "25%", "Carbohidratos" to "45%", "Grasas" to "30%"),
                calorias = "1800-2100 kcal",
                comidas = "3 comidas principales y 2 snacks",
                alimentosPermitidos = listOf("Pescado", "Pollo", "Zanahoria", "Espinacas", "Arándanos", "Avena", "Arroz"),
                alimentosProhibidos = listOf("Cebolla", "Ajo", "Trigo", "Legumbres", "Manzanas", "Peras", "Leche"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Avena con leche sin lactosa y plátano",
                    "Media mañana" to "Arándanos y nueces",
                    "Almuerzo" to "Pechuga de pollo con arroz y zanahorias",
                    "Merienda" to "Yogur sin lactosa",
                    "Cena" to "Pescado con patata y espinacas"
                ),
                suplementacion = listOf("Probióticos específicos"),
                consejos = listOf("Realiza la reintroducción gradual de alimentos", "Lleva un diario de síntomas"),
                descripcion = "Dieta terapéutica temporal que elimina carbohidratos fermentables para aliviar problemas digestivos.",
                estra = "Elimina temporalmente y luego reintroduce ciertos alimentos fermentables.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/5233/5233789.png",
                enlaceWeb = "https://www.lowfodmap.com/"
            ),

// 22. Dieta Crudívora
            Dieta(
                nombre = "Dieta Crudívora",
                tipo = "Raw Food",
                objetivo = "Máxima nutrición enzimática y vitamínica",
                macronutrientes = mapOf("Proteínas" to "10%", "Carbohidratos" to "70%", "Grasas" to "20%"),
                calorias = "1600-2000 kcal",
                comidas = "4-5 comidas a lo largo del día",
                alimentosPermitidos = listOf("Frutas crudas", "Verduras crudas", "Frutos secos sin tostar", "Semillas germinadas", "Aceites prensados en frío"),
                alimentosProhibidos = listOf("Alimentos cocinados a más de 42°C", "Procesados", "Lácteos", "Harinas"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Smoothie verde con espinacas y plátano",
                    "Media mañana" to "Frutas frescas variadas",
                    "Almuerzo" to "Ensalada grande con aguacate y germinados",
                    "Merienda" to "Chips de kale deshidratadas",
                    "Cena" to "Zoodles (espaguetis de calabacín) con salsa de tomate cruda"
                ),
                suplementacion = listOf("Vitamina B12", "Vitamina D"),
                consejos = listOf("Utiliza deshidratador para variar texturas", "Remoja frutos secos y semillas"),
                descripcion = "Alimentación basada en productos vegetales crudos o mínimamente procesados para preservar enzimas y nutrientes.",
                estra = "Elimina la cocción para mantener intactas las propiedades de los alimentos.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/2153/2153786.png",
                enlaceWeb = "https://www.rawfoodlife.com/"
            ),

// 23. Dieta HCG
            Dieta(
                nombre = "Dieta HCG",
                tipo = "Hipocalórica",
                objetivo = "Pérdida rápida de peso",
                macronutrientes = mapOf("Proteínas" to "40%", "Carbohidratos" to "30%", "Grasas" to "30%"),
                calorias = "500-800 kcal",
                comidas = "2-3 comidas pequeñas al día",
                alimentosPermitidos = listOf("Proteínas magras", "Vegetales no feculentos", "Frutas bajas en azúcar"),
                alimentosProhibidos = listOf("Azúcares", "Grasas", "Alcohol", "Alimentos procesados"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Té o café sin azúcar",
                    "Almuerzo" to "100g de pollo a la plancha con espárragos",
                    "Cena" to "100g de pescado blanco con ensalada verde"
                ),
                suplementacion = listOf("HCG (bajo supervisión médica)", "Multivitamínico"),
                consejos = listOf("Solo bajo supervisión médica", "Durante períodos cortos", "Bebe mucha agua"),
                descripcion = "Dieta muy restrictiva que se combina con la hormona HCG para promover una rápida pérdida de peso.",
                estra = "Déficit calórico extremo con mínimas cantidades de alimentos.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/9834/9834234.png",
                enlaceWeb = "https://www.hcgdietinfo.com/"
            ),

// 24. Dieta para Hipotiroidismo
            Dieta(
                nombre = "Dieta para Hipotiroidismo",
                tipo = "Metabólica",
                objetivo = "Optimizar función tiroidea y metabolismo",
                macronutrientes = mapOf("Proteínas" to "30%", "Carbohidratos" to "40%", "Grasas" to "30%"),
                calorias = "1700-2000 kcal",
                comidas = "3 comidas principales y 2 snacks",
                alimentosPermitidos = listOf("Pescados ricos en yodo", "Frutos secos", "Huevos", "Vegetales no crucíferos", "Frutas"),
                alimentosProhibidos = listOf("Soja", "Brócoli", "Coliflor", "Col rizada", "Alimentos ultraprocesados"),
                ejemploMenu = mapOf(
                    "Desayuno" to "Huevos revueltos con espinacas",
                    "Media mañana" to "Manzana con nueces de Brasil",
                    "Almuerzo" to "Salmón con batata y ensalada",
                    "Merienda" to "Yogur con arándanos",
                    "Cena" to "Pavo a la plancha con calabaza asada"
                ),
                suplementacion = listOf("Selenio", "Zinc", "Vitamina D"),
                consejos = listOf("Evita consumir medicación tiroidea junto con alimentos", "Limita crucíferas crudas"),
                descripcion = "Plan nutricional diseñado para personas con hipotiroidismo que buscan mejorar su metabolismo.",
                estra = "Incluye alimentos ricos en yodo y selenio, limitando aquellos que interfieren con la función tiroidea.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/8433/8433376.png",
                enlaceWeb = "https://www.thyroiddiet.com/"
            ),

// 25. Dieta de Ayuno Intermitente
            Dieta(
                nombre = "Dieta de Ayuno Intermitente",
                tipo = "Patrón alimenticio",
                objetivo = "Pérdida de grasa y salud metabólica",
                macronutrientes = mapOf("Proteínas" to "30%", "Carbohidratos" to "40%", "Grasas" to "30%"),
                calorias = "1800-2200 kcal (en ventana alimenticia)",
                comidas = "2-3 comidas concentradas en ventana de 8 horas",
                alimentosPermitidos = listOf("Todos los alimentos saludables dentro de la ventana alimenticia"),
                alimentosProhibidos = listOf("Cualquier alimento fuera de la ventana de alimentación", "Ultraprocesados"),
                ejemploMenu = mapOf(
                    "Primera comida (12:00)" to "Ensalada completa con pollo, aguacate y nueces",
                    "Segunda comida (16:00)" to "Yogur griego con frutos rojos y chía",
                    "Última comida (20:00)" to "Salmón con verduras asadas y quinoa"
                ),
                suplementacion = listOf("Electrolitos", "BCAA (opcional)"),
                consejos = listOf("Mantén hidratación durante el ayuno", "Comienza con protocolos 16/8 y ajusta según necesidad"),
                descripcion = "Patrón alimenticio que alterna períodos de ayuno y alimentación, enfocado en cuándo comer más que en qué comer.",
                estra = "Concentra la ingesta calórica en una ventana de tiempo limitada.",
                imagenUrl = "https://cdn-icons-png.freepik.com/512/6997/6997136.png",
                enlaceWeb = "https://www.intermittentfasting.com/"
            )    )

        // Crear una colección de dietas en Firestore
        val dietasRef = db.collection("dietas")

        // Añadir cada dieta a Firestore
        for (dieta in dietas) {
            dietasRef.add(dieta)
                .addOnSuccessListener { documentReference ->
                    Log.d("Firestore", "Dieta guardada con ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error al guardar dieta", e)
                }
        }
    }
}
