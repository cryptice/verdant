package app.verdant.resource

import app.verdant.filter.OrgContext
import app.verdant.service.PlantService
import io.quarkus.security.Authenticated
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType

@Path("/api/stats")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
class StatsResource(
    private val plantService: PlantService,
    private val orgContext: OrgContext
) {
    @GET
    @Path("/harvests")
    fun harvestStats() = plantService.getHarvestStats(orgContext.orgId)
}
