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

    override suspend fun list(): List<ScheduledTaskResponse> {
        throwOnList?.let { throw it }
        return initial
    }
    override suspend fun get(id: Long) = initial.first { it.id == id }
    override suspend fun create(request: CreateScheduledTaskRequest) = error("not used")
    override suspend fun update(id: Long, request: UpdateScheduledTaskRequest) = error("not used")
    override suspend fun completePartially(id: Long, request: CompleteTaskPartiallyRequest) {}
    override suspend fun delete(id: Long) { deletedIds += id }
}
