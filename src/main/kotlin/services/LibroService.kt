package services

import com.sun.org.apache.xalan.internal.lib.ExsltMath.power
import dto.BookRequest
import exceptions.NotFoundException
import exceptions.UnauthorizedException
import models.EstadoLibro
import models.Libro
import models.TipoCubierta
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import tables.LibroTable
import tables.UsuarioTable
import java.time.Instant
import java.time.LocalDateTime
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.ExperimentalTime

object LibroService {
    fun createLibro(
        uidUsuario: String,
        createRequest: BookRequest
    ): Libro {
        return transaction {
            val id = LibroTable.insert {
                it[LibroTable.uidUsuario] = uidUsuario
                it[titulo] = createRequest.titulo
                it[autor] = createRequest.autor
                it[idioma] = createRequest.idioma
                it[cubierta] = createRequest.cubierta
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
        usuarioId: String,
        updateRequest: BookRequest
    ): Libro {
        return transaction {
            LibroTable
                .select {
                    (LibroTable.id eq libroId) and
                            (LibroTable.uidUsuario eq usuarioId)
                }
                .singleOrNull()
                ?: throw UnauthorizedException("Libro no encontrado o no tienes permiso para modificarlo")

            LibroTable.update({ LibroTable.id eq libroId }) {
                it[titulo] = updateRequest.titulo
                it[autor] = updateRequest.autor
                it[idioma] = updateRequest.idioma
                it[cubierta] = updateRequest.cubierta
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
                        uidUsuario = row[LibroTable.uidUsuario],
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

    fun deleteLibro(id: Int) {
        transaction {
            LibroTable.deleteWhere { LibroTable.id eq id }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun getLibros(filtros: Map<String, String?>, usuarioUid: String): List<Libro> {
        return transaction {
            var query = LibroTable.selectAll()

            // Búsqueda combinada de título y autor
            filtros["busqueda"]?.let { busqueda ->
                query = query.andWhere {
                    (LibroTable.titulo like "%$busqueda%") or
                            (LibroTable.autor like "%$busqueda%")
                }
            }

            // Filtrado por distancia si se proporcionan coordenadas
            val distancia = filtros["distancia"]?.toDoubleOrNull()

            if (distancia != null && distancia > 0.0) {
                // Obtener usuario actual
                val usuarioActual = UsuarioTable
                    .slice(UsuarioTable.ultimaLatitud, UsuarioTable.ultimaLongitud)
                    .select { UsuarioTable.uid eq usuarioUid }
                    .singleOrNull()

                if (usuarioActual != null) {
                    val latitudUsuario = usuarioActual[UsuarioTable.ultimaLatitud]?.toDouble()
                    val longitudUsuario = usuarioActual[UsuarioTable.ultimaLongitud]?.toDouble()

                    if (latitudUsuario != null && longitudUsuario != null) {
                        val usuarios = UsuarioTable
                            .select {
                                (UsuarioTable.uid neq usuarioUid) and
                                        UsuarioTable.ultimaLatitud.isNotNull() and
                                        UsuarioTable.ultimaLongitud.isNotNull()
                            }
                            .map {
                                Triple(
                                    it[UsuarioTable.uid],
                                    it[UsuarioTable.ultimaLatitud]!!.toDouble(),
                                    it[UsuarioTable.ultimaLongitud]!!.toDouble()
                                )
                            }

                        val usuariosCercanos = usuarios.filter { (_, lat, lon) ->
                            val distanciaCalculada = calcularDistancia(latitudUsuario, longitudUsuario, lat, lon)
                            distanciaCalculada <= distancia
                        }.map { it.first }

                        query = query.andWhere { LibroTable.uidUsuario inList usuariosCercanos }
                    }
                }
            }

            filtros["idioma"]?.let {
                query = query.andWhere { LibroTable.idioma eq it }
            }

            filtros["cubierta"]?.let { cubiertaStr ->
                try {
                    val cubierta = TipoCubierta.valueOf(cubiertaStr.uppercase())
                    query = query.andWhere { LibroTable.cubierta eq cubierta }
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException("Cubierta de libro inválida: $cubiertaStr")
                }
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
                    uidUsuario = row[LibroTable.uidUsuario],
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

            // Fórmula Haversine para calcular distancia en km
            fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
                val r = 6371.0 // Radio de la Tierra en km

                val dLat = Math.toRadians(lat2 - lat1)
                val dLon = Math.toRadians(lon2 - lon1)

                val a = sin(dLat / 2).pow(2.0) +
                        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                        sin(dLon / 2).pow(2.0)

                val c = 2 * asin(sqrt(a))

                return r * c // Devuelve la distancia en kilómetros
            }

        }
