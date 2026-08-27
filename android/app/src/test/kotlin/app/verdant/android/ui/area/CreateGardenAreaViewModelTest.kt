package app.verdant.android.ui.area

import androidx.lifecycle.SavedStateHandle
import app.verdant.android.data.model.CreateGardenAreaEventRequest
import app.verdant.android.data.model.CreateGardenAreaPhotoRequest
import app.verdant.android.data.model.CreateGardenAreaRequest
import app.verdant.android.data.model.GardenAreaEventResponse
import app.verdant.android.data.model.GardenAreaPhotoResponse
import app.verdant.android.data.model.GardenAreaResponse
import app.verdant.android.data.model.UpdateGardenAreaRequest
import app.verdant.android.data.repository.GardenAreaRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateGardenAreaViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun viewModel(repo: GardenAreaRepository, gardenId: Long = 1) =
        CreateGardenAreaViewModel(
            savedStateHandle = SavedStateHandle(mapOf("gardenId" to gardenId)),
            areaRepository = repo,
        )

    private class FakeGardenAreaRepository(
        var failCreateWith: Exception? = null,
    ) : GardenAreaRepository {
        val createCalls = mutableListOf<Pair<Long, CreateGardenAreaRequest>>()

        override suspend fun list(gardenId: Long): List<GardenAreaResponse> = emptyList()
        override suspend fun get(id: Long): GardenAreaResponse = error("not used")
        override suspend fun create(gardenId: Long, request: CreateGardenAreaRequest): GardenAreaResponse {
            failCreateWith?.let { throw it }
            createCalls.add(gardenId to request)
            return GardenAreaResponse(
                id = 42, gardenId = gardenId, gardenName = "Trädgården",
                name = request.name, description = request.description,
                category = request.category, boundaryJson = null, sizeSqm = request.sizeSqm,
                createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
            )
        }
        override suspend fun update(id: Long, request: UpdateGardenAreaRequest): GardenAreaResponse = error("not used")
        override suspend fun delete(id: Long) { error("not used") }
        override suspend fun events(id: Long, limit: Int): List<GardenAreaEventResponse> = error("not used")
        override suspend fun logEvent(id: Long, request: CreateGardenAreaEventRequest): GardenAreaEventResponse = error("not used")
        override suspend fun photos(id: Long): List<GardenAreaPhotoResponse> = error("not used")
        override suspend fun addPhoto(id: Long, request: CreateGardenAreaPhotoRequest): GardenAreaPhotoResponse = error("not used")
        override suspend fun deletePhoto(id: Long, photoId: Long) { error("not used") }
    }

    @Test
    fun `a valid submission posts the chosen name and category`() = runTest {
        val repo = FakeGardenAreaRepository()
        val vm = viewModel(repo, gardenId = 7)

        vm.create(name = "Grusgången", category = "WALKWAY", description = "", sizeSqm = null)
        advanceUntilIdle()

        assertEquals(1, repo.createCalls.size)
        val (gardenId, request) = repo.createCalls.first()
        assertEquals(7L, gardenId)
        assertEquals("Grusgången", request.name)
        assertEquals("WALKWAY", request.category)
        assertEquals(42L, vm.uiState.value.createdId)
    }

    @Test
    fun `submission is blocked while the name is blank`() = runTest {
        val repo = FakeGardenAreaRepository()
        val vm = viewModel(repo)

        vm.create(name = "   ", category = "WALKWAY", description = "", sizeSqm = null)
        advanceUntilIdle()

        assertEquals(0, repo.createCalls.size)
        assertNull(vm.uiState.value.createdId)
    }

    @Test
    fun `a failure surfaces an error without navigating away`() = runTest {
        val repo = FakeGardenAreaRepository(failCreateWith = RuntimeException("500"))
        val vm = viewModel(repo)

        vm.create(name = "Grusgången", category = "WALKWAY", description = "", sizeSqm = null)
        advanceUntilIdle()

        assertNull(vm.uiState.value.createdId)
        assertEquals("500", vm.uiState.value.error)
    }
}
