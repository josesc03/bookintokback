package dto

import kotlinx.serialization.Serializable
import models.Chat
import models.EstadoIntercambio
import models.Intercambio

@Serializable
data class CrearChatRequest(
    val idUsuarioInteresado: String,
    val idLibro: String
)

@Serializable
data class EstadoRequest(
    val estado: EstadoIntercambio
)

@Serializable
data class ChatResponse(
    val chats: Map<Int, ChatInfo>
)

@Serializable
data class ChatInfo(
    val chat: Chat,
    val intercambio: Intercambio
)

