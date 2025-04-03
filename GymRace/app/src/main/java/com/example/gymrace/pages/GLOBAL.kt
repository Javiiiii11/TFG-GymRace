import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class GLOBAL {



    companion object {

        fun crearUsuarioEnFirestore(userId: String,nombre: String, onComplete: () -> Unit) {
            if (userId.isEmpty()) {
                Log.e("Firestore", "El ID de usuario está vacío, no se puede crear en Firestore.")
                return
            }

            val datosIniciales = hashMapOf(
                "id" to userId,
                "nombre" to nombre,
                "peso" to "0",
                "altura" to "0",
                "edad" to "0",
                "objetivoFitness" to "Sin objetivo",
                "diasEntrenamientoPorSemana" to "0",
                "nivelExperiencia" to "Sin nivel"
            )

            Firebase.firestore.collection("usuarios")
                .document(userId)
                .set(datosIniciales)
                .addOnSuccessListener {
                    Log.d("Firestore", "Usuario creado con valores iniciales correctamente.")
                    onComplete()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error al crear el usuario en Firestore: $e")
                }
        }


        fun guardarDatosRegistro(
            id: String,
            nombre: String,
            peso: String,
            altura: String,
            edad: String,
            objetivoFitness: String,
            diasEntrenamientoPorSemana: String,
            nivelExperiencia: String,
            onComplete: () -> Unit
        ) {
            // Guardar los datos en Firestore
            Log.d("Firestore", "Guardando datos del usuario en Firestore")
            val usuarioActual = FirebaseAuth.getInstance().currentUser
            if (usuarioActual != null) {
                Firebase.firestore.collection("usuarios")
                    .document(id) // Se guarda con el ID específico del usuario
                    .set(
                        hashMapOf(
                            "id" to id,
                            "nombre" to nombre,
                            "peso" to peso,
                            "altura" to altura,
                            "edad" to edad,
                            "objetivoFitness" to objetivoFitness,
                            "diasEntrenamientoPorSemana" to diasEntrenamientoPorSemana,
                            "nivelExperiencia" to nivelExperiencia
                        )
                    )
                    .addOnSuccessListener {
                        GLOBAL.id = id
                        Log.d("Firestore", "Usuario guardado correctamente")

                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        println("Error al guardar el usuario: $e")
                    }
            } else {
                println("Usuario no autenticado")
            }
        }


        var id: String = ""
        var nombre: String = ""
        var peso: String = ""
        var altura: String = ""
        var edad: String = ""
        var objetivoFitness: String = ""
        var diasEntrenamientoPorSemana: String = ""
        var nivelExperiencia: String = ""

        var name: String = ""
        var email: String = ""
        var password: String = ""

    }
}
