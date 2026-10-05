package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.*
import app.verdant.repository.*
import io.quarkus.test.TestTransaction
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ClientErrorException
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

@QuarkusTest
class AdminPlanningServiceTest {
    @Inject lateinit var service: AdminPlanningService
    @Inject lateinit var defaults: PlanningDefaults
    @Inject lateinit var species: SpeciesRepository
    @Inject lateinit var groups: SpeciesGroupRepository
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var plans: HarvestPlanRepository
    @Inject lateinit var harvest: HarvestPlanService
    private fun key() = "test-${UUID.randomUUID()}"
    private fun template() = service.schedule("zinnia").copy(key = key(), revision = 0, autoMatch = false, useSpeciesTiming = false)
    private fun plant() = species.persist(Species(commonName = key(), scientificName = "Zinnia elegans"))

    @Test @TestTransaction fun `migration preserves all seeded schedules and their validation`() {
        val schedules = service.listSchedules()
        assertEquals(45, schedules.size)
        assertTrue(schedules.all { it.revision == 1 && it.autoMatch && it.useSpeciesTiming })
        schedules.forEach { assertTrue(HarvestPlanCalculator().profileIssues(it.profile, it.reviewRequired).isEmpty(), it.key) }
    }

    @Test @TestTransaction fun `shared schedules can be created edited assigned reset and deleted without changing organization profiles`() {
        val sp = plant()
        val org = orgs.persist(Organization(name = key()))
        val ownProfile = HarvestPlanCalculatorTest.profile()
        plans.saveProfile(org.id!!, sp.id!!, ownProfile)
        val created = service.createSchedule(template())
        assertEquals(1, created.revision)
        service.assign(ScheduleAssignmentRequest(created.key, setOf(sp.id)))
        assertEquals(created.key, defaults.suggest(sp).info.key)
        val updated = service.updateSchedule(created.key, created.copy(profile = created.profile.copy(outputPerPlant = BigDecimal(3))))
        assertEquals(2, updated.revision)
        assertEquals(BigDecimal(3), defaults.suggest(sp).profile.outputPerPlant)
        assertEquals(ownProfile, harvest.candidates(org.id, sp.id, null).single().profile)
        assertEquals(409, assertThrows(ClientErrorException::class.java) { service.updateSchedule(created.key, created) }.response.status)
        assertThrows(ClientErrorException::class.java) { service.deleteSchedule(created.key, updated.revision) }
        service.assign(ScheduleAssignmentRequest(null, setOf(sp.id)))
        assertEquals("zinnia", defaults.suggest(sp).info.key)
        service.deleteSchedule(created.key, updated.revision)
        assertThrows(NotFoundException::class.java) { service.schedule(created.key) }
    }

    @Test @TestTransaction fun `automatic priority edits change future suggestions and deletion falls back safely`() {
        val sp = plant()
        val original = service.createSchedule(template().copy(autoMatch = true, priority = 0))
        assertEquals(original.key, defaults.suggest(sp).info.key)
        val later = service.updateSchedule(original.key, original.copy(priority = 9999))
        assertEquals("zinnia", defaults.suggest(sp).info.key)
        service.deleteSchedule(later.key, later.revision)
        val empty = defaults.suggest(sp, SharedScheduleCatalogue(emptyList(), emptyMap()))
        assertEquals("unassigned", empty.info.key)
        assertTrue(empty.info.reviewRequired)
        assertTrue(HarvestPlanCalculator().profileIssues(empty.profile).isNotEmpty())
    }

    @Test @TestTransaction fun `review drafts allow unknown times but still validate structure and sources`() {
        val base = template()
        val draft = base.copy(reviewRequired = true, profile = base.profile.copy(steps = base.profile.steps.map {
            if (it.activityType == "HARVEST") it else it.copy(daysBeforeHarvest = null)
        }))
        service.createSchedule(draft)
        assertThrows(BadRequestException::class.java) { service.createSchedule(draft.copy(key = key(), reviewRequired = false)) }
        assertThrows(BadRequestException::class.java) { service.createSchedule(base.copy(profile = base.profile.copy(steps = base.profile.steps.reversed()))) }
        assertThrows(BadRequestException::class.java) { service.createSchedule(base.copy(harvestMonths = listOf(13))) }
        assertThrows(BadRequestException::class.java) { service.createSchedule(base.copy(sources = listOf(PlanningSource("Bad", "javascript:alert(1)")))) }
        assertThrows(BadRequestException::class.java) { service.createSchedule(base.copy(profile = base.profile.copy(steps = base.profile.steps.map { it.copy(key = "duplicate") }))) }
    }

    @Test @TestTransaction fun `incompatible material and private species are rejected before any batch assignment`() {
        val shared = plant()
        val org = orgs.persist(Organization(name = key()))
        val private = species.persist(Species(orgId = org.id, commonName = key()))
        val tuber = species.persist(Species(commonName = key(), defaultUnitType = UnitType.TUBER))
        val schedule = service.createSchedule(template())
        assertThrows(NotFoundException::class.java) { service.assign(ScheduleAssignmentRequest(schedule.key, setOf(shared.id!!, private.id!!))) }
        assertNull(defaults.catalogue().assignments[shared.id])
        assertThrows(BadRequestException::class.java) { service.assign(ScheduleAssignmentRequest(schedule.key, setOf(shared.id!!, tuber.id!!))) }
        assertNull(defaults.catalogue().assignments[shared.id])
        service.assign(ScheduleAssignmentRequest(schedule.key, setOf(shared.id!!)))
        val mismatch = schedule.copy(startingUnits = listOf(UnitType.TUBER), profile = schedule.profile.copy(startingUnit = UnitType.TUBER,
            steps = schedule.profile.steps.map { if (it.activityType == "SOW") it.copy(activityType = "PLANT") else it }))
        assertThrows(BadRequestException::class.java) { service.updateSchedule(schedule.key, mismatch) }
    }

    @Test @TestTransaction fun `shared group management is scoped and deleting a group keeps species`() {
        val sp = plant()
        val org = orgs.persist(Organization(name = key()))
        val private = groups.persist(SpeciesGroup(orgId = org.id, name = key()))
        val group = service.saveGroup(null, "  ${key()}  ")
        assertEquals(group.name.trim(), group.name)
        service.addMembers(group.id, GroupMembersRequest(setOf(sp.id!!)))
        service.addMembers(group.id, GroupMembersRequest(setOf(sp.id)))
        assertEquals(listOf(sp.id), service.listGroups().single { it.id == group.id }.speciesIds)
        assertThrows(ClientErrorException::class.java) { service.saveGroup(null, group.name.uppercase()) }
        assertThrows(NotFoundException::class.java) { service.saveGroup(private.id, "Private edit") }
        assertThrows(NotFoundException::class.java) { service.deleteGroup(private.id!!) }
        service.saveGroup(group.id, "Renamed ${key()}")
        service.removeMember(group.id, sp.id)
        assertTrue(service.listGroups().single { it.id == group.id }.speciesIds.isEmpty())
        service.addMembers(group.id, GroupMembersRequest(setOf(sp.id)))
        service.deleteGroup(group.id)
        assertNotNull(species.findById(sp.id))
        assertTrue(groups.findGroupIdsBySpeciesId(sp.id).isEmpty())
    }
}
