package app.verdant.resource

import app.verdant.dto.GardenResponse
import app.verdant.entity.DailyWeather
import app.verdant.entity.Garden
import app.verdant.entity.ObservationType
import app.verdant.filter.OrgContext
import app.verdant.repository.DailyWeatherRepository
import app.verdant.service.GardenService
import app.verdant.service.toResponse
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate

class WeatherResourceTest {

    private lateinit var weatherRepo: DailyWeatherRepository
    private lateinit var gardenService: GardenService
    private lateinit var orgContext: OrgContext
    private lateinit var resource: WeatherResource

    private val orgId = 1L
    private val otherOrgId = 2L
    private val gardenId = 10L
    private val today = LocalDate.now()

    @BeforeEach
    fun setup() {
        weatherRepo = mock()
        gardenService = mock()
        orgContext = OrgContext().apply { this.orgId = this@WeatherResourceTest.orgId }
        resource = WeatherResource(weatherRepo, gardenService, orgContext)
    }

    private fun gardenResponse(
        id: Long = gardenId,
        backfillStatus: String? = null,
    ) = GardenResponse(
        id = id,
        name = "Test Garden",
        description = null,
        emoji = null,
        latitude = 59.0,
        longitude = 18.0,
        address = null,
        boundaryJson = null,
        weatherBackfillStatus = backfillStatus,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private fun weatherRow(
        date: LocalDate,
        type: ObservationType = ObservationType.FORECAST,
    ) = DailyWeather(
        gardenId = gardenId,
        date = date,
        observationType = type,
        tempMinC = 10.0,
        tempMaxC = 20.0,
        tempMeanC = 15.0,
        precipitationMm = 2.0,
        windMaxMs = 5.0,
        humidityPct = 70.0,
    )

    @Test
    fun `returns weather rows and garden backfill status`() {
        val garden = gardenResponse(backfillStatus = "COMPLETE")
        val rows = listOf(
            weatherRow(today.minusDays(1), ObservationType.ACTUAL),
            weatherRow(today, ObservationType.FORECAST),
            weatherRow(today.plusDays(1), ObservationType.FORECAST),
        )
        whenever(gardenService.getGarden(gardenId, orgId)).thenReturn(garden)
        whenever(weatherRepo.findByGarden(eq(gardenId), any(), any())).thenReturn(rows)

        val response = resource.forGarden(gardenId, null)

        assertEquals(gardenId, response.gardenId)
        assertEquals("COMPLETE", response.backfillStatus)
        assertEquals(3, response.days.size)
        assertEquals(today.minusDays(1), response.days[0].date)
        assertEquals(ObservationType.ACTUAL, response.days[0].observationType)
        assertEquals(10.0, response.days[0].tempMinC)
    }

    @Test
    fun `default days=10 returns window of 10 days`() {
        val garden = gardenResponse()
        whenever(gardenService.getGarden(gardenId, orgId)).thenReturn(garden)
        whenever(weatherRepo.findByGarden(eq(gardenId), eq(today.minusDays(1)), eq(today.plusDays(10)))).thenReturn(emptyList())

        val response = resource.forGarden(gardenId, null)

        assertEquals(0, response.days.size)
    }

    @Test
    fun `days param is respected within 1-30 range`() {
        val garden = gardenResponse()
        whenever(gardenService.getGarden(gardenId, orgId)).thenReturn(garden)
        whenever(weatherRepo.findByGarden(eq(gardenId), eq(today.minusDays(1)), eq(today.plusDays(5)))).thenReturn(emptyList())

        resource.forGarden(gardenId, 5)
    }

    @Test
    fun `days param is clamped to minimum 1`() {
        val garden = gardenResponse()
        whenever(gardenService.getGarden(gardenId, orgId)).thenReturn(garden)
        whenever(weatherRepo.findByGarden(eq(gardenId), eq(today.minusDays(1)), eq(today.plusDays(1)))).thenReturn(emptyList())

        resource.forGarden(gardenId, 0)
    }

    @Test
    fun `days param is clamped to maximum 30`() {
        val garden = gardenResponse()
        whenever(gardenService.getGarden(gardenId, orgId)).thenReturn(garden)
        whenever(weatherRepo.findByGarden(eq(gardenId), eq(today.minusDays(1)), eq(today.plusDays(30)))).thenReturn(emptyList())

        resource.forGarden(gardenId, 999)
    }

    @Test
    fun `404 when garden does not exist`() {
        whenever(gardenService.getGarden(gardenId, orgId)).thenThrow(NotFoundException("Garden not found"))

        assertThrows<NotFoundException> {
            resource.forGarden(gardenId, null)
        }
    }

    @Test
    fun `404 when garden belongs to a different org`() {
        // Simulate a request from orgId=2 for a garden owned by orgId=1
        orgContext.orgId = otherOrgId
        whenever(gardenService.getGarden(gardenId, otherOrgId)).thenThrow(NotFoundException("Garden not found"))

        assertThrows<NotFoundException> {
            resource.forGarden(gardenId, null)
        }
    }

    @Test
    fun `backfillStatus is null when not set on garden`() {
        val garden = gardenResponse(backfillStatus = null)
        whenever(gardenService.getGarden(gardenId, orgId)).thenReturn(garden)
        whenever(weatherRepo.findByGarden(eq(gardenId), any(), any())).thenReturn(emptyList())

        val response = resource.forGarden(gardenId, null)

        assertNull(response.backfillStatus)
    }

    @Test
    fun `empty weather rows returned when no data exists`() {
        val garden = gardenResponse()
        whenever(gardenService.getGarden(gardenId, orgId)).thenReturn(garden)
        whenever(weatherRepo.findByGarden(eq(gardenId), any(), any())).thenReturn(emptyList())

        val response = resource.forGarden(gardenId, null)

        assertEquals(0, response.days.size)
    }
}
