package app.verdant.repository

import app.verdant.entity.Garden
import app.verdant.entity.GardenArea
import app.verdant.entity.GardenAreaCategory
import app.verdant.entity.MaintenanceActivity
import app.verdant.entity.MaintenanceRule
import app.verdant.entity.MaintenanceTarget
import app.verdant.entity.Organization
import app.verdant.entity.ScheduledTask
import app.verdant.entity.ScheduledTaskStatus
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@QuarkusTest
class MaintenanceRuleRepositoryTest {

    @Inject lateinit var rules: MaintenanceRuleRepository
    @Inject lateinit var areas: GardenAreaRepository
    @Inject lateinit var gardens: GardenRepository
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var tasks: ScheduledTaskRepository

    private var orgId: Long = 0
    private var areaId: Long = 0

    @BeforeEach
    fun setUp() {
        orgId = orgs.persist(Organization(name = "Rule test org")).id!!
        val gardenId = gardens.persist(Garden(name = "Rule garden", orgId = orgId)).id!!
        areaId = areas.persist(
            GardenArea(gardenId = gardenId, name = "Gång", category = GardenAreaCategory.WALKWAY)
        ).id!!
    }

    private fun areaRule(
        activity: MaintenanceActivity = MaintenanceActivity.WEED,
        intervalDays: Int = 21,
        active: Boolean = true,
    ) = MaintenanceRule(
        orgId = orgId,
        gardenAreaId = areaId,
        activity = activity,
        intervalDays = intervalDays,
        anchorDate = LocalDate.of(2026, 5, 1),
        seasonStartMonth = 4, seasonStartDay = 1,
        seasonEndMonth = 10, seasonEndDay = 15,
        active = active,
    )

    @Test
    fun `persist round-trips every field`() {
        val saved = rules.persist(areaRule())
        val found = rules.findById(saved.id!!)!!

        assertEquals(areaId, found.gardenAreaId)
        assertNull(found.bedId)
        assertEquals(MaintenanceActivity.WEED, found.activity)
        assertEquals(21, found.intervalDays)
        assertEquals(LocalDate.of(2026, 5, 1), found.anchorDate)
        assertEquals(4, found.seasonStartMonth)
        assertEquals(15, found.seasonEndDay)
        assertTrue(found.active)
        assertEquals(MaintenanceTarget.GARDEN_AREA, found.target)
    }

    @Test
    fun `a rule with no season window persists nulls`() {
        val saved = rules.persist(
            areaRule().copy(
                seasonStartMonth = null, seasonStartDay = null,
                seasonEndMonth = null, seasonEndDay = null,
            )
        )
        val found = rules.findById(saved.id!!)!!
        assertNull(found.seasonStartMonth)
        assertNull(found.seasonEndDay)
    }

    @Test
    fun `a rule with neither target is rejected by the check constraint`() {
        assertThrows(Exception::class.java) {
            rules.persist(areaRule().copy(gardenAreaId = null))
        }
    }

    @Test
    fun `findByAreaId returns that area's rules`() {
        rules.persist(areaRule(MaintenanceActivity.WEED))
        rules.persist(areaRule(MaintenanceActivity.MOW))
        assertEquals(2, rules.findByAreaId(areaId).size)
    }

    @Test
    fun `findActiveWithoutOpenTask skips inactive rules`() {
        val inactive = rules.persist(areaRule(active = false))
        val active = rules.persist(areaRule(activity = MaintenanceActivity.MOW))

        val found = rules.findActiveWithoutOpenTask().map { it.id }
        assertTrue(active.id in found)
        assertTrue(inactive.id !in found)
    }

    @Test
    fun `findActiveWithoutOpenTask skips a rule that already has a pending task`() {
        val rule = rules.persist(areaRule())
        assertTrue(rules.findActiveWithoutOpenTask().any { it.id == rule.id })

        tasks.persist(
            ScheduledTask(
                orgId = orgId,
                gardenAreaId = areaId,
                maintenanceRuleId = rule.id,
                activityType = "WEED",
                earliestDate = LocalDate.of(2026, 6, 1),
                deadline = LocalDate.of(2026, 6, 1),
                targetCount = 1,
                remainingCount = 1,
                status = ScheduledTaskStatus.PENDING,
            )
        )

        assertTrue(rules.findActiveWithoutOpenTask().none { it.id == rule.id })
    }

    @Test
    fun `a second pending task for the same rule violates the unique index`() {
        val rule = rules.persist(areaRule())
        val task = ScheduledTask(
            orgId = orgId,
            gardenAreaId = areaId,
            maintenanceRuleId = rule.id,
            activityType = "WEED",
            earliestDate = LocalDate.of(2026, 6, 1),
            deadline = LocalDate.of(2026, 6, 1),
            targetCount = 1,
            remainingCount = 1,
        )
        tasks.persist(task)
        assertThrows(Exception::class.java) { tasks.persist(task) }
    }

    @Test
    fun `deleting an area cascades to its rules`() {
        val rule = rules.persist(areaRule())
        areas.delete(areaId)
        assertNull(rules.findById(rule.id!!))
    }
}
