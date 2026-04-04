package app.verdant.resource

import app.verdant.dto.*
import app.verdant.filter.OrgContext
import app.verdant.repository.UserRepository
import app.verdant.service.AiService
import app.verdant.service.SpeciesService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
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
    private val orgContext: OrgContext,
    private val jwt: JsonWebToken
) {
    @GET
    fun list(
        @QueryParam("q") query: String?,
        @QueryParam("groupId") groupId: Long?,
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = when {
        groupId != null -> speciesService.getSpeciesByGroup(groupId, orgContext.orgId)
        query.isNullOrBlank() -> speciesService.getSpeciesForUser(orgContext.orgId, limit.coerceIn(1, 200), offset.coerceAtLeast(0))
        else -> speciesService.searchSpeciesForUser(orgContext.orgId, query.trim(), limit.coerceIn(1, 200))
    }

    @POST
    fun create(@Valid request: CreateSpeciesRequest): Response {
        val species = speciesService.createSpecies(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(species).build()
    }

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = speciesService.getSpecies(id, orgContext.orgId)

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateSpeciesRequest) =
        speciesService.updateSpecies(id, request, orgContext.orgId)

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        speciesService.deleteSpecies(id, orgContext.orgId)
        return Response.noContent().build()
    }

    // ── Extract Info ──

    @POST
    @Path("/extract-info")
    fun extractInfo(@Valid request: ExtractSpeciesInfoRequest): Response {
        val language = userRepository.findById(jwt.subject.toLong())?.language ?: "sv"
        val info = aiService.extractSpeciesInfo(request.imageBase64, language)
            ?: return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "Could not extract species info from image"))
                .build()
        return Response.ok(info).build()
    }

    // ── Groups ──

    @GET
    @Path("/groups")
    fun listGroups() = speciesService.getGroupsForUser(orgContext.orgId)

    @POST
    @Path("/groups")
    fun createGroup(@Valid request: CreateSpeciesGroupRequest): Response {
        val group = speciesService.createGroup(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(group).build()
    }

    @PUT
    @Path("/groups/{id}")
    fun updateGroup(@PathParam("id") id: Long, @Valid request: CreateSpeciesGroupRequest): Response {
        val group = speciesService.updateGroup(id, request, orgContext.orgId)
        return Response.ok(group).build()
    }

    @DELETE
    @Path("/groups/{id}")
    fun deleteGroup(@PathParam("id") id: Long): Response {
        speciesService.deleteGroup(id, orgContext.orgId)
        return Response.noContent().build()
    }

    @GET
    @Path("/groups/{groupId}/species")
    fun listGroupMembers(@PathParam("groupId") groupId: Long) =
        speciesService.getGroupMembers(groupId, orgContext.orgId)

    @POST
    @Path("/groups/{groupId}/species/{speciesId}")
    fun addSpeciesToGroup(@PathParam("groupId") groupId: Long, @PathParam("speciesId") speciesId: Long): Response {
        speciesService.addSpeciesToGroup(groupId, speciesId, orgContext.orgId)
        return Response.noContent().build()
    }

    @DELETE
    @Path("/groups/{groupId}/species/{speciesId}")
    fun removeSpeciesFromGroup(@PathParam("groupId") groupId: Long, @PathParam("speciesId") speciesId: Long): Response {
        speciesService.removeSpeciesFromGroup(groupId, speciesId, orgContext.orgId)
        return Response.noContent().build()
    }

    // ── Tags ──

    @GET
    @Path("/tags")
    fun listTags() = speciesService.getTagsForUser(orgContext.orgId)

    @POST
    @Path("/tags")
    fun createTag(@Valid request: CreateSpeciesTagRequest): Response {
        val tag = speciesService.createTag(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(tag).build()
    }

    @DELETE
    @Path("/tags/{id}")
    fun deleteTag(@PathParam("id") id: Long): Response {
        speciesService.deleteTag(id, orgContext.orgId)
        return Response.noContent().build()
    }

    // ── Species Providers ──

    @GET
    @Path("/{speciesId}/providers")
    fun listProviders(@PathParam("speciesId") speciesId: Long) =
        speciesService.getProvidersForSpecies(speciesId, orgContext.orgId)

    @POST
    @Path("/{speciesId}/providers")
    fun addProvider(@PathParam("speciesId") speciesId: Long, @Valid request: AddSpeciesProviderRequest): Response {
        val sp = speciesService.addProviderToSpecies(speciesId, request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(sp).build()
    }

    @PUT
    @Path("/{speciesId}/providers/{spId}")
    fun updateProvider(
        @PathParam("speciesId") speciesId: Long,
        @PathParam("spId") spId: Long,
        @Valid request: UpdateSpeciesProviderRequest,
    ) = speciesService.updateSpeciesProvider(speciesId, spId, request, orgContext.orgId)

    @DELETE
    @Path("/{speciesId}/providers/{spId}")
    fun removeProvider(@PathParam("speciesId") speciesId: Long, @PathParam("spId") spId: Long): Response {
        speciesService.removeProviderFromSpecies(speciesId, spId, orgContext.orgId)
        return Response.noContent().build()
    }
}
