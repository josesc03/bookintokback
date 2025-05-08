package routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import services.AuthService

fun Route.authRoutes() {
    route("/api/protected") {
        get {
            val authHeader = call.request.headers["Authorization"]

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                call.respond(HttpStatusCode.Unauthorized, "Falta el token")
                return@get
            }

            val idToken = authHeader.removePrefix("Bearer ").trim()

            try {
                val decodedToken = AuthService.verifyToken(idToken)
                val uid = decodedToken.uid

                call.respond(HttpStatusCode.OK, "Token válido. UID: $uid")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Unauthorized, "Token inválido: ${e.message}")
            }
        }
    }
}
