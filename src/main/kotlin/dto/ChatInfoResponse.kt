package dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatInfoResponse(
    val status: String,
    val chats: ChatInfo? = null,
)

@Serializable
data class ChatInfo(
    val tituloLibro: String,
    val imagenLibroUrl: String?,
    val nombreUsuario: String,
    val uidUsuario: String
)