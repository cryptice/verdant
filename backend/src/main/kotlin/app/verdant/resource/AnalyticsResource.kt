package app.verdant.resource

import app.verdant.filter.OrgContext
import app.verdant.service.AnalyticsService
import io.quarkus.security.Authenticated
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType

@Path("/api/analytics")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
class AnalyticsResource(
    private val analyticsService: AnalyticsService,
    private val orgContext: OrgContext
) {
    @GET
    @Path("/seasons")
    fun seasonSummaries() = analyticsService.getSeasonSummaries(orgContext.orgId)

    @GET
    @Path("/species/{speciesId}/compare")
    fun speciesComparison(@PathParam("speciesId") speciesId: Long) =
        analyticsService.getSpeciesComparison(orgContext.orgId, speciesId)

    @GET
    @Path("/yield-per-bed")
    fun yieldPerBed(@QueryParam("seasonId") seasonId: Long?) =
        analyticsService.getYieldPerBed(orgContext.orgId, seasonId)
}
