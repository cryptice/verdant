package app.verdant.resource

import app.verdant.dto.CompleteTaskPartiallyRequest
import app.verdant.dto.CreateScheduledTaskRequest
import app.verdant.dto.UpdateScheduledTaskRequest
import app.verdant.filter.OrgContext
import app.verdant.service.ScheduledTaskService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class ScheduledTaskResource(
    private val taskService: ScheduledTaskService,
    private val orgContext: OrgContext,
) {
    @GET
    fun list(
        @QueryParam("seasonId") seasonId: Long?,
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = taskService.getTasksForUser(orgContext.orgId, seasonId, limit.coerceIn(1, 200), offset.coerceAtLeast(0))

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = taskService.getTask(id, orgContext.orgId)

    @POST
    fun create(@Valid request: CreateScheduledTaskRequest): Response {
        val task = taskService.createTask(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(task).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateScheduledTaskRequest) =
        taskService.updateTask(id, request, orgContext.orgId)

    @POST
    @Path("/{id}/complete")
    fun completePartially(@PathParam("id") id: Long, @Valid request: CompleteTaskPartiallyRequest) =
        taskService.completePartially(id, request.processedCount, orgContext.orgId)

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        taskService.deleteTask(id, orgContext.orgId)
        return Response.noContent().build()
    }
}
