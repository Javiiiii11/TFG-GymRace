import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class GLOBAL {
    companion object {

        fun guardarDatosRegistro(
            id: String,
            peso: String,
            altura: String,
            edad: String,
            objetivoFitness: String,
            diasEntrenamientoPorSemana: String,
            nivelExperiencia: String,
            onComplete: () -> Unit
        ) {
            val usuarioActual = FirebaseAuth.getInstance().currentUser
            if (usuarioActual != null) {
                Firebase.firestore.collection("usuarios")
                    .document(id) // Se guarda con el ID específico del usuario
                    .set(
                        hashMapOf(
                            "id" to id,
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
                        println("Usuario guardado con ID: $id")

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
        var peso: String = ""
        var altura: String = ""
        var edad: String = ""
        var objetivoFitness: String = ""
        var diasEntrenamientoPorSemana: String = ""
        var nivelExperiencia: String = ""
    }
}
