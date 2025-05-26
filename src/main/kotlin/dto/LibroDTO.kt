package dto

import kotlinx.serialization.Serializable
import models.EstadoLibro
import models.TipoCubierta

@Serializable
data class BookRequest(
    val titulo: String,
    val autor: String,
    val idioma: String,
    val cubierta: TipoCubierta,
    val categoriaPrincipal: String,
    val categoriaSecundaria: String? = null,
    val estado: EstadoLibro,
    val imagenUrl: String? = null,
    val descripcion: String? = null
)