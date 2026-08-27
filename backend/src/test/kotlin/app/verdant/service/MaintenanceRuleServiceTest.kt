package app.verdant.service

import app.verdant.dto.CreateMaintenanceRuleRequest
import app.verdant.dto.UpdateMaintenanceRuleRequest
import app.verdant.entity.*
import app.verdant.repository.BedRepository
import app.verdant.repository.GardenRepository
import app.verdant.repository.MaintenanceRuleRepository
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class MaintenanceRuleServiceTest {

    private val rules: MaintenanceRuleRepository = mock()
    private val beds: BedRepository = mock()
    private val gardens: GardenRepository = mock()
    private val areaService: GardenAreaService = mock()
    private val lastDone: LastDoneResolver = mock()
    private val service = MaintenanceRuleService(rules, beds, gardens, areaService, lastDone)

    private val orgId = 10L
    private val bedId = 3L
    private val areaId = 5L
    private val gardenId = 1L

    private val garden = Garden(id = gardenId, name = "Trädgården", orgId = orgId)
    private val bed = Bed(id = bedId, name = "Bädd 1", gardenId = gardenId)
    private val area = GardenArea(id = areaId, gardenId = gardenId, name = "Gång", category = GardenAreaCategory.WALKWAY)

    private fun areaRequest(activity: String = "WEED") = CreateMaintenanceRuleRequest(
        gardenAreaId = areaId, activityType = activity, intervalDays = 21,
    )

    @Test
    fun `creating an area rule persists it`() {
        whenever(areaService.requireArea(areaId, orgId)).thenReturn(area)
        whenever(rules.persist(any())).thenAnswer { (it.arguments[0] as MaintenanceRule).copy(id = 7L) }
        whenever(lastDone.resolve(any())).thenReturn(null)

        val result = service.createRule(areaRequest(), orgId)

        assertEquals(7L, result.id)
        assertEquals("WEED", result.activityType)
        assertEquals(areaId, result.gardenAreaId)
        assertEquals(21, result.intervalDays)
    }

    @Test
    fun `a rule must name exactly one target`() {
        assertThrows<BadRequestException> {
            service.createRule(CreateMaintenanceRuleRequest(activityType = "WEED", intervalDays = 21), orgId)
        }
        assertThrows<BadRequestException> {
            service.createRule(
                CreateMaintenanceRuleRequest(bedId = bedId, gardenAreaId = areaId, activityType = "WEED", intervalDays = 21),
                orgId,
            )
        }
        verify(rules, never()).persist(any())
    }

    @Test
    fun `a bed rule rejects an activity that beds cannot receive`() {
        whenever(beds.findById(bedId)).thenReturn(bed)
        whenever(gardens.findById(gardenId)).thenReturn(garden)

        assertThrows<BadRequestException> {
            service.createRule(
                CreateMaintenanceRuleRequest(bedId = bedId, activityType = "MOW", intervalDays = 30),
                orgId,
            )
        }
        verify(rules, never()).persist(any())
    }

    @Test
    fun `an area rule rejects a bed-only activity`() {
        whenever(areaService.requireArea(areaId, orgId)).thenReturn(area)

        assertThrows<BadRequestException> { service.createRule(areaRequest("FERTILIZE"), orgId) }
        verify(rules, never()).persist(any())
    }

    @Test
    fun `a bed in another org is hidden`() {
        whenever(beds.findById(bedId)).thenReturn(bed)
        whenever(gardens.findById(gardenId)).thenReturn(garden.copy(orgId = 99L))

        assertThrows<NotFoundException> {
            service.createRule(
                CreateMaintenanceRuleRequest(bedId = bedId, activityType = "WEED", intervalDays = 14),
                orgId,
            )
        }
    }

    @Test
    fun `a season window must be complete or absent`() {
        whenever(areaService.requireArea(areaId, orgId)).thenReturn(area)

        assertThrows<BadRequestException> {
            service.createRule(areaRequest().copy(seasonStartMonth = 4, seasonStartDay = 1), orgId)
        }
    }

    @Test
    fun `the response carries the derived last-done and next-due dates`() {
        val persisted = MaintenanceRule(
            id = 7L, orgId = orgId, gardenAreaId = areaId,
            activity = MaintenanceActivity.WEED, intervalDays = 21,
        )
        whenever(areaService.requireArea(areaId, orgId)).thenReturn(area)
        whenever(rules.persist(any())).thenReturn(persisted)
        whenever(lastDone.resolve(persisted)).thenReturn(LocalDate.of(2026, 6, 1))

        val result = service.createRule(areaRequest(), orgId)

        assertEquals(LocalDate.of(2026, 6, 1), result.lastDoneDate)
        assertEquals(LocalDate.of(2026, 6, 22), result.nextDueDate)
    }

    @Test
    fun `an anchor with no events pushes next-due out by a full interval`() {
        val persisted = MaintenanceRule(
            id = 7L, orgId = orgId, gardenAreaId = areaId,
            activity = MaintenanceActivity.WEED, intervalDays = 21,
            anchorDate = LocalDate.of(2026, 6, 1),
        )
        whenever(areaService.requireArea(areaId, orgId)).thenReturn(area)
        whenever(rules.persist(any())).thenReturn(persisted)
        whenever(lastDone.resolve(persisted)).thenReturn(null)

        val result = service.createRule(areaRequest().copy(anchorDate = LocalDate.of(2026, 6, 1)), orgId)

        assertEquals(LocalDate.of(2026, 6, 1), result.lastDoneDate)
        assertEquals(LocalDate.of(2026, 6, 22), result.nextDueDate)
    }

    @Test
    fun `the later of anchor and event drives next-due`() {
        val anchored = MaintenanceRule(
            id = 7L, orgId = orgId, gardenAreaId = areaId,
            activity = MaintenanceActivity.WEED, intervalDays = 21,
            anchorDate = LocalDate.of(2026, 5, 1),
        )
        whenever(areaService.requireArea(areaId, orgId)).thenReturn(area)
        whenever(rules.persist(any())).thenReturn(anchored)
        // An event newer than the anchor must win.
        whenever(lastDone.resolve(anchored)).thenReturn(LocalDate.of(2026, 6, 10))

        val newerEvent = service.createRule(areaRequest().copy(anchorDate = LocalDate.of(2026, 5, 1)), orgId)
        assertEquals(LocalDate.of(2026, 6, 10), newerEvent.lastDoneDate)
        assertEquals(LocalDate.of(2026, 7, 1), newerEvent.nextDueDate)

        // …and an anchor newer than the newest event must not be dragged back.
        val newerAnchor = anchored.copy(anchorDate = LocalDate.of(2026, 6, 10))
        whenever(rules.persist(any())).thenReturn(newerAnchor)
        whenever(lastDone.resolve(newerAnchor)).thenReturn(LocalDate.of(2026, 5, 1))

        val result = service.createRule(areaRequest().copy(anchorDate = LocalDate.of(2026, 6, 10)), orgId)
        assertEquals(LocalDate.of(2026, 6, 10), result.lastDoneDate)
        assertEquals(LocalDate.of(2026, 7, 1), result.nextDueDate)
    }

    @Test
    fun `listing rejects both filters at once`() {
        assertThrows<BadRequestException> { service.listRules(bedId, areaId, orgId) }
    }

    @Test
    fun `updating a rule can deactivate it without touching the interval`() {
        val existing = MaintenanceRule(
            id = 7L, orgId = orgId, gardenAreaId = areaId,
            activity = MaintenanceActivity.WEED, intervalDays = 21,
        )
        whenever(rules.findById(7L)).thenReturn(existing)
        whenever(areaService.requireArea(areaId, orgId)).thenReturn(area)
        whenever(lastDone.resolve(any())).thenReturn(null)

        val result = service.updateRule(7L, UpdateMaintenanceRuleRequest(active = false), orgId)

        assertEquals(false, result.active)
        assertEquals(21, result.intervalDays)
    }

    @Test
    fun `clearing the season window returns a rule to year-round`() {
        val windowed = MaintenanceRule(
            id = 7L, orgId = orgId, gardenAreaId = areaId,
            activity = MaintenanceActivity.WEED, intervalDays = 21,
            seasonStartMonth = 4, seasonStartDay = 1, seasonEndMonth = 10, seasonEndDay = 15,
        )
        whenever(rules.findById(7L)).thenReturn(windowed)
        whenever(areaService.requireArea(areaId, orgId)).thenReturn(area)
        whenever(lastDone.resolve(any())).thenReturn(null)

        val result = service.updateRule(7L, UpdateMaintenanceRuleRequest(clearSeasonWindow = true), orgId)

        assertEquals(null, result.seasonStartMonth)
        assertEquals(null, result.seasonStartDay)
        assertEquals(null, result.seasonEndMonth)
        assertEquals(null, result.seasonEndDay)
    }

    @Test
    fun `clearing the season window rejects supplied bounds`() {
        val windowed = MaintenanceRule(
            id = 7L, orgId = orgId, gardenAreaId = areaId,
            activity = MaintenanceActivity.WEED, intervalDays = 21,
            seasonStartMonth = 4, seasonStartDay = 1, seasonEndMonth = 10, seasonEndDay = 15,
        )
        whenever(rules.findById(7L)).thenReturn(windowed)
        whenever(areaService.requireArea(areaId, orgId)).thenReturn(area)

        assertThrows<BadRequestException> {
            service.updateRule(7L, UpdateMaintenanceRuleRequest(clearSeasonWindow = true, seasonStartMonth = 4), orgId)
        }
    }

    @Test
    fun `an update without the flag leaves an existing season window intact`() {
        val windowed = MaintenanceRule(
            id = 7L, orgId = orgId, gardenAreaId = areaId,
            activity = MaintenanceActivity.WEED, intervalDays = 21,
            seasonStartMonth = 4, seasonStartDay = 1, seasonEndMonth = 10, seasonEndDay = 15,
        )
        whenever(rules.findById(7L)).thenReturn(windowed)
        whenever(areaService.requireArea(areaId, orgId)).thenReturn(area)
        whenever(lastDone.resolve(any())).thenReturn(null)

        val result = service.updateRule(7L, UpdateMaintenanceRuleRequest(intervalDays = 30), orgId)

        assertEquals(30, result.intervalDays)
        assertEquals(4, result.seasonStartMonth)
        assertEquals(1, result.seasonStartDay)
        assertEquals(10, result.seasonEndMonth)
        assertEquals(15, result.seasonEndDay)
    }
}
