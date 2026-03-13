package app.verdant.resource

import app.verdant.dto.UpdateUserRequest
import app.verdant.repository.UserRepository
import app.verdant.service.toResponse
import io.quarkus.security.Authenticated
import jakarta.transaction.Transactional
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class UserResource(
    private val userRepository: UserRepository,
    private val jwt: JsonWebToken
) {
    @GET
    @Path("/me")
    fun getMe() = userRepository.findById(jwt.subject.toLong())?.toResponse()
        ?: throw NotFoundException("User not found")

    @PUT
    @Path("/me")
    @Transactional
    fun updateMe(request: UpdateUserRequest): Any {
        val user = userRepository.findById(jwt.subject.toLong()) ?: throw NotFoundException("User not found")
        request.displayName?.let { user.displayName = it }
        request.avatarUrl?.let { user.avatarUrl = it }
        return user.toResponse()
    }

    @DELETE
    @Path("/me")
    @Transactional
    fun deleteMe() {
        val user = userRepository.findById(jwt.subject.toLong()) ?: throw NotFoundException("User not found")
        userRepository.delete(user)
    }
}
