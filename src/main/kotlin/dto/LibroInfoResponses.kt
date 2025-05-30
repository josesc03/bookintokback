package dto

import kotlinx.serialization.Serializable

@Serializable
data class LibroInfoResponse(
    val status: String,
    val libros: List<LibroInfo> = emptyList()
)

@Serializable
data class LibroInfo(
    val id: String,
    val url: String?,
    val titulo: String,
    val autor: String,
    val isCompleted: Boolean
)