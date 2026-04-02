package app.verdant.resource

import app.verdant.dto.CreateBedRequest
import app.verdant.dto.UpdateBedRequest
import app.verdant.service.BedService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class BedResource(
    private val bedService: BedService,
    private val jwt: JsonWebToken
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    @Path("/beds")
    fun listAll() = bedService.getAllBedsForUser(userId())

    @GET
    @Path("/beds/{id}")
    fun get(@PathParam("id") id: Long) = bedService.getBed(id, userId())

    @PUT
    @Path("/beds/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateBedRequest) =
        bedService.updateBed(id, request, userId())

    @DELETE
    @Path("/beds/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        bedService.deleteBed(id, userId())
        return Response.noContent().build()
    }

    @GET
    @Path("/beds/{id}/history")
    fun history(@PathParam("id") id: Long) = bedService.getBedHistory(id, userId())
}
