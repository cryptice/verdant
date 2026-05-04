package app.verdant.resource

import app.verdant.dto.EditSaleRequest
import app.verdant.dto.RecordSaleRequest
import app.verdant.filter.OrgContext
import app.verdant.service.SaleLotService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

/**
 * Sale-recording lives at /api/sale-lots/{id}/sales (record) and /api/sales/{id}
 * (edit). Two paths so the record action reads as scoped to its parent lot —
 * which it always is — while edit is naturally addressed by the sale's own id.
 */
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class SaleResource(
    private val service: SaleLotService,
    private val orgContext: OrgContext,
) {
    @POST
    @Path("/sale-lots/{lotId}/sales")
    fun recordSale(@PathParam("lotId") lotId: Long, @Valid request: RecordSaleRequest): Response {
        val sale = service.recordSale(lotId, request, orgContext.orgId, orgContext.userId)
        return Response.status(Response.Status.CREATED).entity(sale).build()
    }

    @PUT
    @Path("/sales/{id}")
    fun editSale(@PathParam("id") id: Long, @Valid request: EditSaleRequest) =
        service.editSale(id, request, orgContext.orgId, orgContext.userId)
}
