package app.verdant.android.ui.garden

import androidx.lifecycle.SavedStateHandle
import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.BedResponse
import app.verdant.android.data.model.CreateGardenAreaEventRequest
import app.verdant.android.data.model.CreateGardenAreaPhotoRequest
import app.verdant.android.data.model.CreateGardenAreaRequest
import app.verdant.android.data.model.GardenAreaEventResponse
import app.verdant.android.data.model.GardenAreaPhotoResponse
import app.verdant.android.data.model.GardenAreaResponse
import app.verdant.android.data.model.GardenResponse
import app.verdant.android.data.model.UpdateGardenAreaRequest
import app.verdant.android.data.repository.BedRepository
import app.verdant.android.data.repository.GardenApiRepository
import app.verdant.android.data.repository.GardenAreaRepository
import app.verdant.android.data.repository.PlantRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [GardenApiRepository], [BedRepository], and [PlantRepository] are concrete
 * classes wrapping the ~190-endpoint [VerdantApi] Retrofit interface — not
 * hand-fakeable the way [GardenAreaRepository] is. This module has no mocking
 * library, so — mirroring the existing `stubCustomerRepo()` technique in
 * `SalesViewModelTest` — a JDK dynamic [java.lang.reflect.Proxy] over
 * [VerdantApi] stands in for the network layer: it answers the couple of
 * endpoints this ViewModel actually calls and throws for everything else
 * (including `getTraySummary`, which the ViewModel already treats as a soft
 * failure via `runCatching`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GardenDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun garden(id: Long = 1) = GardenResponse(
        id = id, name = "Trädgården", description = null, emoji = null,
        latitude = null, longitude = null, address = null, boundaryJson = null,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun bed(id: Long, gardenId: Long) = BedResponse(
        id = id, name = "Bädd $id", description = null, gardenId = gardenId,
        boundaryJson = null, createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun area(id: Long, gardenId: Long, name: String) = GardenAreaResponse(
        id = id, gardenId = gardenId, gardenName = "Trädgården", name = name,
        description = null, category = "WALKWAY", boundaryJson = null, sizeSqm = null,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun fakeApi(garden: GardenResponse, beds: List<BedResponse>): VerdantApi {
        return java.lang.reflect.Proxy.newProxyInstance(
            VerdantApi::class.java.classLoader,
            arrayOf(VerdantApi::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getGarden" -> garden
                "getBeds" -> beds
                else -> throw UnsupportedOperationException("not used: ${method.name}")
            }
        } as VerdantApi
    }

    private class FakeGardenAreaRepository(
        var areas: List<GardenAreaResponse> = emptyList(),
        var failListWith: Exception? = null,
    ) : GardenAreaRepository {
        var listCallCount = 0
        override suspend fun list(gardenId: Long): List<GardenAreaResponse> {
            listCallCount++
            failListWith?.let { throw it }
            return areas
        }
        override suspend fun get(id: Long): GardenAreaResponse = error("not used")
        override suspend fun create(gardenId: Long, request: CreateGardenAreaRequest): GardenAreaResponse = error("not used")
        override suspend fun update(id: Long, request: UpdateGardenAreaRequest): GardenAreaResponse = error("not used")
        override suspend fun delete(id: Long) { error("not used") }
        override suspend fun events(id: Long, limit: Int): List<GardenAreaEventResponse> = error("not used")
        override suspend fun logEvent(id: Long, request: CreateGardenAreaEventRequest): GardenAreaEventResponse = error("not used")
        override suspend fun photos(id: Long): List<GardenAreaPhotoResponse> = error("not used")
        override suspend fun addPhoto(id: Long, request: CreateGardenAreaPhotoRequest): GardenAreaPhotoResponse = error("not used")
        override suspend fun deletePhoto(id: Long, photoId: Long) { error("not used") }
    }

    private fun viewModel(
        areaRepo: GardenAreaRepository,
        gardenResponse: GardenResponse,
        beds: List<BedResponse> = emptyList(),
        gardenId: Long,
    ): GardenDetailViewModel {
        val api = fakeApi(gardenResponse, beds)
        return GardenDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("gardenId" to gardenId)),
            gardenApiRepository = GardenApiRepository(api),
            bedRepository = BedRepository(api),
            gardenAreaRepository = areaRepo,
            plantRepository = PlantRepository(api),
        )
    }

    @Test
    fun `refresh loads areas sorted naturally and clears the load-failed flag`() = runTest {
        val g = garden(id = 3)
        val areaRepo = FakeGardenAreaRepository(
            areas = listOf(
                area(id = 10, gardenId = 3, name = "Gång #10"),
                area(id = 11, gardenId = 3, name = "Gång #2"),
            ),
        )
        val vm = viewModel(areaRepo, gardenResponse = g, gardenId = 3)

        vm.refresh()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.areasLoadFailed)
        assertEquals(listOf("Gång #2", "Gång #10"), state.areas.map { it.name })
    }

    @Test
    fun `a failing areas fetch sets areasLoadFailed rather than looking like an empty garden`() = runTest {
        val g = garden(id = 5)
        val areaRepo = FakeGardenAreaRepository(failListWith = RuntimeException("500"))
        val vm = viewModel(areaRepo, gardenResponse = g, beds = emptyList(), gardenId = 5)

        vm.refresh()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(
            "a failed areas fetch must be visible in state, not silently treated as empty",
            state.areasLoadFailed,
        )
        assertTrue(state.areas.isEmpty())
        // This is the exact condition GardenDetailScreen's delete-garden gate
        // evaluates. Both beds and areas read empty here, but the fetch failed —
        // the gate must still resolve to "don't offer delete".
        val wouldOfferDelete = state.beds.isEmpty() && state.areas.isEmpty() && !state.areasLoadFailed
        assertFalse("delete must not be offered while areas are unaccounted for", wouldOfferDelete)
    }

    @Test
    fun `a successful refresh after a failure clears areasLoadFailed`() = runTest {
        val g = garden(id = 7)
        val areaRepo = FakeGardenAreaRepository(failListWith = RuntimeException("500"))
        val vm = viewModel(areaRepo, gardenResponse = g, gardenId = 7)

        vm.refresh()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.areasLoadFailed)

        areaRepo.failListWith = null
        areaRepo.areas = listOf(area(id = 20, gardenId = 7, name = "Komposten"))
        vm.refresh()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.areasLoadFailed)
        assertEquals(listOf("Komposten"), state.areas.map { it.name })
    }
}
