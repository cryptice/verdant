package app.verdant.resource

import app.verdant.dto.ChangeOutletRequest
import app.verdant.dto.ChangePriceRequest
import app.verdant.dto.CreateSaleLotForHarvestRequest
import app.verdant.dto.CreateSaleLotForPlantRequest
import app.verdant.dto.ReturnFromOutletRequest
import app.verdant.entity.SaleLotStatus
import app.verdant.entity.SourceKind
import app.verdant.filter.OrgContext
import app.verdant.service.SaleLotService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/sale-lots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class SaleLotResource(
    private val service: SaleLotService,
    private val orgContext: OrgContext,
) {
    @GET
    fun list(
        @QueryParam("status") statusParam: String?,
        @QueryParam("sourceKind") sourceKindParam: String?,
        @QueryParam("limit") @DefaultValue("200") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = service.list(
        orgId = orgContext.orgId,
        status = statusParam?.let { runCatching { SaleLotStatus.valueOf(it) }.getOrNull() },
        sourceKind = sourceKindParam?.let { runCatching { SourceKind.valueOf(it) }.getOrNull() },
        limit = limit.coerceIn(1, 500),
        offset = offset.coerceAtLeast(0),
    )

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getDetail(id, orgContext.orgId)

    @POST
    @Path("/for-plant")
    fun createForPlant(@Valid request: CreateSaleLotForPlantRequest): Response {
        val lot = service.createForPlant(request, orgContext.orgId, orgContext.userId)
        return Response.status(Response.Status.CREATED).entity(lot).build()
    }

    @POST
    @Path("/for-harvest")
    fun createForHarvestEvent(@Valid request: CreateSaleLotForHarvestRequest): Response {
        val lot = service.createForHarvestEvent(request, orgContext.orgId, orgContext.userId)
        return Response.status(Response.Status.CREATED).entity(lot).build()
    }

    @POST
    @Path("/{id}/price")
    fun changePrice(@PathParam("id") id: Long, @Valid request: ChangePriceRequest) =
        service.changePrice(id, request, orgContext.orgId, orgContext.userId)

    @POST
    @Path("/{id}/outlet")
    fun changeOutlet(@PathParam("id") id: Long, @Valid request: ChangeOutletRequest) =
        service.changeOutlet(id, request, orgContext.orgId, orgContext.userId)

    @POST
    @Path("/{id}/return")
    fun markReturnedFromOutlet(@PathParam("id") id: Long, @Valid request: ReturnFromOutletRequest): Response {
        service.markReturnedFromOutlet(id, request, orgContext.orgId, orgContext.userId)
        return Response.noContent().build()
    }

    @POST
    @Path("/{id}/not-sold")
    fun markNotSold(@PathParam("id") id: Long) =
        service.markNotSold(id, orgContext.orgId, orgContext.userId)

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.delete(id, orgContext.orgId)
        return Response.noContent().build()
    }
}
