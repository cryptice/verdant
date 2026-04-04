package app.verdant.resource

import app.verdant.dto.*
import app.verdant.filter.OrgContext
import app.verdant.service.ProductionTargetService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/production-targets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class ProductionTargetResource(
    private val service: ProductionTargetService,
    private val orgContext: OrgContext
) {
    @GET
    fun list(
        @QueryParam("seasonId") seasonId: Long?,
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = service.getTargetsForUser(orgContext.orgId, seasonId, limit.coerceIn(1, 200), offset.coerceAtLeast(0))

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getTarget(id, orgContext.orgId)

    @POST
    fun create(@Valid request: CreateProductionTargetRequest): Response {
        val target = service.createTarget(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(target).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateProductionTargetRequest) =
        service.updateTarget(id, request, orgContext.orgId)

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteTarget(id, orgContext.orgId)
        return Response.noContent().build()
    }

    @GET
    @Path("/{id}/forecast")
    fun forecast(@PathParam("id") id: Long) =
        service.calculateRequirements(id, orgContext.orgId)
}
