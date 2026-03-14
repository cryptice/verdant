package app.verdant.service

import app.verdant.dto.*
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Optional
import java.util.logging.Logger
import kotlin.math.cos

@ApplicationScoped
class AiService(
    private val objectMapper: ObjectMapper,
    @ConfigProperty(name = "verdant.gemini.api-key")
    private val apiKeyOpt: Optional<String>
) {
    private val log = Logger.getLogger(AiService::class.java.name)
    private val httpClient = HttpClient.newHttpClient()

    fun identifyPlant(imageBase64: String): List<PlantSuggestion> {
        val apiKey = apiKeyOpt.orElse("")
        if (apiKey.isBlank()) {
            log.warning("No Gemini API key configured, cannot identify plant")
            return emptyList()
        }

        val prompt = """Identify the plant in this image. If it's a seed package, identify the plant species from the package.

Return ONLY valid JSON (no markdown, no explanation) as an array of up to 3 suggestions:
[{"species": "Solanum lycopersicum", "commonName": "Tomato", "confidence": 0.95}]

Each suggestion should have:
- species: scientific/Latin name
- commonName: common English name
- confidence: 0.0 to 1.0

If you cannot identify any plant, return an empty array: []"""

        val requestBody = objectMapper.writeValueAsString(mapOf(
            "contents" to listOf(mapOf(
                "parts" to listOf(
                    mapOf("text" to prompt),
                    mapOf("inlineData" to mapOf("mimeType" to "image/jpeg", "data" to imageBase64))
                )
            )),
            "generationConfig" to mapOf(
                "responseMimeType" to "application/json",
                "maxOutputTokens" to 1024
            )
        ))

        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=$apiKey"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            log.warning("Gemini API returned ${response.statusCode()}: ${response.body().take(200)}")
            return emptyList()
        }

        return try {
            val responseJson = objectMapper.readTree(response.body())
            val text = responseJson["candidates"][0]["content"]["parts"][0]["text"].asText()
            val cleanJson = text.replace(Regex("^```json\\s*", RegexOption.MULTILINE), "")
                .replace(Regex("^```\\s*$", RegexOption.MULTILINE), "")
                .trim()
            objectMapper.readValue(
                cleanJson,
                objectMapper.typeFactory.constructCollectionType(List::class.java, PlantSuggestion::class.java)
            )
        } catch (e: Exception) {
            log.warning("Failed to parse Gemini identify response: ${e.message}")
            emptyList()
        }
    }

    fun suggestLayout(request: SuggestLayoutRequest): SuggestLayoutResponse {
        val apiKey = apiKeyOpt.orElse("")
        if (apiKey.isBlank()) {
            log.warning("No Gemini API key configured, returning empty layout")
            return emptyLayout(request)
        }
        log.info("Calling Gemini API for layout suggestion at (${request.latitude}, ${request.longitude})")

        val metersPerDegreeLat = 111_000.0
        val metersPerDegreeLng = 111_000.0 * cos(Math.toRadians(request.latitude))

        val addressContext = if (request.address != null) ", address: \"${request.address}\"" else ""

        val prompt = """You are helping plan a garden layout. The garden is located at coordinates (${request.latitude}, ${request.longitude})$addressContext.

Create a realistic garden layout. A typical home garden is 15-30 meters across.

At this latitude:
- 1 degree latitude ≈ ${metersPerDegreeLat.toInt()} meters
- 1 degree longitude ≈ ${metersPerDegreeLng.toInt()} meters

So for a 20m x 15m garden, that's roughly ${20.0 / metersPerDegreeLat} degrees latitude by ${15.0 / metersPerDegreeLng} degrees longitude.

Return ONLY valid JSON (no markdown, no explanation) in this exact format:
{
  "gardenName": "A suggested name for this garden",
  "boundary": [
    {"lat": ..., "lng": ...}
  ],
  "beds": [
    {
      "name": "Creative bed name",
      "description": "Brief description of what could grow here",
      "boundary": [{"lat": ..., "lng": ...}]
    }
  ]
}

The boundary should be a polygon (4+ points) forming the garden outline, centered near the given coordinates.
Create 4-6 beds arranged in a grid or strip layout within the garden boundary.
Each bed should be a simple rectangle (4 corner points).
Leave 1-2 meter pathways between beds (don't fill the entire garden).

CRITICAL RULE — NO OVERLAPPING BEDS:
- Divide the garden into distinct non-overlapping zones (e.g. top-left, top-right, bottom strip).
- No bed polygon may share ANY interior area with another bed.
- The latitude/longitude ranges of adjacent beds must not intersect.
- Double-check: for any two beds, their rectangles must not overlap.

Guidelines for bed names - use creative, descriptive names like:
- "Behind the greenhouse"
- "Sunflower canyon"
- "The herb spiral"
- "Morning sun strip"
- Names should reflect position in garden, size, or have a creative/whimsical element."""

        val requestBody = objectMapper.writeValueAsString(mapOf(
            "contents" to listOf(mapOf(
                "parts" to listOf(mapOf(
                    "text" to prompt
                ))
            )),
            "generationConfig" to mapOf(
                "responseMimeType" to "application/json",
                "maxOutputTokens" to 2048
            )
        ))

        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=$apiKey"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            log.warning("Gemini API returned ${response.statusCode()}: ${response.body().take(200)}")
            return emptyLayout(request)
        }
        log.info("Gemini API responded successfully")

        return try {
            val responseJson = objectMapper.readTree(response.body())
            val text = responseJson["candidates"][0]["content"]["parts"][0]["text"].asText()
            val cleanJson = text.replace(Regex("^```json\\s*", RegexOption.MULTILINE), "")
                .replace(Regex("^```\\s*$", RegexOption.MULTILINE), "")
                .trim()
            val result = objectMapper.readValue(cleanJson, SuggestLayoutResponse::class.java)
            result.copy(beds = removeOverlappingBeds(result.beds))
        } catch (e: Exception) {
            log.warning("Failed to parse Gemini response: ${e.message}")
            emptyLayout(request)
        }
    }

    /** Remove beds that overlap with any earlier bed in the list. Uses axis-aligned bounding box check. */
    private fun removeOverlappingBeds(beds: List<SuggestedBed>): List<SuggestedBed> {
        val kept = mutableListOf<SuggestedBed>()
        for (bed in beds) {
            val overlaps = kept.any { polygonsOverlap(it.boundary, bed.boundary) }
            if (!overlaps) {
                kept.add(bed)
            } else {
                log.info("Removed overlapping bed: ${bed.name}")
            }
        }
        return kept
    }

    /** Check if two polygons overlap using separating axis theorem on their edges. */
    private fun polygonsOverlap(a: List<LatLng>, b: List<LatLng>): Boolean {
        if (a.size < 3 || b.size < 3) return false
        fun getEdgeNormals(poly: List<LatLng>): List<Pair<Double, Double>> {
            return poly.indices.map { i ->
                val next = (i + 1) % poly.size
                val dx = poly[next].lat - poly[i].lat
                val dy = poly[next].lng - poly[i].lng
                -dy to dx
            }
        }

        fun project(poly: List<LatLng>, axis: Pair<Double, Double>): Pair<Double, Double> {
            var min = Double.MAX_VALUE
            var max = -Double.MAX_VALUE
            for (p in poly) {
                val dot = p.lat * axis.first + p.lng * axis.second
                if (dot < min) min = dot
                if (dot > max) max = dot
            }
            return min to max
        }

        val axes = getEdgeNormals(a) + getEdgeNormals(b)
        for (axis in axes) {
            val (aMin, aMax) = project(a, axis)
            val (bMin, bMax) = project(b, axis)
            if (aMax <= bMin || bMax <= aMin) return false
        }
        return true
    }

    /** Returns a garden boundary with no beds — user adds beds manually. */
    private fun emptyLayout(request: SuggestLayoutRequest): SuggestLayoutResponse {
        val lat = request.latitude
        val lng = request.longitude
        val metersPerDegreeLat = 111_000.0
        val metersPerDegreeLng = 111_000.0 * cos(Math.toRadians(lat))

        val dLat = 20.0 / metersPerDegreeLat / 2.0
        val dLng = 15.0 / metersPerDegreeLng / 2.0

        val boundary = listOf(
            LatLng(lat - dLat, lng - dLng),
            LatLng(lat - dLat, lng + dLng),
            LatLng(lat + dLat, lng + dLng),
            LatLng(lat + dLat, lng - dLng)
        )

        return SuggestLayoutResponse("My garden", boundary, emptyList())
    }
}
