package app.verdant.resource

import app.verdant.repository.GardenRepository
import app.verdant.repository.UserRepository
import app.verdant.service.toResponse
import jakarta.annotation.security.RolesAllowed
import jakarta.transaction.Transactional
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
class AdminResource(
    private val userRepository: UserRepository,
    private val gardenRepository: GardenRepository
) {
    @GET
    @Path("/users")
    fun listUsers() = userRepository.listAll().map { it.toResponse() }

    @DELETE
    @Path("/users/{id}")
    @Transactional
    fun deleteUser(@PathParam("id") id: Long): Response {
        val user = userRepository.findById(id) ?: throw NotFoundException("User not found")
        userRepository.delete(user)
        return Response.noContent().build()
    }

    @GET
    @Path("/gardens")
    fun listGardens() = gardenRepository.listAll().map { it.toResponse() }
}
