package services

import dto.ChatInfo
import models.Chat
import models.EstadoIntercambio
import models.Intercambio
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import tables.ChatTable
import tables.IntercambioTable
import tables.LibroTable
import java.time.LocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object ChatIntercambioService {
    fun crearChatEIntercambio(
        idUsuarioOfertante: Int,
        idUsuarioInteresado: String,
        idLibro: String
    ): Pair<Chat, Intercambio> {
        return transaction {
            val ahora = LocalDateTime.now()

            val idChat = ChatTable.insert {
                it[ChatTable.idUsuarioOfertante] = idUsuarioOfertante
                it[ChatTable.idUsuarioInteresado] = idUsuarioInteresado
                it[ChatTable.idLibro] = idLibro
                it[fechaCreacion] = ahora
            } get ChatTable.id

            val idIntercambio = IntercambioTable.insert {
                it[IntercambioTable.idChat] = idChat
                it[estado] = EstadoIntercambio.PENDIENTE
                it[fechaCreacion] = ahora
            } get IntercambioTable.id

            Pair(
                obtenerChatPorId(idChat),
                obtenerIntercambioPorId(idIntercambio)
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    fun obtenerChatsActivos(idUsuario: String): Map<Int, ChatInfo> {
        return transaction {
            (ChatTable innerJoin IntercambioTable)
                .select {
                    ((ChatTable.idUsuarioOfertante eq idUsuario.toInt()) or
                            (ChatTable.idUsuarioInteresado eq idUsuario)) and
                            (IntercambioTable.estado inList listOf(
                                EstadoIntercambio.PENDIENTE,
                                EstadoIntercambio.ACEPTADO
                            ))
                }
                .associate { row ->
                    val chat = Chat(
                        id = row[ChatTable.id],
                        idUsuarioOfertante = row[ChatTable.idUsuarioOfertante],
                        idUsuarioInteresado = row[ChatTable.idUsuarioInteresado],
                        idLibro = row[ChatTable.idLibro],
                        fechaCreacion = Instant.parse(row[LibroTable.fechaPublicacion].toString())
                    )

                    val intercambio = Intercambio(
                        id = row[IntercambioTable.id],
                        idChat = row[IntercambioTable.idChat],
                        estado = row[IntercambioTable.estado],
                        fechaCreacion = Instant.parse(row[LibroTable.fechaPublicacion].toString())
                    )

                    chat.id to ChatInfo(chat, intercambio)
                }
        }
    }

    fun obtenerEstadoIntercambio(chatId: Int): EstadoIntercambio {
        return transaction {
            IntercambioTable
                .select { IntercambioTable.idChat eq chatId }
                .map { it[IntercambioTable.estado] }
                .firstOrNull()
                ?: throw IllegalStateException("Intercambio no encontrado para el chat $chatId")
        }
    }

    fun actualizarEstadoIntercambio(
        chatId: Int,
        nuevoEstado: EstadoIntercambio,
        idUsuario: String
    ): EstadoIntercambio {
        return transaction {
            val chatExiste = ChatTable
                .select {
                    (ChatTable.id eq chatId) and
                            ((ChatTable.idUsuarioOfertante eq idUsuario.toInt()) or
                                    (ChatTable.idUsuarioInteresado eq idUsuario))
                }
                .count() > 0

            if (!chatExiste) {
                throw IllegalArgumentException("No tienes permiso para modificar este intercambio")
            }

            val estadoActual = obtenerEstadoIntercambio(chatId)
            validarTransicionEstado(estadoActual, nuevoEstado)

            IntercambioTable.update({ IntercambioTable.idChat eq chatId }) {
                it[estado] = nuevoEstado
            }

            nuevoEstado
        }
    }

    private fun validarTransicionEstado(
        estadoActual: EstadoIntercambio,
        nuevoEstado: EstadoIntercambio
    ) {
        val transicionesPermitidas = when (estadoActual) {
            EstadoIntercambio.PENDIENTE -> setOf(
                EstadoIntercambio.ACEPTADO,
                EstadoIntercambio.CANCELADO
            )

            EstadoIntercambio.ACEPTADO -> setOf(
                EstadoIntercambio.COMPLETADO,
                EstadoIntercambio.CANCELADO
            )

            EstadoIntercambio.COMPLETADO -> setOf()
            EstadoIntercambio.CANCELADO -> setOf()
        }

        if (nuevoEstado !in transicionesPermitidas) {
            throw IllegalStateException("Transición de estado no permitida: $estadoActual -> $nuevoEstado")
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun obtenerChatPorId(id: Int): Chat {
        return ChatTable
            .select { ChatTable.id eq id }
            .map { row ->
                Chat(
                    id = row[ChatTable.id],
                    idUsuarioOfertante = row[ChatTable.idUsuarioOfertante],
                    idUsuarioInteresado = row[ChatTable.idUsuarioInteresado],
                    idLibro = row[ChatTable.idLibro],
                    fechaCreacion = Instant.parse(row[LibroTable.fechaPublicacion].toString())
                )
            }
            .firstOrNull()
            ?: throw IllegalStateException("Chat no encontrado")
    }

    @OptIn(ExperimentalTime::class)
    private fun obtenerIntercambioPorId(id: Int): Intercambio {
        return IntercambioTable
            .select { IntercambioTable.id eq id }
            .map { row ->
                Intercambio(
                    id = row[IntercambioTable.id],
                    idChat = row[IntercambioTable.idChat],
                    estado = row[IntercambioTable.estado],
                    fechaCreacion = Instant.parse(row[LibroTable.fechaPublicacion].toString())

                )
            }
            .firstOrNull()
            ?: throw IllegalStateException("Intercambio no encontrado")
    }
}
