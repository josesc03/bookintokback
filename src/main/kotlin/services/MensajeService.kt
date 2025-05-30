package services

import dto.MessageInfo
import exceptions.UnauthorizedException
import models.EstadoIntercambio
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
    fun sendMessage(
        chatId: Int,
        uidRemitente: String,
        contenido: String
    ) {
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

            MensajeTable.insert {
                it[MensajeTable.idChat] = chatId
                it[MensajeTable.uidRemitente] = uidRemitente
                it[MensajeTable.contenido] = contenido
            } get MensajeTable.id
        }
    }

    @OptIn(ExperimentalTime::class)
    fun getMensajes(chatId: Int): List<MessageInfo> {
        return transaction {
            val chatExiste = ChatTable
                .select {
                    (ChatTable.id eq chatId)
                }
                .count() > 0

            if (!chatExiste) {
                throw UnauthorizedException("No tienes permiso para ver los mensajes de este chat")
            }

            MensajeTable
                .select { MensajeTable.idChat eq chatId }
                .orderBy(MensajeTable.timestamp)
                .map { row ->
                    MessageInfo(
                        id_usuario_emisor = row[MensajeTable.uidRemitente],
                        contenido = row[MensajeTable.contenido],
                        timestamp = Instant.parse(row[MensajeTable.timestamp].toString())
                    )
                }
        }
    }
}
