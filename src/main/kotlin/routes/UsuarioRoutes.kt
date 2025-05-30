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
            if (UsuarioService.getUserByUid(uid) != null) {
                throw IllegalArgumentException("Usuario ya registrado con este UID")
            }

            // Verificar si el email ya está en uso
            if (UsuarioService.getUserByEmail(email) != null) {
                throw IllegalArgumentException("El correo electrónico ya está registrado")
            }


            // Crear usuario
            UsuarioService.createUser(
                uid = uid,
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

    get("/usuario/{uid}") {
        println("Iniciando endpoint /usuario/{uid}")
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            decodedToken.uid

            val uid = call.parameters["uid"]
                ?: return@get call.respondError(
                    "ID inválido",
                    HttpStatusCode.BadRequest
                )

            val usuario = UsuarioService.getUserByUid(uid)
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
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val uid = decodedToken.uid

            val usuario = UsuarioService.getUserByUid(uid)
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

    get("/usuario/{uid}/nombre") {
        println("Iniciando endpoint /usuario/{uid}/nombre")

        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            decodedToken.uid

            var userUid: String? = call.parameters["uid"]
            if (userUid == null) {
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "status" to "success",
                        "message" to "Usuario no encontrado"
                    )
                )
            }

            val nombre = UsuarioService.getUserByUid(userUid)?.nombre

            call.respond(
                HttpStatusCode.OK, nombre.toString()
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

    get("/has-completed-exchange/{userUid}") {
        println("Iniciando endpoint /has-completed-exchange/{userUid}")
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val tokenUid = decodedToken.uid

            val userUid = call.parameters["userUid"] ?: return@get call.respondError(
                "ID de usuario inválido",
                HttpStatusCode.BadRequest
            )

            val hasExchanged = UsuarioService.hasCompletedExchange(tokenUid, userUid)

            call.respond(
                HttpStatusCode.OK, hasExchanged
            )
        } catch (e: IllegalArgumentException) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Error en los datos proporcionados")
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "error",
                    "message" to "Error al verificar el intercambio\n${e.message}"
                )
            )
        }
    }

    get("/has-rated/{userUid}") {
        println("Iniciando endpoint /has-rated/{userUid}")
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val tokenUid = decodedToken.uid

            val userUid = call.parameters["userUid"] ?: return@get call.respondError(
                "ID de usuario inválido",
                HttpStatusCode.BadRequest
            )

            val hasRated = UsuarioService.hasRated(tokenUid, userUid)

            call.respond(
                HttpStatusCode.OK, hasRated
            )
        } catch (e: IllegalArgumentException) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Error en los datos proporcionados")
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "error",
                    "message" to "Error al verificar la valoración\n${e.message}"
                )
            )
        }
    }

    post("/usuario/valorar/{uidUsuario}") {
        println("Iniciando endpoint /usuario/valorar/{uidUsuario}")
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val tokenUid = decodedToken.uid

            val userUid = call.parameters["uidUsuario"] ?: return@post call.respondError(
                "ID de usuario inválido",
                HttpStatusCode.BadRequest
            )
            val request = call.receive<Map<String, String>>()
            val puntuacion = request["puntuacion"] as String
            val comentario = request["comentario"] as String
            UsuarioService.valorarUsuario(tokenUid, userUid, comentario, puntuacion.toInt())
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "status" to "success",
                    "message" to "Usuario valorado correctamente"
                )
            )
        } catch (e: IllegalArgumentException) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Error en los datos proporcionados")
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "error",
                    "message" to "Error al valorar el usuario\n${e.message}"
                )
            )
        }
    }

    get("/usuario/valoraciones/{uid}") {
        println("Iniciando endpoint /usuario/valoraciones/{uid}")
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            decodedToken.uid

            val uid = call.parameters["uid"] ?: return@get call.respondError(
                "ID de usuario inválido",
                HttpStatusCode.BadRequest
            )

            val valoraciones = UsuarioService.getValoracionesFromUid(uid)

            call.respond(
                HttpStatusCode.OK,
                valoraciones
            )
        } catch (e: IllegalArgumentException) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Error en los datos proporcionados")
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "error",
                    "message" to "Error al obtener las valoraciones\n${e.message}"
                )
            )
        }
    }

    post("/usuario/update") {
        println("Iniciando endpoint POST /usuario/update")
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val uid = decodedToken.uid

            val request = call.receive<Map<String, String>>()
            val imageUrl = request["imageUrl"] ?: throw IllegalArgumentException("URL de imagen no proporcionada")
            val nombre = request["nombre"] ?: throw IllegalArgumentException("Nombre no proporcionado")

            UsuarioService.updateUser(uid, imageUrl, nombre)
                ?: return@post call.respondError(
                    "No se pudo actualizar el usuario",
                    HttpStatusCode.NotFound
                )

            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "status" to "success",
                    "message" to "Usuario actualizado correctamente"
                )
            )

        } catch (e: IllegalArgumentException) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Error en los datos proporcionados")
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "error",
                    "message" to "Error al actualizar el usuario\n${e.message}"
                )
            )
        }
    }


}