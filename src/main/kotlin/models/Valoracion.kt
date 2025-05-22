package models

import kotlinx.serialization.Serializable
import utils.InstantSerializer
import java.time.Instant
import kotlin.time.ExperimentalTime

@Serializable
data class Valoracion @OptIn(ExperimentalTime::class) constructor(
    val id: Int,
    val uidUsuarioValorado: String,
    val uidUsuarioQueValora: String,
    val puntuacion: Int,
    val comentario: String?,
    @Serializable(with = InstantSerializer::class)
    val fechaValoracion: Instant
)