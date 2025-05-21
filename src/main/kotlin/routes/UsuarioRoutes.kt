package routes

import UsuarioService
import com.google.firebase.auth.FirebaseAuth
import dto.UsuarioResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import utils.respondError

fun Route.usuarioRoutes() {
    post("/login") {
        println("Iniciando endpoint /login")
        try {
            call.receive<Map<String, String>>()
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val uid = decodedToken.uid


            // Buscar usuario
            UsuarioService.getUserByFirebaseUid(uid)
                ?: throw IllegalArgumentException("Usuario no encontrado")

            call.respond(
                HttpStatusCode.OK, mapOf(
                    "status" to "success",
                    "message" to "Login exitoso",
                )
            )

            println("Sesión iniciada correctamente")

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

    post("/register") {
        println("Iniciando endpoint /register")
        try {
            val request = call.receive<Map<String, String>>()
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val uid = decodedToken.uid
            val email = decodedToken.email ?: throw IllegalArgumentException("Email no proporcionado")
            val nickname = request["username"] ?: throw IllegalArgumentException("Nickname no proporcionado")


            // Verificar si el usuario ya existe por UID
            if (UsuarioService.getUserByFirebaseUid(uid) != null) {
                throw IllegalArgumentException("Usuario ya registrado con este UID")
            }

            // Verificar si el email ya está en uso
            if (UsuarioService.getUserByEmail(email) != null) {
                throw IllegalArgumentException("El correo electrónico ya está registrado")
            }


            // Crear usuario
            UsuarioService.createUser(
                firebaseUid = uid,
                nickname = nickname,
                email = email
            )

            call.respond(
                HttpStatusCode.Created, mapOf(
                    "status" to "success",
                    "message" to "Registro exitoso",
                )
            )

            println("Registro creado correctamente")

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
                    "message" to "Error de registro"
                )
            )
        }
    }

    post("/update-location") {
        println("Iniciando endpoint /update-location")

        try {
            val request = call.receive<Map<String, Double>>()
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            authHeader.removePrefix("Bearer ").trim()
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val uid = decodedToken.uid

            val latitud = request["latitud"]
            val longitud = request["longitud"]

            if (latitud == null || latitud !in -90.0..90.0) {
                throw IllegalArgumentException("Latitud fuera de rango válido (-90 a 90) o es nula")
            }
            if (longitud == null || longitud !in -180.0..180.0) {
                throw IllegalArgumentException("Longitud fuera de rango válido (-180 a 180) o es nula")
            }

            val actualizado = UsuarioService.updateUserLocationByUid(
                uid,
                latitud,
                longitud
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
            println("Error al actualizar la ubicación: ${e.message}")
            call.respond(
                HttpStatusCode.InternalServerError, mapOf(
                    "status" to "error",
                    "message" to "Error al actualizar la ubicación"
                )
            )
        }
    }

    get("/usuarios/{id}") {
        println("Iniciando endpoint /usuarios/{id}")
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
        println("Iniciando endpoint /me")

        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            authHeader.removePrefix("Bearer ").trim()
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val uid = decodedToken.uid

            val usuario = UsuarioService.getUserByFirebaseUid(uid)
                ?: return@get call.respondError(
                    "Usuario no encontrado",
                    HttpStatusCode.NotFound
                )

            call.respond(
                HttpStatusCode.OK,
                UsuarioResponse(
                    status = "success",
                    usuario = usuario
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "error",
                    "message" to "Error al obtener el usuario\n${e.message}"
                )
            )
        }
    }

}