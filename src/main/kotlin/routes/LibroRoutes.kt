package routes

import UsuarioService
import com.google.firebase.auth.FirebaseAuth
import dto.BookRequest
import dto.LibrosResponse
import exceptions.NotFoundException
import exceptions.UnauthorizedException
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import services.LibroService
import utils.respondError
import utils.respondSuccess

fun Route.libroRoutes() {
    // recoger todos los libros
    get("/libro/allLibros") {
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            decodedToken.uid

            val libros = LibroService.getAllLibros()
            println(libros)

            val data = LibrosResponse(
                "success",
                libros
            )

            call.respondSuccess(
                data = data,
                message = "Libros encontrados exitosamente"
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "error",
                    "message" to "Error al obtener los libros\n${e.message}"
                )
            )
        }
    }

    // recoger libro por id
    get("/libro/{id}") {
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
    get("/libro/filter") {
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
    post("/libro/crearLibro") {
        try {
            val uid = call.principal<UserIdPrincipal>()?.name
                ?: return@post call.respondError(
                    "No autenticado",
                    HttpStatusCode.Unauthorized
                )

            val usuario = UsuarioService.getUserByUid(uid)
                ?: return@post call.respondError(
                    "Usuario no encontrado",
                    HttpStatusCode.NotFound
                )

            val createRequest = call.receive<BookRequest>()

            val nuevoLibro = LibroService.createLibro(
                uidUsuario = usuario.uid,
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
    post("/libro/{id}") {
        val libroId = call.parameters["id"]?.toIntOrNull()
            ?: return@post call.respondError(
                "ID de libro inválido",
                HttpStatusCode.BadRequest
            )

        val uid = call.principal<UserIdPrincipal>()?.name
            ?: return@post call.respondError(
                "No autenticado",
                HttpStatusCode.Unauthorized
            )

        try {
            val usuario = UsuarioService.getUserByUid(uid)
                ?: return@post call.respondError(
                    "Usuario no encontrado",
                    HttpStatusCode.NotFound
                )

            if (usuario.uid != LibroService.getLibroById(libroId)?.uidUsuario) {
                return@post call.respondError(
                    "No tienes permiso para editar este libro",
                    HttpStatusCode.Forbidden
                )
            } else {
                val updateRequest = call.receive<BookRequest>()

                val libroActualizado = LibroService.updateLibro(
                    libroId = libroId,
                    usuarioId = usuario.uid,
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
    delete("/libro/{id}") {
        val libroId = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respondError(
                "ID de libro inválido",
                HttpStatusCode.BadRequest
            )

        val uid = call.principal<UserIdPrincipal>()?.name
            ?: return@delete call.respondError(
                "No autenticado",
                HttpStatusCode.Unauthorized
            )

        try {
            val usuario = UsuarioService.getUserByUid(uid)
                ?: return@delete call.respondError(
                    "Usuario no encontrado",
                    HttpStatusCode.NotFound
                )

            if (usuario.uid.equals(LibroService.getLibroById(libroId)?.uidUsuario)) {
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