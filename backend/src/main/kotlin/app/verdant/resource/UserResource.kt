package app.verdant.resource

import app.verdant.dto.UpdateUserRequest
import app.verdant.repository.UserRepository
import app.verdant.service.toResponse
import io.quarkus.security.Authenticated
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
    fun updateMe(request: UpdateUserRequest): Any {
        val user = userRepository.findById(jwt.subject.toLong()) ?: throw NotFoundException("User not found")
        val updated = user.copy(
            displayName = request.displayName ?: user.displayName,
            avatarUrl = request.avatarUrl ?: user.avatarUrl,
        )
        userRepository.update(updated)
        return updated.toResponse()
    }

    @DELETE
    @Path("/me")
    fun deleteMe() {
        val user = userRepository.findById(jwt.subject.toLong()) ?: throw NotFoundException("User not found")
        userRepository.delete(user.id!!)
    }
}
