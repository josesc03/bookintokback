package services

import dto.ChatInfo
import dto.ChatItem
import models.EstadoIntercambio
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import tables.*
import java.time.Instant
import kotlin.math.absoluteValue

object ChatService {

    fun getChatInfo(chatId: Int, uidActual: String): ChatInfo {
        val ofertanteAlias = UsuarioTable.alias("ofertante")
        val interesadoAlias = UsuarioTable.alias("interesado")

        return transaction {
            val result = ChatTable
                .join(LibroTable, JoinType.INNER, ChatTable.idLibro, LibroTable.id)
                .join(ofertanteAlias, JoinType.INNER, ChatTable.uidUsuarioOfertante, ofertanteAlias[UsuarioTable.uid])
                .join(
                    interesadoAlias,
                    JoinType.INNER,
                    ChatTable.uidUsuarioInteresado,
                    interesadoAlias[UsuarioTable.uid]
                )
                .join(IntercambioTable, JoinType.INNER, ChatTable.id, IntercambioTable.idChat)
                .select { ChatTable.id eq chatId }
                .singleOrNull() ?: throw NoSuchElementException("No se encontró un chat con ID $chatId")

            val uidOfertante = result[ofertanteAlias[UsuarioTable.uid]]
            val uidInteresado = result[interesadoAlias[UsuarioTable.uid]]

            val nombreOtroUsuario = if (uidOfertante == uidActual) {
                result[interesadoAlias[UsuarioTable.nombre]]
            } else {
                result[ofertanteAlias[UsuarioTable.nombre]]
            }

            val uidOtroUsuario = if (uidOfertante == uidActual) {
                uidInteresado
            } else {
                uidOfertante
            }

            ChatInfo(
                tituloLibro = result[LibroTable.titulo],
                imagenLibroUrl = result[LibroTable.imagenUrl],
                nombreUsuario = nombreOtroUsuario,
                uidUsuario = uidOtroUsuario
            )
        }
    }


    fun obtenerChatsActivos(uidUsuario: String): List<ChatItem> {
        return transaction {
            // Subconsulta para obtener el último mensaje de cada chat
            val ultimoMensajesPorChat = MensajeTable
                .slice(MensajeTable.idChat, MensajeTable.timestamp.max().alias("max_timestamp"))
                .selectAll()
                .groupBy(MensajeTable.idChat)
                .alias("ultimo_mensajes")

            // Join completo con todas las tablas necesarias
            val query = MensajeTable
                .join(ultimoMensajesPorChat, JoinType.INNER, additionalConstraint = {
                    (MensajeTable.idChat eq ultimoMensajesPorChat[MensajeTable.idChat]) and
                            (MensajeTable.timestamp eq ultimoMensajesPorChat[MensajeTable.timestamp.max()])
                })
                .join(ChatTable, JoinType.INNER, onColumn = MensajeTable.idChat, otherColumn = ChatTable.id)
                .join(LibroTable, JoinType.INNER, onColumn = ChatTable.idLibro, otherColumn = LibroTable.id)
                .join(IntercambioTable, JoinType.INNER, onColumn = ChatTable.id, otherColumn = IntercambioTable.idChat)
                .join(UsuarioTable, JoinType.INNER, additionalConstraint = {
                    ((UsuarioTable.uid eq ChatTable.uidUsuarioOfertante) and (ChatTable.uidUsuarioOfertante neq uidUsuario)) or
                            ((UsuarioTable.uid eq ChatTable.uidUsuarioInteresado) and (ChatTable.uidUsuarioInteresado neq uidUsuario))
                })
                .select {
                    ((ChatTable.uidUsuarioOfertante eq uidUsuario) or (ChatTable.uidUsuarioInteresado eq uidUsuario)) and
                            ((IntercambioTable.estado eq EstadoIntercambio.ACEPTADO) or (IntercambioTable.estado eq EstadoIntercambio.PENDIENTE))
                }

            // Mapeo del resultado a ChatItem
            query.map { row ->
                ChatItem(
                    chatId = row[ChatTable.id].absoluteValue,
                    tituloLibro = row[LibroTable.titulo],
                    imagenLibroUrl = row[LibroTable.imagenUrl],
                    nombreUsuario = row[UsuarioTable.nombre],
                    esMio = row[MensajeTable.uidRemitente] == uidUsuario,
                    ultimoMensaje = row[MensajeTable.contenido],
                    timestampUltimoMensaje = Instant.parse(row[MensajeTable.timestamp].toString())
                )
            }
        }
    }

    fun cancelChat(idChat: Int) {
        transaction {
            IntercambioTable.update({ IntercambioTable.idChat eq idChat }) {
                it[estado] = EstadoIntercambio.CANCELADO
            }
        }
    }

    fun createChat(_idLibro: Int, uidSolicitante: String): Pair<Boolean, Int> {
        return transaction {
            val uidUsuarioLibro = LibroTable
                .select { LibroTable.id eq _idLibro }
                .map { it[LibroTable.uidUsuario] }
                .single()

            // Buscar si ya existe un intercambio activo (PENDIENTE o ACEPTADO)
            val chatExistente = (ChatTable innerJoin IntercambioTable)
                .select {
                    ChatTable.idLibro eq _idLibro and
                            (ChatTable.uidUsuarioOfertante eq uidUsuarioLibro) and
                            (ChatTable.uidUsuarioInteresado eq uidSolicitante) and
                            (IntercambioTable.estado inList listOf(
                                EstadoIntercambio.PENDIENTE,
                                EstadoIntercambio.ACEPTADO
                            ))
                }
                .firstOrNull()

            if (chatExistente != null) {
                val idChatExistente = chatExistente[ChatTable.id]
                return@transaction Pair(false, idChatExistente)
            }

            val nuevoChatId = ChatTable.insert {
                it[idLibro] = _idLibro
                it[uidUsuarioOfertante] = uidUsuarioLibro
                it[uidUsuarioInteresado] = uidSolicitante
            } get ChatTable.id

            IntercambioTable.insert {
                it[this.idChat] = nuevoChatId
                it[estado] = EstadoIntercambio.PENDIENTE
            }

            val nombreSolicitante = UsuarioTable
                .select { UsuarioTable.uid eq uidSolicitante }
                .map { it[UsuarioTable.nombre] }
                .single()

            MensajeTable.insert {
                it[this.idChat] = nuevoChatId
                it[uidRemitente] = uidSolicitante
                it[contenido] = "¡$nombreSolicitante ha iniciado esta conversación para intercambiar contigo!"
                it[timestamp] = Instant.now()
            }

            Pair(true, nuevoChatId.absoluteValue)
        }
    }

    fun getUserIdsInChat(idChat: Int): List<String> = transaction {
        val chat = ChatTable.select { ChatTable.id eq idChat }.singleOrNull()
        if (chat != null) {
            listOf(
                chat[ChatTable.uidUsuarioOfertante],
                chat[ChatTable.uidUsuarioInteresado]
            )
        } else {
            emptyList()
        }
    }

    fun hasUserConfirmedExchange(idChat: Int, uidUsuario: String): Boolean {
        return transaction {
            val chat = ChatTable
                .select { ChatTable.id eq idChat }
                .single()

            val esInteresado = chat[ChatTable.uidUsuarioInteresado] == uidUsuario

            IntercambioTable.select { IntercambioTable.idChat eq idChat }
                .map { if (esInteresado) it[IntercambioTable.confirmadoInteresado] else it[IntercambioTable.confirmadoOfertante] }
                .single()
        }
    }

    fun getEstadoIntercambio(idChat: Int): EstadoIntercambio {
        return transaction {
            val estadoIntercambio = IntercambioTable
                .select {
                    IntercambioTable.idChat eq idChat
                }
                .map { it[IntercambioTable.estado] }
                .firstOrNull() ?: EstadoIntercambio.CANCELADO

            estadoIntercambio
        }
    }

    fun confirmarIntercambio(idChat: Int, uidUsuario: String) = transaction {
        val chat = ChatTable
            .select { ChatTable.id eq idChat }
            .single()

        val esInteresado = chat[ChatTable.uidUsuarioInteresado] == uidUsuario

        val intercambio = IntercambioTable
            .select { IntercambioTable.idChat eq idChat }
            .single()

        val confirmadoInteresadoActual = intercambio[IntercambioTable.confirmadoInteresado]
        val confirmadoOfertanteActual = intercambio[IntercambioTable.confirmadoOfertante]

        val confirmadoInteresadoNuevo = if (esInteresado) true else confirmadoInteresadoActual
        val confirmadoOfertanteNuevo = if (!esInteresado) true else confirmadoOfertanteActual

        IntercambioTable.update({ IntercambioTable.idChat eq idChat }) {
            it[confirmadoInteresado] = confirmadoInteresadoNuevo
            it[confirmadoOfertante] = confirmadoOfertanteNuevo

            it[estado] = if (confirmadoInteresadoNuevo && confirmadoOfertanteNuevo) {
                EstadoIntercambio.COMPLETADO
            } else {
                EstadoIntercambio.ACEPTADO
            }
        }
    }


}