package app.verdant.service

import app.verdant.dto.LatLng
import app.verdant.dto.SuggestLayoutRequest
import app.verdant.dto.SuggestLayoutResponse
import app.verdant.dto.SuggestedBed
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Optional
import kotlin.math.cos

@ApplicationScoped
class AiService(
    private val objectMapper: ObjectMapper,
    @ConfigProperty(name = "verdant.gemini.api-key")
    private val apiKeyOpt: Optional<String>
) {
    private val httpClient = HttpClient.newHttpClient()

    fun suggestLayout(request: SuggestLayoutRequest): SuggestLayoutResponse {
        val apiKey = apiKeyOpt.orElse("")
        if (apiKey.isBlank()) {
            return fallbackLayout(request)
        }

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
Create 4-6 beds of varying sizes arranged realistically within the garden boundary.
Leave pathways between beds (don't fill the entire garden with beds).

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
            .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            return fallbackLayout(request)
        }

        return try {
            val responseJson = objectMapper.readTree(response.body())
            val text = responseJson["candidates"][0]["content"]["parts"][0]["text"].asText()
            val cleanJson = text.replace(Regex("^```json\\s*", RegexOption.MULTILINE), "")
                .replace(Regex("^```\\s*$", RegexOption.MULTILINE), "")
                .trim()
            objectMapper.readValue(cleanJson, SuggestLayoutResponse::class.java)
        } catch (e: Exception) {
            fallbackLayout(request)
        }
    }

    private fun fallbackLayout(request: SuggestLayoutRequest): SuggestLayoutResponse {
        val lat = request.latitude
        val lng = request.longitude
        val metersPerDegreeLat = 111_000.0
        val metersPerDegreeLng = 111_000.0 * cos(Math.toRadians(lat))

        // 20m x 15m garden
        val dLat = 20.0 / metersPerDegreeLat / 2.0
        val dLng = 15.0 / metersPerDegreeLng / 2.0

        val boundary = listOf(
            LatLng(lat - dLat, lng - dLng),
            LatLng(lat - dLat, lng + dLng),
            LatLng(lat + dLat, lng + dLng),
            LatLng(lat + dLat, lng - dLng)
        )

        // Create 4 beds with pathways between them
        val bedMargin = 0.15 // 15% margin for paths
        val halfW = dLng * (1 - bedMargin)
        val halfH = dLat * (1 - bedMargin)
        val gap = dLat * 0.08

        val beds = listOf(
            SuggestedBed("Sunrise patch", "The eastern bed, first to catch morning light",
                listOf(LatLng(lat + gap, lng - halfW), LatLng(lat + gap, lng - gap / 2),
                    LatLng(lat + halfH, lng - gap / 2), LatLng(lat + halfH, lng - halfW))),
            SuggestedBed("The herb spiral", "A cozy corner perfect for herbs and aromatics",
                listOf(LatLng(lat + gap, lng + gap / 2), LatLng(lat + gap, lng + halfW),
                    LatLng(lat + halfH, lng + halfW), LatLng(lat + halfH, lng + gap / 2))),
            SuggestedBed("Sunflower canyon", "The longest bed, ideal for tall plants and climbers",
                listOf(LatLng(lat - halfH, lng - halfW), LatLng(lat - halfH, lng + halfW),
                    LatLng(lat - gap, lng + halfW), LatLng(lat - gap, lng - halfW))),
            SuggestedBed("Wild corner", "A free-form area for experimentation",
                listOf(LatLng(lat - halfH * 0.3, lng - halfW * 0.3), LatLng(lat - halfH * 0.3, lng + halfW * 0.3),
                    LatLng(lat + halfH * 0.3, lng + halfW * 0.3), LatLng(lat + halfH * 0.3, lng - halfW * 0.3)))
        )

        return SuggestLayoutResponse("My garden", boundary, beds.take(3) + beds.drop(3))
    }
}
