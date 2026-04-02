package app.verdant.resource

import app.verdant.dto.CompleteTaskPartiallyRequest
import app.verdant.dto.CreateScheduledTaskRequest
import app.verdant.dto.UpdateScheduledTaskRequest
import app.verdant.service.ScheduledTaskService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class ScheduledTaskResource(
    private val taskService: ScheduledTaskService,
    private val jwt: JsonWebToken,
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    fun list(
        @QueryParam("seasonId") seasonId: Long?,
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = taskService.getTasksForUser(userId(), seasonId, limit.coerceIn(1, 200), offset.coerceAtLeast(0))

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = taskService.getTask(id, userId())

    @POST
    fun create(@Valid request: CreateScheduledTaskRequest): Response {
        val task = taskService.createTask(request, userId())
        return Response.status(Response.Status.CREATED).entity(task).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateScheduledTaskRequest) =
        taskService.updateTask(id, request, userId())

    @POST
    @Path("/{id}/complete")
    fun completePartially(@PathParam("id") id: Long, @Valid request: CompleteTaskPartiallyRequest) =
        taskService.completePartially(id, request.processedCount, userId())

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        taskService.deleteTask(id, userId())
        return Response.noContent().build()
    }
}
