package routes

import com.google.firebase.auth.FirebaseAuth
import dto.BookRequest
import dto.LibroInfoResponse
import dto.LibroResponse
import dto.LibrosResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import services.LibroService
import utils.respondError

fun Route.libroRoutes() {
    // recoger todos los libros
    get("/libro/allLibros") {
        println("Iniciando endpoint GET /libro/allLibros")
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            decodedToken.uid

            // Recoger todos los posibles parámetros de filtro
            val filtros = mapOf(
                "busqueda" to call.request.queryParameters["busqueda"],
                "idioma" to call.request.queryParameters["idioma"],
                "cubierta" to call.request.queryParameters["cubierta"],
                "categoriaPrincipal" to call.request.queryParameters["categoriaPrincipal"],
                "categoriaSecundaria" to call.request.queryParameters["categoriaSecundaria"],
                "estado" to call.request.queryParameters["estado"],
                "distancia" to call.request.queryParameters["distancia"],
            )

            println(filtros)

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

    get("/libro/{idLibro}") {
        println("Iniciando endpoint GET /libro/{idLibro}")
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            decodedToken.uid

            val idLibro = call.parameters["idLibro"]?.toInt() ?: return@get call.respondError(
                "ID de libro inválido",
                HttpStatusCode.BadRequest
            )

            val libro = LibroService.getLibroById(idLibro)

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
                    "message" to "Error al obtener el libro\n${e.message}"
                )
            )
        }
    }

    // recoger todos los libros de un usuario
    get("/libros/{uid}") {
        println("Iniciando endpoint GET /libros/{uid}")
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

            val libroInfo = LibroService.getLibrosByUid(uid)

            val data = LibroInfoResponse(
                "success",
                libroInfo
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
        println("Iniciando endpoint POST /libro/crearLibro")
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
        println("Iniciando endpoint POST /libro/actualizarLibro/{id}")
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
        println("Iniciando endpoint DELETE /libro/actualizarLibro/{id}")
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val uid = decodedToken.uid

            val id = call.parameters["id"]?.toInt() ?: return@delete call.respondError(
                "ID de libro inválido",
                HttpStatusCode.BadRequest
            )

            LibroService.deleteLibro(
                id = id,
                uidUsuario = uid
            )

            val data = mapOf(
                "status" to "success",
                "message" to "Libro eliminado correctamente"
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
                    "message" to "Error al eliminar el libro: ${e.message}"
                )
            )
        }
    }
}