package app.verdant.service

import app.verdant.entity.MaintenanceActivity
import app.verdant.entity.MaintenanceRule
import app.verdant.entity.ScheduledTask
import app.verdant.repository.MaintenanceRuleRepository
import app.verdant.repository.ScheduledTaskRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class MaintenanceSchedulerTest {

    private val rules: MaintenanceRuleRepository = mock()
    private val lastDone: LastDoneResolver = mock()
    private val tasks: ScheduledTaskRepository = mock()
    private val scheduler = MaintenanceScheduler(rules, lastDone, tasks)

    private val orgId = 10L
    private val areaId = 5L
    private val today = LocalDate.of(2026, 6, 25)

    private fun rule(
        id: Long = 1L,
        intervalDays: Int = 21,
        seasonStartMonth: Int? = null,
        seasonStartDay: Int? = null,
        seasonEndMonth: Int? = null,
        seasonEndDay: Int? = null,
    ) = MaintenanceRule(
        id = id, orgId = orgId, gardenAreaId = areaId,
        activity = MaintenanceActivity.WEED, intervalDays = intervalDays,
        seasonStartMonth = seasonStartMonth, seasonStartDay = seasonStartDay,
        seasonEndMonth = seasonEndMonth, seasonEndDay = seasonEndDay,
    )

    @Test
    fun `a rule due today produces one task`() {
        val r = rule()
        whenever(rules.findActiveWithoutOpenTask()).thenReturn(listOf(r))
        whenever(lastDone.resolve(r)).thenReturn(LocalDate.of(2026, 6, 4))
        whenever(tasks.persist(any())).thenAnswer { it.arguments[0] as ScheduledTask }

        assertEquals(1, scheduler.run(today))

        val captor = argumentCaptor<ScheduledTask>()
        verify(tasks).persist(captor.capture())
        val created = captor.firstValue
        assertEquals("WEED", created.activityType)
        assertEquals(areaId, created.gardenAreaId)
        assertEquals(1L, created.maintenanceRuleId)
        assertEquals(1, created.targetCount)
        assertEquals(1, created.remainingCount)
        assertEquals(LocalDate.of(2026, 6, 25), created.deadline)
        assertEquals(LocalDate.of(2026, 6, 25), created.earliestDate)
    }

    @Test
    fun `a rule not yet due produces nothing`() {
        val r = rule()
        whenever(rules.findActiveWithoutOpenTask()).thenReturn(listOf(r))
        whenever(lastDone.resolve(r)).thenReturn(LocalDate.of(2026, 6, 20))

        assertEquals(0, scheduler.run(today))
        verify(tasks, never()).persist(any())
    }

    @Test
    fun `an overdue rule uses the due date as the deadline not today`() {
        val r = rule()
        whenever(rules.findActiveWithoutOpenTask()).thenReturn(listOf(r))
        whenever(lastDone.resolve(r)).thenReturn(LocalDate.of(2026, 5, 1))
        whenever(tasks.persist(any())).thenAnswer { it.arguments[0] as ScheduledTask }

        scheduler.run(today)

        val captor = argumentCaptor<ScheduledTask>()
        verify(tasks).persist(captor.capture())
        assertEquals(LocalDate.of(2026, 5, 22), captor.firstValue.deadline)
    }

    @Test
    fun `a rule never done is due immediately`() {
        val r = rule()
        whenever(rules.findActiveWithoutOpenTask()).thenReturn(listOf(r))
        whenever(lastDone.resolve(r)).thenReturn(null)
        whenever(tasks.persist(any())).thenAnswer { it.arguments[0] as ScheduledTask }

        assertEquals(1, scheduler.run(today))
    }

    @Test
    fun `a rule out of season produces nothing`() {
        val r = rule(seasonStartMonth = 11, seasonStartDay = 1, seasonEndMonth = 3, seasonEndDay = 31)
        whenever(rules.findActiveWithoutOpenTask()).thenReturn(listOf(r))
        whenever(lastDone.resolve(r)).thenReturn(null)

        assertEquals(0, scheduler.run(today))
        verify(tasks, never()).persist(any())
    }

    @Test
    fun `a bed rule carries the bed id instead of the area id`() {
        val r = MaintenanceRule(
            id = 2L, orgId = orgId, bedId = 3L,
            activity = MaintenanceActivity.WATER, intervalDays = 7,
        )
        whenever(rules.findActiveWithoutOpenTask()).thenReturn(listOf(r))
        whenever(lastDone.resolve(r)).thenReturn(null)
        whenever(tasks.persist(any())).thenAnswer { it.arguments[0] as ScheduledTask }

        scheduler.run(today)

        val captor = argumentCaptor<ScheduledTask>()
        verify(tasks).persist(captor.capture())
        assertEquals(3L, captor.firstValue.bedId)
        assertEquals(null, captor.firstValue.gardenAreaId)
    }

    @Test
    fun `one failing rule does not stop the others`() {
        val bad = rule(id = 1L)
        val good = rule(id = 2L)
        whenever(rules.findActiveWithoutOpenTask()).thenReturn(listOf(bad, good))
        whenever(lastDone.resolve(bad)).thenThrow(RuntimeException("boom"))
        whenever(lastDone.resolve(good)).thenReturn(null)
        whenever(tasks.persist(any())).thenAnswer { it.arguments[0] as ScheduledTask }

        assertEquals(1, scheduler.run(today))
    }

    @Test
    fun `a second run over an empty work list creates nothing`() {
        whenever(rules.findActiveWithoutOpenTask()).thenReturn(emptyList())
        assertEquals(0, scheduler.run(today))
        verify(tasks, never()).persist(any())
    }
}
