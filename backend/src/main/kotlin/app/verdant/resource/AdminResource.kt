package app.verdant.resource

import app.verdant.dto.CreateSpeciesRequest
import app.verdant.dto.UpdateSpeciesRequest
import app.verdant.dto.UploadPhotoRequest
import app.verdant.repository.GardenRepository
import app.verdant.repository.UserRepository
import app.verdant.service.SpeciesService
import app.verdant.service.toResponse
import jakarta.annotation.security.RolesAllowed
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
class AdminResource(
    private val userRepository: UserRepository,
    private val gardenRepository: GardenRepository,
    private val speciesService: SpeciesService,
) {
    @GET
    @Path("/users")
    fun listUsers() = userRepository.listAll().map { it.toResponse() }

    @DELETE
    @Path("/users/{id}")
    fun deleteUser(@PathParam("id") id: Long): Response {
        userRepository.findById(id) ?: throw NotFoundException("User not found")
        userRepository.delete(id)
        return Response.noContent().build()
    }

    @GET
    @Path("/gardens")
    fun listGardens() = gardenRepository.listAll().map { it.toResponse() }

    // ── Species ──

    @GET
    @Path("/species")
    fun listSpecies() = speciesService.getAllSpecies()

    @GET
    @Path("/species/{id}")
    fun getSpecies(@PathParam("id") id: Long) = speciesService.getSpeciesAdmin(id)

    @POST
    @Path("/species")
    @Consumes(MediaType.APPLICATION_JSON)
    fun createSpecies(request: CreateSpeciesRequest) = speciesService.createSpeciesAdmin(request)

    @PUT
    @Path("/species/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    fun updateSpecies(@PathParam("id") id: Long, request: UpdateSpeciesRequest) =
        speciesService.updateSpeciesAdmin(id, request)

    @DELETE
    @Path("/species/{id}")
    fun deleteSpecies(@PathParam("id") id: Long): Response {
        speciesService.deleteSpeciesAdmin(id)
        return Response.noContent().build()
    }

    // ── Species Photos ──

    @POST
    @Path("/species/{id}/photos")
    @Consumes(MediaType.APPLICATION_JSON)
    fun uploadSpeciesPhoto(@PathParam("id") id: Long, request: UploadPhotoRequest) =
        speciesService.uploadSpeciesPhoto(id, request.imageBase64)

    @DELETE
    @Path("/species/{id}/photos/{photoId}")
    fun deleteSpeciesPhoto(@PathParam("id") id: Long, @PathParam("photoId") photoId: Long): Response {
        speciesService.deleteSpeciesPhoto(id, photoId)
        return Response.noContent().build()
    }
}
