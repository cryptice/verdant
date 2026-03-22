package app.verdant.resource

import app.verdant.dto.*
import app.verdant.service.SuccessionScheduleService
import io.quarkus.security.Authenticated
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/succession-schedules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class SuccessionScheduleResource(
    private val service: SuccessionScheduleService,
    private val jwt: JsonWebToken
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    fun list(@QueryParam("seasonId") seasonId: Long?) =
        service.getSchedulesForUser(userId(), seasonId)

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getSchedule(id, userId())

    @POST
    fun create(request: CreateSuccessionScheduleRequest): Response {
        val schedule = service.createSchedule(request, userId())
        return Response.status(Response.Status.CREATED).entity(schedule).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, request: UpdateSuccessionScheduleRequest) =
        service.updateSchedule(id, request, userId())

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteSchedule(id, userId())
        return Response.noContent().build()
    }

    @POST
    @Path("/{id}/generate-tasks")
    fun generateTasks(@PathParam("id") id: Long) =
        service.generateTasks(id, userId())
}
