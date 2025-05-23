package dto

import kotlinx.serialization.Serializable
import models.Libro

@Serializable
data class LibrosResponse(
    val status: String,
    val libros: List<Libro> = emptyList()
)