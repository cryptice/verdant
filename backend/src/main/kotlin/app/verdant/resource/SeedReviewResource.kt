package app.verdant.resource

import app.verdant.dto.SaveOrderListRequest
import app.verdant.dto.SeedReviewResponse
import app.verdant.filter.OrgContext
import app.verdant.service.SeedReviewService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType

@Path("/api/seed-review")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class SeedReviewResource(
    private val service: SeedReviewService,
    private val orgContext: OrgContext
) {
    @GET
    fun review(@QueryParam("seasonId") seasonId: Long?): SeedReviewResponse {
        if (seasonId == null) throw BadRequestException("seasonId is required")
        return service.review(seasonId, orgContext.orgId)
    }

    @PUT
    @Path("/{seasonId}/order-list")
    fun saveOrderList(@PathParam("seasonId") seasonId: Long, @Valid request: SaveOrderListRequest) =
        service.saveOrderList(seasonId, request, orgContext.orgId)
}
