package app.verdant.resource

import app.verdant.dto.*
import app.verdant.service.ListingService
import io.quarkus.security.Authenticated
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
    fun browse() = service.getActiveListings()

    @GET
    @Path("/mine")
    fun mine() = service.getListingsForUser(userId())

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getListing(id, userId())

    @POST
    fun create(request: CreateListingRequest): Response {
        val listing = service.createListing(request, userId())
        return Response.status(Response.Status.CREATED).entity(listing).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, request: UpdateListingRequest) =
        service.updateListing(id, request, userId())

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteListing(id, userId())
        return Response.noContent().build()
    }
}
