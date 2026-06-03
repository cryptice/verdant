package app.verdant.service

import app.verdant.entity.Bed
import app.verdant.entity.Garden
import app.verdant.entity.Season
import app.verdant.repository.BedPhotoRepository
import app.verdant.repository.BedRepository
import app.verdant.repository.DailyWeatherRepository
import app.verdant.repository.GardenRepository
import app.verdant.repository.HarvestWeekBucket
import app.verdant.repository.PlantEventRepository
import app.verdant.repository.SeasonRepository
import app.verdant.repository.SpeciesRepository
import app.verdant.service.weather.WeatherIngestionService
import io.agroal.api.AgroalDataSource
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests (no DB) for the harvest-stats business logic: the season-scoped
 * summary math and the per-bed / per-garden org-scoping. SQL aggregation
 * correctness is covered by PlantEventHarvestStatsTest (@QuarkusTest).
 */
class HarvestStatsServiceTest {

    private val orgId = 1L
    private val otherOrgId = 2L
    private val seasonId = 100L

    // ── AnalyticsService.getHarvestSummary ──────────────────────────────────

    private fun analytics(
        seasonRepo: SeasonRepository = mock(),
        plantEvents: PlantEventRepository = mock(),
    ) = AnalyticsService(mock<AgroalDataSource>(), mock<SpeciesRepository>(), seasonRepo, plantEvents)

    @Test
    fun `harvest summary totals stems, picks best week, and reads prior year`() {
        val seasonRepo = mock<SeasonRepository>()
        val plantEvents = mock<PlantEventRepository>()
        whenever(seasonRepo.findById(seasonId)).thenReturn(Season(id = seasonId, orgId = orgId, name = "2025", year = 2025))
        whenever(plantEvents.harvestWeeklyBucketsBySeason(orgId, seasonId)).thenReturn(
            listOf(HarvestWeekBucket(20, 7), HarvestWeekBucket(32, 15), HarvestWeekBucket(33, 4)),
        )
        whenever(plantEvents.totalStemsByOrgYear(orgId, 2024)).thenReturn(21)

        val result = analytics(seasonRepo, plantEvents).getHarvestSummary(orgId, seasonId)

        assertEquals(26, result.totalStems)
        assertEquals(32, result.bestWeek?.isoWeek)
        assertEquals(15, result.bestWeek?.stems)
        assertEquals(21, result.prevYearTotalStems)
        verify(plantEvents).totalStemsByOrgYear(orgId, 2024)
    }

    @Test
    fun `harvest summary with no harvests is empty, not null`() {
        val seasonRepo = mock<SeasonRepository>()
        val plantEvents = mock<PlantEventRepository>()
        whenever(seasonRepo.findById(seasonId)).thenReturn(Season(id = seasonId, orgId = orgId, name = "2025", year = 2025))
        whenever(plantEvents.harvestWeeklyBucketsBySeason(orgId, seasonId)).thenReturn(emptyList())
        whenever(plantEvents.totalStemsByOrgYear(orgId, 2024)).thenReturn(0)

        val result = analytics(seasonRepo, plantEvents).getHarvestSummary(orgId, seasonId)

        assertEquals(0, result.totalStems)
        assertNull(result.bestWeek)
        assertEquals(0, result.prevYearTotalStems)
    }

    @Test
    fun `harvest summary 404s when season is missing`() {
        val seasonRepo = mock<SeasonRepository>()
        whenever(seasonRepo.findById(seasonId)).thenReturn(null)
        assertThrows<NotFoundException> { analytics(seasonRepo = seasonRepo).getHarvestSummary(orgId, seasonId) }
    }

    @Test
    fun `harvest summary 404s when season belongs to another org`() {
        val seasonRepo = mock<SeasonRepository>()
        val plantEvents = mock<PlantEventRepository>()
        whenever(seasonRepo.findById(seasonId)).thenReturn(Season(id = seasonId, orgId = otherOrgId, name = "2025", year = 2025))

        assertThrows<NotFoundException> { analytics(seasonRepo, plantEvents).getHarvestSummary(orgId, seasonId) }
        verify(plantEvents, never()).harvestWeeklyBucketsBySeason(any(), any())
    }

    // ── BedService.getBedHarvestStats ───────────────────────────────────────

    private fun bedService(
        bedRepo: BedRepository = mock(),
        gardenRepo: GardenRepository = mock(),
        plantEvents: PlantEventRepository = mock(),
    ) = BedService(bedRepo, gardenRepo, mock<BedPhotoRepository>(), mock<StorageService>(), plantEvents, mock<AgroalDataSource>())

    @Test
    fun `bed harvest stats returns total when bed is owned`() {
        val bedRepo = mock<BedRepository>()
        val gardenRepo = mock<GardenRepository>()
        val plantEvents = mock<PlantEventRepository>()
        whenever(bedRepo.findById(5L)).thenReturn(Bed(id = 5L, name = "A", gardenId = 9L))
        whenever(gardenRepo.findById(9L)).thenReturn(Garden(id = 9L, name = "G", orgId = orgId))
        whenever(plantEvents.totalStemsByBed(5L, seasonId)).thenReturn(42)

        val result = bedService(bedRepo, gardenRepo, plantEvents).getBedHarvestStats(5L, orgId, seasonId)

        assertEquals(42, result.totalStems)
        verify(plantEvents).totalStemsByBed(eq(5L), eq(seasonId))
    }

    @Test
    fun `bed harvest stats 404s when bed belongs to another org`() {
        val bedRepo = mock<BedRepository>()
        val gardenRepo = mock<GardenRepository>()
        val plantEvents = mock<PlantEventRepository>()
        whenever(bedRepo.findById(5L)).thenReturn(Bed(id = 5L, name = "A", gardenId = 9L))
        whenever(gardenRepo.findById(9L)).thenReturn(Garden(id = 9L, name = "G", orgId = otherOrgId))

        assertThrows<NotFoundException> { bedService(bedRepo, gardenRepo, plantEvents).getBedHarvestStats(5L, orgId, null) }
        verify(plantEvents, never()).totalStemsByBed(any(), any())
    }

    @Test
    fun `bed harvest stats 404s when bed is missing`() {
        val bedRepo = mock<BedRepository>()
        whenever(bedRepo.findById(5L)).thenReturn(null)
        assertThrows<NotFoundException> { bedService(bedRepo = bedRepo).getBedHarvestStats(5L, orgId, null) }
    }

    // ── GardenService.getGardenHarvestStats ─────────────────────────────────

    private fun gardenService(
        gardenRepo: GardenRepository = mock(),
        plantEvents: PlantEventRepository = mock(),
    ) = GardenService(gardenRepo, mock<BedRepository>(), mock<AiService>(), mock<WeatherIngestionService>(), mock<DailyWeatherRepository>(), plantEvents)

    @Test
    fun `garden harvest stats returns total when garden is owned`() {
        val gardenRepo = mock<GardenRepository>()
        val plantEvents = mock<PlantEventRepository>()
        whenever(gardenRepo.findById(9L)).thenReturn(Garden(id = 9L, name = "G", orgId = orgId))
        whenever(plantEvents.totalStemsByGarden(9L, null)).thenReturn(123)

        val result = gardenService(gardenRepo, plantEvents).getGardenHarvestStats(9L, orgId, null)

        assertEquals(123, result.totalStems)
    }

    @Test
    fun `garden harvest stats 404s when garden belongs to another org`() {
        val gardenRepo = mock<GardenRepository>()
        val plantEvents = mock<PlantEventRepository>()
        whenever(gardenRepo.findById(9L)).thenReturn(Garden(id = 9L, name = "G", orgId = otherOrgId))

        assertThrows<NotFoundException> { gardenService(gardenRepo, plantEvents).getGardenHarvestStats(9L, orgId, seasonId) }
        verify(plantEvents, never()).totalStemsByGarden(any(), any())
    }
}
