package app.verdant.resource

import app.verdant.dto.*
import app.verdant.service.ListingService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/market/listings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class ListingResource(
    private val service: ListingService,
    private val jwt: JsonWebToken,
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    fun browse(
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = service.getActiveListings(limit.coerceIn(1, 200), offset.coerceAtLeast(0))

    @GET
    @Path("/mine")
    fun mine(
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = service.getListingsForUser(userId(), limit.coerceIn(1, 200), offset.coerceAtLeast(0))

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getListing(id, userId())

    @POST
    fun create(@Valid request: CreateListingRequest): Response {
        val listing = service.createListing(request, userId())
        return Response.status(Response.Status.CREATED).entity(listing).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateListingRequest) =
        service.updateListing(id, request, userId())

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteListing(id, userId())
        return Response.noContent().build()
    }
}
