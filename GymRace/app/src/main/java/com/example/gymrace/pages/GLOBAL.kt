package com.example.gymrace.pages

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// Clase GLOBAL para manejar datos globales y operaciones de Firestore
class GLOBAL {
    // Variables globales para almacenar datos del usuario
    companion object {

        //Variables globales para almacenar datos del usuario
        var id: String = ""
        var nombre: String = ""
        var peso: String = ""
        var altura: String = ""
        var edad: String = ""
        var objetivoFitness: String = ""
        var diasEntrenamientoPorSemana: String = ""
        var nivelExperiencia: String = ""
        var cuentaPrivada: Boolean = false

        // Variables para el registro de usuario
        var name: String = ""
        var email: String = ""
        var password: String = ""

        // Función para crear un usuario en Firestore con valores iniciales
        fun crearUsuarioEnFirestore(userId: String,nombre: String, onComplete: () -> Unit) {
            // Crear un nuevo usuario en Firestore con valores iniciales
            if (userId.isEmpty()) {
                Log.e("Firestore", "El ID de usuario está vacío, no se puede crear en Firestore.")
                return
            }
            // Crear un HashMap con los datos iniciales del usuario
            val datosIniciales = hashMapOf(
                "id" to userId,
                "nombre" to nombre,
                "peso" to "0",
                "altura" to "0",
                "edad" to "0",
                "objetivoFitness" to "Sin objetivo",
                "diasEntrenamientoPorSemana" to "0",
                "nivelExperiencia" to "Sin nivel",
                "cuentaPrivada" to false,
                "fechaCreacion" to FieldValue.serverTimestamp()
            )
            // Guardar los datos en Firestore
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

        // Función para guardar los datos del registro del usuario en Firestore
        fun guardarDatosRegistro(
            id: String,
            nombre: String,
            peso: String,
            altura: String,
            edad: String,
            objetivoFitness: String,
            diasEntrenamientoPorSemana: String,
            nivelExperiencia: String,
            cuentaPrivada: Boolean,
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
                            "nivelExperiencia" to nivelExperiencia,
                            "cuentaPrivada" to cuentaPrivada
                        )
                    )
                    .addOnSuccessListener {
                        GLOBAL.id = id
                        Log.d("Firestore", "Usuario guardado correctamente")

                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error al guardar el usuario: $e")
                    }
            } else {
                Log.e("Firestore", "No se pudo guardar el usuario, no está autenticado")
            }
        }

    }

}
