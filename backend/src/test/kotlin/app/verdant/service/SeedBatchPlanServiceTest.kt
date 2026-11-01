package app.verdant.service

import app.verdant.dto.SeedBatchPlanRequest
import app.verdant.entity.Organization
import app.verdant.entity.SeedInventory
import app.verdant.entity.Species
import app.verdant.entity.SpeciesWorkflowStep
import app.verdant.repository.OrganizationRepository
import app.verdant.repository.ScheduledTaskRepository
import app.verdant.repository.SeedInventoryRepository
import app.verdant.repository.SpeciesRepository
import app.verdant.repository.WorkflowRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

@QuarkusTest
class SeedBatchPlanServiceTest {

    @Inject lateinit var service: SeedBatchPlanService
    @Inject lateinit var tasks: ScheduledTaskRepository
    @Inject lateinit var inventory: SeedInventoryRepository
    @Inject lateinit var workflows: WorkflowRepository
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var species: SpeciesRepository

    private val today = LocalDate.of(2026, 11, 1)
    private val ready = LocalDate.of(2027, 5, 1)

    private data class Fixture(val orgId: Long, val batchId: Long)

    /** The dahlia workflow: 120 days from sowing to a sellable plant in flower, hardening optional. */
    private fun fixture(quantity: Int = 250, steps: List<Triple<String?, Int?, Boolean>> = DAHLIA): Fixture {
        val orgId = orgs.persist(Organization(name = "Seed plan org")).id!!
        val speciesId = species.persist(Species(commonName = "Seed plan dahlia", orgId = orgId)).id!!
        steps.forEachIndexed { i, (event, days, optional) ->
            workflows.persistSpeciesStep(SpeciesWorkflowStep(speciesId = speciesId, name = "Step $i", eventType = event,
                daysAfterPrevious = days, isOptional = optional, sortOrder = i))
        }
        workflows.persistSpeciesStep(SpeciesWorkflowStep(speciesId = speciesId, name = "Side branch", eventType = "NOTE",
            daysAfterPrevious = 400, isSideBranch = true, sortOrder = 99))
        val batchId = inventory.persist(SeedInventory(orgId = orgId, speciesId = speciesId, quantity = quantity)).id!!
        return Fixture(orgId, batchId)
    }

    private fun request(seeds: Int = 200, plants: Int = 150) = SeedBatchPlanRequest(seeds, plants, ready)

    @Test
    fun `counts back through the workflow from the ready date`() {
        val f = fixture()
        val plan = service.preview(f.batchId, request(), f.orgId)

        assertEquals(
            listOf("2027-01-01", "2027-01-21", "2027-02-15", "2027-02-22", "2027-04-01", "2027-04-13", "2027-04-21", "2027-05-01"),
            plan.steps.map { it.date.toString() },
        )
        assertEquals(listOf("SOW", "POT_UP", "POT_UP", "PINCH", "INSPECT", "INSPECT", "INSPECT", "INSPECT"), plan.steps.map { it.activityType })
        assertEquals(listOf(200, 150, 150, 150, 150, 150, 150, 150), plan.steps.map { it.count })
        assertTrue(plan.taskIds.isEmpty())
    }

    @Test
    fun `creates a dated task per required step, the sowing one tied to the batch`() {
        val f = fixture()
        val plan = service.create(f.batchId, request(), f.orgId, today)

        val created = plan.taskIds.map { tasks.findById(it)!! }
        assertEquals(7, created.size, "the optional hardening step gets no task")
        assertEquals(listOf("Step 0", "Step 1", "Step 2", "Step 3", "Step 4", "Step 6", "Step 7"), created.map { it.notes })
        val sow = created.first()
        assertEquals("SOW", sow.activityType)
        assertEquals(LocalDate.of(2027, 1, 1), sow.deadline)
        assertEquals(200, sow.targetCount)
        assertTrue(created.all { it.seedInventoryId == f.batchId && it.orgId == f.orgId })
        assertEquals(150, created.last().remainingCount)
    }

    @Test
    fun `refuses plans the batch or the workflow cannot carry`() {
        val f = fixture(quantity = 100)
        assertThrows<BadRequestException> { service.preview(f.batchId, request(seeds = 150, plants = 100), f.orgId) }
        assertThrows<BadRequestException> { service.preview(f.batchId, request(seeds = 80, plants = 90), f.orgId) }
        assertThrows<BadRequestException> {
            service.create(f.batchId, request(seeds = 80, plants = 60), f.orgId, today = LocalDate.of(2027, 1, 2))
        }

        val gap = fixture(steps = listOf(Triple("SEEDED", 0, false), Triple("POTTED_UP", null, false)))
        assertThrows<BadRequestException> { service.preview(gap.batchId, request(), gap.orgId) }

        val other = fixture()
        assertThrows<NotFoundException> { service.preview(f.batchId, request(seeds = 80, plants = 60), other.orgId) }
    }

    companion object {
        val DAHLIA = listOf(
            Triple("SEEDED", 0, false), Triple("POTTED_UP", 20, false), Triple("POTTED_UP", 25, false), Triple("PINCHED", 7, false),
            Triple("BUDDING", 38, false), Triple("MOVED", 12, true), Triple("FIRST_BLOOM", 8, false), Triple(null, 10, false),
        )
    }
}
