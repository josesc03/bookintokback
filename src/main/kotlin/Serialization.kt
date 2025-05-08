import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = true
            prettyPrint = true
            ignoreUnknownKeys = true
        })
    }
    println("\u001B[38;2;152;246;160mSerialización configurada correctamente\u001B[0m\n")
}
