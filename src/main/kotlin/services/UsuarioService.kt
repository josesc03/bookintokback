import models.Usuario
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import tables.UsuarioTable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object UsuarioService {

    @OptIn(ExperimentalTime::class)
    fun createUser(firebaseUid: String, nickname: String, nombre: String, email: String): Usuario {
        return transaction {
            val id = UsuarioTable.insert {
                it[UsuarioTable.firebaseUid] = firebaseUid
                it[UsuarioTable.nickname] = nickname
                it[UsuarioTable.nombre] = nombre
                it[UsuarioTable.email] = email
                it[UsuarioTable.fechaRegistro] = java.time.LocalDateTime.now()
            } get UsuarioTable.id

            getUserById(id)!!
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

    fun updateUserLocation(id: Int, firebaseUid: String, latitud: Double, longitud: Double): Usuario? {
        return transaction {
            // Verificar que el usuario existe y pertenece al usuario autenticado
            UsuarioTable.select {
                (UsuarioTable.id eq id) and (UsuarioTable.firebaseUid eq firebaseUid)
            }.singleOrNull() ?: return@transaction null

            // Actualizar ubicación
            val updateStatement = UsuarioTable.update({ UsuarioTable.id eq id }) { table ->
                table[ultimaLatitud] = latitud.toBigDecimal()
                table[ultimaLongitud] = longitud.toBigDecimal()
            }

            if (updateStatement > 0) {
                getUserById(id)
            } else {
                null
            }
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
        return transaction {
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
    }

}