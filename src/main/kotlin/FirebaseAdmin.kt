package com.bookintok

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.server.application.*
import java.io.FileInputStream
import java.io.FileNotFoundException

fun Application.configureFirebase() {
    try {
        val serviceAccountPath = "src/main/resources/firebase/firebase-config.json"
        val serviceAccount = FileInputStream(serviceAccountPath)

        val options = FirebaseOptions.builder()
            .setCredentials(com.google.auth.oauth2.GoogleCredentials.fromStream(serviceAccount))
            .build()

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
            println("\u001B[38;2;152;246;160mFirebase inicializado correctamente\u001B[0m\n")
        }
    } catch (e: FileNotFoundException) {
        println("\u001B[38;2;181;71;71mError: No se encontró el archivo de configuración de Firebase\u001B[0m\n")
        println("\u001B[38;2;161;130;64mAsegúrate de que el archivo firebase-config.json está en src/main/resources/firebase/\u001B[0m\n")
        throw e
    } catch (e: Exception) {
        println("\u001B[38;2;181;71;71mError al inicializar Firebase: ${e.message}")
        throw e
    }
}