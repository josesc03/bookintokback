import models.Usuario
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import tables.UsuarioTable
import java.time.Instant
import kotlin.time.ExperimentalTime

object UsuarioService {

    @OptIn(ExperimentalTime::class)
    fun createUser(uid: String, nickname: String, email: String): Usuario {
        println("Creando usuario $nickname $email")
        return transaction {
            UsuarioTable.insert {
                it[UsuarioTable.uid] = uid
                it[UsuarioTable.nickname] = nickname
                it[UsuarioTable.nombre] = nickname
                it[UsuarioTable.email] = email
                it[UsuarioTable.fechaRegistro] = Instant.now()
            } get UsuarioTable.uid

            Usuario(uid, nickname, nickname, email, null, null, null, null, Instant.now())

        }
    }


    fun updateUser(uid: String, updateData: Map<String, Any?>): Usuario? {
        return transaction {
            // Verificar que el usuario existe y pertenece al usuario autenticado
            UsuarioTable.select {
                (UsuarioTable.uid eq uid) and (UsuarioTable.uid eq uid)
            }.singleOrNull() ?: return@transaction null

            // Preparar la actualización
            val updateStatement = UsuarioTable.update({ UsuarioTable.uid eq uid }) { table ->
                updateData.forEach { (key, value) ->
                    when (key) {
                        "nickname" -> table[nickname] = value as String
                        "nombre" -> table[nombre] = value as String
                        "urlImage" -> table[imagenUrl] = value as? String
                    }
                }
            }

            if (updateStatement > 0) {
                getUserByUid(id)
            } else {
                null
            }
        }
    }

    fun updateUserLocationByUid(
        uid: String,
        latitud: Double,
        longitud: Double
    ): Boolean? {
        return transaction {
            val filaActualizada = UsuarioTable.update({ UsuarioTable.uid eq uid }) {
                it[UsuarioTable.ultimaLongitud] = longitud.toBigDecimal()
                it[UsuarioTable.ultimaLatitud] = latitud.toBigDecimal()
            }

            return@transaction if (filaActualizada > 0) true else null
        }
    }

    @OptIn(ExperimentalTime::class)
    fun getUserByUid(uid: String): Usuario? {
        println("Iniciando sesion con UID")
        return try {
            transaction {
                UsuarioTable.select { UsuarioTable.uid eq uid }
                    .singleOrNull()
                    ?.let { row ->
                        Usuario(
                            uid = row[UsuarioTable.uid],
                            nickname = row[UsuarioTable.nickname],
                            nombre = row[UsuarioTable.nombre],
                            email = row[UsuarioTable.email],
                            imagenUrl = row[UsuarioTable.imagenUrl],
                            ultimaLatitud = row[UsuarioTable.ultimaLatitud],
                            ultimaLongitud = row[UsuarioTable.ultimaLongitud],
                            valoracionPromedio = row[UsuarioTable.valoracionPromedio],
                            fechaRegistro = Instant.parse(row[UsuarioTable.fechaRegistro].toString())
                        )
                    }
            }
        } catch (e: Exception) {
            println("Error al buscar usuario por uid: ${e.message}")
            null
        }
    }

    fun getUserByEmail(email: String): Usuario? {
        return try {
            transaction {
                UsuarioTable.select { UsuarioTable.email eq email }
                    .singleOrNull()
                    ?.let { row ->
                        Usuario(
                            uid = row[UsuarioTable.uid],
                            nickname = row[UsuarioTable.nickname],
                            nombre = row[UsuarioTable.nombre],
                            email = row[UsuarioTable.email],
                            imagenUrl = row[UsuarioTable.imagenUrl],
                            ultimaLatitud = row[UsuarioTable.ultimaLatitud],
                            ultimaLongitud = row[UsuarioTable.ultimaLongitud],
                            valoracionPromedio = row[UsuarioTable.valoracionPromedio],
                            fechaRegistro = Instant.parse(row[UsuarioTable.fechaRegistro].toString())
                        )
                    }
            }
        } catch (e: Exception) {
            println("Error al buscar usuario por email: ${e.message}")
            null
        }
    }
}