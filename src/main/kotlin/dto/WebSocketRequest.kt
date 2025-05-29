package dto

import kotlinx.serialization.Serializable

@Serializable
data class WebSocketRequest(val action: String)