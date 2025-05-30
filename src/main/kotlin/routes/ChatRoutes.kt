// ChatWebSocketRoute.kt
package routes

import com.google.firebase.auth.FirebaseAuth
import dto.ChatInfoResponse
import dto.ChatListResponse
import dto.MessageInfoResponse
import dto.WebSocketRequest
import io.ktor.http.*
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

fun Route.chatRoute() {
    webSocket("/ws/chats") {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.trim()
        val uid = try {
            FirebaseAuth.getInstance().verifyIdToken(token).uid
        } catch (e: Exception) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Token inválido"))
            return@webSocket
        }

        ChatConnectionManager.addSession(uid, this)

        try {
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val action = Json.decodeFromString<WebSocketRequest>(text)

                    when (action.action) {
                        "get_chats" -> {
                            val chats = ChatService.obtenerChatsActivos(uid)
                            val response = ChatListResponse(type = "chat_list", chats = chats)
                            outgoing.send(Frame.Text(Json.encodeToString(response)))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("WebSocket error: ${e.message}")
        } finally {
            ChatConnectionManager.removeSession(uid, this)
        }
    }

    get("/chats/{id}") {
        println("Iniciando endpoint GET /chats/{id}")
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val uid = decodedToken.uid

            val id = call.parameters["id"]?.toInt() ?: return@get call.respondError(
                "ID de chat inválido",
                HttpStatusCode.BadRequest
            )

            val chat = ChatService.getChatInfo(chatId = id, uidActual = uid)

            val response = ChatInfoResponse("success", chat)

            call.respond(HttpStatusCode.OK, response)

        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "error",
                    "message" to "Error al obtener el chat: ${e.message}"
                )
            )
        }
    }

    post("/chat/{idLibro}") {
        println("Iniciando endpoint POST /chat/{idLibro}")
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val uid = decodedToken.uid

            val idLibro = call.parameters["idLibro"] ?: return@post call.respondError(
                "ID de libro inválido",
                HttpStatusCode.BadRequest
            )

            val chat = ChatService.createChat(idLibro.toInt(), uid)

            val response = mapOf(
                "status" to "success",
                "message" to if (chat.first) "Chat creado con exito" else "Chat encontrado con exito",
                "idChat" to chat.second.toString()
            )

            call.respond(HttpStatusCode.OK, response)

        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "error",
                    "message" to "Error al crear el chat: ${e.message}"
                )
            )
        }
    }

    post("/chat/cancel/{idChat}") {
        println("Iniciando endpoint POST /chat/cancel/{idChat}")
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            decodedToken.uid

            val idChat = call.parameters["idChat"]?.toInt() ?: return@post call.respondError(
                "ID de chat inválido",
                HttpStatusCode.BadRequest
            )

            ChatService.cancelChat(idChat)
            val response = mapOf(
                "status" to "success",
                "message" to "Intercambio cancelado con exito"
            )

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

            call.respond(HttpStatusCode.OK, response)

        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "error",
                    "message" to "Error al cancelar el intercambio: ${e.message}"
                )
            )
        }
    }

    post("/chat/confirmar/{idChat}") {
        println("Iniciando endpoint POST /chat/confirmar/{idChat}")
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

            ChatService.confirmarIntercambio(idChat, uidUsuario = uid)
            val response = mapOf(
                "status" to "success",
                "message" to "Intercambio confirmado con exito"
            )

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

            call.respond(HttpStatusCode.OK, response)

        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "error",
                    "message" to "Error al confirmar el intercambio: ${e.message}"
                )
            )
        }
    }

}
