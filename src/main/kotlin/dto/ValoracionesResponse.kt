package dto

import kotlinx.serialization.Serializable
import models.Valoracion

@Serializable
data class ValoracionesResponse(
    val status: String,
    val valoraciones: List<Valoracion>
)
