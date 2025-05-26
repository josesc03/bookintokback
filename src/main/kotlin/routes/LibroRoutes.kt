package routes

import UsuarioService
import com.google.firebase.auth.FirebaseAuth
import dto.BookRequest
import dto.LibroResponse
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

            val libros = LibroService.getLibros(filtros, usuarioUid = decodedToken.uid)
            println(libros)

            val data = LibrosResponse(
                "success",
                libros
            )

            call.respond(
                HttpStatusCode.OK,
                data
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

    // recoger todos los libros
    get("/libros/{uid}") {
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            decodedToken.uid

            val uid = call.parameters["uid"] ?: return@get call.respondError(
                "ID de usuario inválido",
                HttpStatusCode.BadRequest
            )

            val libros = LibroService.getLibrosByUid(uid)

            val data = LibrosResponse(
                "success",
                libros
            )

            call.respond(
                HttpStatusCode.OK,
                data
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
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            decodedToken.uid

            val id = call.parameters["id"]?.toInt() ?: return@get call.respondError(
                "ID de usuario inválido",
                HttpStatusCode.BadRequest
            )

            var libro = LibroService.getLibroById(id)

            val data = LibroResponse(
                "success",
                libro
            )

            call.respond(
                HttpStatusCode.OK,
                data
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

    // crear libro
    post("/libro/crearLibro") {
        println("Iniciando endpoint /libro/crearLibro")
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val uid = decodedToken.uid

            val request = call.receive<BookRequest>()

            var libro = LibroService.createLibro(
                createRequest = request,
                uidUsuario = uid
            )


            val data = LibroResponse(
                "success",
                libro
            )

            call.respond(
                HttpStatusCode.OK,
                data
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

    // modificar libro
    post("/libro/actualizarLibro/{id}") {
        println("Iniciando endpoint /libro/actualizarLibro/{id}")
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val uid = decodedToken.uid

            val id = call.parameters["id"]?.toInt() ?: return@post call.respondError(
                "ID de libro inválido",
                HttpStatusCode.BadRequest
            )


            val request = call.receive<BookRequest>()

            var libro = LibroService.updateLibro(
                libroId = id,
                usuarioUid = uid,
                updateRequest = request
            )

            val data = LibroResponse(
                "success",
                libro
            )

            call.respond(
                HttpStatusCode.OK,
                data
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