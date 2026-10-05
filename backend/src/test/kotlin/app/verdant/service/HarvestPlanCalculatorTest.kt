package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.UnitType
import jakarta.ws.rs.BadRequestException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate

class HarvestPlanCalculatorTest {
    private val calculator = HarvestPlanCalculator()
    private val today = LocalDate.of(2027, 1, 1)
    private val target = HarvestPlanRequest(1, groupId = 1, quantity = 301, sellableUnit = SellableUnit.STEM, harvestDate = LocalDate.of(2027, 7, 1))
    private fun species(id: Long, unit: SellableUnit = SellableUnit.STEM) = PlanningSpecies(id, "Dahlia $id", profile().copy(sellableUnit = unit), true)

    @Test fun `default shares are output shares with stable integer remainders`() {
        val result = calculator.calculate(target, listOf(species(3), species(1), species(2)), today)
        assertTrue(result.canSave)
        assertEquals(listOf(101, 100, 100), result.allocations.map { it.outputQuantity })
        assertEquals(listOf(1L, 2L, 3L), result.allocations.map { it.speciesId })
        assertEquals(51, result.allocations.first().plantsNeeded)
        assertEquals(64, result.allocations.first().startingQuantity)
        assertEquals(target.harvestDate.minusDays(120), result.allocations.first().steps.first().date)
        assertEquals(listOf("SEED", "SEED", "PLANT", "STEM"), result.allocations.first().steps.map { it.unit })
        assertEquals(listOf(64, 64, 51, 101), result.allocations.first().steps.map { it.quantity })
    }

    @Test fun `pinned outputs and excluded species are preserved`() {
        val result = calculator.calculate(target.copy(allocations = mapOf(1L to 75, 2L to 0)), listOf(species(1), species(2), species(3)), today)
        assertEquals(listOf(75, 226), result.allocations.map { it.outputQuantity })
        assertTrue(result.canSave)
    }

    @Test fun `incompatible and incomplete profiles are excluded from suggestion`() {
        val incomplete = species(3).let { it.copy(profile = it.profile.copy(outputPerPlant = null)) }
        val result = calculator.calculate(target, listOf(species(1), species(2, SellableUnit.FLOWER), incomplete), today)
        assertEquals(301, result.allocations.single().outputQuantity)
        assertTrue(result.canSave)
        assertEquals(setOf(2L, 3L), result.issues.map { it.speciesId }.toSet())
    }

    @Test fun `infeasible pinned output is not silently reallocated`() {
        val result = calculator.calculate(target.copy(allocations = mapOf(2L to 50)), listOf(species(1), species(2, SellableUnit.FLOWER)), today)
        assertFalse(result.canSave)
        assertEquals(50, result.unallocatedQuantity)
        assertEquals(251, result.allocations.single().outputQuantity)
    }

    @Test fun `past purchase dates prevent saving`() {
        val result = calculator.calculate(target.copy(harvestDate = today.plusDays(119)), listOf(species(1)), today)
        assertFalse(result.canSave)
        assertTrue(result.issues.any { it.message.contains("past") })
        assertTrue(calculator.calculate(target.copy(harvestDate = today.plusDays(120)), listOf(species(1)), today).canSave)
    }

    @Test fun `empty groups and insufficient fixed shares remain unallocated`() {
        assertEquals(301, calculator.calculate(target, emptyList(), today).unallocatedQuantity)
        assertEquals(300, calculator.calculate(target.copy(allocations = mapOf(1L to 1)), listOf(species(1)), today).unallocatedQuantity)
    }

    @Test fun `invalid and overflowing quantities are rejected`() {
        for (allocations in listOf(mapOf(9L to 1), mapOf(1L to -1), mapOf(1L to Int.MAX_VALUE, 2L to Int.MAX_VALUE))) {
            assertThrows<BadRequestException> { calculator.calculate(target.copy(allocations = allocations), listOf(species(1), species(2)), today) }
        }
        assertThrows<BadRequestException> { calculator.calculate(target.copy(quantity = 0), listOf(species(1)), today) }
        val enormous = species(1).let { it.copy(profile = it.profile.copy(outputPerPlant = BigDecimal("0.01"), establishmentPercent = BigDecimal("0.01"))) }
        assertFalse(calculator.calculate(target.copy(quantity = 1_000_000), listOf(enormous), today).canSave)
    }

    @Test fun `profile requires coherent purchase propagation harvest sequence`() {
        assertTrue(calculator.profileIssues(profile()).isEmpty())
        assertTrue(calculator.profileIssues(profile().copy(steps = profile().steps.reversed())).isNotEmpty())
        assertTrue(calculator.profileIssues(profile().copy(startingUnit = UnitType.TUBER)).isNotEmpty())
        assertTrue(calculator.profileIssues(profile().copy(steps = profile().steps.drop(1))).isNotEmpty())
        assertTrue(calculator.profileIssues(profile().copy(establishmentPercent = BigDecimal.ZERO)).isNotEmpty())
    }

    @Test fun `flower plans retain flower quantities`() {
        val result = calculator.calculate(target.copy(sellableUnit = SellableUnit.FLOWER), listOf(species(1, SellableUnit.FLOWER)), today)
        assertEquals("FLOWER", result.allocations.single().steps.last().unit)
    }

    companion object {
        fun profile() = ProductionProfile(outputPerPlant = BigDecimal(2), establishmentPercent = BigDecimal(80), steps = listOf(
            LifecycleStep("purchase", "Purchase", "PURCHASE", 120, PlanningQuantityBasis.START),
            LifecycleStep("sow", "Sow", "SOW", 100, PlanningQuantityBasis.START),
            LifecycleStep("plant", "Plant out", "PLANT", 60, PlanningQuantityBasis.PLANT),
            LifecycleStep("harvest", "Harvest", "HARVEST", 0, PlanningQuantityBasis.OUTPUT),
        ))
    }
}
