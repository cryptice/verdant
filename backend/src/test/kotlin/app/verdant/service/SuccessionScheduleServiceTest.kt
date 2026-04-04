package app.verdant.service

import app.verdant.entity.ScheduledTask
import app.verdant.entity.SuccessionSchedule
import app.verdant.repository.ScheduledTaskRepository
import app.verdant.repository.SpeciesRepository
import app.verdant.repository.SuccessionScheduleRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class SuccessionScheduleServiceTest {

    private val repo: SuccessionScheduleRepository = mock()
    private val speciesRepo: SpeciesRepository = mock()
    private val taskRepo: ScheduledTaskRepository = mock()
    private val service = SuccessionScheduleService(repo, speciesRepo, taskRepo)

    private fun makeSchedule(
        id: Long = 1L,
        orgId: Long = 10L,
        speciesId: Long = 42L,
        firstSowDate: LocalDate = LocalDate.of(2025, 4, 1),
        intervalDays: Int = 14,
        totalSuccessions: Int = 4,
        seedsPerSuccession: Int = 50,
    ) = SuccessionSchedule(
        id = id,
        orgId = orgId,
        seasonId = 1L,
        speciesId = speciesId,
        firstSowDate = firstSowDate,
        intervalDays = intervalDays,
        totalSuccessions = totalSuccessions,
        seedsPerSuccession = seedsPerSuccession,
    )

    private fun persistedTask(task: ScheduledTask, idOffset: Int = 0) =
        task.copy(id = 100L + idOffset)

    @Test
    fun `generateTasks creates correct number of tasks`() {
        val schedule = makeSchedule(totalSuccessions = 5, intervalDays = 7)
        whenever(repo.findById(1L)).thenReturn(schedule)
        whenever(taskRepo.persist(any())).thenAnswer { invocation ->
            val arg = invocation.getArgument<ScheduledTask>(0)
            arg.copy(id = 200L)
        }

        val taskIds = service.generateTasks(1L, schedule.orgId)

        assertEquals(5, taskIds.size)
        verify(taskRepo, times(5)).persist(any())
    }

    @Test
    fun `generateTasks calculates correct dates`() {
        val firstSow = LocalDate.of(2025, 5, 1)
        val interval = 10
        val schedule = makeSchedule(
            firstSowDate = firstSow,
            intervalDays = interval,
            totalSuccessions = 3,
        )
        whenever(repo.findById(1L)).thenReturn(schedule)

        val captor = argumentCaptor<ScheduledTask>()
        whenever(taskRepo.persist(captor.capture())).thenAnswer { invocation ->
            invocation.getArgument<ScheduledTask>(0).copy(id = 300L)
        }

        service.generateTasks(1L, schedule.orgId)

        val capturedTasks = captor.allValues
        assertEquals(3, capturedTasks.size)
        assertEquals(firstSow, capturedTasks[0].deadline)
        assertEquals(firstSow.plusDays(10), capturedTasks[1].deadline)
        assertEquals(firstSow.plusDays(20), capturedTasks[2].deadline)
    }

    @Test
    fun `generateTasks uses species info from schedule`() {
        val speciesId = 77L
        val schedule = makeSchedule(speciesId = speciesId, totalSuccessions = 2, seedsPerSuccession = 30)
        whenever(repo.findById(1L)).thenReturn(schedule)

        val captor = argumentCaptor<ScheduledTask>()
        whenever(taskRepo.persist(captor.capture())).thenAnswer { invocation ->
            invocation.getArgument<ScheduledTask>(0).copy(id = 400L)
        }

        service.generateTasks(1L, schedule.orgId)

        val capturedTasks = captor.allValues
        assertEquals(2, capturedTasks.size)
        capturedTasks.forEach { task ->
            assertEquals(speciesId, task.speciesId)
            assertEquals(30, task.targetCount)
            assertEquals(30, task.remainingCount)
            assertEquals("SOW", task.activityType)
            assertEquals(schedule.id, task.successionScheduleId)
        }
    }
}
