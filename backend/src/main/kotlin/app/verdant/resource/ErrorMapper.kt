package app.verdant.resource

import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class ErrorMapper : ExceptionMapper<Exception> {
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
        return Response.status(status)
            .entity(ErrorResponse(exception.message ?: "Internal server error", status))
            .build()
    }
}
