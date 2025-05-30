package tables

import org.jetbrains.exposed.sql.Table

object ChatTable : Table("chat") {
    val id = integer("id").autoIncrement()
    val uidUsuarioOfertante = varchar("uid_usuario_ofertante", 255)
    val uidUsuarioInteresado = varchar("uid_usuario_interesado", 255)
    val idLibro = integer("id_libro")

    override val primaryKey = PrimaryKey(id)
}