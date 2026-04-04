package app.verdant.resource

import app.verdant.dto.*
import app.verdant.filter.OrgContext
import app.verdant.service.SuccessionScheduleService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/succession-schedules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class SuccessionScheduleResource(
    private val service: SuccessionScheduleService,
    private val orgContext: OrgContext
) {
    @GET
    fun list(
        @QueryParam("seasonId") seasonId: Long?,
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = service.getSchedulesForUser(orgContext.orgId, seasonId, limit.coerceIn(1, 200), offset.coerceAtLeast(0))

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getSchedule(id, orgContext.orgId)

    @POST
    fun create(@Valid request: CreateSuccessionScheduleRequest): Response {
        val schedule = service.createSchedule(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(schedule).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateSuccessionScheduleRequest) =
        service.updateSchedule(id, request, orgContext.orgId)

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteSchedule(id, orgContext.orgId)
        return Response.noContent().build()
    }

    @POST
    @Path("/{id}/generate-tasks")
    fun generateTasks(@PathParam("id") id: Long) =
        service.generateTasks(id, orgContext.orgId)
}
