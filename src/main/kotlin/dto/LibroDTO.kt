package dto

import kotlinx.serialization.Serializable
import models.TipoCubierta
import models.EstadoLibro

@Serializable
data class BookRequest(
    val titulo: String,
    val autor: String,
    val idioma: String,
    val cubierta: TipoCubierta,
    val categoriaPrincipal: String,
    val categoriaSecundaria: String? = null,
    val estado: EstadoLibro,
    val imagenUrl: String? = null
)