package app.verdant.resource

import app.verdant.dto.*
import app.verdant.service.ProductionTargetService
import io.quarkus.security.Authenticated
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/production-targets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class ProductionTargetResource(
    private val service: ProductionTargetService,
    private val jwt: JsonWebToken
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    fun list(@QueryParam("seasonId") seasonId: Long?) =
        service.getTargetsForUser(userId(), seasonId)

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getTarget(id, userId())

    @POST
    fun create(request: CreateProductionTargetRequest): Response {
        val target = service.createTarget(request, userId())
        return Response.status(Response.Status.CREATED).entity(target).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, request: UpdateProductionTargetRequest) =
        service.updateTarget(id, request, userId())

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteTarget(id, userId())
        return Response.noContent().build()
    }

    @GET
    @Path("/{id}/forecast")
    fun forecast(@PathParam("id") id: Long) =
        service.calculateRequirements(id, userId())
}
