package routes

import dto.ChatInfo
import dto.ChatResponse
import dto.CrearChatRequest
import dto.EstadoRequest
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import services.ChatIntercambioService
import utils.respondError
import utils.respondSuccess

fun Route.chatIntercambioRoutes() {
    route("/chats") {
        // Crear chat e intercambio
        post {
            val principal = call.principal<UserIdPrincipal>()
                ?: return@post call.respondError(
                    "No autenticado",
                    HttpStatusCode.Unauthorized
                )

            try {
                val request = call.receive<CrearChatRequest>()
                val (chat, intercambio) = ChatIntercambioService.crearChatEIntercambio(
                    uidUsuarioOfertante = principal.name,
                    uidUsuarioInteresado = request.uidUsuarioInteresado,
                    idLibro = request.idLibro
                )

                call.respondSuccess(ChatInfo(chat, intercambio))
            } catch (e: Exception) {
                call.respondError(
                    "Error al crear chat e intercambio: ${e.message}",
                    HttpStatusCode.InternalServerError
                )
            }
        }

        // Obtener chats activos (pendientes o aceptados)
        get {
            val principal = call.principal<UserIdPrincipal>()
                ?: return@get call.respondError(
                    "No autenticado",
                    HttpStatusCode.Unauthorized
                )

            try {
                val chatsActivos = ChatIntercambioService.obtenerChatsActivos(principal.name)
                call.respondSuccess(ChatResponse(chatsActivos))
            } catch (e: Exception) {
                call.respondError(
                    "Error al obtener chats: ${e.message}",
                    HttpStatusCode.InternalServerError
                )
            }
        }

        // Obtener estado del intercambio de un chat
        get("/{chatId}/estado") {
            call.principal<UserIdPrincipal>()
                ?: return@get call.respondError(
                    "No autenticado",
                    HttpStatusCode.Unauthorized
                )

            val chatId = call.parameters["chatId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    "ID de chat inválido",
                    HttpStatusCode.BadRequest
                )

            try {
                val estado = ChatIntercambioService.obtenerEstadoIntercambio(chatId)
                call.respondSuccess(mapOf("estado" to estado))
            } catch (e: Exception) {
                call.respondError(
                    "Error al obtener estado: ${e.message}",
                    HttpStatusCode.InternalServerError
                )
            }
        }

        // Actualizar estado del intercambio
        put("/{chatId}/estado") {
            val principal = call.principal<UserIdPrincipal>()
                ?: return@put call.respondError(
                    "No autenticado",
                    HttpStatusCode.Unauthorized
                )

            val chatId = call.parameters["chatId"]?.toIntOrNull()
                ?: return@put call.respondError(
                    "ID de chat inválido",
                    HttpStatusCode.BadRequest
                )

            try {
                val request = call.receive<EstadoRequest>()
                val nuevoEstado = ChatIntercambioService.actualizarEstadoIntercambio(
                    chatId = chatId,
                    nuevoEstado = request.estado,
                    uidUsuario = principal.name
                )
                call.respondSuccess(mapOf("estado" to nuevoEstado))
            } catch (e: Exception) {
                call.respondError(
                    "Error al actualizar estado: ${e.message}",
                    HttpStatusCode.InternalServerError
                )
            }
        }
    }
}