package app.verdant.resource

import app.verdant.dto.*
import app.verdant.repository.UserRepository
import app.verdant.service.AiService
import app.verdant.service.SpeciesService
import io.quarkus.security.Authenticated
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/species")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class SpeciesResource(
    private val speciesService: SpeciesService,
    private val aiService: AiService,
    private val userRepository: UserRepository,
    private val jwt: JsonWebToken
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    fun list(@QueryParam("q") query: String?, @QueryParam("limit") limit: Int?) =
        if (query.isNullOrBlank()) speciesService.getSpeciesForUser(userId())
        else speciesService.searchSpeciesForUser(userId(), query.trim(), limit ?: 20)

    @POST
    fun create(request: CreateSpeciesRequest): Response {
        val species = speciesService.createSpecies(request, userId())
        return Response.status(Response.Status.CREATED).entity(species).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, request: UpdateSpeciesRequest) =
        speciesService.updateSpecies(id, request, userId())

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        speciesService.deleteSpecies(id, userId())
        return Response.noContent().build()
    }

    // ── Extract Info ──

    @POST
    @Path("/extract-info")
    fun extractInfo(request: ExtractSpeciesInfoRequest): Response {
        val language = userRepository.findById(userId())?.language ?: "sv"
        val info = aiService.extractSpeciesInfo(request.imageBase64, language)
            ?: return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "Could not extract species info from image"))
                .build()
        return Response.ok(info).build()
    }

    // ── Groups ──

    @GET
    @Path("/groups")
    fun listGroups() = speciesService.getGroupsForUser(userId())

    @POST
    @Path("/groups")
    fun createGroup(request: CreateSpeciesGroupRequest): Response {
        val group = speciesService.createGroup(request, userId())
        return Response.status(Response.Status.CREATED).entity(group).build()
    }

    @DELETE
    @Path("/groups/{id}")
    fun deleteGroup(@PathParam("id") id: Long): Response {
        speciesService.deleteGroup(id, userId())
        return Response.noContent().build()
    }

    // ── Tags ──

    @GET
    @Path("/tags")
    fun listTags() = speciesService.getTagsForUser(userId())

    @POST
    @Path("/tags")
    fun createTag(request: CreateSpeciesTagRequest): Response {
        val tag = speciesService.createTag(request, userId())
        return Response.status(Response.Status.CREATED).entity(tag).build()
    }

    @DELETE
    @Path("/tags/{id}")
    fun deleteTag(@PathParam("id") id: Long): Response {
        speciesService.deleteTag(id, userId())
        return Response.noContent().build()
    }

    // ── Species Providers ──

    @GET
    @Path("/{speciesId}/providers")
    fun listProviders(@PathParam("speciesId") speciesId: Long) =
        speciesService.getProvidersForSpecies(speciesId, userId())

    @POST
    @Path("/{speciesId}/providers")
    fun addProvider(@PathParam("speciesId") speciesId: Long, request: AddSpeciesProviderRequest): Response {
        val sp = speciesService.addProviderToSpecies(speciesId, request, userId())
        return Response.status(Response.Status.CREATED).entity(sp).build()
    }

    @PUT
    @Path("/{speciesId}/providers/{spId}")
    fun updateProvider(
        @PathParam("speciesId") speciesId: Long,
        @PathParam("spId") spId: Long,
        request: UpdateSpeciesProviderRequest,
    ) = speciesService.updateSpeciesProvider(speciesId, spId, request, userId())

    @DELETE
    @Path("/{speciesId}/providers/{spId}")
    fun removeProvider(@PathParam("speciesId") speciesId: Long, @PathParam("spId") spId: Long): Response {
        speciesService.removeProviderFromSpecies(speciesId, spId, userId())
        return Response.noContent().build()
    }
}
