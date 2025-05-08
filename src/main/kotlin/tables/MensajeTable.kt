package tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object MensajeTable : Table("mensaje") {
    val id = integer("id").autoIncrement()
    val idChat = integer("id_chat").references(ChatTable.id)
    val idRemitente = integer("id_remitente")
    val contenido = text("contenido")
    val timestamp = datetime("timestamp")

    override val primaryKey = PrimaryKey(id)
}