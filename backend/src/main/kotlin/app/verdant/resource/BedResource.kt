package app.verdant.resource

import app.verdant.dto.CreateBedRequest
import app.verdant.dto.UpdateBedRequest
import app.verdant.service.BedService
import io.quarkus.security.Authenticated
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
    @Path("/gardens/{gardenId}/beds")
    fun list(@PathParam("gardenId") gardenId: Long) = bedService.getBedsForGarden(gardenId, userId())

    @GET
    @Path("/beds/{id}")
    fun get(@PathParam("id") id: Long) = bedService.getBed(id, userId())

    @POST
    @Path("/gardens/{gardenId}/beds")
    fun create(@PathParam("gardenId") gardenId: Long, request: CreateBedRequest): Response {
        val bed = bedService.createBed(gardenId, request, userId())
        return Response.status(Response.Status.CREATED).entity(bed).build()
    }

    @PUT
    @Path("/beds/{id}")
    fun update(@PathParam("id") id: Long, request: UpdateBedRequest) =
        bedService.updateBed(id, request, userId())

    @DELETE
    @Path("/beds/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        bedService.deleteBed(id, userId())
        return Response.noContent().build()
    }
}
