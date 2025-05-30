package com.bookintok

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import routes.chatRoute
import routes.libroRoutes
import routes.mensajeRoutes
import routes.usuarioRoutes
import utils.respondSuccess

fun Application.configureRouting() {
    authentication {
        jwt("firebase_auth") {
            validate { credential ->
                try {
                    var decodedToken =
                        FirebaseAuth.getInstance().verifyIdToken(credential.payload.getClaim("id_token").asString())
                    decodedToken?.let {
                        UserIdPrincipal(decodedToken.uid)
                    }
                } catch (e: FirebaseAuthException) {
                    null
                }
            }
        }
    }

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        route("/prueba") {
            get {
                val lista = listOf(1, 2, 3, 4, 5)
                call.respondSuccess(
                    message = "Lista obtenida con éxito",
                    data = lista
                )
            }

        }

        usuarioRoutes()
        libroRoutes()
        mensajeRoutes()
        chatRoute()

        print("\u001B[38;2;152;246;160mRutas importadas con exito\u001B[0m\n\n")
    }
}