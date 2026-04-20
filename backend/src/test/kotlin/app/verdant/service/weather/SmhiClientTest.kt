package app.verdant.service.weather

import app.verdant.entity.ObservationType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SmhiClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: SmhiClient

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        val baseUrl = server.url("/").toString().trimEnd('/')
        client = SmhiClient(forecastBaseUrl = baseUrl, archiveBaseUrl = baseUrl)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    // ── Forecast parser tests ──────────────────────────────────────────────────

    @Test
    fun `forecast parser aggregates hourly entries into daily rows`() {
        val body = javaClass.getResource("/smhi/forecast_sample.json")!!.readText()
        val rows = SmhiForecastParser.parse(body, gardenId = 7L)

        assertEquals(3, rows.size, "Expected exactly 3 daily rows, got ${rows.size}")
        rows.forEach { row ->
            assertEquals(ObservationType.FORECAST, row.observationType)
            assertEquals(7L, row.gardenId)
        }
        val first = rows.first()
        assertNotNull(first.tempMinC, "tempMinC should not be null")
        assertNotNull(first.tempMaxC, "tempMaxC should not be null")
        assertTrue(
            first.tempMaxC!! > first.tempMinC!!,
            "tempMaxC (${first.tempMaxC}) should be greater than tempMinC (${first.tempMinC})"
        )
    }

    @Test
    fun `forecast parser returns empty list for empty timeSeries`() {
        val emptyBody = """{"approvedTime":"2026-04-18T12:00:00Z","referenceTime":"2026-04-18T12:00:00Z","geometry":{"type":"Point","coordinates":[[18.07,59.33]]},"timeSeries":[]}"""
        val rows = SmhiForecastParser.parse(emptyBody, gardenId = 1L)
        assertTrue(rows.isEmpty(), "Expected empty list for empty timeSeries")
    }

    @Test
    fun `forecast parser sets correct gardenId on all rows`() {
        val body = javaClass.getResource("/smhi/forecast_sample.json")!!.readText()
        val rows = SmhiForecastParser.parse(body, gardenId = 42L)
        rows.forEach { assertEquals(42L, it.gardenId) }
    }

    @Test
    fun `forecast parser produces non-null precipitation for entries with pmax`() {
        val body = javaClass.getResource("/smhi/forecast_sample.json")!!.readText()
        val rows = SmhiForecastParser.parse(body, gardenId = 1L)
        // At least some rows should have non-null precipitation (fixture has non-zero pmax entries)
        val withPrecip = rows.filter { it.precipitationMm != null }
        assertTrue(withPrecip.isNotEmpty(), "Expected at least one row with precipitation data")
    }

    // ── Client HTTP error handling tests ───────────────────────────────────────

    @Test
    fun `client returns empty list on 429`() {
        server.enqueue(MockResponse().setResponseCode(429))
        val result = client.fetchForecast(lat = 59.33, lon = 18.07, gardenId = 1L)
        assertTrue(result.isEmpty(), "Expected empty list on 429, got $result")
    }

    @Test
    fun `client returns empty list on 500`() {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = client.fetchForecast(lat = 59.33, lon = 18.07, gardenId = 1L)
        assertTrue(result.isEmpty(), "Expected empty list on 500, got $result")
    }

    @Test
    fun `client returns empty list on 503`() {
        server.enqueue(MockResponse().setResponseCode(503))
        val result = client.fetchForecast(lat = 59.33, lon = 18.07, gardenId = 1L)
        assertTrue(result.isEmpty(), "Expected empty list on 503, got $result")
    }

    @Test
    fun `client throws on unexpected 4xx (404)`() {
        server.enqueue(MockResponse().setResponseCode(404))
        assertThrows<IllegalStateException> {
            client.fetchForecast(lat = 59.33, lon = 18.07, gardenId = 1L)
        }
    }

    @Test
    fun `client throws on unexpected 4xx (400)`() {
        server.enqueue(MockResponse().setResponseCode(400))
        assertThrows<IllegalStateException> {
            client.fetchForecast(lat = 59.33, lon = 18.07, gardenId = 1L)
        }
    }

    @Test
    fun `client returns null for fetchActual on 429`() {
        server.enqueue(MockResponse().setResponseCode(429))
        val result = client.fetchActual(
            lat = 59.33, lon = 18.07, gardenId = 1L, date = java.time.LocalDate.of(2026, 4, 19)
        )
        assertNull(result, "Expected null on 429 for fetchActual")
    }

    @Test
    fun `fetchActual returns null on 500`() {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = client.fetchActual(
            lat = 59.33, lon = 18.07, gardenId = 1L, date = java.time.LocalDate.of(2026, 4, 19)
        )
        assertNull(result, "Expected null on 500 for fetchActual")
    }

    @Test
    fun `fetchActual throws on unexpected 4xx (404)`() {
        server.enqueue(MockResponse().setResponseCode(404))
        assertThrows<IllegalStateException> {
            client.fetchActual(
                lat = 59.33, lon = 18.07, gardenId = 1L, date = java.time.LocalDate.of(2026, 4, 19)
            )
        }
    }

    // ── Archive parser tests ───────────────────────────────────────────────────

    @Test
    fun `archive parser returns null gracefully for any input`() {
        val body = javaClass.getResource("/smhi/archive_sample.json")!!.readText()
        val result = SmhiArchiveParser.parseForPoint(
            body = body,
            gardenId = 1L,
            date = java.time.LocalDate.of(2026, 4, 19),
            lat = 59.33,
            lon = 18.07,
        )
        assertNull(result, "Archive parser should return null (no-op pending M1+ implementation)")
    }
}
