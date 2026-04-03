package app.verdant.resource

import app.verdant.dto.*
import app.verdant.service.SeedInventoryService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/seed-stock")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class SeedInventoryResource(
    private val service: SeedInventoryService,
    private val jwt: JsonWebToken
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    fun list(
        @QueryParam("speciesId") speciesId: Long?,
        @QueryParam("seasonId") seasonId: Long?,
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = service.getInventoryForUser(userId(), speciesId, seasonId, limit.coerceIn(1, 200), offset.coerceAtLeast(0))

    @POST
    fun create(@Valid request: CreateSeedInventoryRequest): Response {
        val inventory = service.createInventory(request, userId())
        return Response.status(Response.Status.CREATED).entity(inventory).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateSeedInventoryRequest) =
        service.updateInventory(id, request, userId())

    @POST
    @Path("/{id}/decrement")
    fun decrement(@PathParam("id") id: Long, @Valid request: DecrementSeedInventoryRequest) =
        service.decrementInventory(id, request, userId())

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteInventory(id, userId())
        return Response.noContent().build()
    }
}
