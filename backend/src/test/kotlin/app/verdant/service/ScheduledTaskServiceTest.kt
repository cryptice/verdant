package app.verdant.service

import app.verdant.dto.CreateScheduledTaskRequest
import app.verdant.dto.UpdateScheduledTaskRequest
import app.verdant.entity.ScheduledTask
import app.verdant.entity.ScheduledTaskStatus
import app.verdant.entity.Species
import app.verdant.entity.SpeciesGroup
import app.verdant.repository.BedRepository
import app.verdant.repository.GardenRepository
import app.verdant.repository.ScheduledTaskRepository
import app.verdant.repository.SpeciesGroupRepository
import app.verdant.repository.SpeciesRepository
import jakarta.ws.rs.BadRequestException
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
    private val speciesGroupRepository: SpeciesGroupRepository = mock()
    private val bedRepository: BedRepository = mock()
    private val gardenRepository: GardenRepository = mock()
    private val service = ScheduledTaskService(
        taskRepository, speciesRepository, speciesGroupRepository,
        bedRepository, gardenRepository,
    )

    private val orgId = 10L
    private val speciesId = 100L
    private val deadline = LocalDate.of(2025, 9, 1)
    private val speciesNames = mapOf(speciesId to "Zinnia")

    private fun makeTask(
        id: Long = 1L,
        speciesId: Long? = this.speciesId,
        targetCount: Int = 100,
        remainingCount: Int = 100,
        status: ScheduledTaskStatus = ScheduledTaskStatus.PENDING,
        originGroupId: Long? = null,
    ) = ScheduledTask(
        id = id,
        orgId = orgId,
        speciesId = speciesId,
        activityType = "SOW",
        deadline = deadline,
        targetCount = targetCount,
        remainingCount = remainingCount,
        status = status,
        originGroupId = originGroupId,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private fun makeSpecies(id: Long, name: String) = Species(
        id = id,
        commonName = name,
    )

    private fun stubBuildResponses(task: ScheduledTask, acceptableSpeciesIds: List<Long> = listOf(task.speciesId ?: speciesId)) {
        val taskIds = setOf(task.id!!)
        whenever(taskRepository.findAcceptableSpeciesIdsByTaskIds(taskIds))
            .thenReturn(mapOf(task.id!! to acceptableSpeciesIds))
        val allSpeciesIds = acceptableSpeciesIds.toSet() + setOfNotNull(task.speciesId)
        whenever(speciesRepository.findNamesByIds(allSpeciesIds))
            .thenReturn(allSpeciesIds.associateWith { "Species$it" })
        val groupIds = setOfNotNull(task.originGroupId)
        whenever(speciesGroupRepository.findNamesByIds(groupIds))
            .thenReturn(groupIds.associateWith { "Group$it" })
    }

    // ── createTask: single speciesId ─────────────────────────────────────────

    @Test
    fun `createTask with single speciesId creates task with one acceptable species`() {
        val request = CreateScheduledTaskRequest(
            speciesId = speciesId,
            activityType = "SOW",
            deadline = deadline,
            targetCount = 50,
        )
        val persisted = makeTask(id = 1L, targetCount = 50, remainingCount = 50)

        whenever(speciesRepository.findById(speciesId)).thenReturn(makeSpecies(speciesId, "Zinnia"))
        whenever(taskRepository.persist(any())).thenReturn(persisted)
        stubBuildResponses(persisted, listOf(speciesId))

        val result = service.createTask(request, orgId)

        verify(taskRepository).setAcceptableSpecies(1L, listOf(speciesId))
        assertEquals(speciesId, result.speciesId)
        assertEquals(1, result.acceptableSpecies.size)
        assertEquals(speciesId, result.acceptableSpecies[0].speciesId)
    }

    // ── createTask: group snapshots all members ──────────────────────────────

    @Test
    fun `createTask with speciesGroupId snapshots group members`() {
        val groupId = 200L
        val sp1 = makeSpecies(101L, "Rose")
        val sp2 = makeSpecies(102L, "Lily")

        val request = CreateScheduledTaskRequest(
            speciesGroupId = groupId,
            activityType = "SOW",
            deadline = deadline,
            targetCount = 30,
        )
        val persisted = makeTask(id = 2L, speciesId = null, targetCount = 30, remainingCount = 30, originGroupId = groupId)

        whenever(speciesGroupRepository.findById(groupId)).thenReturn(SpeciesGroup(id = groupId, orgId = orgId, name = "Flowers"))
        whenever(speciesRepository.findByGroupId(groupId)).thenReturn(listOf(sp1, sp2))
        whenever(taskRepository.persist(any())).thenReturn(persisted)
        stubBuildResponses(persisted, listOf(101L, 102L))

        val result = service.createTask(request, orgId)

        val speciesCaptor = argumentCaptor<List<Long>>()
        verify(taskRepository).setAcceptableSpecies(eq(2L), speciesCaptor.capture())
        assertEquals(listOf(101L, 102L), speciesCaptor.firstValue)
        assertNull(result.speciesId)
        assertEquals(groupId, result.originGroupId)
    }

    // ── createTask: group with subset ────────────────────────────────────────

    @Test
    fun `createTask with speciesGroupId and speciesIds subset only includes subset`() {
        val groupId = 200L
        val sp1 = makeSpecies(101L, "Rose")
        val sp2 = makeSpecies(102L, "Lily")
        val sp3 = makeSpecies(103L, "Daisy")

        val request = CreateScheduledTaskRequest(
            speciesGroupId = groupId,
            speciesIds = listOf(101L, 103L),
            activityType = "SOW",
            deadline = deadline,
            targetCount = 20,
        )
        val persisted = makeTask(id = 3L, speciesId = null, targetCount = 20, remainingCount = 20, originGroupId = groupId)

        whenever(speciesGroupRepository.findById(groupId)).thenReturn(SpeciesGroup(id = groupId, orgId = orgId, name = "Flowers"))
        whenever(speciesRepository.findByGroupId(groupId)).thenReturn(listOf(sp1, sp2, sp3))
        whenever(taskRepository.persist(any())).thenReturn(persisted)
        stubBuildResponses(persisted, listOf(101L, 103L))

        service.createTask(request, orgId)

        val speciesCaptor = argumentCaptor<List<Long>>()
        verify(taskRepository).setAcceptableSpecies(eq(3L), speciesCaptor.capture())
        assertEquals(listOf(101L, 103L), speciesCaptor.firstValue)
    }

    // ── createTask: group rejects species not in group ───────────────────────

    @Test
    fun `createTask with speciesGroupId rejects speciesIds not in group`() {
        val groupId = 200L
        val sp1 = makeSpecies(101L, "Rose")

        val request = CreateScheduledTaskRequest(
            speciesGroupId = groupId,
            speciesIds = listOf(101L, 999L),
            activityType = "SOW",
            deadline = deadline,
            targetCount = 10,
        )

        whenever(speciesGroupRepository.findById(groupId)).thenReturn(SpeciesGroup(id = groupId, orgId = orgId, name = "Flowers"))
        whenever(speciesRepository.findByGroupId(groupId)).thenReturn(listOf(sp1))

        assertThrows<BadRequestException> {
            service.createTask(request, orgId)
        }
    }

    // ── createTask: empty group throws ───────────────────────────────────────

    @Test
    fun `createTask with empty group throws BadRequestException`() {
        val groupId = 200L

        val request = CreateScheduledTaskRequest(
            speciesGroupId = groupId,
            activityType = "SOW",
            deadline = deadline,
            targetCount = 10,
        )

        whenever(speciesGroupRepository.findById(groupId)).thenReturn(SpeciesGroup(id = groupId, orgId = orgId, name = "Empty"))
        whenever(speciesRepository.findByGroupId(groupId)).thenReturn(emptyList())

        assertThrows<BadRequestException> {
            service.createTask(request, orgId)
        }
    }

    // ── completePartially validates species ──────────────────────────────────

    @Test
    fun `completePartially validates species is in acceptable list`() {
        val existing = makeTask(targetCount = 100, remainingCount = 50)

        whenever(taskRepository.findById(1L)).thenReturn(existing)
        whenever(taskRepository.findAcceptableSpeciesIds(1L)).thenReturn(listOf(100L, 101L))

        assertThrows<BadRequestException> {
            service.completePartially(taskId = 1L, speciesId = 999L, processedCount = 5, orgId = orgId)
        }
    }

    @Test
    fun `completePartially succeeds with valid species from acceptable list`() {
        val existing = makeTask(targetCount = 100, remainingCount = 50)
        val afterDecrement = existing.copy(remainingCount = 45)

        whenever(taskRepository.findById(1L))
            .thenReturn(existing)
            .thenReturn(afterDecrement)
        whenever(taskRepository.findAcceptableSpeciesIds(1L)).thenReturn(listOf(100L, 101L))
        stubBuildResponses(afterDecrement, listOf(100L, 101L))

        val result = service.completePartially(taskId = 1L, speciesId = 100L, processedCount = 5, orgId = orgId)

        verify(taskRepository).decrementRemainingCount(1L, 5)
        assertEquals(45, result.remainingCount)
    }

    // ── updateTask recalculates remaining when target changes ────────────────

    @Test
    fun `updateTask recalculates remaining count when targetCount is updated`() {
        val existing = makeTask(targetCount = 100, remainingCount = 60)
        val captor = argumentCaptor<ScheduledTask>()

        whenever(taskRepository.findById(1L)).thenReturn(existing)
        whenever(taskRepository.update(any())).then { }
        stubBuildResponses(existing.copy(targetCount = 120, remainingCount = 80))

        val request = UpdateScheduledTaskRequest(targetCount = 120)
        val result = service.updateTask(taskId = 1L, request = request, orgId = orgId)

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
    fun `updateTask sets status to COMPLETED when new remaining count reaches zero`() {
        val existing = makeTask(targetCount = 100, remainingCount = 20)
        val captor = argumentCaptor<ScheduledTask>()

        whenever(taskRepository.findById(1L)).thenReturn(existing)
        whenever(taskRepository.update(any())).then { }
        stubBuildResponses(existing.copy(targetCount = 80, remainingCount = 0, status = ScheduledTaskStatus.COMPLETED))

        val request = UpdateScheduledTaskRequest(targetCount = 80)
        val result = service.updateTask(taskId = 1L, request = request, orgId = orgId)

        assertEquals(0, result.remainingCount)
        assertEquals(ScheduledTaskStatus.COMPLETED.name, result.status)

        verify(taskRepository).update(captor.capture())
        val saved = captor.firstValue
        assertEquals(ScheduledTaskStatus.COMPLETED, saved.status)
        assertEquals(0, saved.remainingCount)
    }

    // ── ownership checks ─────────────────────────────────────────────────────

    @Test
    fun `updateTask throws NotFoundException when task does not exist`() {
        whenever(taskRepository.findById(99L)).thenReturn(null)

        assertThrows<NotFoundException> {
            service.updateTask(taskId = 99L, request = UpdateScheduledTaskRequest(), orgId = orgId)
        }
    }

    @Test
    fun `completePartially throws NotFoundException when task does not exist`() {
        whenever(taskRepository.findById(99L)).thenReturn(null)

        assertThrows<NotFoundException> {
            service.completePartially(taskId = 99L, speciesId = speciesId, processedCount = 1, orgId = orgId)
        }
    }

    // ── createTask: non-TODO must have deadline and targetCount ─────────────

    @Test
    fun `createTask without deadline for non-TODO is rejected`() {
        val request = CreateScheduledTaskRequest(
            speciesId = speciesId,
            activityType = "SOW",
            deadline = null,
            targetCount = 10,
        )
        whenever(speciesRepository.findById(speciesId)).thenReturn(makeSpecies(speciesId, "Zinnia"))

        assertThrows<BadRequestException> { service.createTask(request, orgId) }
    }

    @Test
    fun `createTask without targetCount for non-TODO is rejected`() {
        val request = CreateScheduledTaskRequest(
            speciesId = speciesId,
            activityType = "SOW",
            deadline = deadline,
            targetCount = null,
        )
        whenever(speciesRepository.findById(speciesId)).thenReturn(makeSpecies(speciesId, "Zinnia"))

        assertThrows<BadRequestException> { service.createTask(request, orgId) }
    }

    @Test
    fun `createTask with non-positive targetCount for non-TODO is rejected`() {
        val request = CreateScheduledTaskRequest(
            speciesId = speciesId,
            activityType = "SOW",
            deadline = deadline,
            targetCount = 0,
        )
        whenever(speciesRepository.findById(speciesId)).thenReturn(makeSpecies(speciesId, "Zinnia"))

        assertThrows<BadRequestException> { service.createTask(request, orgId) }
    }

    // ── createTask: TODO branch ─────────────────────────────────────────────

    @Test
    fun `createTask creates a TODO with only a description`() {
        val request = CreateScheduledTaskRequest(
            activityType = "TODO",
            notes = "Beställ nya pinnar",
        )
        val persisted = ScheduledTask(
            id = 42L,
            orgId = orgId,
            speciesId = null,
            bedId = null,
            activityType = "TODO",
            earliestDate = null,
            deadline = null,
            targetCount = 1,
            remainingCount = 1,
            notes = "Beställ nya pinnar",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        whenever(taskRepository.persist(any())).thenReturn(persisted)
        stubBuildResponses(persisted, emptyList())

        val result = service.createTask(request, orgId)

        verify(taskRepository).persist(check {
            assertEquals("TODO", it.activityType)
            assertNull(it.speciesId)
            assertNull(it.bedId)
            assertNull(it.deadline)
            assertEquals(1, it.targetCount)
            assertEquals(1, it.remainingCount)
            assertEquals("Beställ nya pinnar", it.notes)
        })
        assertEquals("TODO", result.activityType)
        assertNull(result.deadline)
        assertEquals(1, result.targetCount)
        verify(taskRepository, never()).setAcceptableSpecies(any(), any())
    }

    @Test
    fun `createTask TODO with blank notes is rejected`() {
        val request = CreateScheduledTaskRequest(activityType = "TODO", notes = "   ")
        assertThrows<BadRequestException> { service.createTask(request, orgId) }
    }

    @Test
    fun `createTask TODO with null notes is rejected`() {
        val request = CreateScheduledTaskRequest(activityType = "TODO", notes = null)
        assertThrows<BadRequestException> { service.createTask(request, orgId) }
    }

    @Test
    fun `createTask TODO with speciesId is rejected`() {
        val request = CreateScheduledTaskRequest(
            activityType = "TODO",
            speciesId = speciesId,
            notes = "Boka tid",
        )
        assertThrows<BadRequestException> { service.createTask(request, orgId) }
    }

    @Test
    fun `createTask TODO with bedId is rejected`() {
        val request = CreateScheduledTaskRequest(
            activityType = "TODO",
            bedId = 7L,
            notes = "Boka tid",
        )
        assertThrows<BadRequestException> { service.createTask(request, orgId) }
    }

    // ── updateTask: TODO transitions ────────────────────────────────────────

    @Test
    fun `updateTask cannot convert a TODO to another activity type`() {
        val todoTask = ScheduledTask(
            id = 5L, orgId = orgId, speciesId = null, bedId = null,
            activityType = "TODO", deadline = null,
            targetCount = 1, remainingCount = 1, notes = "Original",
            createdAt = Instant.now(), updatedAt = Instant.now(),
        )
        whenever(taskRepository.findById(5L)).thenReturn(todoTask)

        val request = UpdateScheduledTaskRequest(activityType = "SOW")
        assertThrows<BadRequestException> { service.updateTask(5L, request, orgId) }
    }

    @Test
    fun `updateTask cannot convert a non-TODO to TODO`() {
        val sowTask = makeTask(id = 6L)
        whenever(taskRepository.findById(6L)).thenReturn(sowTask)

        val request = UpdateScheduledTaskRequest(activityType = "TODO")
        assertThrows<BadRequestException> { service.updateTask(6L, request, orgId) }
    }

    @Test
    fun `updateTask updating a TODO description works`() {
        val todoTask = ScheduledTask(
            id = 7L, orgId = orgId, speciesId = null, bedId = null,
            activityType = "TODO", deadline = null,
            targetCount = 1, remainingCount = 1, notes = "Original",
            createdAt = Instant.now(), updatedAt = Instant.now(),
        )
        whenever(taskRepository.findById(7L)).thenReturn(todoTask)
        stubBuildResponses(todoTask.copy(notes = "Uppdaterad"), emptyList())

        val request = UpdateScheduledTaskRequest(notes = "Uppdaterad")
        val result = service.updateTask(7L, request, orgId)

        verify(taskRepository).update(check { assertEquals("Uppdaterad", it.notes) })
        assertEquals("Uppdaterad", result.notes)
    }

    @Test
    fun `updateTask TODO with blank notes is rejected`() {
        val todoTask = ScheduledTask(
            id = 8L, orgId = orgId, speciesId = null, bedId = null,
            activityType = "TODO", deadline = null,
            targetCount = 1, remainingCount = 1, notes = "Original",
            createdAt = Instant.now(), updatedAt = Instant.now(),
        )
        whenever(taskRepository.findById(8L)).thenReturn(todoTask)

        val request = UpdateScheduledTaskRequest(notes = "  ")
        assertThrows<BadRequestException> { service.updateTask(8L, request, orgId) }
    }

    // ── completePartially: TODO ─────────────────────────────────────────────

    @Test
    fun `completePartially completes a TODO with processedCount 1`() {
        val todoTask = ScheduledTask(
            id = 9L, orgId = orgId, speciesId = null, bedId = null,
            activityType = "TODO", deadline = null,
            targetCount = 1, remainingCount = 1, notes = "Boka tid",
            createdAt = Instant.now(), updatedAt = Instant.now(),
        )
        val completed = todoTask.copy(remainingCount = 0, status = ScheduledTaskStatus.COMPLETED)
        whenever(taskRepository.findById(9L)).thenReturn(todoTask, completed)
        stubBuildResponses(completed, emptyList())

        val result = service.completePartially(9L, speciesId = null, processedCount = 1, orgId = orgId)

        verify(taskRepository).decrementRemainingCount(9L, 1)
        assertEquals("COMPLETED", result.status)
    }
}
