package models

import kotlinx.serialization.Serializable
import utils.InstantSerializer
import java.time.Instant
import kotlin.time.ExperimentalTime

enum class EstadoIntercambio {
    PENDIENTE,
    ACEPTADO,
    COMPLETADO,
    CANCELADO
}

@Serializable
data class Intercambio @OptIn(ExperimentalTime::class) constructor(
    val id: Int,
    val idChat: Int,
    val estado: EstadoIntercambio,
    @Serializable(with = InstantSerializer::class)
    val fechaCreacion: Instant,
)
