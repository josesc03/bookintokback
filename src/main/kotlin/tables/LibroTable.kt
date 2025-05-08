package tables

import models.EstadoLibro
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object LibroTable : Table("Libro") {
    val id = integer("id").autoIncrement()
    val idUsuario = integer("id_usuario").references(UsuarioTable.id)
    val titulo = varchar("titulo", 255)
    val autor = varchar("autor", 255)
    val categoriaPrincipal = varchar("categoria_principal", 255)
    val categoriaSecundaria = varchar("categoria_secundaria", 255).nullable()
    val idioma = varchar("idioma", 255)
    val cubierta = varchar("cubierta", 255)
    val estado = enumerationByName("estado", 20, EstadoLibro::class)
    val imagenUrl = varchar("imagen_url", 255).nullable()
    val fechaPublicacion = datetime("fecha_publicacion")

    override val primaryKey = PrimaryKey(id)
}