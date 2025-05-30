package routes

import com.google.firebase.auth.FirebaseAuth
import dto.ChatListResponse
import dto.MessageInfoResponse
import dto.WebSocketRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import services.ChatConnectionManager
import services.ChatService
import services.MensajeService
import services.MessageConnectionManager
import utils.respondError

fun Route.mensajeRoutes() {
    webSocket("/ws/messages/{idChat}") {
        println("Iniciando websocket /ws/messages/{idChat}")

        val authHeader =
            call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
        val token = authHeader.removePrefix("Bearer ").trim()
        val uid: String

        try {
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            uid = decodedToken.uid

        } catch (e: Exception) {
            println("excepcion encontrada token invalido, $e")
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Token inválido"))
            return@webSocket
        }

        val idChat = call.parameters["idChat"]?.toInt() ?: run {
            println("excepcion encontrada ID de chat invalido")
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "ID de chat inválido"))
            return@webSocket
        }

        print(idChat)

        MessageConnectionManager.addSession(uid, this)

        try {
            println("Enviando mensajes iniciales")
            val messages = MensajeService.getMensajes(idChat)
            val hasUserConfirmedExchange = ChatService.hasUserConfirmedExchange(idChat, uid)
            ChatService.getEstadoIntercambio(idChat)
            val response = MessageInfoResponse(
                type = "message_list",
                messages = messages,
                hasUserConfirmedExchange = hasUserConfirmedExchange,
                estadoIntercambio = ChatService.getEstadoIntercambio(idChat)
            )
            outgoing.send(Frame.Text(Json.encodeToString(response)))
        } catch (e: Exception) {
            println("Error al enviar mensajes iniciales: ${e.message}")
        }

        try {
            println("Iniciando actualizacion de mensajes")
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val action = Json.decodeFromString<WebSocketRequest>(text)

                    when (action.action) {
                        "get_messages" -> {
                            val messages = MensajeService.getMensajes(idChat)
                            val hasUserConfirmedExchange =
                                ChatService.hasUserConfirmedExchange(idChat, uid)
                            ChatService.getEstadoIntercambio(idChat)
                            val response = MessageInfoResponse(
                                type = "message_list",
                                messages = messages,
                                hasUserConfirmedExchange = hasUserConfirmedExchange,
                                estadoIntercambio = ChatService.getEstadoIntercambio(idChat)
                            )
                            outgoing.send(Frame.Text(Json.encodeToString(response)))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("WebSocket error: ${e.message}")
        } finally {
            MessageConnectionManager.removeSession(uid, this)
        }
    }

    post("/chat/send/{idChat}") {
        println("Iniciando endpoint POST /chat/send/{idChat}")
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val uid = decodedToken.uid

            val idChat = call.parameters["idChat"]?.toInt() ?: return@post call.respondError(
                "ID de chat inválido",
                HttpStatusCode.BadRequest
            )

            val mensaje = call.receive<String>()

            MensajeService.sendMessage(idChat, uid, mensaje)

            val participantes = ChatService.getUserIdsInChat(idChat)

            participantes.forEach { participantUid ->
                val updateMessage = Json.encodeToString(
                    MessageInfoResponse(
                        type = "message_list",
                        messages = MensajeService.getMensajes(idChat),
                        hasUserConfirmedExchange = ChatService.hasUserConfirmedExchange(idChat, participantUid),
                        estadoIntercambio = ChatService.getEstadoIntercambio(idChat)
                    )
                )
                MessageConnectionManager.sendToUser(participantUid, updateMessage)

                val updateChatList = Json.encodeToString(
                    ChatListResponse(
                        type = "chat_list",
                        chats = ChatService.obtenerChatsActivos(participantUid)
                    )
                )
                ChatConnectionManager.sendToUser(participantUid, updateChatList)
            }

            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "status" to "success",
                    "message" to "mensaje enviado con exito"
                )
            )

        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "error",
                    "message" to "Error al enviar el mensaje: ${e.message}"
                )
            )
        }
    }


}
