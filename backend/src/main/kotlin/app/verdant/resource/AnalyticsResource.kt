package app.verdant.resource

import app.verdant.service.AnalyticsService
import io.quarkus.security.Authenticated
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/analytics")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
class AnalyticsResource(
    private val analyticsService: AnalyticsService,
    private val jwt: JsonWebToken
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    @Path("/seasons")
    fun seasonSummaries() = analyticsService.getSeasonSummaries(userId())

    @GET
    @Path("/species/{speciesId}/compare")
    fun speciesComparison(@PathParam("speciesId") speciesId: Long) =
        analyticsService.getSpeciesComparison(userId(), speciesId)

    @GET
    @Path("/yield-per-bed")
    fun yieldPerBed(@QueryParam("seasonId") seasonId: Long?) =
        analyticsService.getYieldPerBed(userId(), seasonId)
}
