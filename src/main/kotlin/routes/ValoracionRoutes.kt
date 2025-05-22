package routes

import exceptions.UnauthorizedException
import exceptions.ValidationException
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import services.ValoracionService

fun Route.valoracionRoutes() {
    route("/valoraciones") {
        authenticate("firebase_auth") {
            post("/usuarios/{uidUsuarioValorado}") {
                val uid = call.principal<UserIdPrincipal>()?.name
                    ?: return@post call.respondText(
                        "No autenticado",
                        status = HttpStatusCode.Unauthorized
                    )

                val uidUsuarioValorado = call.parameters["uidUsuarioValorado"]
                    ?: return@post call.respondText(
                        "ID de usuario inválido",
                        status = HttpStatusCode.BadRequest
                    )

                try {
                    val puntuacion = call.parameters["puntuacion"]?.toIntOrNull()
                        ?: return@post call.respondText(
                            "Puntuación inválida",
                            status = HttpStatusCode.BadRequest
                        )

                    val comentario = try {
                        call.receive<String>()
                    } catch (e: Exception) {
                        null
                    }

                    val valoracion = ValoracionService.crearValoracion(
                        uidUsuarioValorado = uidUsuarioValorado,
                        uidUsuarioQueValora = uid,
                        puntuacion = puntuacion,
                        comentario = comentario
                    )
                    call.respond(valoracion)
                } catch (e: Exception) {
                    when (e) {
                        is UnauthorizedException -> call.respondText(
                            e.message ?: "No autorizado",
                            status = HttpStatusCode.Unauthorized
                        )

                        is ValidationException -> call.respondText(
                            e.message ?: "Error de validación",
                            status = HttpStatusCode.BadRequest
                        )

                        else -> call.respondText(
                            e.message ?: "Error al crear valoración",
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                }

            }

            // Obtener valoraciones de un usuario
            get("/usuarios/{uidUsuario}") {
                val uidUsuario = call.parameters["uidUsuario"]
                    ?: return@get call.respondText(
                        "ID de usuario inválido",
                        status = HttpStatusCode.BadRequest
                    )

                try {
                    val valoraciones = ValoracionService.getValoracionesDeUsuario(uidUsuario)
                    call.respond(valoraciones)
                } catch (e: Exception) {
                    call.respondText(
                        e.message ?: "Error al obtener valoraciones",
                        status = HttpStatusCode.InternalServerError
                    )
                }

            }

            // Añadir dentro del bloque authenticate
            get("/usuarios/{uidUsuario}/promedio") {
                val uidUsuario = call.parameters["uidUsuario"]
                    ?: return@get call.respondText(
                        "ID de usuario inválido",
                        status = HttpStatusCode.BadRequest
                    )

                try {
                    val promedio = ValoracionService.getValoracionPromedio(uidUsuario)
                    call.respond(mapOf("promedio" to promedio))
                } catch (e: Exception) {
                    call.respondText(
                        e.message ?: "Error al obtener el promedio",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
        }
    }
}