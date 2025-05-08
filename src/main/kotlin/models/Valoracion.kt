package models

import kotlinx.serialization.Serializable
import utils.InstantSerializer
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class Valoracion @OptIn(ExperimentalTime::class) constructor(
    val id: Int,
    val idUsuarioValorado: Int,
    val idUsuarioQueValora: Int,
    val puntuacion: Int,
    val comentario: String?,
    @Serializable(with = InstantSerializer::class)
    val fechaValoracion: Instant
)