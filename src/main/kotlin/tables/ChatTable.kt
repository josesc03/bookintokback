package tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ChatTable : Table("chat") {
    val id = integer("id").autoIncrement()
    val uidUsuarioOfertante = varchar("id_usuario_ofertante", 255)
    val uidUsuarioInteresado = varchar("id_usuario_interesado", 255)
    val idLibro = varchar("id_libro", 255)
    val fechaCreacion = datetime("fecha_creacion")

    override val primaryKey = PrimaryKey(id)
}