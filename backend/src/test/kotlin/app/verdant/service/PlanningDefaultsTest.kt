package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.*
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@QuarkusTest
class PlanningDefaultsTest {
    @Inject lateinit var defaults: PlanningDefaults
    @Inject lateinit var mapper: ObjectMapper
    private val calculator = HarvestPlanCalculator()
    private fun plant(genus: String, type: PlantType = PlantType.ANNUAL, unit: UnitType = UnitType.SEED) =
        Species(id = 1, commonName = genus, scientificName = genus, plantType = type, defaultUnitType = unit)

    @Test fun `zinnias have a complete lifecycle with date-specific output`() {
        val result = defaults.suggest(plant("Zinnia elegans"))
        assertEquals("zinnia", result.info.key)
        assertFalse(result.info.reviewRequired)
        assertEquals(BigDecimal.ONE, result.profile.outputPerPlant)
        assertEquals(84, result.profile.steps.first { it.key == "start" }.daysBeforeHarvest)
        assertTrue(calculator.profileIssues(result.profile).isEmpty())
    }

    @Test fun `dahlia tubers and seed use different schedules and quantities`() {
        val tuber = defaults.suggest(plant("Dahlia x pinnata", PlantType.TUBER, UnitType.TUBER))
        val seed = defaults.suggest(plant("Dahlia x pinnata"))
        assertEquals("dahlia-tuber", tuber.info.key)
        assertEquals("dahlia-seed", seed.info.key)
        assertEquals("PLANT", tuber.profile.steps[1].activityType)
        assertEquals("SOW", seed.profile.steps[1].activityType)
        assertEquals(PlanningQuantityBasis.START, tuber.profile.steps[1].quantityBasis)
    }

    @Test fun `perennials and unknown stock do not receive annual flowering promises`() {
        for (sp in listOf(plant("Magnolia kobus", PlantType.PERENNIAL),
            plant("Paeonia lactiflora", PlantType.PERENNIAL, UnitType.PLANT),
            plant("Begonia", PlantType.TUBER, UnitType.TUBER))) {
            val result = defaults.suggest(sp)
            assertTrue(result.info.reviewRequired)
            assertNull(result.profile.steps.first { it.key == "start" }.daysBeforeHarvest)
            assertTrue(calculator.profileIssues(result.profile).isNotEmpty())
        }
    }

    @Test fun `existing harvest duration and germination ceiling are respected`() {
        val result = defaults.suggest(plant("Zinnia elegans").copy(daysToHarvestMin = 100, germinationRate = 50))
        assertEquals(100, result.profile.steps.first { it.key == "start" }.daysBeforeHarvest)
        assertEquals(121, result.profile.steps.first().daysBeforeHarvest)
        assertEquals(0, BigDecimal("45").compareTo(result.profile.establishmentPercent))
        assertTrue(calculator.profileIssues(result.profile).isEmpty())
    }

    @Test fun `summer tulips and unreviewed profiles cannot silently generate tasks`() {
        val sp = plant("Tulipa", PlantType.BULB, UnitType.BULB).copy(bloomMonths = listOf(4, 5))
        val result = defaults.suggest(sp)
        val candidate = PlanningSpecies(1, "Tulip", result.profile, false, result.info)
        val target = HarvestPlanRequest(1, speciesId = 1, quantity = 10, sellableUnit = SellableUnit.STEM, harvestDate = LocalDate.of(2030, 8, 15))
        assertFalse(calculator.calculate(target, listOf(candidate), LocalDate.of(2029, 1, 1)).canSave)
        assertTrue(calculator.calculate(target.copy(harvestDate = LocalDate.of(2030, 5, 15)), listOf(candidate), LocalDate.of(2029, 1, 1)).canSave)
        assertTrue(calculator.calculate(target, listOf(candidate.copy(customized = true)), LocalDate.of(2029, 1, 1)).canSave)
        val draft = candidate.copy(defaultSchedule = result.info.copy(reviewRequired = true))
        assertFalse(calculator.calculate(target.copy(harvestDate = LocalDate.of(2030, 5, 15)), listOf(draft), LocalDate.of(2029, 1, 1)).canSave)
    }

    @Test fun `supplier season conflicts require review instead of unrestricted dates`() {
        val result = defaults.suggest(plant("Zinnia elegans").copy(bloomMonths = listOf(12)))
        assertTrue(result.info.reviewRequired)
    }

    @Test fun `indoor sowing may precede the outdoor planting season`() {
        val result = defaults.suggest(plant("Zinnia elegans"))
        val candidate = PlanningSpecies(1, "Zinnia", result.profile, false, result.info)
        val request = HarvestPlanRequest(1, speciesId = 1, quantity = 10, sellableUnit = SellableUnit.STEM,
            harvestDate = LocalDate.of(2030, 7, 15))
        assertTrue(calculator.calculate(request, listOf(candidate), LocalDate.of(2029, 1, 1)).canSave)
        assertFalse(calculator.calculate(request.copy(harvestDate = LocalDate.of(2030, 6, 15)),
            listOf(candidate), LocalDate.of(2029, 1, 1)).canSave)
    }

    @Test fun `all complete templates satisfy calculator constraints`() {
        val root = javaClass.getResourceAsStream("/planning/defaults.json")!!.use(mapper::readTree)
        assertEquals(45, root["schedules"].size())
        for (rule in root["schedules"]) {
            val profile = mapper.treeToValue(rule["profile"], ProductionProfile::class.java)
            if (profile.steps.all { it.daysBeforeHarvest != null })
                assertTrue(calculator.profileIssues(profile).isEmpty(), rule["key"].asText())
            assertTrue(rule["sourceKeys"].all { root["sources"].has(it.asText()) })
        }
    }

    @Test fun `groups use botanical identity and only documented dahlia forms`() {
        assertEquals(listOf("Dahlior", "Dahlior – pompon"), defaults.groupNames(plant("Dahlia x pinnata").copy(commonNameSv = "Pompondahlia")))
        assertEquals(listOf("Dahlior"), defaults.groupNames(plant("Dahlia").copy(commonNameSv = "Dahlia", variantName = "Wizard of Oz")))
        assertEquals(listOf("Zinnior"), defaults.groupNames(plant("Zinnia marylandica")))
        assertEquals(listOf("Zinnior"), defaults.groupNames(plant("").copy(commonNameSv = "Zinnia")))
    }
}
