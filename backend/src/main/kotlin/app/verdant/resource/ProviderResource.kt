package app.verdant.resource

import app.verdant.dto.CreateProviderRequest
import app.verdant.dto.ProviderResponse
import app.verdant.dto.UpdateProviderRequest
import app.verdant.entity.Provider
import app.verdant.repository.ProviderRepository
import jakarta.annotation.security.RolesAllowed
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/providers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
class ProviderResource(
    private val providerRepository: ProviderRepository,
) {
    @GET
    fun list() = providerRepository.findAll().map { it.toResponse() }

    @POST
    fun create(@Valid request: CreateProviderRequest): Response {
        val provider = providerRepository.persist(Provider(name = request.name, identifier = request.identifier))
        return Response.status(Response.Status.CREATED).entity(provider.toResponse()).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateProviderRequest): ProviderResponse {
        val provider = providerRepository.findById(id) ?: throw NotFoundException("Provider not found")
        val updated = provider.copy(
            name = request.name ?: provider.name,
            identifier = request.identifier ?: provider.identifier,
        )
        providerRepository.update(updated)
        return updated.toResponse()
    }

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        providerRepository.findById(id) ?: throw NotFoundException("Provider not found")
        providerRepository.delete(id)
        return Response.noContent().build()
    }

    private fun Provider.toResponse() = ProviderResponse(id = id!!, name = name, identifier = identifier)
}
