package app.verdant.resource

import app.verdant.dto.*
import app.verdant.filter.OrgContext
import app.verdant.service.VarietyTrialService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/variety-trials")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class VarietyTrialResource(
    private val service: VarietyTrialService,
    private val orgContext: OrgContext
) {
    @GET
    fun list(
        @QueryParam("seasonId") seasonId: Long?,
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = service.getTrialsForUser(orgContext.orgId, seasonId, limit.coerceIn(1, 200), offset.coerceAtLeast(0))

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getTrial(id, orgContext.orgId)

    @POST
    fun create(@Valid request: CreateVarietyTrialRequest): Response {
        val trial = service.createTrial(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(trial).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateVarietyTrialRequest) =
        service.updateTrial(id, request, orgContext.orgId)

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteTrial(id, orgContext.orgId)
        return Response.noContent().build()
    }
}
