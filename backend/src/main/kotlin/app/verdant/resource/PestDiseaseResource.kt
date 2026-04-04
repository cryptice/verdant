package app.verdant.resource

import app.verdant.dto.*
import app.verdant.filter.OrgContext
import app.verdant.service.PestDiseaseService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/pest-disease-logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class PestDiseaseResource(
    private val service: PestDiseaseService,
    private val orgContext: OrgContext
) {
    @GET
    fun list(
        @QueryParam("seasonId") seasonId: Long?,
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = service.getLogsForUser(orgContext.orgId, seasonId, limit.coerceIn(1, 200), offset.coerceAtLeast(0))

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getLog(id, orgContext.orgId)

    @POST
    fun create(@Valid request: CreatePestDiseaseLogRequest): Response {
        val log = service.createLog(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(log).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdatePestDiseaseLogRequest) =
        service.updateLog(id, request, orgContext.orgId)

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteLog(id, orgContext.orgId)
        return Response.noContent().build()
    }
}
