package dto

import kotlinx.serialization.Serializable
import models.Usuario

@Serializable
data class UsuarioResponse(
    val status: String,
    val usuario: Usuario
)