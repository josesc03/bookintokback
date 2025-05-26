package routes

import com.google.firebase.auth.FirebaseAuth
import dto.ValoracionesResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import services.ValoracionService
import services.ValoracionService.getValoracionesDeUsuario
import services.ValoracionService.hasRatedUser

fun Route.valoracionRoutes() {
    post("/valorar/{uid}") {
        try {
            val request = call.receive<Map<String, String>>()

            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val uid = decodedToken.uid

            val uidUsuarioValorado = call.parameters["uid"]
                ?: return@post call.respondText(
                    "UID de usuario inválido",
                    status = HttpStatusCode.BadRequest
                )

            val valoracion =
                request["valoracion"]?.toInt() ?: throw IllegalArgumentException("Valoración no proporcionada")

            val comentario = request["comentario"] ?: ""

            val valoracionCreada = ValoracionService.crearValoracion(
                uidUsuarioValorado,
                uid,
                valoracion,
                comentario
            )

            call.respond(
                HttpStatusCode.OK,
                "Valoracion creada correctamente: ${valoracionCreada.puntuacion} - $comentario"
            )

        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "error",
                    "message" to "Error al crear la valoracion"
                )
            )
        }

    }

    // Obtener valoraciones de un usuario
    get("/valoraciones/{uidUsuario}") {
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            FirebaseAuth.getInstance().verifyIdToken(token)

            val uidUsuario =
                call.parameters["uidUsuario"] ?: throw IllegalArgumentException("UID de usuario no proporcionado")

            val valoraciones = getValoracionesDeUsuario(uidUsuario)


            val data = ValoracionesResponse(
                "success",
                valoraciones
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
                    "message" to "Error al recoger las valoraciones"
                )
            )
        }

    }

    get("/has-rated/{uidUsuario}") {
        try {
            val authHeader =
                call.request.headers["Authorization"] ?: throw IllegalArgumentException("Token no proporcionado")
            val token = authHeader.removePrefix("Bearer ").trim()
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val uid = decodedToken.uid

            val uidUsuario =
                call.parameters["uidUsuario"] ?: throw IllegalArgumentException("UID de usuario no proporcionado")

            val hasRated = hasRatedUser(uid, uidUsuario)


            val data = mapOf(
                "status" to "success",
                "message" to hasRated
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
                    "message" to "Error al comprobar las valoraciones"
                )
            )
        }
    }

}