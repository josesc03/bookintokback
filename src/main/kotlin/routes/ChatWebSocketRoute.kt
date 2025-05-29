// ChatWebSocketRoute.kt
package routes

import com.google.firebase.auth.FirebaseAuth
import dto.ChatListResponse
import dto.WebSocketRequest
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import services.ChatConnectionManager
import services.ChatService

fun Route.chatWebSocketRoute() {
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
}
