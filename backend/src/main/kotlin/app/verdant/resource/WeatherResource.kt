package app.verdant.resource

import app.verdant.dto.DailyWeatherResponse
import app.verdant.dto.GardenWeatherResponse
import app.verdant.entity.DailyWeather
import app.verdant.filter.OrgContext
import app.verdant.repository.DailyWeatherRepository
import app.verdant.service.GardenService
import io.quarkus.security.Authenticated
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import java.time.LocalDate

@Path("/api/weather")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
class WeatherResource(
    private val weather: DailyWeatherRepository,
    private val gardenService: GardenService,
    private val orgContext: OrgContext,
) {
    @GET
    @Path("/garden/{id}")
    fun forGarden(@PathParam("id") gardenId: Long, @QueryParam("days") days: Int?): GardenWeatherResponse {
        val garden = gardenService.getGarden(gardenId, orgContext.orgId)
        val window = (days ?: 10).coerceIn(1, 30)
        val rows = weather.findByGarden(
            gardenId,
            fromDate = LocalDate.now().minusDays(1),
            toDate = LocalDate.now().plusDays(window.toLong()),
        )
        return GardenWeatherResponse(
            gardenId = gardenId,
            backfillStatus = garden.weatherBackfillStatus,
            days = rows.map { it.toResponse() },
        )
    }
}

private fun DailyWeather.toResponse() = DailyWeatherResponse(
    date = date,
    observationType = observationType,
    tempMinC = tempMinC,
    tempMaxC = tempMaxC,
    tempMeanC = tempMeanC,
    precipitationMm = precipitationMm,
    windMaxMs = windMaxMs,
    humidityPct = humidityPct,
)
