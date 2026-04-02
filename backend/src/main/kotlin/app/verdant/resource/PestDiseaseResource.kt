package app.verdant.resource

import app.verdant.dto.*
import app.verdant.service.PestDiseaseService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/pest-disease-logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class PestDiseaseResource(
    private val service: PestDiseaseService,
    private val jwt: JsonWebToken
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    fun list(
        @QueryParam("seasonId") seasonId: Long?,
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = service.getLogsForUser(userId(), seasonId, limit.coerceIn(1, 200), offset.coerceAtLeast(0))

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getLog(id, userId())

    @POST
    fun create(@Valid request: CreatePestDiseaseLogRequest): Response {
        val log = service.createLog(request, userId())
        return Response.status(Response.Status.CREATED).entity(log).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdatePestDiseaseLogRequest) =
        service.updateLog(id, request, userId())

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteLog(id, userId())
        return Response.noContent().build()
    }
}
