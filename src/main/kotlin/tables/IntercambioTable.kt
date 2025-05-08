package tables

import models.EstadoIntercambio
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object IntercambioTable : Table("intercambio") {
    val id = integer("id").autoIncrement()
    val idChat = integer("id_chat").references(ChatTable.id)
    val estado = enumerationByName("estado", 20, EstadoIntercambio::class)
    val fechaCreacion = datetime("fecha_creacion")

    override val primaryKey = PrimaryKey(id)
}

