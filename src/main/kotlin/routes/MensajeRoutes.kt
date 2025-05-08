package routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import services.MensajeService

fun Route.mensajeRoutes() {
    route("/chats/{chatId}/mensajes") {
        authenticate("firebase_auth") {
            post {
                val uid = call.principal<UserIdPrincipal>()?.name
                    ?: return@post call.respondText(
                        "No autenticado",
                        status = HttpStatusCode.Unauthorized
                    )

                val chatId = call.parameters["chatId"]?.toIntOrNull()
                    ?: return@post call.respondText(
                        "ID de chat inválido",
                        status = HttpStatusCode.BadRequest
                    )

                try {
                    val contenido = call.receive<String>()
                    val mensaje = MensajeService.enviarMensaje(
                        chatId = chatId,
                        idRemitente = uid.toInt(),
                        contenido = contenido
                    )
                    call.respond(mensaje)
                } catch (e: Exception) {
                    call.respondText(
                        e.message ?: "Error al enviar mensaje",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }

            get {
                val uid = call.principal<UserIdPrincipal>()?.name
                    ?: return@get call.respondText(
                        "No autenticado",
                        status = HttpStatusCode.Unauthorized
                    )

                val chatId = call.parameters["chatId"]?.toIntOrNull()
                    ?: return@get call.respondText(
                        "ID de chat inválido",
                        status = HttpStatusCode.BadRequest
                    )

                try {
                    val mensajes = MensajeService.getMensajes(
                        chatId = chatId,
                        idUsuario = uid.toInt()
                    )
                    call.respond(mensajes)
                } catch (e: Exception) {
                    call.respondText(
                        e.message ?: "Error al obtener mensajes",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
        }
    }
}
