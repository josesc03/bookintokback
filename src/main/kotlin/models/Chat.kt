package models

import kotlinx.serialization.Serializable
import utils.InstantSerializer
import java.time.Instant
import kotlin.time.ExperimentalTime

@Serializable
data class Chat @OptIn(ExperimentalTime::class) constructor(
    val id: Int,
    val uidUsuarioOfertante: String,
    val uidUsuarioInteresado: String,
    val idLibro: String,
    @Serializable(with = InstantSerializer::class)
    val fechaCreacion: Instant
)