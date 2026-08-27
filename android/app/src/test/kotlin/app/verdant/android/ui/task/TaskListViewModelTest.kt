package app.verdant.android.ui.task

import app.cash.turbine.test
import app.verdant.android.data.model.CompleteTaskPartiallyRequest
import app.verdant.android.data.model.CreateScheduledTaskRequest
import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.data.model.UpdateScheduledTaskRequest
import app.verdant.android.data.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loadTasks emits success state with tasks`() = runTest {
        val tasks = listOf(
            sampleTask(id = 1, deadline = "2026-05-01"),
            sampleTask(id = 2, deadline = "2026-05-02"),
        )
        val vm = TaskListViewModel(FakeTaskRepository(tasks))

        vm.uiState.test {
            // initial load triggered in init {}
            assertEquals(TaskListState(isLoading = true), awaitItem())
            advanceUntilIdle()
            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(tasks, loaded.tasks)
            assertNull(loaded.error)
        }
    }

    @Test
    fun `deleteTask removes task from state without reloading`() = runTest {
        val tasks = listOf(
            sampleTask(id = 1, deadline = "2026-05-01"),
            sampleTask(id = 2, deadline = "2026-05-02"),
        )
        val repo = FakeTaskRepository(tasks)
        val vm = TaskListViewModel(repo)
        advanceUntilIdle()

        vm.deleteTask(1)
        advanceUntilIdle()

        assertEquals(listOf(2L), vm.uiState.value.tasks.map { it.id })
        assertEquals(listOf(1L), repo.deletedIds)
    }

    @Test
    fun `loadTasks emits error state when repository fails on first load`() = runTest {
        val vm = TaskListViewModel(FakeTaskRepository(throwOnList = RuntimeException("boom")))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("boom", state.error)
    }

    @Test
    fun `completeTask on area-scoped task posts speciesId null and processedCount remainingCount`() = runTest {
        val task = ScheduledTaskResponse(
            id = 42,
            speciesId = null,
            speciesName = null,
            bedId = null,
            bedName = null,
            gardenAreaId = 7,
            gardenAreaName = "Komposthög",
            maintenanceRuleId = 3,
            activityType = "MOW",
            deadline = "2026-05-01",
            targetCount = 1,
            remainingCount = 1,
            status = "PENDING",
            notes = null,
            createdAt = "2026-04-01T00:00:00Z",
            updatedAt = "2026-04-01T00:00:00Z",
        )
        val repo = FakeTaskRepository(listOf(task))
        val vm = TaskListViewModel(repo)
        advanceUntilIdle()

        vm.completeTask(task)
        advanceUntilIdle()

        val (id, request) = repo.completedRequests.single()
        assertEquals(42L, id)
        assertNull(request.speciesId)
        assertEquals(1, request.processedCount)
        assertEquals(emptyList<Long>(), vm.uiState.value.tasks.map { it.id })
    }

    @Test
    fun `isPlaceScopedTask is true for an area task and drives a readable title, not the raw enum`() {
        val areaTask = ScheduledTaskResponse(
            id = 1,
            speciesId = null,
            speciesName = null,
            bedId = null,
            bedName = null,
            gardenAreaId = 7,
            gardenAreaName = "Komposthög",
            maintenanceRuleId = 3,
            activityType = "MOW",
            deadline = "2026-05-01",
            targetCount = 1,
            remainingCount = 1,
            status = "PENDING",
            notes = null,
            createdAt = "2026-04-01T00:00:00Z",
            updatedAt = "2026-04-01T00:00:00Z",
        )

        assertTrue(isPlaceScopedTask(areaTask))
        assertEquals("Komposthög", taskPlaceName(areaTask))
        assertTrue(areaTask.isRuleBacked())
    }

    @Test
    fun `isPlaceScopedTask is false for a species task so it keeps the species-based title`() {
        val speciesTask = sampleTask(id = 5, deadline = "2026-05-01")

        assertFalse(isPlaceScopedTask(speciesTask))
        assertFalse(speciesTask.isRuleBacked())
    }

    @Test
    fun `TODO task uses notes as title and has no species or bed`() = runTest {
        val tasks = listOf(
            ScheduledTaskResponse(
                id = 99,
                speciesId = null,
                speciesName = null,
                activityType = "TODO",
                deadline = null,
                targetCount = 1,
                remainingCount = 1,
                status = "PENDING",
                notes = "Beställ nya pinnar",
                createdAt = "2026-04-01T00:00:00Z",
                updatedAt = "2026-04-01T00:00:00Z",
            ),
        )
        val vm = TaskListViewModel(FakeTaskRepository(tasks))
        advanceUntilIdle()

        val loaded = vm.uiState.value.tasks.single()
        assertEquals("TODO", loaded.activityType)
        assertNull(loaded.deadline)
        assertEquals("Beställ nya pinnar", loaded.notes)
    }

    private fun sampleTask(id: Long, deadline: String) = ScheduledTaskResponse(
        id = id,
        speciesId = 100,
        speciesName = "Calendula",
        activityType = "SOW",
        deadline = deadline,
        targetCount = 10,
        remainingCount = 10,
        status = "PENDING",
        notes = null,
        createdAt = "2026-04-01T00:00:00Z",
        updatedAt = "2026-04-01T00:00:00Z",
    )
}

private class FakeTaskRepository(
    private val initial: List<ScheduledTaskResponse> = emptyList(),
    private val throwOnList: Throwable? = null,
) : TaskRepository {
    val deletedIds = mutableListOf<Long>()
    val completedRequests = mutableListOf<Pair<Long, CompleteTaskPartiallyRequest>>()

    override suspend fun list(): List<ScheduledTaskResponse> {
        throwOnList?.let { throw it }
        return initial
    }
    override suspend fun get(id: Long) = initial.first { it.id == id }
    override suspend fun create(request: CreateScheduledTaskRequest) = error("not used")
    override suspend fun update(id: Long, request: UpdateScheduledTaskRequest) = error("not used")
    override suspend fun completePartially(id: Long, request: CompleteTaskPartiallyRequest) {
        completedRequests += id to request
    }
    override suspend fun delete(id: Long) { deletedIds += id }
}
