package app.verdant.android.ui.area

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.verdant.android.data.model.CreateGardenAreaEventRequest
import app.verdant.android.data.model.CreateGardenAreaPhotoRequest
import app.verdant.android.data.model.CreateGardenAreaRequest
import app.verdant.android.data.model.CreateMaintenanceRuleRequest
import app.verdant.android.data.model.GardenAreaEventResponse
import app.verdant.android.data.model.GardenAreaPhotoResponse
import app.verdant.android.data.model.GardenAreaResponse
import app.verdant.android.data.model.MaintenanceRuleResponse
import app.verdant.android.data.model.UpdateGardenAreaRequest
import app.verdant.android.data.model.UpdateMaintenanceRuleRequest
import app.verdant.android.data.repository.GardenAreaRepository
import app.verdant.android.data.repository.MaintenanceRuleRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GardenAreaDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun area(id: Long = 5) = GardenAreaResponse(
        id = id, gardenId = 1, gardenName = "Trädgården", name = "Grusgången",
        description = "Vid växthuset", category = "WALKWAY", boundaryJson = null, sizeSqm = 12.5,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun event(id: Long = 1, eventType: String = "WEED") = GardenAreaEventResponse(
        id = id, gardenAreaId = 5, eventType = eventType,
        eventDate = "2026-06-01", notes = null, createdAt = "2026-06-01T00:00:00Z",
    )

    private fun rule(id: Long = 1, activityType: String = "WEED") = MaintenanceRuleResponse(
        id = id, bedId = null, bedName = null,
        gardenAreaId = 5, gardenAreaName = "Grusgången",
        activityType = activityType, intervalDays = 21, anchorDate = null,
        seasonStartMonth = null, seasonStartDay = null,
        seasonEndMonth = null, seasonEndDay = null,
        active = true, notes = null,
        lastDoneDate = null, nextDueDate = "2026-06-25",
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private class FakeGardenAreaRepository(
        var area: GardenAreaResponse,
        var events: MutableList<GardenAreaEventResponse> = mutableListOf(),
        var photos: MutableList<GardenAreaPhotoResponse> = mutableListOf(),
        var failGetWith: Exception? = null,
        var failLogEventWith: Exception? = null,
    ) : GardenAreaRepository {
        var deletedIds = mutableListOf<Long>()
        var lastLogEvent: CreateGardenAreaEventRequest? = null
        var lastUpdate: UpdateGardenAreaRequest? = null
        var eventsCallCount = 0

        /** When set, get() parks here until the test completes it. */
        var getGate: CompletableDeferred<Unit>? = null

        override suspend fun list(gardenId: Long): List<GardenAreaResponse> = listOf(area)
        override suspend fun get(id: Long): GardenAreaResponse {
            failGetWith?.let { throw it }
            getGate?.await()
            return area
        }
        override suspend fun create(gardenId: Long, request: CreateGardenAreaRequest): GardenAreaResponse = area
        override suspend fun update(id: Long, request: UpdateGardenAreaRequest): GardenAreaResponse {
            lastUpdate = request
            return area
        }
        override suspend fun delete(id: Long) {
            deletedIds.add(id)
        }
        override suspend fun events(id: Long, limit: Int): List<GardenAreaEventResponse> {
            eventsCallCount++
            return events.toList()
        }
        override suspend fun logEvent(id: Long, request: CreateGardenAreaEventRequest): GardenAreaEventResponse {
            failLogEventWith?.let { throw it }
            lastLogEvent = request
            val created = GardenAreaEventResponse(
                id = 100, gardenAreaId = id, eventType = request.activityType,
                eventDate = request.eventDate ?: "2026-06-25", notes = request.notes,
                createdAt = "2026-06-25T00:00:00Z",
            )
            events.add(0, created)
            return created
        }
        override suspend fun photos(id: Long): List<GardenAreaPhotoResponse> = photos.toList()
        override suspend fun addPhoto(id: Long, request: CreateGardenAreaPhotoRequest): GardenAreaPhotoResponse {
            throw NotImplementedError("not exercised by this test")
        }
        override suspend fun deletePhoto(id: Long, photoId: Long) {}
    }

    private class FakeMaintenanceRuleRepository(
        var rules: MutableList<MaintenanceRuleResponse> = mutableListOf(),
    ) : MaintenanceRuleRepository {
        var listCallCount = 0
        var lastListAreaId: Long? = null

        override suspend fun list(bedId: Long?, areaId: Long?): List<MaintenanceRuleResponse> {
            listCallCount++
            lastListAreaId = areaId
            return rules.toList()
        }
        override suspend fun create(request: CreateMaintenanceRuleRequest): MaintenanceRuleResponse {
            val created = MaintenanceRuleResponse(
                id = 99, bedId = null, bedName = null,
                gardenAreaId = request.gardenAreaId, gardenAreaName = "Grusgången",
                activityType = request.activityType, intervalDays = request.intervalDays, anchorDate = null,
                seasonStartMonth = null, seasonStartDay = null, seasonEndMonth = null, seasonEndDay = null,
                active = true, notes = request.notes,
                lastDoneDate = null, nextDueDate = "2026-07-01",
                createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
            )
            rules.add(created)
            return created
        }
        override suspend fun update(id: Long, request: UpdateMaintenanceRuleRequest): MaintenanceRuleResponse =
            rules.first { it.id == id }
        override suspend fun delete(id: Long) {
            rules.removeAll { it.id == id }
        }
    }

    private fun viewModel(
        areaRepo: GardenAreaRepository,
        ruleRepo: MaintenanceRuleRepository,
        areaId: Long = 5,
    ) = GardenAreaDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("areaId" to areaId)),
        areaRepository = areaRepo,
        ruleRepository = ruleRepo,
    )

    @Test
    fun `loading an area emits Loading then Loaded with area, events, and rules`() = runTest {
        val areaRepo = FakeGardenAreaRepository(area = area(), events = mutableListOf(event()))
        val ruleRepo = FakeMaintenanceRuleRepository(mutableListOf(rule()))
        val vm = viewModel(areaRepo, ruleRepo)

        vm.uiState.test {
            assertEquals(GardenAreaDetailUiState.Loading, awaitItem())
            vm.load()
            advanceUntilIdle()
            val loaded = awaitItem() as GardenAreaDetailUiState.Loaded
            assertEquals("Grusgången", loaded.area.name)
            assertEquals(1, loaded.events.size)
            assertEquals(false, loaded.deleted)
        }
        assertEquals(1, vm.rulesController.state.value.rules.size)
        assertEquals(5L, ruleRepo.lastListAreaId)
    }

    @Test
    fun `a load failure emits Error`() = runTest {
        val areaRepo = FakeGardenAreaRepository(area = area(), failGetWith = RuntimeException("nätverksfel"))
        val ruleRepo = FakeMaintenanceRuleRepository()
        val vm = viewModel(areaRepo, ruleRepo)

        vm.uiState.test {
            assertEquals(GardenAreaDetailUiState.Loading, awaitItem())
            vm.load()
            advanceUntilIdle()
            assertTrue(awaitItem() is GardenAreaDetailUiState.Error)
        }
    }

    @Test
    fun `logEvent posts the chosen activity, then reloads both events and rules`() = runTest {
        val areaRepo = FakeGardenAreaRepository(area = area())
        val ruleRepo = FakeMaintenanceRuleRepository(mutableListOf(rule()))
        val vm = viewModel(areaRepo, ruleRepo)
        vm.load()
        advanceUntilIdle()
        val listCallsAfterInitialLoad = ruleRepo.listCallCount
        assertTrue(listCallsAfterInitialLoad > 0)

        vm.logEvent(activityType = "WEED", notes = "Rensade grus")
        advanceUntilIdle()

        assertEquals("WEED", areaRepo.lastLogEvent?.activityType)
        assertEquals("Rensade grus", areaRepo.lastLogEvent?.notes)
        // The visible payoff of the derived-clock design: logging work must
        // reload the rules, not just the event list, or a stale
        // nextDueDate keeps showing after the user just did the work.
        assertTrue(
            "expected rule repository.list to be called again after logEvent",
            ruleRepo.listCallCount > listCallsAfterInitialLoad,
        )
        val loaded = vm.uiState.value as GardenAreaDetailUiState.Loaded
        assertEquals(1, loaded.events.size)
    }

    @Test
    fun `logEvent accepts NOTE as well as activity types`() = runTest {
        val areaRepo = FakeGardenAreaRepository(area = area())
        val ruleRepo = FakeMaintenanceRuleRepository()
        val vm = viewModel(areaRepo, ruleRepo)
        vm.load()
        advanceUntilIdle()

        vm.logEvent(activityType = "NOTE", notes = "Grus påfyllt")
        advanceUntilIdle()

        assertEquals("NOTE", areaRepo.lastLogEvent?.activityType)
        assertEquals("Grus påfyllt", areaRepo.lastLogEvent?.notes)
    }

    @Test
    fun `deleting the area sets the deleted flag the screen navigates on`() = runTest {
        val areaRepo = FakeGardenAreaRepository(area = area())
        val ruleRepo = FakeMaintenanceRuleRepository()
        val vm = viewModel(areaRepo, ruleRepo)
        vm.load()
        advanceUntilIdle()

        vm.delete()
        advanceUntilIdle()

        val loaded = vm.uiState.value as GardenAreaDetailUiState.Loaded
        assertTrue(loaded.deleted)
        assertEquals(listOf(5L), areaRepo.deletedIds)
    }

    @Test
    fun `a failed logEvent surfaces a toast without losing the loaded state`() = runTest {
        val areaRepo = FakeGardenAreaRepository(area = area(), failLogEventWith = RuntimeException("500"))
        val ruleRepo = FakeMaintenanceRuleRepository()
        val vm = viewModel(areaRepo, ruleRepo)
        vm.load()
        advanceUntilIdle()

        vm.logEvent(activityType = "WEED")
        advanceUntilIdle()

        val loaded = vm.uiState.value as GardenAreaDetailUiState.Loaded
        assertNull(areaRepo.lastLogEvent) // never reached — logEvent threw
        assertTrue(loaded.toastMessage != null)
    }

    @Test
    fun `emptying the description or size sends the clear flags, not nulls`() = runTest {
        val areaRepo = FakeGardenAreaRepository(area = area())
        val vm = viewModel(areaRepo, FakeMaintenanceRuleRepository())
        vm.load()
        advanceUntilIdle()

        vm.update(name = "Grusgången", description = null, category = "WALKWAY", sizeSqm = null)
        advanceUntilIdle()

        val sent = areaRepo.lastUpdate!!
        // A null reads as "keep the current value" server-side, so the only
        // way to empty a field is the flag — and it may not carry a value.
        assertTrue(sent.clearDescription)
        assertTrue(sent.clearSizeSqm)
        assertNull(sent.description)
        assertNull(sent.sizeSqm)
    }

    @Test
    fun `a rewritten description travels as a value, never with the clear flag`() = runTest {
        val areaRepo = FakeGardenAreaRepository(area = area())
        val vm = viewModel(areaRepo, FakeMaintenanceRuleRepository())
        vm.load()
        advanceUntilIdle()

        vm.update(name = "Grusgången", description = "Ny text", category = "WALKWAY", sizeSqm = 4.0)
        advanceUntilIdle()

        val sent = areaRepo.lastUpdate!!
        assertEquals("Ny text", sent.description)
        assertEquals(4.0, sent.sizeSqm!!, 0.0001)
        assertEquals(false, sent.clearDescription)
        assertEquals(false, sent.clearSizeSqm)
    }

    @Test
    fun `an area that never had a description does not ask to clear one`() = runTest {
        val areaRepo = FakeGardenAreaRepository(area = area().copy(description = null, sizeSqm = null))
        val vm = viewModel(areaRepo, FakeMaintenanceRuleRepository())
        vm.load()
        advanceUntilIdle()

        vm.update(name = "Grusgången", description = null, category = "WALKWAY", sizeSqm = null)
        advanceUntilIdle()

        val sent = areaRepo.lastUpdate!!
        assertEquals(false, sent.clearDescription)
        assertEquals(false, sent.clearSizeSqm)
    }

    @Test
    fun `a toast consumed while a refresh is in flight does not come back`() = runTest {
        val areaRepo = FakeGardenAreaRepository(area = area(), failLogEventWith = RuntimeException("nätverksfel"))
        val vm = viewModel(areaRepo, FakeMaintenanceRuleRepository())
        vm.load()
        advanceUntilIdle()

        vm.logEvent(activityType = "WEED")
        advanceUntilIdle()
        assertEquals("nätverksfel", (vm.uiState.value as GardenAreaDetailUiState.Loaded).toastMessage)

        // Park the reload mid-flight, then let the screen show and consume the
        // snackbar exactly as it does in production.
        val gate = CompletableDeferred<Unit>()
        areaRepo.getGate = gate
        vm.refresh()
        advanceUntilIdle()
        vm.consumeToast()
        gate.complete(Unit)
        advanceUntilIdle()

        // Restoring the pre-flight snapshot here would fire the same snackbar
        // a second time.
        assertNull((vm.uiState.value as GardenAreaDetailUiState.Loaded).toastMessage)
    }

    @Test
    fun `refreshing the area alone does not refetch the rules`() = runTest {
        val ruleRepo = FakeMaintenanceRuleRepository(mutableListOf(rule()))
        val vm = viewModel(FakeGardenAreaRepository(area = area()), ruleRepo)
        vm.load()
        advanceUntilIdle()
        val callsAfterLoad = ruleRepo.listCallCount

        vm.refresh()
        advanceUntilIdle()

        // Only logging work can move a rule's next-due date; renaming an area
        // or deleting a photo cannot, so refetching there is a round-trip that
        // can only return what the client already has.
        assertEquals(callsAfterLoad, ruleRepo.listCallCount)
    }
}
