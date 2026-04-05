package app.verdant.resource

import app.verdant.dto.*
import app.verdant.filter.OrgContext
import app.verdant.service.SupplyService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/supplies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class SupplyResource(
    private val supplyService: SupplyService,
    private val orgContext: OrgContext,
) {
    // ── Types ──

    @GET
    @Path("/types")
    fun listTypes() = supplyService.getTypesForUser(orgContext.orgId)

    @POST
    @Path("/types")
    fun createType(@Valid request: CreateSupplyTypeRequest): Response {
        val type = supplyService.createType(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(type).build()
    }

    @PUT
    @Path("/types/{id}")
    fun updateType(@PathParam("id") id: Long, @Valid request: UpdateSupplyTypeRequest) =
        supplyService.updateType(id, request, orgContext.orgId)

    @DELETE
    @Path("/types/{id}")
    fun deleteType(@PathParam("id") id: Long): Response {
        supplyService.deleteType(id, orgContext.orgId)
        return Response.noContent().build()
    }

    // ── Inventory ──

    @GET
    fun listInventory() = supplyService.getInventoryForUser(orgContext.orgId)

    @POST
    fun createInventory(@Valid request: CreateSupplyInventoryRequest): Response {
        val item = supplyService.createInventory(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(item).build()
    }

    @PUT
    @Path("/{id}")
    fun updateInventory(@PathParam("id") id: Long, @Valid request: UpdateSupplyInventoryRequest) =
        supplyService.updateInventory(id, request, orgContext.orgId)

    @POST
    @Path("/{id}/decrement")
    fun decrementInventory(@PathParam("id") id: Long, @Valid request: DecrementSupplyRequest): Response {
        supplyService.decrementInventory(id, request.quantity, orgContext.orgId)
        return Response.noContent().build()
    }

    @DELETE
    @Path("/{id}")
    fun deleteInventory(@PathParam("id") id: Long): Response {
        supplyService.deleteInventory(id, orgContext.orgId)
        return Response.noContent().build()
    }
}
