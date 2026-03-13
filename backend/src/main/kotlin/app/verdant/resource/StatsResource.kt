package app.verdant.resource

import app.verdant.service.PlantService
import io.quarkus.security.Authenticated
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/stats")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
class StatsResource(
    private val plantService: PlantService,
    private val jwt: JsonWebToken
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    @Path("/harvests")
    fun harvestStats() = plantService.getHarvestStats(userId())
}
