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
    fun createUser(firebaseUid: String, nickname: String, email: String): Usuario {
        println("Creando usuario $nickname $email")
        return transaction {
            val id = UsuarioTable.insert {
                it[UsuarioTable.firebaseUid] = firebaseUid
                it[UsuarioTable.nickname] = nickname
                it[UsuarioTable.nombre] = nickname
                it[UsuarioTable.email] = email
                it[UsuarioTable.fechaRegistro] = Instant.now()
            } get UsuarioTable.id

            Usuario(id, firebaseUid, nickname, nickname, email, null, null, null, null, Instant.now())

        }
    }


    fun updateUser(id: Int, firebaseUid: String, updateData: Map<String, Any?>): Usuario? {
        return transaction {
            // Verificar que el usuario existe y pertenece al usuario autenticado
            UsuarioTable.select {
                (UsuarioTable.id eq id) and (UsuarioTable.firebaseUid eq firebaseUid)
            }.singleOrNull() ?: return@transaction null

            // Preparar la actualización
            val updateStatement = UsuarioTable.update({ UsuarioTable.id eq id }) { table ->
                updateData.forEach { (key, value) ->
                    when (key) {
                        "nickname" -> table[nickname] = value as String
                        "nombre" -> table[nombre] = value as String
                        "urlImage" -> table[imagenUrl] = value as? String
                    }
                }
            }

            if (updateStatement > 0) {
                getUserById(id)
            } else {
                null
            }
        }
    }

    fun updateUserLocationByUid(
        firebaseUid: String,
        latitud: Double,
        longitud: Double
    ): Boolean? {
        return transaction {
            val filaActualizada = UsuarioTable.update({ UsuarioTable.firebaseUid eq firebaseUid }) {
                it[UsuarioTable.ultimaLongitud] = longitud.toBigDecimal()
                it[UsuarioTable.ultimaLatitud] = latitud.toBigDecimal()
            }

            return@transaction if (filaActualizada > 0) true else null
        }
    }

    // Asegúrate de actualizar también la función getUserById para incluir el nuevo campo
    @OptIn(ExperimentalTime::class)
    fun getUserById(id: Int): Usuario? {
        return transaction {
            UsuarioTable.select { UsuarioTable.id eq id }
                .singleOrNull()
                ?.let { row ->
                    Usuario(
                        id = row[UsuarioTable.id],
                        firebaseUid = row[UsuarioTable.firebaseUid],
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
    }

    @OptIn(ExperimentalTime::class)
    fun getUserByFirebaseUid(firebaseUid: String): Usuario? {
        println("Iniciando sesion con UID")
        return try {
            transaction {
                UsuarioTable.select { UsuarioTable.firebaseUid eq firebaseUid }
                    .singleOrNull()
                    ?.let { row ->
                        Usuario(
                            id = row[UsuarioTable.id],
                            firebaseUid = row[UsuarioTable.firebaseUid],
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
            println("Error al buscar usuario por firebaseUid: ${e.message}")
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
                            id = row[UsuarioTable.id],
                            firebaseUid = row[UsuarioTable.firebaseUid],
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