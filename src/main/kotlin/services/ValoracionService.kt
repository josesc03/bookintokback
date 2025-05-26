package services

import exceptions.UnauthorizedException
import exceptions.ValidationException
import models.EstadoIntercambio
import models.Valoracion
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import tables.ChatTable
import tables.IntercambioTable
import tables.ValoracionTable
import java.time.Instant
import java.time.LocalDateTime
import kotlin.time.ExperimentalTime

object ValoracionService {
    @OptIn(ExperimentalTime::class)
    fun crearValoracion(
        uidUsuarioValorado: String,
        uidUsuarioQueValora: String,
        puntuacion: Int,
        comentario: String?
    ): Valoracion {
        return transaction {
            // Verificar que existe un intercambio completado entre los usuarios
            val intercambioCompletado = (IntercambioTable
                .join(ChatTable, JoinType.INNER, IntercambioTable.idChat, ChatTable.id)
                .select {
                    IntercambioTable.estado.eq(EstadoIntercambio.COMPLETADO) and (
                            (ChatTable.uidUsuarioOfertante.eq(uidUsuarioValorado) and
                                    ChatTable.uidUsuarioInteresado.eq(uidUsuarioQueValora.toString())) or
                                    (ChatTable.uidUsuarioOfertante.eq(uidUsuarioQueValora) and
                                            ChatTable.uidUsuarioInteresado.eq(uidUsuarioValorado.toString()))
                            )
                })
                .count() > 0

            if (!intercambioCompletado) {
                throw UnauthorizedException("No puedes valorar a este usuario sin haber completado un intercambio")
            }

            // Verificar que no existe una valoración previa
            val valoracionPrevia = ValoracionTable
                .select {
                    (ValoracionTable.uidUsuarioValorado eq uidUsuarioValorado) and
                            (ValoracionTable.uidUsuarioQueValora eq uidUsuarioQueValora)
                }
                .count() > 0

            if (valoracionPrevia) {
                throw ValidationException("Ya has valorado a este usuario")
            }

            // Validar puntuación
            if (puntuacion !in 1..5) {
                throw ValidationException("La puntuación debe estar entre 1 y 5")
            }

            val id = ValoracionTable.insert {
                it[ValoracionTable.uidUsuarioValorado] = uidUsuarioValorado
                it[ValoracionTable.uidUsuarioQueValora] = uidUsuarioQueValora
                it[ValoracionTable.puntuacion] = puntuacion
                it[ValoracionTable.comentario] = comentario
                it[fechaValoracion] = LocalDateTime.now()
            } get ValoracionTable.id

            ValoracionTable
                .select { ValoracionTable.id eq id }
                .map { row ->
                    Valoracion(
                        id = row[ValoracionTable.id],
                        uidUsuarioValorado = row[ValoracionTable.uidUsuarioValorado],
                        uidUsuarioQueValora = row[ValoracionTable.uidUsuarioQueValora],
                        puntuacion = row[ValoracionTable.puntuacion],
                        comentario = row[ValoracionTable.comentario],
                        fechaValoracion = Instant.parse(row[ValoracionTable.fechaValoracion].toString())
                    )
                }
                .single()
        }
    }

    @OptIn(ExperimentalTime::class)
    fun getValoracionesDeUsuario(uidUsuario: String): List<Valoracion> {
        return transaction {
            ValoracionTable
                .select { ValoracionTable.uidUsuarioValorado eq uidUsuario }
                .map { row ->
                    Valoracion(
                        id = row[ValoracionTable.id],
                        uidUsuarioValorado = row[ValoracionTable.uidUsuarioValorado],
                        uidUsuarioQueValora = row[ValoracionTable.uidUsuarioQueValora],
                        puntuacion = row[ValoracionTable.puntuacion],
                        comentario = row[ValoracionTable.comentario],
                        fechaValoracion = Instant.parse(row[ValoracionTable.fechaValoracion].toString())
                    )
                }
        }
    }

    fun hasRatedUser(
        uidUsuarioQueValora: String,
        uidUsuarioValorado: String
    ): Boolean {
        return ValoracionTable
            .select {
                (ValoracionTable.uidUsuarioValorado eq uidUsuarioValorado) and
                        (ValoracionTable.uidUsuarioQueValora eq uidUsuarioQueValora)
            }
            .count() > 0
    }
}