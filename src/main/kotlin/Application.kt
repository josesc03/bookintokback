import com.bookintok.configureDatabases
import com.bookintok.configureFirebase
import com.bookintok.configureRouting
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    // Configura las dependencias
    configureSerialization()
    configureFirebase()
    configureDatabases()

    // Configura las rutas
    configureRouting()

    println("\u001B[38;2;152;246;160mServidor iniciado correctamente\u001B[0m\n")
}