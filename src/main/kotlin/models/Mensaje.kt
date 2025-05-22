package models

import kotlinx.serialization.Serializable
import utils.InstantSerializer
import java.time.Instant
import kotlin.time.ExperimentalTime

@Serializable
data class Mensaje @OptIn(ExperimentalTime::class) constructor(
    val id: Int,
    val idChat: Int,
    val idRemitente: String,
    val contenido: String,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant
)
