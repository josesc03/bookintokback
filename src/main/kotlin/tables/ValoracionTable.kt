package tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ValoracionTable : Table("valoracion") {
    val id = integer("id").autoIncrement()
    val idUsuarioValorado = integer("id_usuario_valorado").references(UsuarioTable.id)
    val idUsuarioQueValora = integer("id_usuario_que_valora").references(UsuarioTable.id)
    val puntuacion = integer("puntuacion")
    val comentario = text("comentario").nullable()
    val fechaValoracion = datetime("fecha_valoracion")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("indice_valoracion", idUsuarioValorado, idUsuarioQueValora)
    }
}
