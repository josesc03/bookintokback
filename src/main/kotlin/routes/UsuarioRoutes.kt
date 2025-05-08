package routes

import UsuarioService
import com.google.firebase.auth.FirebaseAuth
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import utils.respondError
import java.math.BigDecimal

fun Route.usuarioRoutes() {
    post("/login") {
        try {
            val requestBody = call.receive<Map<String, String>>()
            val token = requestBody["token"] ?: throw IllegalArgumentException("Token no proporcionado")

            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val uid = decodedToken.uid

            // Buscar o crear usuario
            val usuario = UsuarioService.getUserByFirebaseUid(uid) ?: UsuarioService.createUser(
                firebaseUid = uid,
                nickname = decodedToken.name?.plus(uid) ?: uid,
                nombre = decodedToken.name,
                email = decodedToken.email
            )

            call.respond(
                HttpStatusCode.OK, mapOf(
                    "status" to "success",
                    "message" to "Login exitoso",
                    "user" to usuario
                )
            )

        } catch (e: IllegalArgumentException) {
            call.respond(
                HttpStatusCode.BadRequest, mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Token inválido")
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.Unauthorized, mapOf(
                    "status" to "error",
                    "message" to "Error de autenticación"
                )
            )
        }
    }

    post("/update-location") {
        try {
            val requestBody = call.receive<Map<String, Any>>()
            val firebaseUid = call.principal<UserIdPrincipal>()?.name
                ?: return@post call.respondError(
                    "No autenticado",
                    HttpStatusCode.Unauthorized
                )

            // Validación y conversión de tipos más segura
            val latitudRaw = requestBody["latitud"] ?: throw IllegalArgumentException("Latitud no proporcionada")
            val longitudRaw = requestBody["longitud"] ?: throw IllegalArgumentException("Longitud no proporcionada")

            val latitud = when (latitudRaw) {
                is Number -> BigDecimal(latitudRaw.toString())
                else -> throw IllegalArgumentException("Latitud debe ser un número")
            }

            val longitud = when (longitudRaw) {
                is Number -> BigDecimal(longitudRaw.toString())
                else -> throw IllegalArgumentException("Longitud debe ser un número")
            }

            // Validación con compareTo
            if (latitud.compareTo(BigDecimal(-90)) < 0 || latitud.compareTo(BigDecimal(90)) > 0) {
                throw IllegalArgumentException("Latitud fuera de rango válido (-90 a 90)")
            }
            if (longitud.compareTo(BigDecimal(-180)) < 0 || longitud.compareTo(BigDecimal(180)) > 0) {
                throw IllegalArgumentException("Longitud fuera de rango válido (-180 a 180)")
            }

            // Obtener el ID del usuario de la sesión o contexto
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw IllegalArgumentException("ID de usuario no válido")

            val actualizado = UsuarioService.updateUserLocation(
                id,
                firebaseUid,
                latitud.toDouble(),
                longitud.toDouble()
            )

            if (actualizado == null) {
                return@post call.respondError(
                    "No se pudo actualizar la ubicación",
                    HttpStatusCode.NotFound
                )
            }

            call.respond(
                HttpStatusCode.OK, mapOf(
                    "status" to "success",
                    "message" to "Ubicación actualizada correctamente"
                )
            )

        } catch (e: IllegalArgumentException) {
            call.respond(
                HttpStatusCode.BadRequest, mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Error en los datos proporcionados")
                )
            )
        } catch (e: Exception) {
            // Aquí podrías agregar logging del error
            call.respond(
                HttpStatusCode.InternalServerError, mapOf(
                    "status" to "error",
                    "message" to "Error al actualizar la ubicación"
                )
            )
        }
    }

    get("/usuarios/{id}") {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respondError(
                    "ID inválido",
                    HttpStatusCode.BadRequest
                )

            val usuario = UsuarioService.getUserById(id)
                ?: return@get call.respondError(
                    "Usuario no encontrado",
                    HttpStatusCode.NotFound
                )

            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "status" to "success",
                    "usuario" to usuario
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "error",
                    "message" to "Error al obtener el usuario"
                )
            )
        }
    }

    // Ruta para obtener el usuario actual por su UID de Firebase
    get("/me") {
        try {
            val firebaseUid = call.principal<UserIdPrincipal>()?.name
                ?: return@get call.respondError(
                    "No autenticado",
                    HttpStatusCode.Unauthorized
                )

            val usuario = UsuarioService.getUserByFirebaseUid(firebaseUid)
                ?: return@get call.respondError(
                    "Usuario no encontrado",
                    HttpStatusCode.NotFound
                )

            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "status" to "success",
                    "usuario" to usuario
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "error",
                    "message" to "Error al obtener el usuario"
                )
            )
        }
    }

}