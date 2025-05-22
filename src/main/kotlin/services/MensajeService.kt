package services

import exceptions.UnauthorizedException
import io.ktor.server.plugins.*
import models.EstadoIntercambio
import models.Mensaje
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import tables.ChatTable
import tables.IntercambioTable
import tables.MensajeTable
import java.time.Instant
import kotlin.time.ExperimentalTime

object MensajeService {
    fun enviarMensaje(
        chatId: Int,
        uidRemitente: String,
        contenido: String
    ): Mensaje {
        return transaction {
            val chatExiste = ChatTable
                .select {
                    (ChatTable.id eq chatId) and
                            ((ChatTable.uidUsuarioOfertante eq uidRemitente) or
                                    (ChatTable.uidUsuarioInteresado eq uidRemitente))
                }
                .count() > 0

            if (!chatExiste) {
                throw UnauthorizedException("No tienes permiso para enviar mensajes en este chat")
            }

            val intercambioActivo = IntercambioTable
                .select {
                    (IntercambioTable.idChat eq chatId) and
                            (IntercambioTable.estado inList listOf(
                                EstadoIntercambio.PENDIENTE,
                                EstadoIntercambio.ACEPTADO
                            ))
                }
                .count() > 0

            if (!intercambioActivo) {
                throw IllegalStateException("No se pueden enviar mensajes en un chat inactivo")
            }

            val idMensaje = MensajeTable.insert {
                it[MensajeTable.idChat] = chatId
                it[MensajeTable.uidRemitente] = uidRemitente
                it[MensajeTable.contenido] = contenido
            } get MensajeTable.id

            getMensajeById(idMensaje)
                ?: throw NotFoundException("Error al crear el mensaje")
        }
    }

    @OptIn(ExperimentalTime::class)
    fun getMensajes(chatId: Int, uidUsuario: String): List<Mensaje> {
        return transaction {
            val chatExiste = ChatTable
                .select {
                    (ChatTable.id eq chatId) and
                            ((ChatTable.uidUsuarioOfertante eq uidUsuario) or
                                    (ChatTable.uidUsuarioInteresado eq uidUsuario.toString()))
                }
                .count() > 0

            if (!chatExiste) {
                throw UnauthorizedException("No tienes permiso para ver los mensajes de este chat")
            }

            MensajeTable
                .select { MensajeTable.idChat eq chatId }
                .orderBy(MensajeTable.timestamp)
                .map { row ->
                    Mensaje(
                        id = row[MensajeTable.id],
                        idChat = row[MensajeTable.idChat],
                        idRemitente = row[MensajeTable.uidRemitente],
                        contenido = row[MensajeTable.contenido],
                        timestamp = Instant.parse(row[MensajeTable.timestamp].toString())
                    )
                }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun getMensajeById(id: Int): Mensaje? {
        return transaction {
            MensajeTable
                .select { MensajeTable.id eq id }
                .map { row ->
                    Mensaje(
                        id = row[MensajeTable.id],
                        idChat = row[MensajeTable.idChat],
                        idRemitente = row[MensajeTable.uidRemitente],
                        contenido = row[MensajeTable.contenido],
                        timestamp = Instant.parse(row[MensajeTable.timestamp].toString())
                    )
                }
                .singleOrNull()
        }
    }
}
