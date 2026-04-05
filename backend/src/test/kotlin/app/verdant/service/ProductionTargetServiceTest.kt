package app.verdant.service

import app.verdant.entity.ProductionTarget
import app.verdant.entity.Species
import app.verdant.repository.ProductionTargetRepository
import app.verdant.repository.SpeciesRepository
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate

class ProductionTargetServiceTest {

    private val repo: ProductionTargetRepository = mock()
    private val speciesRepo: SpeciesRepository = mock()
    private val service = ProductionTargetService(repo, speciesRepo)

    private fun makeTarget(
        id: Long = 1L,
        orgId: Long = 10L,
        speciesId: Long = 100L,
        stemsPerWeek: Int = 50,
        startDate: LocalDate = LocalDate.of(2025, 6, 1),
        endDate: LocalDate = LocalDate.of(2025, 8, 31),
    ) = ProductionTarget(
        id = id,
        orgId = orgId,
        seasonId = 1L,
        speciesId = speciesId,
        stemsPerWeek = stemsPerWeek,
        startDate = startDate,
        endDate = endDate,
        createdAt = Instant.now(),
    )

    private fun makeSpecies(
        id: Long = 100L,
        commonName: String = "Sunflower",
        germinationRate: Int? = 75,
        expectedStemsPerPlant: Int? = 5,
        daysToHarvestMin: Int? = 60,
    ) = Species(
        id = id,
        commonName = commonName,
        germinationRate = germinationRate,
        expectedStemsPerPlant = expectedStemsPerPlant,
        daysToHarvestMin = daysToHarvestMin,
    )

    // ── calculateRequirements with complete species data ───────────────────────

    @Test
    fun `calculateRequirements returns correct forecast when all species data is present`() {
        // 13 weeks between 2025-06-01 and 2025-08-31
        val target = makeTarget(
            stemsPerWeek = 50,
            startDate = LocalDate.of(2025, 6, 1),
            endDate = LocalDate.of(2025, 8, 31),
        )
        val species = makeSpecies(
            germinationRate = 75,
            expectedStemsPerPlant = 5,
            daysToHarvestMin = 60,
        )

        whenever(repo.findById(1L)).thenReturn(target)
        whenever(speciesRepo.findById(100L)).thenReturn(species)

        val result = service.calculateRequirements(id = 1L, orgId = 10L)

        // 13 weeks * 50 stems/week = 650 total stems
        assertEquals(13L, result.totalWeeks)
        assertEquals(650L, result.totalStemsNeeded)

        // ceil(650 / 5) = 130 plants
        assertEquals(5, result.stemsPerPlant)
        assertEquals(130L, result.plantsNeeded)

        // germination rate 75%, seeds = ceil(130 / 0.75) = ceil(173.33) = 174
        assertEquals(75, result.germinationRate)
        assertEquals(174L, result.seedsNeeded)

        // suggested sow date = startDate - 60 days = 2025-04-02
        assertEquals(60, result.daysToHarvest)
        assertEquals(LocalDate.of(2025, 4, 2), result.suggestedSowDate)

        assertEquals("Sunflower", result.speciesName)
        assertTrue(result.warnings.isEmpty())
    }

    // ── calculateRequirements with missing germination rate ────────────────────

    @Test
    fun `calculateRequirements uses default germination rate and adds warning when germinationRate is null`() {
        val target = makeTarget(stemsPerWeek = 100)
        val species = makeSpecies(
            germinationRate = null,    // missing
            expectedStemsPerPlant = 10,
            daysToHarvestMin = 90,
        )

        whenever(repo.findById(1L)).thenReturn(target)
        whenever(speciesRepo.findById(100L)).thenReturn(species)

        val result = service.calculateRequirements(id = 1L, orgId = 10L)

        // Default germination rate is 80%
        assertEquals(80, result.germinationRate)
        assertTrue(result.warnings.any { it.contains("germinationRate") || it.contains("80%") },
            "Expected a warning about missing germinationRate, got: ${result.warnings}")
    }

    // ── calculateRequirements with missing daysToHarvestMin ─────────────────────

    @Test
    fun `calculateRequirements uses default daysToHarvest and adds warning when daysToHarvestMin is null`() {
        val startDate = LocalDate.of(2025, 7, 1)
        val target = makeTarget(startDate = startDate)
        val species = makeSpecies(
            germinationRate = 80,
            expectedStemsPerPlant = 5,
            daysToHarvestMin = null,   // missing
        )

        whenever(repo.findById(1L)).thenReturn(target)
        whenever(speciesRepo.findById(100L)).thenReturn(species)

        val result = service.calculateRequirements(id = 1L, orgId = 10L)

        // Default days to harvest is 90
        assertEquals(90, result.daysToHarvest)
        assertEquals(startDate.minusDays(90), result.suggestedSowDate)
        assertTrue(result.warnings.any { it.contains("daysToHarvest") || it.contains("90 days") },
            "Expected a warning about missing daysToHarvest, got: ${result.warnings}")
    }

    // ── calculateRequirements seeds needed rounds up ───────────────────────────

    @Test
    fun `calculateRequirements rounds seeds needed up to nearest whole seed`() {
        // Design a scenario that forces a fractional result.
        // stemsPerWeek=10, 4 weeks => 40 total stems
        // stemsPerPlant=3 => ceil(40/3) = 14 plants
        // germinationRate=70 => ceil(14 / 0.70) = ceil(20.0) = 20 — exact, try 71%
        // germinationRate=71 => ceil(14 / 0.71) = ceil(19.718...) = 20
        // Use stemsPerPlant=7 => ceil(40/7) = ceil(5.71) = 6 plants
        // germinationRate=70 => ceil(6 / 0.70) = ceil(8.571...) = 9 — fractional, good

        val startDate = LocalDate.of(2025, 5, 1)
        val endDate = startDate.plusWeeks(4)   // exactly 4 weeks
        val target = makeTarget(
            stemsPerWeek = 10,
            startDate = startDate,
            endDate = endDate,
        )
        val species = makeSpecies(
            germinationRate = 70,
            expectedStemsPerPlant = 7,
            daysToHarvestMin = 30,
        )

        whenever(repo.findById(1L)).thenReturn(target)
        whenever(speciesRepo.findById(100L)).thenReturn(species)

        val result = service.calculateRequirements(id = 1L, orgId = 10L)

        // plantsNeeded = ceil(40 / 7) = 6
        assertEquals(6L, result.plantsNeeded)
        // seedsNeeded = ceil(6 / 0.70) = ceil(8.571) = 9
        assertEquals(9L, result.seedsNeeded)
        assertTrue(result.warnings.isEmpty())
    }

    // ── calculateRequirements authorization ───────────────────────────────────

    @Test
    fun `calculateRequirements throws NotFoundException when target does not exist`() {
        whenever(repo.findById(99L)).thenReturn(null)

        assertThrows<NotFoundException> {
            service.calculateRequirements(id = 99L, orgId = 10L)
        }
    }
}
