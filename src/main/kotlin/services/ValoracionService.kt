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
import java.time.LocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object ValoracionService {
    @OptIn(ExperimentalTime::class)
    fun crearValoracion(
        idUsuarioValorado: Int,
        idUsuarioQueValora: Int,
        puntuacion: Int,
        comentario: String?
    ): Valoracion {
        return transaction {
            // Verificar que existe un intercambio completado entre los usuarios
            val intercambioCompletado = (IntercambioTable
                .join(ChatTable, JoinType.INNER, IntercambioTable.idChat, ChatTable.id)
                .select {
                    IntercambioTable.estado.eq(EstadoIntercambio.COMPLETADO) and (
                            (ChatTable.idUsuarioOfertante.eq(idUsuarioValorado) and
                                    ChatTable.idUsuarioInteresado.eq(idUsuarioQueValora.toString())) or
                                    (ChatTable.idUsuarioOfertante.eq(idUsuarioQueValora) and
                                            ChatTable.idUsuarioInteresado.eq(idUsuarioValorado.toString()))
                            )
                })
                .count() > 0

            if (!intercambioCompletado) {
                throw UnauthorizedException("No puedes valorar a este usuario sin haber completado un intercambio")
            }

            // Verificar que no existe una valoración previa
            val valoracionPrevia = ValoracionTable
                .select {
                    (ValoracionTable.idUsuarioValorado eq idUsuarioValorado) and
                            (ValoracionTable.idUsuarioQueValora eq idUsuarioQueValora)
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
                it[ValoracionTable.idUsuarioValorado] = idUsuarioValorado
                it[ValoracionTable.idUsuarioQueValora] = idUsuarioQueValora
                it[ValoracionTable.puntuacion] = puntuacion
                it[ValoracionTable.comentario] = comentario
                it[fechaValoracion] = LocalDateTime.now()
            } get ValoracionTable.id

            ValoracionTable
                .select { ValoracionTable.id eq id }
                .map { row ->
                    Valoracion(
                        id = row[ValoracionTable.id],
                        idUsuarioValorado = row[ValoracionTable.idUsuarioValorado],
                        idUsuarioQueValora = row[ValoracionTable.idUsuarioQueValora],
                        puntuacion = row[ValoracionTable.puntuacion],
                        comentario = row[ValoracionTable.comentario],
                        fechaValoracion = Instant.parse(row[ValoracionTable.fechaValoracion].toString())
                    )
                }
                .single()
        }
    }

    @OptIn(ExperimentalTime::class)
    fun getValoracionesDeUsuario(idUsuario: Int): List<Valoracion> {
        return transaction {
            ValoracionTable
                .select { ValoracionTable.idUsuarioValorado eq idUsuario }
                .map { row ->
                    Valoracion(
                        id = row[ValoracionTable.id],
                        idUsuarioValorado = row[ValoracionTable.idUsuarioValorado],
                        idUsuarioQueValora = row[ValoracionTable.idUsuarioQueValora],
                        puntuacion = row[ValoracionTable.puntuacion],
                        comentario = row[ValoracionTable.comentario],
                        fechaValoracion = Instant.parse(row[ValoracionTable.fechaValoracion].toString())
                    )
                }
        }
    }

    fun getValoracionPromedio(idUsuario: Int): Double {
        return transaction {
            ValoracionTable
                .slice(ValoracionTable.puntuacion.avg())
                .select { ValoracionTable.idUsuarioValorado eq idUsuario }
                .map { it[ValoracionTable.puntuacion.avg()] ?: 0.0 }
                .single() as Double
        }
    }
}