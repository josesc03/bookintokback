package dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatListResponse(
    val type: String,
    val chats: List<ChatItem>? = null,
    val chat: ChatItem? = null
)