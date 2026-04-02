package app.verdant.service

import app.verdant.dto.UpdateScheduledTaskRequest
import app.verdant.entity.ScheduledTask
import app.verdant.entity.ScheduledTaskStatus
import app.verdant.repository.ScheduledTaskRepository
import app.verdant.repository.SpeciesRepository
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant
import java.time.LocalDate

class ScheduledTaskServiceTest {

    private val taskRepository: ScheduledTaskRepository = mock()
    private val speciesRepository: SpeciesRepository = mock()
    private val service = ScheduledTaskService(taskRepository, speciesRepository)

    private val userId = 10L
    private val speciesId = 100L
    private val deadline = LocalDate.of(2025, 9, 1)
    private val speciesNames = mapOf(speciesId to "Zinnia")

    private fun makeTask(
        id: Long = 1L,
        targetCount: Int = 100,
        remainingCount: Int = 100,
        status: ScheduledTaskStatus = ScheduledTaskStatus.PENDING,
    ) = ScheduledTask(
        id = id,
        userId = userId,
        speciesId = speciesId,
        activityType = "SOW",
        deadline = deadline,
        targetCount = targetCount,
        remainingCount = remainingCount,
        status = status,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    // ── updateTask recalculates remaining when target changes ─────────────────

    @Test
    fun `updateTask recalculates remaining count when targetCount is updated`() {
        // Initial state: target=100, remaining=60 → 40 already completed
        val existing = makeTask(targetCount = 100, remainingCount = 60)
        val captor = argumentCaptor<ScheduledTask>()

        whenever(taskRepository.findById(1L)).thenReturn(existing)
        whenever(speciesRepository.findNamesByIds(setOf(speciesId))).thenReturn(speciesNames)
        whenever(taskRepository.update(any())).then { }

        // Raise target to 120 — 40 completed, so remaining = 120 - 40 = 80
        val request = UpdateScheduledTaskRequest(targetCount = 120)
        val result = service.updateTask(taskId = 1L, request = request, userId = userId)

        assertEquals(120, result.targetCount)
        assertEquals(80, result.remainingCount)
        assertEquals(ScheduledTaskStatus.PENDING.name, result.status)

        verify(taskRepository).update(captor.capture())
        val saved = captor.firstValue
        assertEquals(120, saved.targetCount)
        assertEquals(80, saved.remainingCount)
        assertEquals(ScheduledTaskStatus.PENDING, saved.status)
    }

    @Test
    fun `updateTask does not change remaining when targetCount is not provided`() {
        val existing = makeTask(targetCount = 100, remainingCount = 40)

        whenever(taskRepository.findById(1L)).thenReturn(existing)
        whenever(speciesRepository.findNamesByIds(setOf(speciesId))).thenReturn(speciesNames)
        whenever(taskRepository.update(any())).then { }

        val request = UpdateScheduledTaskRequest(activityType = "TRANSPLANT")
        val result = service.updateTask(taskId = 1L, request = request, userId = userId)

        assertEquals(40, result.remainingCount)
        assertEquals(100, result.targetCount)
    }

    // ── updateTask auto-completes when remaining reaches 0 ────────────────────

    @Test
    fun `updateTask sets status to COMPLETED when new remaining count reaches zero`() {
        // completed = target - remaining = 100 - 20 = 80
        // Lower target to 80: newRemaining = max(80 - 80, 0) = 0 → COMPLETED
        val existing = makeTask(targetCount = 100, remainingCount = 20)
        val captor = argumentCaptor<ScheduledTask>()

        whenever(taskRepository.findById(1L)).thenReturn(existing)
        whenever(speciesRepository.findNamesByIds(setOf(speciesId))).thenReturn(speciesNames)
        whenever(taskRepository.update(any())).then { }

        val request = UpdateScheduledTaskRequest(targetCount = 80)
        val result = service.updateTask(taskId = 1L, request = request, userId = userId)

        assertEquals(0, result.remainingCount)
        assertEquals(ScheduledTaskStatus.COMPLETED.name, result.status)

        verify(taskRepository).update(captor.capture())
        val saved = captor.firstValue
        assertEquals(ScheduledTaskStatus.COMPLETED, saved.status)
        assertEquals(0, saved.remainingCount)
    }

    @Test
    fun `updateTask clamps remaining to zero and completes when new target is below completed count`() {
        // completed = 100 - 10 = 90; lower target to 50 → remaining = max(50 - 90, 0) = 0
        val existing = makeTask(targetCount = 100, remainingCount = 10)
        val captor = argumentCaptor<ScheduledTask>()

        whenever(taskRepository.findById(1L)).thenReturn(existing)
        whenever(speciesRepository.findNamesByIds(setOf(speciesId))).thenReturn(speciesNames)
        whenever(taskRepository.update(any())).then { }

        val request = UpdateScheduledTaskRequest(targetCount = 50)
        service.updateTask(taskId = 1L, request = request, userId = userId)

        verify(taskRepository).update(captor.capture())
        val saved = captor.firstValue
        assertEquals(0, saved.remainingCount)
        assertEquals(ScheduledTaskStatus.COMPLETED, saved.status)
    }

    // ── completePartially decrements remaining ────────────────────────────────

    @Test
    fun `completePartially decrements remaining count by the given amount`() {
        val existing = makeTask(targetCount = 100, remainingCount = 50)
        // After decrement the repository returns the updated state
        val afterDecrement = existing.copy(remainingCount = 30, status = ScheduledTaskStatus.PENDING)

        whenever(taskRepository.findById(1L))
            .thenReturn(existing)           // ownership check
            .thenReturn(afterDecrement)     // re-fetch after decrement

        whenever(speciesRepository.findNamesByIds(setOf(speciesId))).thenReturn(speciesNames)

        val result = service.completePartially(taskId = 1L, processedCount = 20, userId = userId)

        verify(taskRepository).decrementRemainingCount(1L, 20)
        assertEquals(30, result.remainingCount)
        assertEquals(ScheduledTaskStatus.PENDING.name, result.status)
    }

    // ── completePartially with count exceeding remaining ──────────────────────

    @Test
    fun `completePartially completes the task when processedCount exceeds remaining`() {
        val existing = makeTask(targetCount = 100, remainingCount = 5)
        // Repository clamps to 0 and sets COMPLETED via SQL GREATEST / CASE
        val afterDecrement = existing.copy(remainingCount = 0, status = ScheduledTaskStatus.COMPLETED)

        whenever(taskRepository.findById(1L))
            .thenReturn(existing)
            .thenReturn(afterDecrement)

        whenever(speciesRepository.findNamesByIds(setOf(speciesId))).thenReturn(speciesNames)

        val result = service.completePartially(taskId = 1L, processedCount = 999, userId = userId)

        verify(taskRepository).decrementRemainingCount(1L, 999)
        assertEquals(0, result.remainingCount)
        assertEquals(ScheduledTaskStatus.COMPLETED.name, result.status)
    }

    // ── ownership checks ──────────────────────────────────────────────────────

    @Test
    fun `updateTask throws NotFoundException when task does not exist`() {
        whenever(taskRepository.findById(99L)).thenReturn(null)

        assertThrows<NotFoundException> {
            service.updateTask(taskId = 99L, request = UpdateScheduledTaskRequest(), userId = userId)
        }
    }

    @Test
    fun `completePartially throws NotFoundException when task does not exist`() {
        whenever(taskRepository.findById(99L)).thenReturn(null)

        assertThrows<NotFoundException> {
            service.completePartially(taskId = 99L, processedCount = 1, userId = userId)
        }
    }
}
