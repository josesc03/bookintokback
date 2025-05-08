package models

import kotlinx.serialization.Serializable
import utils.InstantSerializer
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class Chat @OptIn(ExperimentalTime::class) constructor(
    val id: Int,
    val idUsuarioOfertante: Int,
    val idUsuarioInteresado: String,
    val idLibro: String,
    @Serializable(with = InstantSerializer::class)
    val fechaCreacion: Instant
)