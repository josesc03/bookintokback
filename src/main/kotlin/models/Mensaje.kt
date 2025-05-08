package models

import kotlinx.serialization.Serializable
import utils.InstantSerializer
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class Mensaje @OptIn(ExperimentalTime::class) constructor(
    val id: Int,
    val idChat: Int,
    val idRemitente: Int,
    val contenido: String,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant
)
