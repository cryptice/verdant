package app.verdant.resource

import app.verdant.dto.*
import app.verdant.filter.OrgContext
import app.verdant.service.SeedBatchPlanService
import app.verdant.service.SeedInventoryService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/seed-stock")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class SeedInventoryResource(
    private val service: SeedInventoryService,
    private val planService: SeedBatchPlanService,
    private val orgContext: OrgContext
) {
    @GET
    fun list(
        @QueryParam("speciesId") speciesId: Long?,
        @QueryParam("seasonId") seasonId: Long?,
        @QueryParam("limit") limit: Int?,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = service.getInventoryForUser(orgContext.orgId, speciesId, seasonId, (limit?.coerceIn(1, 200) ?: Int.MAX_VALUE), offset.coerceAtLeast(0))

    @POST
    fun create(@Valid request: CreateSeedInventoryRequest): Response {
        val inventory = service.createInventory(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(inventory).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateSeedInventoryRequest) =
        service.updateInventory(id, request, orgContext.orgId)

    /** Plans several batches of one variety, drawn from in the order of `seedBatchIds`. */
    @POST
    @Path("/plan/preview")
    fun previewPlan(@Valid request: SeedBatchPlanRequest) =
        planService.preview(request.seedBatchIds.orEmpty(), request, orgContext.orgId)

    @POST
    @Path("/plan")
    fun createPlan(@Valid request: SeedBatchPlanRequest): Response =
        Response.status(Response.Status.CREATED).entity(planService.create(request.seedBatchIds.orEmpty(), request, orgContext.orgId)).build()

    @POST
    @Path("/{id}/plan/preview")
    fun previewBatchPlan(@PathParam("id") id: Long, @Valid request: SeedBatchPlanRequest) =
        planService.preview(listOf(id), request, orgContext.orgId)

    @POST
    @Path("/{id}/plan")
    fun createBatchPlan(@PathParam("id") id: Long, @Valid request: SeedBatchPlanRequest): Response =
        Response.status(Response.Status.CREATED).entity(planService.create(listOf(id), request, orgContext.orgId)).build()

    @POST
    @Path("/{id}/decrement")
    fun decrement(@PathParam("id") id: Long, @Valid request: DecrementSeedInventoryRequest) =
        service.decrementInventory(id, request, orgContext.orgId)

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteInventory(id, orgContext.orgId)
        return Response.noContent().build()
    }
}
