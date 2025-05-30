package services

import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap


object MessageConnectionManager {
    private val sessions = ConcurrentHashMap<String, MutableList<WebSocketSession>>() // uid -> sesiones

    fun addSession(uid: String, session: WebSocketSession) {
        sessions.computeIfAbsent(uid) { mutableListOf() }.add(session)
    }

    fun removeSession(uid: String, session: WebSocketSession) {
        sessions[uid]?.remove(session)
        if (sessions[uid].isNullOrEmpty()) {
            sessions.remove(uid)
        }
    }

    suspend fun sendToUser(uid: String, message: String) {
        sessions[uid]?.forEach { session ->
            session.send(Frame.Text(message))
        }
    }

    suspend fun broadcastToAll(message: String) {
        sessions.values.flatten().forEach { session ->
            session.send(Frame.Text(message))
        }
    }
}
