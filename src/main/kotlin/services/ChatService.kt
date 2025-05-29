package services

import dto.ChatItem
import models.EstadoIntercambio
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import tables.*
import java.time.Instant
import kotlin.math.absoluteValue

object ChatService {

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
                    esMio = row[LibroTable.uidUsuario] == uidUsuario,
                    ultimoMensaje = row[MensajeTable.contenido],
                    timestampUltimoMensaje = Instant.parse(row[MensajeTable.timestamp].toString())
                )
            }
        }
    }

}