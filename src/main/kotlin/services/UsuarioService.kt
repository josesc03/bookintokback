import dto.ValoracionesResponse
import models.EstadoIntercambio
import models.Usuario
import models.Valoracion
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import tables.ChatTable
import tables.IntercambioTable
import tables.UsuarioTable
import tables.ValoracionTable
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


    fun updateUser(uid: String, imagenUrl: String?, nombre: String?): Usuario? {
        return transaction {
            UsuarioTable.select {
                (UsuarioTable.uid eq uid) and (UsuarioTable.uid eq uid)
            }.singleOrNull() ?: return@transaction null

            val updateStatement = UsuarioTable.update({ UsuarioTable.uid eq uid }) { table ->
                if (nombre != null) {
                    table[UsuarioTable.nombre] = nombre
                }
                table[UsuarioTable.imagenUrl] = imagenUrl
            }

            if (updateStatement > 0) {
                getUserByUid(uid)
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
    fun getUserByUid(uid: String?): Usuario? {
        println("Iniciando sesion con UID")
        return try {
            transaction {
                UsuarioTable.select { UsuarioTable.uid eq uid.toString() }
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

    fun hasCompletedExchange(uid: String, uid2: String): Boolean {
        return transaction {
            ChatTable
                .innerJoin(IntercambioTable)
                .select {
                    (((ChatTable.uidUsuarioOfertante eq uid) and (ChatTable.uidUsuarioInteresado eq uid2)) or
                            ((ChatTable.uidUsuarioOfertante eq uid2) and (ChatTable.uidUsuarioInteresado eq uid))) and
                            (IntercambioTable.idChat eq ChatTable.id) and
                            (IntercambioTable.estado eq EstadoIntercambio.COMPLETADO)
                }
                .count() > 0
        }
    }

    fun hasRated(uid: String, uid_valorado: String): Boolean {
        return transaction {
            ValoracionTable
                .select {
                    (ValoracionTable.uidUsuarioQueValora eq uid) and
                            (ValoracionTable.uidUsuarioValorado eq uid_valorado)
                }
                .count() > 0
        }
    }

    fun valorarUsuario(uid: String, uid_valorado: String, comentario: String?, puntuacion: Int): Boolean {
        return transaction {
            if (hasCompletedExchange(uid, uid_valorado) && !hasRated(uid, uid_valorado)) {
                ValoracionTable.insert {
                    it[uidUsuarioQueValora] = uid
                    it[uidUsuarioValorado] = uid_valorado
                    it[ValoracionTable.puntuacion] = puntuacion
                    it[ValoracionTable.comentario] = comentario
                }

                val promedio = ValoracionTable
                    .select { ValoracionTable.uidUsuarioValorado eq uid_valorado }
                    .count()
                    .toBigDecimal()

                UsuarioTable.update({ UsuarioTable.uid eq uid_valorado }) {
                    it[valoracionPromedio] = promedio
                }

                true
            } else {
                false
            }
        }
    }

    fun getValoracionesFromUid(uid: String): ValoracionesResponse {
        return transaction {
            val valoraciones = ValoracionTable
                .select { ValoracionTable.uidUsuarioValorado eq uid }
                .map { row ->
                    Valoracion(
                        id = row[ValoracionTable.id],
                        uidUsuarioQueValora = row[ValoracionTable.uidUsuarioQueValora],
                        uidUsuarioValorado = row[ValoracionTable.uidUsuarioValorado],
                        comentario = row[ValoracionTable.comentario],
                        puntuacion = row[ValoracionTable.puntuacion],
                        fechaValoracion = Instant.parse(row[ValoracionTable.fechaValoracion].toString()),
                    )
                }
            ValoracionesResponse(
                status = "success",
                valoraciones = valoraciones
            )
        }
    }
}