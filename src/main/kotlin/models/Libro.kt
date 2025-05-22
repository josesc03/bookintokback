package models

import kotlinx.serialization.Serializable
import utils.InstantSerializer
import java.time.Instant
import kotlin.time.ExperimentalTime

enum class EstadoLibro {
    DISPONIBLE,
    RESERVADO,
    INTERCAMBIADO
}

enum class TipoCubierta {
    TAPA_BLANDA,
    TAPA_DURA
}

@Serializable
data class Libro @OptIn(ExperimentalTime::class) constructor(
    val id: Int,
    val uidUsuario: String,
    val titulo: String,
    val autor: String,
    val idioma: String,
    val cubierta: TipoCubierta,
    val categoriaPrincipal: String,
    val categoriaSecundaria: String? = null,
    val estado: EstadoLibro,
    val imagenUrl: String?,
    @Serializable(with = InstantSerializer::class)
    val fechaPublicacion: Instant
)

