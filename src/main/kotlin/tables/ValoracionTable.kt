package tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object ValoracionTable : Table("valoracion") {
    val id = integer("id").autoIncrement()
    val uidUsuarioValorado = varchar("uid_usuario_valorado", 255).references(UsuarioTable.uid)
    val uidUsuarioQueValora = varchar("uid_usuario_que_valora", 255).references(UsuarioTable.uid)
    val puntuacion = integer("puntuacion")
    val comentario = text("comentario").nullable()
    val fechaValoracion = timestamp("fecha_valoracion").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("indice_valoracion", uidUsuarioValorado, uidUsuarioQueValora)
    }
}
