package app.verdant.android.ui.inventory

import androidx.lifecycle.SavedStateHandle
import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.SeedBatchPlanRequest
import app.verdant.android.data.model.SeedBatchPlanResponse
import app.verdant.android.data.model.SeedBatchPlanStep
import app.verdant.android.data.model.SeedInventoryResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.repository.SeedInventoryRepository
import app.verdant.android.data.repository.SpeciesRepositoryImpl
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SeedPlanViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    private val batch = SeedInventoryResponse(id = 9, speciesId = 3, speciesName = "Pompondahlia – Lilliput", quantity = 250,
        collectionDate = null, expirationDate = null, createdAt = "")
    private val species = Gson().fromJson("""{"id":3,"commonName":"Dahlia","germinationRate":80,"groups":[],"tags":[],"photos":[],"growingPositions":[],"soils":[],"providers":[]}""", SpeciesResponse::class.java)
    private val plan = SeedBatchPlanResponse(9, 3, 200, 150, "2099-05-01", listOf(
        SeedBatchPlanStep("Sådd", "SOW", "2099-01-01", 200, false),
        SeedBatchPlanStep("Avhärdning", "INSPECT", "2099-04-13", 150, true),
        SeedBatchPlanStep("Säljklar", "INSPECT", "2099-05-01", 150, false),
    ))
    private val previews = mutableListOf<SeedBatchPlanRequest>()
    private val creates = mutableListOf<SeedBatchPlanRequest>()

    private fun api(): VerdantApi = Proxy.newProxyInstance(VerdantApi::class.java.classLoader, arrayOf(VerdantApi::class.java)) { _, method, args ->
        when (method.name) {
            "getSeedInventory" -> listOf(batch)
            "getSpeciesById" -> species
            "previewSeedBatchPlan" -> { previews += args!![1] as SeedBatchPlanRequest; plan }
            "createSeedBatchPlan" -> { creates += args!![1] as SeedBatchPlanRequest; plan.copy(taskIds = listOf(11, 12)) }
            else -> error(method.name)
        }
    } as VerdantApi

    private fun viewModel() = api().let { SeedPlanViewModel(SavedStateHandle(mapOf("batchId" to 9L)), SeedInventoryRepository(it), SpeciesRepositoryImpl(it)) }

    @Test fun `previews the schedule once the inputs settle, then creates the tasks`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals("250", vm.uiState.value.seeds)
        assertEquals(200, vm.uiState.value.suggestedPlants)

        vm.setSeeds("200")
        vm.setPlants("15")
        vm.setPlants("150")
        vm.setReadyDate(LocalDate.of(2099, 5, 1))
        advanceUntilIdle()

        val request = SeedBatchPlanRequest(200, 150, "2099-05-01")
        assertEquals(listOf(request), previews)
        assertEquals(plan, vm.uiState.value.preview)
        assertFalse(vm.uiState.value.startsInPast(LocalDate.of(2026, 11, 1)))

        vm.create()
        advanceUntilIdle()
        assertEquals(listOf(request), creates)
        assertEquals(listOf(11L, 12L), vm.uiState.value.created?.taskIds)
    }

    @Test fun `catches more seeds than the batch holds without asking the server`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.setSeeds("300")
        vm.setPlants("150")
        vm.setReadyDate(LocalDate.of(2099, 5, 1))
        advanceUntilIdle()

        assertEquals("Partiet har bara 250 frön.", vm.uiState.value.inputError)
        assertTrue(previews.isEmpty())
        assertNull(vm.uiState.value.request)
    }

    @Test fun `a plan starting before today cannot be created`() {
        val state = SeedPlanState(preview = plan.copy(steps = listOf(plan.steps.first().copy(date = "2026-10-01"))))
        assertTrue(state.startsInPast(LocalDate.of(2026, 11, 1)))
    }
}
