package tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import kotlin.time.ExperimentalTime

object MensajeTable : Table("mensaje") {
    val id = integer("id").autoIncrement()
    val idChat = integer("id_chat").references(ChatTable.id)
    val uidRemitente = varchar("uid_remitente", 255)
    val contenido = text("contenido")

    @OptIn(ExperimentalTime::class)
    val timestamp = timestamp("timestamp").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}