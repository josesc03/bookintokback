package services

import dto.BookRequest
import exceptions.NotFoundException
import exceptions.UnauthorizedException
import models.EstadoLibro
import models.Libro
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import tables.LibroTable
import java.time.LocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object LibroService {
    fun createLibro(
        idUsuario: Int,
        createRequest: BookRequest
    ): Libro {
        return transaction {
            val id = LibroTable.insert {
                it[LibroTable.idUsuario] = idUsuario
                it[titulo] = createRequest.titulo
                it[autor] = createRequest.autor
                it[idioma] = createRequest.idioma
                it[cubierta] = createRequest.cubierta.name
                it[categoriaPrincipal] = createRequest.categoriaPrincipal
                it[categoriaSecundaria] = createRequest.categoriaSecundaria
                it[estado] = EstadoLibro.DISPONIBLE
                it[imagenUrl] = createRequest.imagenUrl
                it[fechaPublicacion] = LocalDateTime.now()
            } get LibroTable.id

            getLibroById(id) ?: throw Exception("Error al crear el libro")
        }
    }

    fun updateLibro(
        libroId: Int,
        usuarioId: Int,
        updateRequest: BookRequest
    ): Libro {
        return transaction {
            LibroTable
                .select {
                    (LibroTable.id eq libroId) and
                            (LibroTable.idUsuario eq usuarioId)
                }
                .singleOrNull()
                ?: throw UnauthorizedException("Libro no encontrado o no tienes permiso para modificarlo")

            LibroTable.update({ LibroTable.id eq libroId }) {
                it[titulo] = updateRequest.titulo
                it[autor] = updateRequest.autor
                it[idioma] = updateRequest.idioma
                it[cubierta] = updateRequest.cubierta.name
                it[categoriaPrincipal] = updateRequest.categoriaPrincipal
                it[categoriaSecundaria] = updateRequest.categoriaSecundaria
                it[estado] = updateRequest.estado
                it[imagenUrl] = updateRequest.imagenUrl
            }

            getLibroById(libroId)
                ?: throw NotFoundException("Libro no encontrado después de la actualización")
        }
    }

    @OptIn(ExperimentalTime::class)
    fun getLibroById(id: Int): Libro? {
        return transaction {
            LibroTable
                .select { LibroTable.id eq id }
                .mapNotNull { row ->
                    Libro(
                        id = row[LibroTable.id],
                        idUsuario = row[LibroTable.idUsuario],
                        titulo = row[LibroTable.titulo],
                        autor = row[LibroTable.autor],
                        idioma = row[LibroTable.idioma],
                        cubierta = row[LibroTable.cubierta],
                        categoriaPrincipal = row[LibroTable.categoriaPrincipal],
                        categoriaSecundaria = row[LibroTable.categoriaSecundaria],
                        estado = row[LibroTable.estado],
                        imagenUrl = row[LibroTable.imagenUrl],
                        fechaPublicacion = Instant.parse(row[LibroTable.fechaPublicacion].toString())
                    )
                }
                .singleOrNull()
        }
    }

    @OptIn(ExperimentalTime::class)
    fun getLibros(): List<Libro> {
        return transaction {
            LibroTable
                .selectAll()
                .mapNotNull { row ->
                    Libro(
                        id = row[LibroTable.id],
                        idUsuario = row[LibroTable.idUsuario],
                        titulo = row[LibroTable.titulo],
                        autor = row[LibroTable.autor],
                        idioma = row[LibroTable.idioma],
                        cubierta = row[LibroTable.cubierta],
                        categoriaPrincipal = row[LibroTable.categoriaPrincipal],
                        categoriaSecundaria = row[LibroTable.categoriaSecundaria],
                        estado = row[LibroTable.estado],
                        imagenUrl = row[LibroTable.imagenUrl],
                        fechaPublicacion = Instant.parse(row[LibroTable.fechaPublicacion].toString())
                    )
                }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun getLibrosConFiltros(filtros: Map<String, String?>): List<Libro> {
        return transaction {
            var query = LibroTable.selectAll()

            // Búsqueda combinada de título y autor
            filtros["busqueda"]?.let { busqueda ->
                query = query.andWhere {
                    (LibroTable.titulo like "%$busqueda%") or
                            (LibroTable.autor like "%$busqueda%")
                }
            }

            filtros["idioma"]?.let {
                query = query.andWhere { LibroTable.idioma eq it }
            }

            filtros["cubierta"]?.let {
                query = query.andWhere { LibroTable.cubierta eq it }
            }

            filtros["categoriaPrincipal"]?.let {
                query = query.andWhere { LibroTable.categoriaPrincipal eq it }
            }

            filtros["categoriaSecundaria"]?.let {
                query = query.andWhere { LibroTable.categoriaSecundaria eq it }
            }

            filtros["estado"]?.let { estadoStr ->
                try {
                    val estado = EstadoLibro.valueOf(estadoStr.uppercase())
                    query = query.andWhere { LibroTable.estado eq estado }
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException("Estado de libro inválido: $estadoStr")
                }
            }

            query.map { row ->
                Libro(
                    id = row[LibroTable.id],
                    idUsuario = row[LibroTable.idUsuario],
                    titulo = row[LibroTable.titulo],
                    autor = row[LibroTable.autor],
                    idioma = row[LibroTable.idioma],
                    cubierta = row[LibroTable.cubierta],
                    categoriaPrincipal = row[LibroTable.categoriaPrincipal],
                    categoriaSecundaria = row[LibroTable.categoriaSecundaria],
                    estado = row[LibroTable.estado],
                    imagenUrl = row[LibroTable.imagenUrl],
                    fechaPublicacion = Instant.parse(row[LibroTable.fechaPublicacion].toString())
                )
            }
        }
    }


    fun deleteLibro(id: Int) {
        transaction {
            LibroTable.deleteWhere { LibroTable.id eq id }
        }
    }
}