package app.verdant.repository

import app.verdant.dto.*
import app.verdant.entity.*
import app.verdant.service.HarvestPlanCalculatorTest.Companion.profile
import app.verdant.service.HarvestPlanService
import app.verdant.service.ScheduledTaskService
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ClientErrorException
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.Callable

@QuarkusTest
class HarvestPlanRepositoryTest {
    @Inject lateinit var service: HarvestPlanService
    @Inject lateinit var plans: HarvestPlanRepository
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var seasons: SeasonRepository
    @Inject lateinit var species: SpeciesRepository
    @Inject lateinit var groups: SpeciesGroupRepository
    @Inject lateinit var tasks: ScheduledTaskRepository
    @Inject lateinit var taskService: ScheduledTaskService
    @Inject lateinit var workflows: WorkflowRepository
    private var orgId = 0L
    private var otherOrg = 0L
    private lateinit var target: HarvestPlanRequest

    @BeforeEach fun setup() {
        orgId = orgs.persist(Organization(name = "Planning test")).id!!
        otherOrg = orgs.persist(Organization(name = "Other planning org")).id!!
        val season = seasons.persist(Season(orgId = orgId, name = "Next season", year = 2027))
        val sp = species.persist(Species(orgId = orgId, commonName = "Dahlia", daysToHarvestMin = 100))
        target = HarvestPlanRequest(season.id!!, speciesId = sp.id!!, quantity = 301, sellableUnit = SellableUnit.STEM, harvestDate = LocalDate.now().plusDays(365))
        service.saveProfile(orgId, sp.id, profile())
    }

    private fun request(): CreateHarvestPlanRequest {
        val preview = service.preview(orgId, target)
        return CreateHarvestPlanRequest(UUID.randomUUID(), preview.fingerprint, target)
    }

    @Test fun `plan snapshot tasks units and profiles round trip`() {
        val plan = service.create(orgId, request())
        assertEquals(4, plan.tasks.size)
        assertEquals(301, plan.snapshot.allocations.single().outputQuantity)
        assertEquals(plan, service.get(orgId, plan.id))
        assertEquals(profile(), plans.profile(orgId, target.speciesId!!))
        val task = taskService.getTask(plan.tasks.first().id, orgId)
        assertEquals(plan.id, task.harvestPlanId)
        assertEquals("SEED", task.quantityUnit)
        assertEquals(listOf(target.speciesId), task.acceptableSpecies.map { it.speciesId })
    }

    @Test fun `concurrent retries create only one plan and task set`() {
        val request = request()
        Executors.newFixedThreadPool(2).use { pool ->
            val results = pool.invokeAll(listOf(Callable { service.create(orgId, request) }, Callable { service.create(orgId, request) })).map { it.get() }
            assertEquals(results[0].id, results[1].id)
        }
        assertEquals(1, plans.list(orgId, 50, 0).size)
        assertEquals(4, tasks.findByOrgId(orgId).size)
        assertThrows<ClientErrorException> { service.create(orgId, request.copy(target = target.copy(quantity = 302))) }
    }

    @Test fun `stale preview rejects changed assumptions but saved retries still work`() {
        val request = request()
        val plan = service.create(orgId, request)
        service.saveProfile(orgId, target.speciesId!!, profile().copy(outputPerPlant = 3.toBigDecimal()))
        assertThrows<ClientErrorException> { service.create(orgId, request.copy(requestKey = UUID.randomUUID())) }
        assertEquals(plan.id, service.create(orgId, request).id)
        assertEquals(2.toBigDecimal(), plans.get(orgId, plan.id).snapshot.allocations.single().profile.outputPerPlant)
    }

    @Test fun `a failed task insert rolls back the whole plan`() {
        val request = request()
        val preview = service.preview(orgId, target)
        val allocation = preview.allocations.single()
        val invalid = preview.copy(allocations = listOf(allocation.copy(steps = allocation.steps + allocation.steps.first())))
        assertThrows<Exception> { plans.create(orgId, request, invalid) }
        assertTrue(plans.list(orgId, 50, 0).isEmpty())
        assertTrue(tasks.findByOrgId(orgId).isEmpty())
    }

    @Test fun `progress is optimistic and cancellation retains completed and partial work`() {
        val plan = service.create(orgId, request())
        val first = plan.tasks.first()
        val progress = CompleteHarvestPlanTaskRequest(first.remainingCount, 1)
        service.complete(orgId, plan.id, first.id, progress)
        assertThrows<ClientErrorException> { service.complete(orgId, plan.id, first.id, progress) }
        val harvest = plan.tasks.last()
        service.complete(orgId, plan.id, harvest.id, CompleteHarvestPlanTaskRequest(harvest.remainingCount, harvest.remainingCount))
        val cancelled = service.cancel(orgId, plan.id)
        assertEquals("COMPLETED", cancelled.tasks.last().status)
        assertEquals("CANCELLED", cancelled.tasks.first().status)
        assertEquals(first.remainingCount - 1, cancelled.tasks.first().remainingCount)
        assertEquals(ScheduledTaskStatus.CANCELLED, tasks.findById(first.id)!!.status)
        assertThrows<ClientErrorException> { service.complete(orgId, plan.id, first.id, CompleteHarvestPlanTaskRequest(first.remainingCount - 1, 1)) }
    }

    @Test fun `all plan access and referenced entities are organization scoped`() {
        val plan = service.create(orgId, request())
        assertTrue(service.list(otherOrg, 50, 0).isEmpty())
        assertThrows<NotFoundException> { service.get(otherOrg, plan.id) }
        assertThrows<NotFoundException> { service.cancel(otherOrg, plan.id) }
        assertThrows<NotFoundException> { service.complete(otherOrg, plan.id, plan.tasks.first().id, CompleteHarvestPlanTaskRequest(1, 1)) }
        assertThrows<NotFoundException> { service.preview(otherOrg, target) }
        assertThrows<NotFoundException> { service.saveProfile(otherOrg, target.speciesId!!, profile()) }
        val foreignSpecies = species.persist(Species(orgId = otherOrg, commonName = "Private"))
        assertThrows<NotFoundException> { service.preview(orgId, target.copy(speciesId = foreignSpecies.id)) }
        val group = groups.persist(SpeciesGroup(orgId = otherOrg, name = "Private group"))
        assertThrows<NotFoundException> { service.preview(orgId, target.copy(speciesId = null, groupId = group.id)) }
    }

    @Test fun `system species settings are private to each organization`() {
        val system = species.persist(Species(commonName = "Shared", expectedStemsPerPlant = 50))
        service.saveProfile(orgId, system.id!!, profile())
        assertNull(service.candidates(otherOrg, system.id, null).single().profile.outputPerPlant)
        assertFalse(service.candidates(otherOrg, system.id, null).single().customized)
        assertEquals(profile(), service.candidates(orgId, system.id, null).single().profile)
    }

    @Test fun `workflow defaults import cumulative timing and event activity names`() {
        val sp = species.persist(Species(orgId = orgId, commonName = "Workflow dahlia", germinationRate = 85))
        workflows.persistSpeciesStep(SpeciesWorkflowStep(speciesId = sp.id!!, name = "Sow", eventType = "SEEDED", sortOrder = 0))
        workflows.persistSpeciesStep(SpeciesWorkflowStep(speciesId = sp.id, name = "Pot up", eventType = "POTTED_UP", daysAfterPrevious = 20, sortOrder = 1))
        workflows.persistSpeciesStep(SpeciesWorkflowStep(speciesId = sp.id, name = "Harvest", eventType = "HARVESTED", daysAfterPrevious = 80, sortOrder = 2))
        val profile = service.candidates(orgId, sp.id, null).single().profile
        assertEquals(listOf("PURCHASE", "SOW", "POT_UP", "HARVEST"), profile.steps.map { it.activityType })
        assertEquals(listOf(null, 100, 80, 0), profile.steps.map { it.daysBeforeHarvest })
        assertEquals(85.toBigDecimal(), profile.establishmentPercent)
        assertNull(profile.outputPerPlant)
    }

    @Test fun `linked tasks reject legacy mutations and invalid completion amounts`() {
        val plan = service.create(orgId, request())
        val task = plan.tasks.first()
        assertThrows<BadRequestException> { taskService.deleteTask(task.id, orgId) }
        assertThrows<BadRequestException> { taskService.updateTask(task.id, UpdateScheduledTaskRequest(targetCount = 5), orgId) }
        assertThrows<BadRequestException> { taskService.completePartially(task.id, target.speciesId, 1, orgId) }
        assertThrows<BadRequestException> { service.complete(orgId, plan.id, task.id, CompleteHarvestPlanTaskRequest(task.remainingCount, 0)) }
        assertThrows<BadRequestException> { service.complete(orgId, plan.id, task.id, CompleteHarvestPlanTaskRequest(task.remainingCount, task.remainingCount + 1)) }
    }
}
