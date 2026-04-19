package app.verdant.service.weather

import app.verdant.entity.DailyWeather
import app.verdant.entity.ObservationType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ── SmhiForecastParser ─────────────────────────────────────────────────────────

object SmhiForecastParser {

    // SMHI validTime is UTC. Sweden uses Europe/Stockholm (CET/CEST) for day boundaries.
    private val STOCKHOLM = ZoneId.of("Europe/Stockholm")
    private val mapper = ObjectMapper()

    /**
     * Parses a SMHI point forecast JSON body into daily [DailyWeather] rows.
     *
     * Aggregation per Stockholm local date:
     *   - tempMinC        = min(t)
     *   - tempMaxC        = max(t)
     *   - tempMeanC       = avg(t)
     *   - precipitationMm = sum(pmax)  — upper-bound estimate per hour, summed for daily total
     *   - windMaxMs       = max(ws)
     *   - humidityPct     = avg(r)
     *
     * Returns an empty list if [body] contains no timeSeries entries.
     */
    fun parse(body: String, gardenId: Long): List<DailyWeather> {
        val root: JsonNode = mapper.readTree(body)
        val timeSeries = root.path("timeSeries")
        if (timeSeries.isEmpty || timeSeries.isNull) return emptyList()

        // Accumulate per-day observations keyed by Stockholm local date
        data class DayAcc(
            val temps: MutableList<Double> = mutableListOf(),
            val winds: MutableList<Double> = mutableListOf(),
            val humidities: MutableList<Double> = mutableListOf(),
            val precips: MutableList<Double> = mutableListOf(),
        )

        val byDay = linkedMapOf<LocalDate, DayAcc>()

        for (entry in timeSeries) {
            val validTime = entry.path("validTime").asText()
            val instant = Instant.parse(validTime)
            val localDate = instant.atZone(STOCKHOLM).toLocalDate()

            val params = entry.path("parameters")
            val paramMap = mutableMapOf<String, Double>()
            for (param in params) {
                val name = param.path("name").asText()
                val valueNode = param.path("values").firstOrNull()
                if (valueNode != null && !valueNode.isNull) {
                    paramMap[name] = valueNode.asDouble()
                }
            }

            val acc = byDay.getOrPut(localDate) { DayAcc() }
            paramMap["t"]?.let { acc.temps.add(it) }
            paramMap["ws"]?.let { acc.winds.add(it) }
            paramMap["r"]?.let { acc.humidities.add(it) }
            paramMap["pmax"]?.let { acc.precips.add(it) }
        }

        return byDay.map { (date, acc) ->
            DailyWeather(
                gardenId        = gardenId,
                date            = date,
                observationType = ObservationType.FORECAST,
                tempMinC        = acc.temps.minOrNull(),
                tempMaxC        = acc.temps.maxOrNull(),
                tempMeanC       = if (acc.temps.isEmpty()) null else acc.temps.average(),
                precipitationMm = if (acc.precips.isEmpty()) null else acc.precips.sum(),
                windMaxMs       = acc.winds.maxOrNull(),
                humidityPct     = if (acc.humidities.isEmpty()) null else acc.humidities.average(),
            )
        }
    }
}

// ── SmhiArchiveParser ──────────────────────────────────────────────────────────

// TODO(M1+): real archive parser wired to station-based endpoint.
// SMHI's observation archive uses a station-based API (/parameter/2/station-set/all/period/latest-day)
// which returns per-station hourly temperature readings. A full implementation would:
//   1. Parse the station list with lat/lon from the response.
//   2. Find the nearest station to the requested (lat, lon).
//   3. Aggregate that station's readings into a DailyWeather(ACTUAL).
// For now this returns null so WeatherIngestionService backfill skips archive rows gracefully.
object SmhiArchiveParser {

    /**
     * Parses a SMHI station observation archive response for a given [date] and point ([lat], [lon]).
     *
     * Currently a no-op returning null. See file-level TODO for the full implementation plan.
     */
    @Suppress("UNUSED_PARAMETER")
    fun parseForPoint(
        body: String,
        gardenId: Long,
        date: LocalDate,
        lat: Double,
        lon: Double,
    ): DailyWeather? = null
}

// ── SmhiClient ────────────────────────────────────────────────────────────────

@ApplicationScoped
class SmhiClient(
    @ConfigProperty(
        name = "smhi.forecast.base-url",
        defaultValue = "https://opendata-download-metfcst.smhi.se",
    )
    private val forecastBaseUrl: String,

    @ConfigProperty(
        name = "smhi.archive.base-url",
        defaultValue = "https://opendata-download-metobs.smhi.se",
    )
    private val archiveBaseUrl: String,
) {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    fun fetchForecast(lat: Double, lon: Double, gardenId: Long): List<DailyWeather> {
        val url = "$forecastBaseUrl/api/category/pmp3g/version/2/geotype/point/" +
            "lon/${"%.4f".format(lon)}/lat/${"%.4f".format(lat)}/data.json"
        val body = get(url) ?: return emptyList()
        return SmhiForecastParser.parse(body, gardenId)
    }

    fun fetchActual(lat: Double, lon: Double, gardenId: Long, date: LocalDate): DailyWeather? {
        // Archive uses station-based URLs; the parser picks the nearest available station by param.
        val url = "$archiveBaseUrl/api/version/1.0/parameter/2/station-set/all/period/latest-day/data.json"
        val body = get(url) ?: return null
        return SmhiArchiveParser.parseForPoint(body, gardenId, date, lat, lon)
    }

    private fun get(url: String): String? {
        val req = HttpRequest.newBuilder(URI.create(url))
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(15))
            .GET().build()
        val res = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (res.statusCode() in 200..299) return res.body()
        if (res.statusCode() == 429 || res.statusCode() >= 500) return null
        error("SMHI unexpected status ${res.statusCode()} for $url")
    }
}
