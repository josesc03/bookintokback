package tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object UsuarioTable : Table("usuario") {
    val id = integer("id").autoIncrement()
    val firebaseUid = varchar("firebase_uid", 100)
    val nickname = varchar("nickname", 100).uniqueIndex()
    val nombre = varchar("nombre", 100)
    val email = varchar("email", 100).uniqueIndex()
    val ultimaLatitud = decimal("ultima_latitud", 9, 6).nullable()  // Cambiado de varchar a double
    val ultimaLongitud = decimal("ultima_longitud", 9, 6).nullable()  // Cambiado de varchar a double
    val valoracionPromedio = decimal("valoracion_promedio", 3, 2).nullable()  // Cambiado de varchar a double
    val imagenUrl = varchar("imagen_url", 255).nullable()
    val fechaRegistro = datetime("fecha_registro")

    override val primaryKey = PrimaryKey(id, name = "PK_Id")
}