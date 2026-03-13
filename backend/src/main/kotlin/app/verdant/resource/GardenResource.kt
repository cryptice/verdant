package app.verdant.resource

import app.verdant.dto.CreateGardenRequest
import app.verdant.dto.UpdateGardenRequest
import app.verdant.service.GardenService
import io.quarkus.security.Authenticated
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/gardens")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class GardenResource(
    private val gardenService: GardenService,
    private val jwt: JsonWebToken
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    fun list() = gardenService.getGardensForUser(userId())

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = gardenService.getGarden(id, userId())

    @POST
    fun create(request: CreateGardenRequest): Response {
        val garden = gardenService.createGarden(request, userId())
        return Response.status(Response.Status.CREATED).entity(garden).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, request: UpdateGardenRequest) =
        gardenService.updateGarden(id, request, userId())

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        gardenService.deleteGarden(id, userId())
        return Response.noContent().build()
    }
}
