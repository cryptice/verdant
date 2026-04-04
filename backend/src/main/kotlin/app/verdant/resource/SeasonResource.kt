package app.verdant.resource

import app.verdant.dto.*
import app.verdant.filter.OrgContext
import app.verdant.service.SeasonService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/seasons")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class SeasonResource(
    private val service: SeasonService,
    private val orgContext: OrgContext
) {
    @GET
    fun list() = service.getSeasonsForUser(orgContext.orgId)

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getSeason(id, orgContext.orgId)

    @POST
    fun create(@Valid request: CreateSeasonRequest): Response {
        val season = service.createSeason(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(season).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateSeasonRequest) =
        service.updateSeason(id, request, orgContext.orgId)

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteSeason(id, orgContext.orgId)
        return Response.noContent().build()
    }
}
