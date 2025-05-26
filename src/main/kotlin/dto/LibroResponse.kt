package dto

import kotlinx.serialization.Serializable
import models.Libro

@Serializable
data class LibroResponse(
    val status: String,
    val libro: Libro?
)
