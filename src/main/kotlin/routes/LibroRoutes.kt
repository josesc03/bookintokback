package routes

import UsuarioService
import dto.BookRequest
import exceptions.NotFoundException
import exceptions.UnauthorizedException
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import services.LibroService
import utils.respondError
import utils.respondSuccess

fun Route.libroRoutes() {
    authenticate("firebase_auth") {
        route("/libros") {
            // recoger todos los libros
            get {
                call.principal<UserIdPrincipal>()?.name
                    ?: return@get call.respondError(
                        "No autenticado",
                        HttpStatusCode.Unauthorized
                    )

                val libros = LibroService.getLibros()

                call.respondSuccess(
                    data = libros,
                    message = "Libros encontrados exitosamente"
                )
            }

            // recoger libro por id
            get("/{id}") {
                call.principal<UserIdPrincipal>()?.name
                    ?: return@get call.respondError(
                        "No autenticado",
                        HttpStatusCode.Unauthorized
                    )

                val libroId = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respondError(
                        "ID de libro inválido",
                        HttpStatusCode.BadRequest
                    )

                val libro = LibroService.getLibroById(id = libroId)

                call.respondSuccess(
                    data = libro,
                    message = "Libro encontrado exitosamente"
                )
            }

            // regoger libro por filtro
            get {
                call.principal<UserIdPrincipal>()?.name
                    ?: return@get call.respondError(
                        "No autenticado",
                        HttpStatusCode.Unauthorized
                    )

                // Recoger todos los posibles parámetros de filtro
                val filtros = mapOf(
                    "titulo" to call.parameters["titulo"],
                    "autor" to call.parameters["autor"],
                    "idioma" to call.parameters["idioma"],
                    "cubierta" to call.parameters["cubierta"],
                    "categoriaPrincipal" to call.parameters["categoriaPrincipal"],
                    "categoriaSecundaria" to call.parameters["categoriaSecundaria"],
                    "estado" to call.parameters["estado"]
                )

                try {
                    val libros = LibroService.getLibrosConFiltros(filtros)
                    call.respondSuccess(
                        data = libros,
                        message = "Libros encontrados exitosamente"
                    )
                } catch (e: Exception) {
                    call.respondError(
                        "Error al obtener los libros: ${e.message}",
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // crear libro
            post {
                try {
                    val firebaseUid = call.principal<UserIdPrincipal>()?.name
                        ?: return@post call.respondError(
                            "No autenticado",
                            HttpStatusCode.Unauthorized
                        )

                    val usuario = UsuarioService.getUserByFirebaseUid(firebaseUid)
                        ?: return@post call.respondError(
                            "Usuario no encontrado",
                            HttpStatusCode.NotFound
                        )

                    val createRequest = call.receive<BookRequest>()

                    val nuevoLibro = LibroService.createLibro(
                        idUsuario = usuario.id,
                        createRequest = createRequest
                    )

                    call.respondSuccess(
                        data = nuevoLibro,
                        status = HttpStatusCode.Created,
                        message = "Libro creado exitosamente"
                    )
                } catch (e: Exception) {
                    call.respondError(
                        "Error al crear el libro: ${e.message}",
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // modificar libro
            put("/{id}") {
                val libroId = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respondError(
                        "ID de libro inválido",
                        HttpStatusCode.BadRequest
                    )

                val firebaseUid = call.principal<UserIdPrincipal>()?.name
                    ?: return@put call.respondError(
                        "No autenticado",
                        HttpStatusCode.Unauthorized
                    )

                try {
                    val usuario = UsuarioService.getUserByFirebaseUid(firebaseUid)
                        ?: return@put call.respondError(
                            "Usuario no encontrado",
                            HttpStatusCode.NotFound
                        )

                    if (usuario.id != LibroService.getLibroById(libroId)?.idUsuario) {
                        return@put call.respondError(
                            "No tienes permiso para editar este libro",
                            HttpStatusCode.Forbidden
                        )
                    } else {
                        val updateRequest = call.receive<BookRequest>()

                        val libroActualizado = LibroService.updateLibro(
                            libroId = libroId,
                            usuarioId = usuario.id,
                            updateRequest = updateRequest
                        )

                        call.respondSuccess(
                            data = libroActualizado,
                            message = "Libro actualizado exitosamente"
                        )
                    }

                } catch (e: UnauthorizedException) {
                    call.respondError(
                        e.message ?: "No autorizado",
                        HttpStatusCode.Forbidden
                    )
                } catch (e: NotFoundException) {
                    call.respondError(
                        e.message ?: "Libro no encontrado",
                        HttpStatusCode.NotFound
                    )
                } catch (e: Exception) {
                    call.respondError(
                        "Error al actualizar el libro: ${e.message}",
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // borrar libro
            delete("/{id}") {
                val libroId = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respondError(
                        "ID de libro inválido",
                        HttpStatusCode.BadRequest
                    )

                val firebaseUid = call.principal<UserIdPrincipal>()?.name
                    ?: return@delete call.respondError(
                        "No autenticado",
                        HttpStatusCode.Unauthorized
                    )

                try {
                    val usuario = UsuarioService.getUserByFirebaseUid(firebaseUid)
                        ?: return@delete call.respondError(
                            "Usuario no encontrado",
                            HttpStatusCode.NotFound
                        )

                    if (usuario.id != LibroService.getLibroById(libroId)?.idUsuario) {
                        return@delete call.respondError(
                            "No tienes permiso para eliminar este libro",
                            HttpStatusCode.Forbidden
                        )
                    }

                    LibroService.deleteLibro(libroId)

                    call.respondSuccess(
                        data = null,
                        message = "Libro eliminado exitosamente"
                    )

                } catch (e: UnauthorizedException) {
                    call.respondError(
                        e.message ?: "No autorizado",
                        HttpStatusCode.Forbidden
                    )
                } catch (e: NotFoundException) {
                    call.respondError(
                        e.message ?: "Libro no encontrado",
                        HttpStatusCode.NotFound
                    )
                } catch (e: Exception) {
                    call.respondError(
                        "Error al eliminar el libro: ${e.message}",
                        HttpStatusCode.InternalServerError
                    )
                }
            }
        }
    }
}