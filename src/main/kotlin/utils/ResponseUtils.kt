package utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val status: String,
    val data: T? = null,
    val message: String? = null
)

suspend inline fun <reified T> ApplicationCall.respondSuccess(
    data: T,
    message: String? = null,
    status: HttpStatusCode = HttpStatusCode.OK
) {
    respond(
        status,
        ApiResponse(
            status = "success",
            data = data,
            message = message
        )
    )
}

suspend fun ApplicationCall.respondError(
    message: String,
    status: HttpStatusCode
) {
    respond(
        status,
        ApiResponse<Nothing>(
            status = "error",
            message = message
        )
    )
}