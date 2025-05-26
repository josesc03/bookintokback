package models

import kotlinx.serialization.Serializable
import utils.InstantSerializer
import java.time.Instant

@Serializable
enum class EstadoLibro {
    NUEVO,

    COMO_NUEVO,

    USADO,

    ANTIGUO
}

@Serializable
enum class TipoCubierta {
    TAPA_DURA,

    TAPA_BLANDA
}

@Serializable
data class Libro(
    val id: Int,
    val uidUsuario: String,
    val titulo: String,
    val autor: String,
    val descripcion: String?,
    val idioma: String,
    val cubierta: TipoCubierta,
    val categoriaPrincipal: String,
    val categoriaSecundaria: String? = null,
    val estado: EstadoLibro,
    val imagenUrl: String?,
    @Serializable(with = InstantSerializer::class)
    val fechaPublicacion: Instant
)

