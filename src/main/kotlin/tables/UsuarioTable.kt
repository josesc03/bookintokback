package tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import kotlin.time.ExperimentalTime

object UsuarioTable : Table("usuario") {
    val uid = varchar("uid", 100)
    val nickname = varchar("nickname", 100).uniqueIndex()
    val nombre = varchar("nombre", 100)
    val email = varchar("email", 100).uniqueIndex()
    val ultimaLatitud = decimal("ultima_latitud", 9, 6).nullable()
    val ultimaLongitud = decimal("ultima_longitud", 9, 6).nullable()
    val valoracionPromedio = decimal("valoracion_promedio", 3, 2).nullable()
    val imagenUrl = varchar("imagen_url", 255).nullable()

    @OptIn(ExperimentalTime::class)
    val fechaRegistro = timestamp("fecha_registro").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(uid, name = "PK_Id")
}