package dto

import kotlinx.serialization.Serializable
import utils.InstantSerializer
import java.time.Instant

@Serializable
data class ChatListResponse(
    val type: String,
    val chats: List<ChatItem>? = null,
    val chat: ChatItem? = null
)

@Serializable
data class ChatItem(
    val chatId: Int,
    val tituloLibro: String,
    val imagenLibroUrl: String?,
    val nombreUsuario: String,
    val esMio: Boolean,
    val ultimoMensaje: String,
    @Serializable(with = InstantSerializer::class)
    val timestampUltimoMensaje: Instant
)