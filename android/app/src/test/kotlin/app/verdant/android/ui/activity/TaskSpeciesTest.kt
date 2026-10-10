package app.verdant.android.ui.activity

import androidx.lifecycle.SavedStateHandle
import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.repository.*
import app.verdant.android.ui.task.TaskFormViewModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class TaskSpeciesTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private val species = Gson().fromJson("""{"id":1991,"commonName":"Chili","commonNameSv":"Chilipeppar","variantName":"Naga Black","groups":[],"tags":[],"photos":[],"growingPositions":[],"soils":[],"providers":[]} """, SpeciesResponse::class.java)
    private val task = ScheduledTaskResponse(id = 1, speciesId = 1991, speciesName = "Naga Black", activityType = "SOW", targetCount = 20, remainingCount = 20, status = "PENDING", notes = null, createdAt = "", updatedAt = "")
    private val lookedUp = mutableListOf<Long>()
    private fun api(): VerdantApi = Proxy.newProxyInstance(VerdantApi::class.java.classLoader, arrayOf(VerdantApi::class.java)) { _, method, args ->
        when (method.name) {
            "getSpecies", "getAllBeds", "getFrequentComments", "getTrayLocations" -> emptyList<Any>()
            "getTask" -> task
            "getSpeciesById" -> { lookedUp += args!![0] as Long; species }
            else -> error(method.name)
        }
    } as VerdantApi
    private fun sow(api: VerdantApi, state: SavedStateHandle) = SowActivityViewModel(state, SpeciesRepositoryImpl(api), BedRepository(api), DefaultTaskRepository(api), TrayLocationRepository(api), SeedInventoryRepository(api), PlantRepository(api), SupplyRepository(api))

    @Test fun `sow resolves linked species outside catalogue page`() = runTest {
        val vm = sow(api(), SavedStateHandle(mapOf("taskId" to 1L)))
        advanceUntilIdle()
        assertEquals(listOf(1991L), lookedUp)
        assertEquals(species, vm.uiState.value.species.single())
        assertEquals(1991L, vm.uiState.value.task?.speciesId)
    }
    @Test fun `sow resolves explicit species outside catalogue page`() = runTest {
        val vm = sow(api(), SavedStateHandle(mapOf("speciesId" to 1991L)))
        advanceUntilIdle()
        assertEquals(listOf(1991L), lookedUp)
        assertEquals(species, vm.uiState.value.species.single())
    }
    @Test fun `task edit resolves linked species outside catalogue page`() = runTest {
        val api = api()
        val vm = TaskFormViewModel(SavedStateHandle(mapOf("taskId" to 1L)), SpeciesRepositoryImpl(api), BedRepository(api), DefaultTaskRepository(api))
        advanceUntilIdle()
        assertEquals(listOf(1991L), lookedUp)
        assertEquals(species, vm.uiState.value.species.single())
    }
}
