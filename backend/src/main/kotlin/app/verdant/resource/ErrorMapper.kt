package app.verdant.resource

import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class ErrorMapper : ExceptionMapper<Exception> {
    private val log = java.util.logging.Logger.getLogger(ErrorMapper::class.java.name)
    data class ErrorResponse(val message: String, val status: Int)

    override fun toResponse(exception: Exception): Response {
        val status = when (exception) {
            is jakarta.ws.rs.NotFoundException -> 404
            is jakarta.ws.rs.ForbiddenException -> 403
            is jakarta.ws.rs.BadRequestException -> 400
            is IllegalArgumentException -> 400
            is io.quarkus.security.UnauthorizedException -> 401
            else -> 500
        }
        if (status >= 500) {
            log.severe("Unhandled exception: ${exception.javaClass.name}: ${exception.message}")
            log.log(java.util.logging.Level.SEVERE, "Stack trace", exception)
        }
        val message = if (status >= 500) "Internal server error" else (exception.message ?: "Internal server error")
        return Response.status(status)
            .entity(ErrorResponse(message, status))
            .build()
    }
}
