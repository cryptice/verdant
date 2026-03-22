package app.verdant.resource

import app.verdant.dto.*
import app.verdant.service.SeasonService
import io.quarkus.security.Authenticated
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/seasons")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class SeasonResource(
    private val service: SeasonService,
    private val jwt: JsonWebToken
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    fun list() = service.getSeasonsForUser(userId())

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getSeason(id, userId())

    @POST
    fun create(request: CreateSeasonRequest): Response {
        val season = service.createSeason(request, userId())
        return Response.status(Response.Status.CREATED).entity(season).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, request: UpdateSeasonRequest) =
        service.updateSeason(id, request, userId())

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteSeason(id, userId())
        return Response.noContent().build()
    }
}
