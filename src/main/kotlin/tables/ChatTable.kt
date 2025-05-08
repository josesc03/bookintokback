package tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ChatTable : Table("chat") {
    val id = integer("id").autoIncrement()
    val idUsuarioOfertante = integer("id_usuario_ofertante")
    val idUsuarioInteresado = varchar("id_usuario_interesado", 255)
    val idLibro = varchar("id_libro", 255)
    val fechaCreacion = datetime("fecha_creacion")

    override val primaryKey = PrimaryKey(id)
}