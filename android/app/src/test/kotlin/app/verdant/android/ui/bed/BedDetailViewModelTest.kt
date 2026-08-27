package app.verdant.android.ui.bed

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.BedResponse
import app.verdant.android.data.model.BulkLocationActionResponse
import app.verdant.android.data.model.CreateMaintenanceRuleRequest
import app.verdant.android.data.model.GardenResponse
import app.verdant.android.data.model.MaintenanceRuleResponse
import app.verdant.android.data.model.PlantResponse
import app.verdant.android.data.model.UpdateBedRequest
import app.verdant.android.data.model.UpdateMaintenanceRuleRequest
import app.verdant.android.data.repository.BedRepository
import app.verdant.android.data.repository.GardenApiRepository
import app.verdant.android.data.repository.MaintenanceRuleRepository
import app.verdant.android.data.repository.PlantRepository
import app.verdant.android.data.repository.SupplyApplicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [BedRepository], [PlantRepository], [SupplyApplicationRepository], and
 * [GardenApiRepository] are concrete classes wrapping the ~190-endpoint
 * [VerdantApi] Retrofit interface — not hand-fakeable the way
 * [MaintenanceRuleRepository] is. This module has no mocking library, so —
 * mirroring `GardenDetailViewModelTest`'s technique — a JDK dynamic
 * [java.lang.reflect.Proxy] over [VerdantApi] stands in for the network
 * layer: it answers the endpoints this ViewModel actually calls and throws
 * for everything else.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BedDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun bed(id: Long = 5, gardenId: Long = 1) = BedResponse(
        id = id, name = "Bädd $id", description = null, gardenId = gardenId,
        boundaryJson = null, createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun garden(id: Long = 1) = GardenResponse(
        id = id, name = "Trädgården", description = null, emoji = null,
        latitude = null, longitude = null, address = null, boundaryJson = null,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun rule(id: Long = 1, bedId: Long = 5, activityType: String = "WEED") = MaintenanceRuleResponse(
        id = id, bedId = bedId, bedName = "Bädd $bedId",
        gardenAreaId = null, gardenAreaName = null,
        activityType = activityType, intervalDays = 14, anchorDate = null,
        seasonStartMonth = null, seasonStartDay = null,
        seasonEndMonth = null, seasonEndDay = null,
        active = true, notes = null,
        lastDoneDate = null, nextDueDate = "2026-07-01",
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    /** Captures the body of the last updateBed call made through [fakeApi]. */
    private var lastBedUpdate: UpdateBedRequest? = null

    private fun fakeApi(
        bed: BedResponse,
        garden: GardenResponse,
        plants: List<PlantResponse> = emptyList(),
        weedResult: BulkLocationActionResponse = BulkLocationActionResponse(plantsAffected = 3),
        waterResult: BulkLocationActionResponse = BulkLocationActionResponse(plantsAffected = 3),
    ): VerdantApi {
        return java.lang.reflect.Proxy.newProxyInstance(
            VerdantApi::class.java.classLoader,
            arrayOf(VerdantApi::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getBed" -> bed
                "updateBed" -> {
                    lastBedUpdate = args[1] as UpdateBedRequest
                    bed
                }
                "getPlants" -> plants
                "listSupplyApplicationsByBed" -> emptyList<Any>()
                "getBedEvents" -> emptyList<Any>()
                "getBedPhotos" -> emptyList<Any>()
                "getGarden" -> garden
                "weedBed" -> weedResult
                "waterBed" -> waterResult
                else -> throw UnsupportedOperationException("not used: ${method.name}")
            }
        } as VerdantApi
    }

    private class FakeMaintenanceRuleRepository(
        var rules: MutableList<MaintenanceRuleResponse> = mutableListOf(),
    ) : MaintenanceRuleRepository {
        var listCallCount = 0
        var lastListBedId: Long? = null

        override suspend fun list(bedId: Long?, areaId: Long?): List<MaintenanceRuleResponse> {
            listCallCount++
            lastListBedId = bedId
            return rules.toList()
        }
        override suspend fun create(request: CreateMaintenanceRuleRequest): MaintenanceRuleResponse =
            error("not used")
        override suspend fun update(id: Long, request: UpdateMaintenanceRuleRequest): MaintenanceRuleResponse =
            error("not used")
        override suspend fun delete(id: Long) { error("not used") }
    }

    private fun viewModel(
        api: VerdantApi,
        ruleRepo: MaintenanceRuleRepository,
        bedId: Long = 5,
    ) = BedDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("bedId" to bedId)),
        bedRepository = BedRepository(api),
        plantRepository = PlantRepository(api),
        supplyApplicationRepository = SupplyApplicationRepository(api),
        gardenApiRepository = GardenApiRepository(api),
        ruleRepository = ruleRepo,
    )

    @Test
    fun `loading a bed emits Loading then Loaded, and loads its maintenance rules`() = runTest {
        val bed = bed(id = 5, gardenId = 1)
        val api = fakeApi(bed = bed, garden = garden(id = 1))
        val ruleRepo = FakeMaintenanceRuleRepository(mutableListOf(rule()))
        val vm = viewModel(api, ruleRepo, bedId = 5)

        vm.uiState.test {
            assertEquals(BedDetailUiState.Loading, awaitItem())
            vm.refresh()
            advanceUntilIdle()
            val loaded = awaitItem() as BedDetailUiState.Loaded
            assertEquals("Bädd 5", loaded.bed.name)
        }
        assertEquals(1, vm.rulesController.state.value.rules.size)
        assertEquals(5L, ruleRepo.lastListBedId)
    }

    @Test
    fun `a successful weed refreshes the maintenance rules`() = runTest {
        val bed = bed(id = 5, gardenId = 1)
        val api = fakeApi(bed = bed, garden = garden(id = 1), weedResult = BulkLocationActionResponse(plantsAffected = 4))
        val ruleRepo = FakeMaintenanceRuleRepository(mutableListOf(rule()))
        val vm = viewModel(api, ruleRepo, bedId = 5)
        vm.refresh()
        advanceUntilIdle()
        val listCallsAfterInitialLoad = ruleRepo.listCallCount
        assertTrue(listCallsAfterInitialLoad > 0)

        vm.weed()
        advanceUntilIdle()

        // The visible payoff of the derived-clock design: weeding must
        // reload the rules, not just the bed's own state, or a stale
        // nextDueDate keeps showing after the user just did the work.
        assertTrue(
            "expected rule repository.list to be called again after weed",
            ruleRepo.listCallCount > listCallsAfterInitialLoad,
        )
        val loaded = vm.uiState.value as BedDetailUiState.Loaded
        assertEquals("Rensade ogräs · 4 plantor", loaded.toastMessage)
    }

    @Test
    fun `a successful water also refreshes the maintenance rules`() = runTest {
        val bed = bed(id = 5, gardenId = 1)
        val api = fakeApi(bed = bed, garden = garden(id = 1), waterResult = BulkLocationActionResponse(plantsAffected = 2))
        val ruleRepo = FakeMaintenanceRuleRepository(mutableListOf(rule(activityType = "WATER")))
        val vm = viewModel(api, ruleRepo, bedId = 5)
        vm.refresh()
        advanceUntilIdle()
        val listCallsAfterInitialLoad = ruleRepo.listCallCount
        assertTrue(listCallsAfterInitialLoad > 0)

        vm.water()
        advanceUntilIdle()

        assertTrue(
            "expected rule repository.list to be called again after water",
            ruleRepo.listCallCount > listCallsAfterInitialLoad,
        )
        val loaded = vm.uiState.value as BedDetailUiState.Loaded
        assertEquals("Vattnade · 2 plantor", loaded.toastMessage)
    }

    @Test
    fun `a condition the user cleared is emptied with its flag, not left as null`() = runTest {
        val bed = bed().copy(
            soilType = "SANDY", soilPh = 6.5, sunExposure = "FULL_SUN", drainage = "GOOD",
            sunDirections = listOf("S"), irrigationType = "DRIP", protection = "OPEN_FIELD",
        )
        val vm = viewModel(fakeApi(bed = bed, garden = garden()), FakeMaintenanceRuleRepository())
        vm.refresh()
        advanceUntilIdle()

        vm.update(
            name = "Bädd 5", description = "", soilType = null, soilPh = null,
            sunExposure = null, drainage = null, sunDirections = emptySet(),
            irrigationType = null, protection = null, raisedBed = false,
        )
        advanceUntilIdle()

        val sent = lastBedUpdate!!
        // A null reads as "keep the current value" server-side, so each
        // cleared condition has to say so explicitly.
        assertTrue(sent.clearSoilType)
        assertTrue(sent.clearSoilPh)
        assertTrue(sent.clearSunExposure)
        assertTrue(sent.clearDrainage)
        assertTrue(sent.clearSunDirections)
        assertTrue(sent.clearIrrigationType)
        assertTrue(sent.clearProtection)
    }

    @Test
    fun `conditions a bed never had are not asked to be cleared`() = runTest {
        val vm = viewModel(fakeApi(bed = bed(), garden = garden()), FakeMaintenanceRuleRepository())
        vm.refresh()
        advanceUntilIdle()

        vm.update(
            name = "Bädd 5", description = "", soilType = null, soilPh = null,
            sunExposure = null, drainage = null, sunDirections = emptySet(),
            irrigationType = null, protection = null, raisedBed = false,
        )
        advanceUntilIdle()

        val sent = lastBedUpdate!!
        assertEquals(false, sent.clearSoilType)
        assertEquals(false, sent.clearSoilPh)
        assertEquals(false, sent.clearSunDirections)
    }

    @Test
    fun `a replacement value travels without the matching clear flag`() = runTest {
        val bed = bed().copy(soilType = "SANDY", soilPh = 6.5)
        val vm = viewModel(fakeApi(bed = bed, garden = garden()), FakeMaintenanceRuleRepository())
        vm.refresh()
        advanceUntilIdle()

        vm.update(
            name = "Bädd 5", description = "", soilType = "CLAY", soilPh = 7.0,
            sunExposure = null, drainage = null, sunDirections = emptySet(),
            irrigationType = null, protection = null, raisedBed = false,
        )
        advanceUntilIdle()

        val sent = lastBedUpdate!!
        // Sending both for one field is a 400 server-side.
        assertEquals("CLAY", sent.soilType)
        assertEquals(false, sent.clearSoilType)
        assertEquals(false, sent.clearSoilPh)
    }
}
