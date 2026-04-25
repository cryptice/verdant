package app.verdant.resource

import app.verdant.dto.*
import app.verdant.filter.OrgContext
import app.verdant.service.BouquetService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/bouquets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class BouquetResource(
    private val service: BouquetService,
    private val orgContext: OrgContext,
) {
    @GET
    fun list(
        @QueryParam("limit") @DefaultValue("100") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = service.getBouquetsForUser(orgContext.orgId, limit.coerceIn(1, 500), offset.coerceAtLeast(0))

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getBouquet(id, orgContext.orgId)

    @POST
    fun create(@Valid request: CreateBouquetRequest): Response {
        val bouquet = service.createBouquet(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(bouquet).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateBouquetRequest) =
        service.updateBouquet(id, request, orgContext.orgId)

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteBouquet(id, orgContext.orgId)
        return Response.noContent().build()
    }
}
