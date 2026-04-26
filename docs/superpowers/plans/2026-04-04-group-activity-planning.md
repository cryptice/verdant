# Group Activity Planning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow users to plan activities against a group of acceptable species (e.g., "sow 200 pompon dahlias") instead of only a single species, while keeping single-species tasks as the default.

**Architecture:** Tasks gain an acceptable species list via a join table `scheduled_task_species`. `speciesId` on `scheduled_task` becomes nullable. An optional `origin_group_id` stores which group was used for display labeling. The unified autocomplete searches both species and groups. Completion requires specifying which species from the acceptable list was used.

**Tech Stack:** Quarkus/Kotlin backend, PostgreSQL with Flyway migrations, React/TypeScript frontend with React Query.

---

## File Structure

### Backend — New Files
- `(none)` — all changes go in existing files

### Backend — Modified Files
- `backend/src/main/resources/db/migration/V9__task_species_list.sql` — new migration
- `backend/src/main/kotlin/app/verdant/entity/ScheduledTask.kt` — make speciesId nullable, add originGroupId
- `backend/src/main/kotlin/app/verdant/dto/ScheduledTaskDtos.kt` — new request/response shapes
- `backend/src/main/kotlin/app/verdant/repository/ScheduledTaskRepository.kt` — join table CRUD, nullable speciesId
- `backend/src/main/kotlin/app/verdant/repository/SpeciesRepository.kt` — add findByGroupId
- `backend/src/main/kotlin/app/verdant/service/ScheduledTaskService.kt` — group creation, species validation on complete
- `backend/src/main/kotlin/app/verdant/resource/ScheduledTaskResource.kt` — updated request handling
- `backend/src/test/kotlin/app/verdant/service/ScheduledTaskServiceTest.kt` — new tests

### Frontend — Modified Files
- `web/src/api/client.ts` — updated types and API methods
- `web/src/components/SpeciesAutocomplete.tsx` — unified species+group search
- `web/src/pages/TaskForm.tsx` — group selection with editable species checklist
- `web/src/pages/TaskList.tsx` — group task display with visual indicator
- `web/src/pages/SowActivity.tsx` — species picker from acceptable list when completing group tasks

---

### Task 1: Database Migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V9__task_species_list.sql`

- [ ] **Step 1: Write the migration**

```sql
-- Join table: acceptable species for a task
CREATE TABLE scheduled_task_species (
    scheduled_task_id  BIGINT NOT NULL REFERENCES scheduled_task(id) ON DELETE CASCADE,
    species_id         BIGINT NOT NULL REFERENCES species(id) ON DELETE CASCADE,
    PRIMARY KEY (scheduled_task_id, species_id)
);

CREATE INDEX idx_scheduled_task_species_task ON scheduled_task_species(scheduled_task_id);
CREATE INDEX idx_scheduled_task_species_species ON scheduled_task_species(species_id);

-- Make species_id nullable (group tasks don't have a single species)
ALTER TABLE scheduled_task ALTER COLUMN species_id DROP NOT NULL;

-- Add origin group reference for display labeling
ALTER TABLE scheduled_task ADD COLUMN origin_group_id BIGINT REFERENCES species_group(id) ON DELETE SET NULL;

-- Backfill: all existing tasks get their species_id as the sole acceptable species
INSERT INTO scheduled_task_species (scheduled_task_id, species_id)
SELECT id, species_id FROM scheduled_task WHERE species_id IS NOT NULL;
```

- [ ] **Step 2: Verify migration applies cleanly**

Run: `cd backend && ./gradlew quarkusDev` (let it start, confirm no migration errors in logs, then stop)
Expected: Flyway applies V9 without errors.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/V9__task_species_list.sql
git commit -m "feat: add scheduled_task_species join table and make species_id nullable"
```

---

### Task 2: Backend Entity Changes

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/entity/ScheduledTask.kt`

- [ ] **Step 1: Update ScheduledTask entity**

Make `speciesId` nullable and add `originGroupId`:

```kotlin
package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class ScheduledTask(
    val id: Long? = null,
    val orgId: Long,
    val speciesId: Long? = null,
    val activityType: String,
    val deadline: LocalDate,
    val targetCount: Int,
    val remainingCount: Int,
    val status: ScheduledTaskStatus = ScheduledTaskStatus.PENDING,
    val notes: String? = null,
    val seasonId: Long? = null,
    val successionScheduleId: Long? = null,
    val originGroupId: Long? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class ScheduledTaskStatus { PENDING, COMPLETED }
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/entity/ScheduledTask.kt
git commit -m "feat: make ScheduledTask.speciesId nullable, add originGroupId"
```

---

### Task 3: Backend DTOs

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/dto/ScheduledTaskDtos.kt`

- [ ] **Step 1: Update DTOs**

```kotlin
package app.verdant.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

data class ScheduledTaskResponse(
    val id: Long,
    val speciesId: Long?,
    val speciesName: String?,
    val activityType: String,
    val deadline: LocalDate,
    val targetCount: Int,
    val remainingCount: Int,
    val status: String,
    val notes: String?,
    val seasonId: Long?,
    val successionScheduleId: Long?,
    val originGroupId: Long?,
    val originGroupName: String?,
    val acceptableSpecies: List<AcceptableSpeciesEntry>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class AcceptableSpeciesEntry(
    val speciesId: Long,
    val speciesName: String,
)

data class CreateScheduledTaskRequest(
    val speciesId: Long? = null,
    val speciesGroupId: Long? = null,
    val speciesIds: List<Long>? = null,
    @field:NotBlank @field:Size(max = 255)
    val activityType: String,
    @field:NotNull
    val deadline: LocalDate,
    @field:Min(1)
    val targetCount: Int,
    @field:Size(max = 2000)
    val notes: String? = null,
    val seasonId: Long? = null,
    val successionScheduleId: Long? = null,
)

data class UpdateScheduledTaskRequest(
    val speciesId: Long? = null,
    @field:Size(max = 255)
    val activityType: String? = null,
    val deadline: LocalDate? = null,
    @field:Min(1)
    val targetCount: Int? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
    val seasonId: Long? = null,
)

data class CompleteTaskPartiallyRequest(
    @field:NotNull
    val speciesId: Long,
    @field:Min(1)
    val processedCount: Int,
)
```

The create request supports three modes:
- `speciesId` only: single-species task (backward compatible)
- `speciesGroupId` + optional `speciesIds` override: group task, snapshots group members (or the provided subset)
- `speciesIds` only: custom multi-species task (future extensibility)

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/dto/ScheduledTaskDtos.kt
git commit -m "feat: update task DTOs for group planning and species-level completion"
```

---

### Task 4: Repository — Species findByGroupId

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/repository/SpeciesRepository.kt:53-64`

- [ ] **Step 1: Add findByGroupId method**

Add this method to `SpeciesRepository` after the `searchAll` method (after line 52):

```kotlin
    fun findByGroupId(groupId: Long): List<Species> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species WHERE group_id = ? ORDER BY common_name").use { ps ->
                ps.setLong(1, groupId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSpecies()) }
                }
            }
        }
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/repository/SpeciesRepository.kt
git commit -m "feat: add SpeciesRepository.findByGroupId"
```

---

### Task 5: Repository — Task Species Join Table

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/repository/ScheduledTaskRepository.kt`

- [ ] **Step 1: Update persist to handle nullable speciesId and originGroupId**

Replace the `persist` method:

```kotlin
    fun persist(task: ScheduledTask): ScheduledTask {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO scheduled_task (org_id, species_id, activity_type, deadline, target_count, remaining_count, status, notes, season_id, succession_schedule_id, origin_group_id, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, task.orgId)
                ps.setObject(2, task.speciesId)
                ps.setString(3, task.activityType)
                ps.setDate(4, Date.valueOf(task.deadline))
                ps.setInt(5, task.targetCount)
                ps.setInt(6, task.remainingCount)
                ps.setString(7, task.status.name)
                ps.setString(8, task.notes)
                ps.setObject(9, task.seasonId)
                ps.setObject(10, task.successionScheduleId)
                ps.setObject(11, task.originGroupId)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return task.copy(id = rs.getLong(1))
                }
            }
        }
    }
```

- [ ] **Step 2: Update the `update` method to handle nullable speciesId and originGroupId**

Replace the `update` method:

```kotlin
    fun update(task: ScheduledTask) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE scheduled_task SET species_id = ?, activity_type = ?, deadline = ?,
                   target_count = ?, remaining_count = ?, status = ?, notes = ?,
                   season_id = ?, succession_schedule_id = ?, origin_group_id = ?, updated_at = now()
                   WHERE id = ?"""
            ).use { ps ->
                ps.setObject(1, task.speciesId)
                ps.setString(2, task.activityType)
                ps.setDate(3, Date.valueOf(task.deadline))
                ps.setInt(4, task.targetCount)
                ps.setInt(5, task.remainingCount)
                ps.setString(6, task.status.name)
                ps.setString(7, task.notes)
                ps.setObject(8, task.seasonId)
                ps.setObject(9, task.successionScheduleId)
                ps.setObject(10, task.originGroupId)
                ps.setLong(11, task.id!!)
                ps.executeUpdate()
            }
        }
    }
```

- [ ] **Step 3: Add join table CRUD methods**

Add these methods to the repository:

```kotlin
    fun setAcceptableSpecies(taskId: Long, speciesIds: List<Long>) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM scheduled_task_species WHERE scheduled_task_id = ?").use { ps ->
                ps.setLong(1, taskId)
                ps.executeUpdate()
            }
            if (speciesIds.isNotEmpty()) {
                conn.prepareStatement("INSERT INTO scheduled_task_species (scheduled_task_id, species_id) VALUES (?, ?)").use { ps ->
                    for (speciesId in speciesIds) {
                        ps.setLong(1, taskId)
                        ps.setLong(2, speciesId)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }
        }
    }

    fun findAcceptableSpeciesIds(taskId: Long): List<Long> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT species_id FROM scheduled_task_species WHERE scheduled_task_id = ?").use { ps ->
                ps.setLong(1, taskId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.getLong("species_id")) }
                }
            }
        }

    fun findAcceptableSpeciesIdsByTaskIds(taskIds: Set<Long>): Map<Long, List<Long>> {
        if (taskIds.isEmpty()) return emptyMap()
        val placeholders = taskIds.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement("SELECT scheduled_task_id, species_id FROM scheduled_task_species WHERE scheduled_task_id IN ($placeholders)").use { ps ->
                taskIds.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    val result = mutableMapOf<Long, MutableList<Long>>()
                    while (rs.next()) {
                        result.getOrPut(rs.getLong("scheduled_task_id")) { mutableListOf() }
                            .add(rs.getLong("species_id"))
                    }
                    result
                }
            }
        }
    }
```

- [ ] **Step 4: Update `toScheduledTask` mapper to read `origin_group_id`**

```kotlin
    private fun ResultSet.toScheduledTask() = ScheduledTask(
        id = getLong("id"),
        orgId = getLong("org_id"),
        speciesId = getObject("species_id") as? Long,
        activityType = getString("activity_type"),
        deadline = getDate("deadline").toLocalDate(),
        targetCount = getInt("target_count"),
        remainingCount = getInt("remaining_count"),
        status = ScheduledTaskStatus.valueOf(getString("status")),
        notes = getString("notes"),
        seasonId = getObject("season_id") as? Long,
        successionScheduleId = getObject("succession_schedule_id") as? Long,
        originGroupId = getObject("origin_group_id") as? Long,
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/repository/ScheduledTaskRepository.kt
git commit -m "feat: update ScheduledTaskRepository for join table and nullable fields"
```

---

### Task 6: Service Layer — Group Task Creation and Species-Validated Completion

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/service/ScheduledTaskService.kt`

- [ ] **Step 1: Rewrite the service**

```kotlin
package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.ScheduledTask
import app.verdant.entity.ScheduledTaskStatus
import app.verdant.repository.ScheduledTaskRepository
import app.verdant.repository.SpeciesGroupRepository
import app.verdant.repository.SpeciesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class ScheduledTaskService(
    private val taskRepository: ScheduledTaskRepository,
    private val speciesRepository: SpeciesRepository,
    private val speciesGroupRepository: SpeciesGroupRepository,
) {
    private fun checkOwnership(taskId: Long, orgId: Long): ScheduledTask {
        val task = taskRepository.findById(taskId) ?: throw NotFoundException("Task not found")
        if (task.orgId != orgId) throw NotFoundException("Task not found")
        return task
    }

    fun getTasksForUser(orgId: Long, seasonId: Long? = null, limit: Int = 50, offset: Int = 0): List<ScheduledTaskResponse> {
        val tasks = if (seasonId != null) {
            taskRepository.findBySeasonId(orgId, seasonId, limit, offset)
        } else {
            taskRepository.findByOrgId(orgId, limit, offset)
        }
        return tasks.map { it.toResponse(tasks) }
    }

    fun getTask(taskId: Long, orgId: Long): ScheduledTaskResponse {
        val task = checkOwnership(taskId, orgId)
        return task.toResponse(listOf(task))
    }

    fun createTask(request: CreateScheduledTaskRequest, orgId: Long): ScheduledTaskResponse {
        val acceptableSpeciesIds: List<Long>
        var originGroupId: Long? = null

        if (request.speciesGroupId != null) {
            // Group task: snapshot species from the group, optionally filtered
            speciesGroupRepository.findById(request.speciesGroupId)
                ?: throw NotFoundException("Species group not found")
            originGroupId = request.speciesGroupId
            val groupSpecies = speciesRepository.findByGroupId(request.speciesGroupId)
            if (groupSpecies.isEmpty()) throw BadRequestException("Species group is empty")

            acceptableSpeciesIds = if (request.speciesIds != null) {
                // User provided a subset — validate all are in the group
                val groupIds = groupSpecies.map { it.id!! }.toSet()
                val invalid = request.speciesIds.filter { it !in groupIds }
                if (invalid.isNotEmpty()) throw BadRequestException("Species not in group: $invalid")
                request.speciesIds
            } else {
                groupSpecies.map { it.id!! }
            }
        } else if (request.speciesIds != null && request.speciesIds.size > 1) {
            // Custom multi-species task (no group origin)
            val found = speciesRepository.findByIds(request.speciesIds.toSet())
            if (found.size != request.speciesIds.size) throw NotFoundException("One or more species not found")
            acceptableSpeciesIds = request.speciesIds
        } else {
            // Single-species task (backward compatible)
            val singleId = request.speciesId ?: request.speciesIds?.firstOrNull()
                ?: throw BadRequestException("Either speciesId, speciesGroupId, or speciesIds must be provided")
            speciesRepository.findById(singleId) ?: throw NotFoundException("Species not found")
            acceptableSpeciesIds = listOf(singleId)
        }

        val task = taskRepository.persist(
            ScheduledTask(
                orgId = orgId,
                speciesId = if (acceptableSpeciesIds.size == 1) acceptableSpeciesIds.first() else null,
                activityType = request.activityType,
                deadline = request.deadline,
                targetCount = request.targetCount,
                remainingCount = request.targetCount,
                notes = request.notes,
                originGroupId = originGroupId,
            )
        )
        taskRepository.setAcceptableSpecies(task.id!!, acceptableSpeciesIds)
        return task.toResponse(listOf(task))
    }

    fun updateTask(taskId: Long, request: UpdateScheduledTaskRequest, orgId: Long): ScheduledTaskResponse {
        val task = checkOwnership(taskId, orgId)

        val newTarget = request.targetCount ?: task.targetCount
        val newRemaining = if (request.targetCount != null) {
            val completed = task.targetCount - task.remainingCount
            maxOf(newTarget - completed, 0)
        } else {
            task.remainingCount
        }
        val newStatus = if (newRemaining <= 0) ScheduledTaskStatus.COMPLETED else ScheduledTaskStatus.PENDING

        val updated = task.copy(
            activityType = request.activityType ?: task.activityType,
            deadline = request.deadline ?: task.deadline,
            targetCount = newTarget,
            remainingCount = newRemaining,
            status = newStatus,
            notes = request.notes ?: task.notes,
        )
        taskRepository.update(updated)
        return updated.toResponse(listOf(updated))
    }

    fun completePartially(taskId: Long, speciesId: Long, processedCount: Int, orgId: Long): ScheduledTaskResponse {
        checkOwnership(taskId, orgId)
        // Validate species is in the acceptable list
        val acceptableIds = taskRepository.findAcceptableSpeciesIds(taskId)
        if (speciesId !in acceptableIds) {
            throw BadRequestException("Species $speciesId is not in the acceptable species list for this task")
        }
        taskRepository.decrementRemainingCount(taskId, processedCount)
        val task = taskRepository.findById(taskId)!!
        return task.toResponse(listOf(task))
    }

    fun deleteTask(taskId: Long, orgId: Long) {
        checkOwnership(taskId, orgId)
        taskRepository.delete(taskId)
    }

    private fun ScheduledTask.toResponse(allTasks: List<ScheduledTask>): ScheduledTaskResponse {
        // Gather all species IDs needed across all tasks for batch lookup
        val taskIds = allTasks.map { it.id!! }.toSet()
        val acceptableByTask = taskRepository.findAcceptableSpeciesIdsByTaskIds(taskIds)
        val allSpeciesIds = acceptableByTask.values.flatten().toSet() +
            allTasks.mapNotNull { it.speciesId }.toSet()
        val speciesNames = speciesRepository.findNamesByIds(allSpeciesIds)

        // Group names
        val groupIds = allTasks.mapNotNull { it.originGroupId }.toSet()
        val groupNames = if (groupIds.isNotEmpty()) {
            groupIds.associateWith { id -> speciesGroupRepository.findById(id)?.name ?: "Unknown" }
        } else emptyMap()

        val myAcceptable = acceptableByTask[this.id] ?: emptyList()

        return ScheduledTaskResponse(
            id = id!!,
            speciesId = speciesId,
            speciesName = speciesId?.let { speciesNames[it] },
            activityType = activityType,
            deadline = deadline,
            targetCount = targetCount,
            remainingCount = remainingCount,
            status = status.name,
            notes = notes,
            seasonId = seasonId,
            successionScheduleId = successionScheduleId,
            originGroupId = originGroupId,
            originGroupName = originGroupId?.let { groupNames[it] },
            acceptableSpecies = myAcceptable.map { sid ->
                AcceptableSpeciesEntry(sid, speciesNames[sid] ?: "Unknown")
            },
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
```

Note: The `toResponse` method is now a standalone function that batch-fetches all data. This avoids N+1 queries when building lists.

- [ ] **Step 2: Verify compilation**

Run: `cd backend && ./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/service/ScheduledTaskService.kt
git commit -m "feat: service layer for group task creation and species-validated completion"
```

---

### Task 7: Resource Layer Update

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/resource/ScheduledTaskResource.kt`

- [ ] **Step 1: Update the completePartially endpoint**

```kotlin
    @POST
    @Path("/{id}/complete")
    fun completePartially(@PathParam("id") id: Long, @Valid request: CompleteTaskPartiallyRequest) =
        taskService.completePartially(id, request.speciesId, request.processedCount, orgContext.orgId)
```

The rest of the resource remains unchanged — create/update already delegate to the service.

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/resource/ScheduledTaskResource.kt
git commit -m "feat: pass speciesId through complete endpoint"
```

---

### Task 8: Backend Tests

**Files:**
- Modify: `backend/src/test/kotlin/app/verdant/service/ScheduledTaskServiceTest.kt`

- [ ] **Step 1: Update existing tests and add new ones**

```kotlin
package app.verdant.service

import app.verdant.dto.CreateScheduledTaskRequest
import app.verdant.dto.UpdateScheduledTaskRequest
import app.verdant.entity.ScheduledTask
import app.verdant.entity.ScheduledTaskStatus
import app.verdant.entity.Species
import app.verdant.entity.SpeciesGroup
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
    private val service = ScheduledTaskService(taskRepository, speciesRepository, speciesGroupRepository)

    private val orgId = 10L
    private val speciesId = 100L
    private val deadline = LocalDate.of(2025, 9, 1)
    private val speciesNames = mapOf(speciesId to "Zinnia")

    private fun makeTask(
        id: Long = 1L,
        targetCount: Int = 100,
        remainingCount: Int = 100,
        status: ScheduledTaskStatus = ScheduledTaskStatus.PENDING,
        speciesId: Long? = this.speciesId,
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

    private fun makeSpecies(id: Long, name: String, groupId: Long? = null) = Species(
        id = id,
        commonName = name,
        groupId = groupId,
    )

    // ── createTask with single species ───────────────────────────────────────

    @Test
    fun `createTask with single speciesId creates task with one acceptable species`() {
        val species = makeSpecies(speciesId, "Zinnia")
        whenever(speciesRepository.findById(speciesId)).thenReturn(species)
        whenever(taskRepository.persist(any())).thenAnswer { (it.arguments[0] as ScheduledTask).copy(id = 1L) }
        whenever(taskRepository.findAcceptableSpeciesIdsByTaskIds(setOf(1L))).thenReturn(mapOf(1L to listOf(speciesId)))
        whenever(speciesRepository.findNamesByIds(any())).thenReturn(speciesNames)

        val request = CreateScheduledTaskRequest(
            speciesId = speciesId,
            activityType = "SOW",
            deadline = deadline,
            targetCount = 50,
        )
        val result = service.createTask(request, orgId)

        verify(taskRepository).setAcceptableSpecies(1L, listOf(speciesId))
        assertEquals(speciesId, result.speciesId)
        assertEquals(1, result.acceptableSpecies.size)
        assertNull(result.originGroupId)
    }

    // ── createTask with group ────────────────────────────────────────────────

    @Test
    fun `createTask with speciesGroupId snapshots group members`() {
        val groupId = 5L
        val group = SpeciesGroup(id = groupId, orgId = orgId, name = "Pompon Dahlias")
        val sp1 = makeSpecies(101, "Cornel", groupId)
        val sp2 = makeSpecies(102, "Franz Kafka", groupId)

        whenever(speciesGroupRepository.findById(groupId)).thenReturn(group)
        whenever(speciesRepository.findByGroupId(groupId)).thenReturn(listOf(sp1, sp2))
        whenever(taskRepository.persist(any())).thenAnswer { (it.arguments[0] as ScheduledTask).copy(id = 2L) }
        whenever(taskRepository.findAcceptableSpeciesIdsByTaskIds(setOf(2L))).thenReturn(mapOf(2L to listOf(101L, 102L)))
        whenever(speciesRepository.findNamesByIds(any())).thenReturn(mapOf(101L to "Cornel", 102L to "Franz Kafka"))
        whenever(speciesGroupRepository.findById(groupId)).thenReturn(group)

        val request = CreateScheduledTaskRequest(
            speciesGroupId = groupId,
            activityType = "SOW",
            deadline = deadline,
            targetCount = 200,
        )
        val result = service.createTask(request, orgId)

        val captor = argumentCaptor<ScheduledTask>()
        verify(taskRepository).persist(captor.capture())
        assertNull(captor.firstValue.speciesId) // group task has no single species
        assertEquals(groupId, captor.firstValue.originGroupId)

        verify(taskRepository).setAcceptableSpecies(2L, listOf(101L, 102L))
        assertEquals(2, result.acceptableSpecies.size)
        assertEquals("Pompon Dahlias", result.originGroupName)
    }

    @Test
    fun `createTask with speciesGroupId and speciesIds subset only includes subset`() {
        val groupId = 5L
        val group = SpeciesGroup(id = groupId, orgId = orgId, name = "Pompon Dahlias")
        val sp1 = makeSpecies(101, "Cornel", groupId)
        val sp2 = makeSpecies(102, "Franz Kafka", groupId)
        val sp3 = makeSpecies(103, "Wizard of Oz", groupId)

        whenever(speciesGroupRepository.findById(groupId)).thenReturn(group)
        whenever(speciesRepository.findByGroupId(groupId)).thenReturn(listOf(sp1, sp2, sp3))
        whenever(taskRepository.persist(any())).thenAnswer { (it.arguments[0] as ScheduledTask).copy(id = 3L) }
        whenever(taskRepository.findAcceptableSpeciesIdsByTaskIds(setOf(3L))).thenReturn(mapOf(3L to listOf(101L, 102L)))
        whenever(speciesRepository.findNamesByIds(any())).thenReturn(mapOf(101L to "Cornel", 102L to "Franz Kafka"))
        whenever(speciesGroupRepository.findById(groupId)).thenReturn(group)

        val request = CreateScheduledTaskRequest(
            speciesGroupId = groupId,
            speciesIds = listOf(101L, 102L), // exclude Wizard of Oz
            activityType = "SOW",
            deadline = deadline,
            targetCount = 100,
        )
        service.createTask(request, orgId)

        verify(taskRepository).setAcceptableSpecies(3L, listOf(101L, 102L))
    }

    @Test
    fun `createTask with speciesGroupId rejects speciesIds not in group`() {
        val groupId = 5L
        val group = SpeciesGroup(id = groupId, orgId = orgId, name = "Pompon Dahlias")
        val sp1 = makeSpecies(101, "Cornel", groupId)

        whenever(speciesGroupRepository.findById(groupId)).thenReturn(group)
        whenever(speciesRepository.findByGroupId(groupId)).thenReturn(listOf(sp1))

        val request = CreateScheduledTaskRequest(
            speciesGroupId = groupId,
            speciesIds = listOf(101L, 999L), // 999 not in group
            activityType = "SOW",
            deadline = deadline,
            targetCount = 100,
        )

        assertThrows<BadRequestException> {
            service.createTask(request, orgId)
        }
    }

    @Test
    fun `createTask with empty group throws BadRequestException`() {
        val groupId = 5L
        whenever(speciesGroupRepository.findById(groupId)).thenReturn(SpeciesGroup(groupId, orgId, "Empty"))
        whenever(speciesRepository.findByGroupId(groupId)).thenReturn(emptyList())

        val request = CreateScheduledTaskRequest(
            speciesGroupId = groupId,
            activityType = "SOW",
            deadline = deadline,
            targetCount = 100,
        )

        assertThrows<BadRequestException> {
            service.createTask(request, orgId)
        }
    }

    // ── completePartially with species validation ────────────────────────────

    @Test
    fun `completePartially validates species is in acceptable list`() {
        val task = makeTask(speciesId = null, originGroupId = 5L)
        whenever(taskRepository.findById(1L)).thenReturn(task)
        whenever(taskRepository.findAcceptableSpeciesIds(1L)).thenReturn(listOf(101L, 102L))

        assertThrows<BadRequestException> {
            service.completePartially(taskId = 1L, speciesId = 999L, processedCount = 10, orgId = orgId)
        }
    }

    @Test
    fun `completePartially succeeds with valid species from acceptable list`() {
        val task = makeTask(speciesId = null, originGroupId = 5L, targetCount = 100, remainingCount = 50)
        val afterDecrement = task.copy(remainingCount = 30)

        whenever(taskRepository.findById(1L))
            .thenReturn(task)
            .thenReturn(afterDecrement)
        whenever(taskRepository.findAcceptableSpeciesIds(1L)).thenReturn(listOf(101L, 102L))
        whenever(taskRepository.findAcceptableSpeciesIdsByTaskIds(setOf(1L))).thenReturn(mapOf(1L to listOf(101L, 102L)))
        whenever(speciesRepository.findNamesByIds(any())).thenReturn(mapOf(101L to "Cornel", 102L to "Franz Kafka"))
        whenever(speciesGroupRepository.findById(5L)).thenReturn(SpeciesGroup(5L, orgId, "Pompon Dahlias"))

        val result = service.completePartially(taskId = 1L, speciesId = 101L, processedCount = 20, orgId = orgId)

        verify(taskRepository).decrementRemainingCount(1L, 20)
        assertEquals(30, result.remainingCount)
    }

    // ── updateTask (unchanged behavior, updated for new constructor) ─────────

    @Test
    fun `updateTask recalculates remaining count when targetCount is updated`() {
        val existing = makeTask(targetCount = 100, remainingCount = 60)
        val captor = argumentCaptor<ScheduledTask>()

        whenever(taskRepository.findById(1L)).thenReturn(existing)
        whenever(taskRepository.findAcceptableSpeciesIdsByTaskIds(setOf(1L))).thenReturn(mapOf(1L to listOf(speciesId)))
        whenever(speciesRepository.findNamesByIds(any())).thenReturn(speciesNames)
        whenever(taskRepository.update(any())).then { }

        val request = UpdateScheduledTaskRequest(targetCount = 120)
        val result = service.updateTask(taskId = 1L, request = request, orgId = orgId)

        assertEquals(120, result.targetCount)
        assertEquals(80, result.remainingCount)
        assertEquals(ScheduledTaskStatus.PENDING.name, result.status)

        verify(taskRepository).update(captor.capture())
        val saved = captor.firstValue
        assertEquals(120, saved.targetCount)
        assertEquals(80, saved.remainingCount)
    }

    @Test
    fun `updateTask sets status to COMPLETED when new remaining count reaches zero`() {
        val existing = makeTask(targetCount = 100, remainingCount = 20)
        val captor = argumentCaptor<ScheduledTask>()

        whenever(taskRepository.findById(1L)).thenReturn(existing)
        whenever(taskRepository.findAcceptableSpeciesIdsByTaskIds(setOf(1L))).thenReturn(mapOf(1L to listOf(speciesId)))
        whenever(speciesRepository.findNamesByIds(any())).thenReturn(speciesNames)
        whenever(taskRepository.update(any())).then { }

        val request = UpdateScheduledTaskRequest(targetCount = 80)
        val result = service.updateTask(taskId = 1L, request = request, orgId = orgId)

        assertEquals(0, result.remainingCount)
        assertEquals(ScheduledTaskStatus.COMPLETED.name, result.status)
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
}
```

- [ ] **Step 2: Run tests**

Run: `cd backend && ./gradlew test`
Expected: All tests pass.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/kotlin/app/verdant/service/ScheduledTaskServiceTest.kt
git commit -m "test: add tests for group task creation and species-validated completion"
```

---

### Task 9: Frontend API Client Types

**Files:**
- Modify: `web/src/api/client.ts:148-153` (ScheduledTaskResponse) and `web/src/api/client.ts:409-421` (tasks API methods)

- [ ] **Step 1: Update ScheduledTaskResponse type**

Replace the existing `ScheduledTaskResponse` interface (lines 148-153):

```typescript
export interface AcceptableSpeciesEntry {
  speciesId: number
  speciesName: string
}

export interface ScheduledTaskResponse {
  id: number; speciesId: number | null; speciesName: string | null; activityType: string
  deadline: string; targetCount: number; remainingCount: number
  status: string; notes?: string; seasonId?: number; successionScheduleId?: number
  originGroupId?: number; originGroupName?: string
  acceptableSpecies: AcceptableSpeciesEntry[]
  createdAt: string; updatedAt: string
}
```

- [ ] **Step 2: Update tasks API methods**

Replace the tasks section (lines 409-421):

```typescript
  tasks: {
    list: () => apiRequest<ScheduledTaskResponse[]>('/api/tasks'),
    get: (id: number) => apiRequest<ScheduledTaskResponse>(`/api/tasks/${id}`),
    create: (data: {
      speciesId?: number; speciesGroupId?: number; speciesIds?: number[]
      activityType: string; deadline: string; targetCount: number; notes?: string
    }) => apiRequest<ScheduledTaskResponse>('/api/tasks', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: {
      activityType?: string; deadline?: string; targetCount?: number; notes?: string
    }) => apiRequest<ScheduledTaskResponse>(`/api/tasks/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    complete: (id: number, speciesId: number, processedCount: number) =>
      apiRequest<void>(`/api/tasks/${id}/complete`, { method: 'POST', body: JSON.stringify({ speciesId, processedCount }) }),
    delete: (id: number) => apiRequest<void>(`/api/tasks/${id}`, { method: 'DELETE' }),
  },
```

- [ ] **Step 3: Add species groups API**

Add after the existing `species` section (after line 395):

```typescript
  speciesGroups: {
    list: () => apiRequest<{ id: number; name: string }[]>('/api/species/groups'),
  },
```

- [ ] **Step 4: Commit**

```bash
git add web/src/api/client.ts
git commit -m "feat: update frontend API types for group tasks"
```

---

### Task 10: Unified SpeciesAutocomplete — Search Both Species and Groups

**Files:**
- Modify: `web/src/components/SpeciesAutocomplete.tsx`

- [ ] **Step 1: Rewrite SpeciesAutocomplete to support groups**

```tsx
import { useState, useEffect, useRef } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'

export type AutocompleteSelection =
  | { type: 'species'; species: SpeciesResponse }
  | { type: 'group'; groupId: number; groupName: string; speciesIds: number[] }

interface Props {
  value: SpeciesResponse | null
  onChange: (species: SpeciesResponse | null) => void
  onGroupSelect?: (groupId: number, groupName: string) => void
  placeholder?: string
  showGroups?: boolean
}

function speciesLabel(s: SpeciesResponse, lang: string) {
  const name = lang === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
  const variant = lang === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
  return variant ? `${name} — ${variant}` : name
}

export function SpeciesAutocomplete({ value, onChange, onGroupSelect, placeholder, showGroups = false }: Props) {
  const { t, i18n } = useTranslation()
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!search) { setDebouncedSearch(''); return }
    const timer = setTimeout(() => setDebouncedSearch(search), 250)
    return () => clearTimeout(timer)
  }, [search])

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const { data: results, isFetching } = useQuery({
    queryKey: ['species-search', debouncedSearch],
    queryFn: () => api.species.search(debouncedSearch),
    enabled: debouncedSearch.length >= 1,
    staleTime: 60_000,
  })

  const { data: groups } = useQuery({
    queryKey: ['species-groups'],
    queryFn: api.speciesGroups.list,
    enabled: showGroups,
    staleTime: 60_000,
  })

  // Filter groups by search term
  const filteredGroups = showGroups && debouncedSearch && groups
    ? groups.filter(g => g.name.toLowerCase().includes(debouncedSearch.toLowerCase()))
    : []

  const displayValue = value ? speciesLabel(value, i18n.language) : ''

  return (
    <div className="relative" ref={ref}>
      <input
        value={open ? search : displayValue}
        onChange={e => { setSearch(e.target.value); onChange(null); setOpen(true) }}
        onFocus={() => { if (!value) setOpen(true) }}
        placeholder={placeholder ?? t('common.searchSpecies')}
        className="input w-full"
      />
      {open && debouncedSearch && (
        <div className="absolute z-10 left-0 right-0 mt-1 border border-divider rounded-xl bg-bg shadow-md max-h-48 overflow-y-auto">
          {isFetching && (!results || results.length === 0) && filteredGroups.length === 0 && (
            <p className="px-3 py-2 text-sm text-text-secondary">...</p>
          )}
          {filteredGroups.map(g => (
            <button
              key={`group-${g.id}`}
              onClick={() => {
                onGroupSelect?.(g.id, g.name)
                setSearch('')
                setOpen(false)
              }}
              className="w-full text-left px-3 py-2 text-sm hover:bg-surface transition-colors flex items-center gap-2"
            >
              <span className="text-xs bg-accent/15 text-accent px-1.5 py-0.5 rounded">{t('common.group')}</span>
              {g.name}
            </button>
          ))}
          {filteredGroups.length > 0 && results && results.length > 0 && (
            <div className="border-t border-divider" />
          )}
          {results?.map(s => (
            <button
              key={s.id}
              onClick={() => { onChange(s); setSearch(''); setOpen(false) }}
              className="w-full text-left px-3 py-2 text-sm hover:bg-surface transition-colors"
            >
              {speciesLabel(s, i18n.language)}
            </button>
          ))}
          {results && results.length === 0 && filteredGroups.length === 0 && !isFetching && (
            <p className="px-3 py-2 text-sm text-text-secondary">{t('species.noSpeciesFoundDropdown')}</p>
          )}
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Add i18n key for "group" label**

Check which i18n file is used and add `common.group` key. If using JSON translation files, add `"group": "Group"` (en) and `"group": "Grupp"` (sv) under the `common` namespace.

- [ ] **Step 3: Commit**

```bash
git add web/src/components/SpeciesAutocomplete.tsx
git commit -m "feat: unified species+group autocomplete"
```

---

### Task 11: TaskForm — Group Selection with Editable Species Checklist

**Files:**
- Modify: `web/src/pages/TaskForm.tsx`

- [ ] **Step 1: Rewrite TaskForm**

```tsx
import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import type { BreadcrumbItem } from '../components/Breadcrumb'
import { OnboardingHint } from '../onboarding/OnboardingHint'
import { useOnboarding } from '../onboarding/OnboardingContext'

const activityTypes = ['SOW', 'POT_UP', 'PLANT', 'HARVEST', 'RECOVER', 'DISCARD']

function speciesLabel(s: SpeciesResponse, lang: string) {
  const name = lang === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
  const variant = lang === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
  return variant ? `${name} — ${variant}` : name
}

export function TaskForm() {
  const { taskId } = useParams<{ taskId: string }>()
  const isEdit = !!taskId
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t, i18n } = useTranslation()
  const { completeStep } = useOnboarding()

  const { data: existing } = useQuery({
    queryKey: ['task', Number(taskId)],
    queryFn: () => api.tasks.get(Number(taskId)),
    enabled: isEdit,
  })

  // Fetch preset species for edit mode
  const { data: presetSpecies } = useQuery({
    queryKey: ['species-by-id', existing?.speciesId],
    queryFn: () => api.species.search(String(existing!.speciesId), 1).then(list => list.find(s => s.id === existing!.speciesId) ?? null),
    enabled: !!existing?.speciesId,
  })

  const [selectedSpecies, setSelectedSpecies] = useState<SpeciesResponse | null>(null)
  const [selectedGroupId, setSelectedGroupId] = useState<number | null>(null)
  const [selectedGroupName, setSelectedGroupName] = useState<string>('')
  const [groupSpecies, setGroupSpecies] = useState<SpeciesResponse[]>([])
  const [checkedSpeciesIds, setCheckedSpeciesIds] = useState<Set<number>>(new Set())
  const [activityType, setActivityType] = useState('SOW')
  const [deadline, setDeadline] = useState('')
  const [targetCount, setTargetCount] = useState('')
  const [notes, setNotes] = useState('')

  // Fetch group member species when a group is selected
  const { data: allSpecies } = useQuery({
    queryKey: ['species'],
    queryFn: api.species.list,
    enabled: !!selectedGroupId,
  })

  useEffect(() => {
    if (selectedGroupId && allSpecies) {
      // The species API doesn't filter by group, so we need to check groupId on SpeciesResponse.
      // Since SpeciesResponse doesn't include groupId, we fetch all and match via the groups endpoint.
      // Alternative: use a dedicated endpoint. For now, fetch species search with group name.
      // Actually, the backend will resolve group members. We just need the species for the checklist.
      // We'll call search with the group name to approximate, but ideally we'd have a dedicated endpoint.
      // For now, let's search by the group name to find related species.
      api.species.search(selectedGroupName, 100).then(results => {
        // This is imperfect — ideally the backend would have GET /api/species?groupId=X
        // For the MVP, we'll show all results and let the user check/uncheck
        setGroupSpecies(results)
        setCheckedSpeciesIds(new Set(results.map(s => s.id)))
      })
    }
  }, [selectedGroupId, allSpecies, selectedGroupName])

  useEffect(() => {
    if (existing) {
      setActivityType(existing.activityType)
      setDeadline(existing.deadline)
      setTargetCount(String(existing.targetCount))
      setNotes(existing.notes ?? '')
    }
  }, [existing])

  useEffect(() => {
    if (presetSpecies && !selectedSpecies) setSelectedSpecies(presetSpecies)
  }, [presetSpecies, selectedSpecies])

  const handleGroupSelect = (groupId: number, groupName: string) => {
    setSelectedSpecies(null)
    setSelectedGroupId(groupId)
    setSelectedGroupName(groupName)
  }

  const handleSpeciesSelect = (species: SpeciesResponse | null) => {
    setSelectedSpecies(species)
    setSelectedGroupId(null)
    setSelectedGroupName('')
    setGroupSpecies([])
    setCheckedSpeciesIds(new Set())
  }

  const toggleSpecies = (id: number) => {
    setCheckedSpeciesIds(prev => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const isGroupMode = selectedGroupId !== null
  const hasSelection = isGroupMode ? checkedSpeciesIds.size > 0 : !!selectedSpecies
  const valid = hasSelection && deadline && Number(targetCount) > 0

  const createMut = useMutation({
    mutationFn: () => {
      if (isGroupMode) {
        return api.tasks.create({
          speciesGroupId: selectedGroupId!,
          speciesIds: Array.from(checkedSpeciesIds),
          activityType,
          deadline,
          targetCount: Number(targetCount),
          notes: notes || undefined,
        })
      }
      return api.tasks.create({
        speciesId: selectedSpecies!.id,
        activityType,
        deadline,
        targetCount: Number(targetCount),
        notes: notes || undefined,
      })
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tasks'] }); completeStep('create_task'); navigate('/tasks', { replace: true }) },
  })

  const updateMut = useMutation({
    mutationFn: () => api.tasks.update(Number(taskId), {
      activityType, deadline, targetCount: Number(targetCount), notes: notes || undefined,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tasks'] }); navigate('/tasks', { replace: true }) },
  })

  const mutation = isEdit ? updateMut : createMut

  const breadcrumbs: BreadcrumbItem[] = [{ label: t('nav.tasks'), to: '/tasks' }]

  return (
    <div className="max-w-lg">
      <PageHeader title={isEdit ? t('tasks.editTaskTitle') : t('tasks.newTaskTitle')} breadcrumbs={breadcrumbs} />
      <OnboardingHint />
      <div data-onboarding="task-form" className="form-card">
        <div>
          <label className="field-label">{t('tasks.activityLabel')}</label>
          <select value={activityType} onChange={e => setActivityType(e.target.value)} className="input w-full">
            {activityTypes.map(tp => (
              <option key={tp} value={tp}>{t(`activityType.${tp}`, { defaultValue: tp.replace(/_/g, ' ') })}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="field-label">{t('common.speciesLabel')}</label>
          {isGroupMode ? (
            <div>
              <div className="flex items-center gap-2 mb-2">
                <span className="text-xs bg-accent/15 text-accent px-1.5 py-0.5 rounded">{t('common.group')}</span>
                <span className="text-sm font-medium">{selectedGroupName}</span>
                <button onClick={() => handleSpeciesSelect(null)} className="text-xs text-text-secondary ml-auto">{t('common.clear')}</button>
              </div>
              <div className="border border-divider rounded-xl p-2 max-h-48 overflow-y-auto space-y-1">
                {groupSpecies.map(s => (
                  <label key={s.id} className="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-surface cursor-pointer text-sm">
                    <input
                      type="checkbox"
                      checked={checkedSpeciesIds.has(s.id)}
                      onChange={() => toggleSpecies(s.id)}
                      className="rounded"
                    />
                    {speciesLabel(s, i18n.language)}
                  </label>
                ))}
                {groupSpecies.length === 0 && (
                  <p className="text-xs text-text-secondary px-2 py-1">{t('tasks.loadingGroupSpecies')}</p>
                )}
              </div>
            </div>
          ) : (
            <SpeciesAutocomplete
              value={selectedSpecies}
              onChange={handleSpeciesSelect}
              onGroupSelect={handleGroupSelect}
              showGroups
            />
          )}
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="field-label">{t('tasks.deadlineLabel')}</label>
            <input type="date" value={deadline} onChange={e => setDeadline(e.target.value)} className="input w-full" />
          </div>
          <div>
            <label className="field-label">{t('tasks.targetCountLabel')}</label>
            <input type="number" value={targetCount} onChange={e => setTargetCount(e.target.value)} placeholder="e.g. 10" className="input w-full" />
          </div>
        </div>

        <div>
          <label className="field-label">{t('common.notesLabel')}</label>
          <textarea value={notes} onChange={e => setNotes(e.target.value)} placeholder={t('common.optional')} rows={2} className="input w-full" />
        </div>
      </div>

      {mutation.error && <p className="text-error text-sm mt-3">{mutation.error instanceof Error ? mutation.error.message : String(mutation.error)}</p>}
      <div className="mt-4 flex justify-end">
        <button onClick={() => mutation.mutate()} disabled={!valid || mutation.isPending} className="btn-primary">
          {mutation.isPending ? t('common.saving') : isEdit ? t('tasks.updateTask') : t('tasks.createTask')}
        </button>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Add i18n keys**

Add these keys to the translation files:
- `tasks.loadingGroupSpecies`: "Loading species..." / "Laddar arter..."
- `common.clear`: "Clear" / "Rensa"

- [ ] **Step 3: Commit**

```bash
git add web/src/pages/TaskForm.tsx
git commit -m "feat: task form with group selection and editable species checklist"
```

---

### Task 12: Backend — Species by Group Endpoint

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/resource/SpeciesResource.kt:65-83`
- Modify: `backend/src/main/kotlin/app/verdant/service/SpeciesService.kt` (add method)

The TaskForm needs to fetch species belonging to a group. Add a query parameter to the species list endpoint.

- [ ] **Step 1: Update species list endpoint to accept groupId filter**

In `SpeciesResource.kt`, update the `list` method:

```kotlin
    @GET
    fun list(
        @QueryParam("q") query: String?,
        @QueryParam("groupId") groupId: Long?,
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = when {
        groupId != null -> speciesService.getSpeciesByGroup(groupId, orgContext.orgId)
        query.isNullOrBlank() -> speciesService.getSpeciesForUser(orgContext.orgId, limit.coerceIn(1, 200), offset.coerceAtLeast(0))
        else -> speciesService.searchSpeciesForUser(orgContext.orgId, query.trim(), limit.coerceIn(1, 200))
    }
```

- [ ] **Step 2: Add service method**

In `SpeciesService`, add:

```kotlin
    fun getSpeciesByGroup(groupId: Long, orgId: Long): List<SpeciesResponse> {
        val species = speciesRepository.findByGroupId(groupId)
        // Filter to species visible to this org
        return species.filter { it.orgId == null || it.orgId == orgId }
            .map { it.toResponse(/* existing toResponse pattern */) }
    }
```

Adapt to match the existing `toResponse` pattern in SpeciesService.

- [ ] **Step 3: Update frontend API client**

Add to the species section in `client.ts`:

```typescript
    byGroup: (groupId: number) => apiRequest<SpeciesResponse[]>(`/api/species?groupId=${groupId}`),
```

- [ ] **Step 4: Update TaskForm to use the byGroup endpoint**

Replace the `useEffect` that fetches group species (the one using `api.species.search(selectedGroupName, 100)`) with:

```tsx
  const { data: fetchedGroupSpecies } = useQuery({
    queryKey: ['species-by-group', selectedGroupId],
    queryFn: () => api.species.byGroup(selectedGroupId!),
    enabled: !!selectedGroupId,
  })

  useEffect(() => {
    if (fetchedGroupSpecies) {
      setGroupSpecies(fetchedGroupSpecies)
      setCheckedSpeciesIds(new Set(fetchedGroupSpecies.map(s => s.id)))
    }
  }, [fetchedGroupSpecies])
```

Remove the `allSpecies` query and the old `useEffect` that called `api.species.search`.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/resource/SpeciesResource.kt
git add backend/src/main/kotlin/app/verdant/service/SpeciesService.kt
git add web/src/api/client.ts
git add web/src/pages/TaskForm.tsx
git commit -m "feat: species by group endpoint and clean TaskForm group fetching"
```

---

### Task 13: TaskList — Group Task Display

**Files:**
- Modify: `web/src/pages/TaskList.tsx`

- [ ] **Step 1: Update task card to show group name with indicator**

Replace the species name line (line 67) and the perform button logic (lines 72-79):

```tsx
                  <p className="text-sm text-text-secondary">
                    {task.originGroupName ? (
                      <span className="inline-flex items-center gap-1">
                        <span className="text-xs bg-accent/15 text-accent px-1 py-0.5 rounded">{t('common.group')}</span>
                        {task.originGroupName}
                      </span>
                    ) : (
                      task.speciesName
                    )}
                  </p>
```

For the "Perform" button, update the navigation to handle group tasks. A group task doesn't have a single speciesId, so navigate without it and let SowActivity handle the species picker:

```tsx
                {!isCompleted && (
                  <button
                    onClick={() => {
                      const params = new URLSearchParams({ taskId: String(task.id) })
                      if (task.speciesId) params.set('speciesId', String(task.speciesId))
                      navigate(`/sow?${params}`)
                    }}
                    className="btn-primary text-xs py-1.5 px-3 flex-1"
                  >
                    {t('tasks.perform')}
                  </button>
                )}
```

- [ ] **Step 2: Commit**

```bash
git add web/src/pages/TaskList.tsx
git commit -m "feat: display group name with indicator in task list"
```

---

### Task 14: SowActivity — Species Selection for Group Tasks

**Files:**
- Modify: `web/src/pages/SowActivity.tsx`

- [ ] **Step 1: Add species picker from acceptable list for group tasks**

The SowActivity needs to let the user pick which species they're sowing when completing a group task. Update the component:

After the `task` query (line 24-28), add awareness of acceptable species:

```tsx
  const isGroupTask = task ? task.acceptableSpecies.length > 1 : false
```

After the SpeciesAutocomplete section (around line 136-142), conditionally show a dropdown instead when the task is a group task:

Replace the species selection section:

```tsx
        <div data-onboarding="sow-species">
          <label className="field-label">{t('common.speciesLabel')}</label>
          {isGroupTask && task ? (
            <select
              value={speciesId}
              onChange={e => {
                const sp = allSpecies?.find(s => s.id === Number(e.target.value)) ?? null
                setSelectedSpecies(sp)
              }}
              className="input w-full"
            >
              <option value="">{t('sow.selectSpecies')}</option>
              {task.acceptableSpecies.map(as => (
                <option key={as.speciesId} value={as.speciesId}>{as.speciesName}</option>
              ))}
            </select>
          ) : (
            <SpeciesAutocomplete
              value={selectedSpecies}
              onChange={s => { setSelectedSpecies(s); setSeedBatchId('') }}
            />
          )}
        </div>
```

Update the `sowMut` to pass speciesId to the complete call. Change line 108-109:

```tsx
      if (taskId && count > 0) {
        await api.tasks.complete(taskId, Number(speciesId), count)
      }
```

- [ ] **Step 2: Add i18n key**

Add `sow.selectSpecies`: "Select species" / "Välj art"

- [ ] **Step 3: Commit**

```bash
git add web/src/pages/SowActivity.tsx
git commit -m "feat: species selection from acceptable list when completing group tasks"
```

---

### Task 15: Add i18n Translation Keys

**Files:**
- Modify: translation JSON files (find the exact paths in `web/src/`)

- [ ] **Step 1: Find translation files**

Run: `find web/src -name '*.json' | grep -i 'i18n\|locale\|lang\|translation'`

- [ ] **Step 2: Add all new keys**

English:
```json
"common.group": "Group",
"common.clear": "Clear",
"tasks.loadingGroupSpecies": "Loading species...",
"sow.selectSpecies": "Select species"
```

Swedish:
```json
"common.group": "Grupp",
"common.clear": "Rensa",
"tasks.loadingGroupSpecies": "Laddar arter...",
"sow.selectSpecies": "Välj art"
```

- [ ] **Step 3: Commit**

```bash
git add web/src/
git commit -m "feat: add i18n keys for group task UI"
```

---

### Task 16: End-to-End Verification

- [ ] **Step 1: Start the backend**

Run: `cd backend && ./gradlew quarkusDev`
Expected: Starts without errors, V9 migration applied.

- [ ] **Step 2: Start the web app**

Run: `cd web && npm run dev`
Expected: No TypeScript errors, app loads.

- [ ] **Step 3: Manual verification checklist**

Test these flows:
1. Create a single-species task (existing behavior still works)
2. Search in the task form autocomplete — groups appear with "Group" badge
3. Select a group — species checklist appears, all pre-checked
4. Uncheck one species, create the group task
5. Group task appears in task list with group name and indicator
6. Click "Perform" on the group task — species dropdown shows acceptable species
7. Complete partially with a specific species — remaining count decrements
8. Try completing with a species not in the acceptable list — should get error

- [ ] **Step 4: Run backend tests**

Run: `cd backend && ./gradlew test`
Expected: All tests pass.

- [ ] **Step 5: Run frontend checks**

Run: `cd web && npx tsc --noEmit`
Expected: No type errors.
