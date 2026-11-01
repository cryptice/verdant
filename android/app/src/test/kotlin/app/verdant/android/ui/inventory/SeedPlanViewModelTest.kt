package app.verdant.android.ui.inventory

import androidx.lifecycle.SavedStateHandle
import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.SeedBatchAllocation
import app.verdant.android.data.model.SeedBatchPlanRequest
import app.verdant.android.data.model.SeedBatchPlanResponse
import app.verdant.android.data.model.SeedBatchPlanStep
import app.verdant.android.data.model.SeedInventoryResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.repository.SeedInventoryRepository
import app.verdant.android.data.repository.SpeciesRepositoryImpl
import app.verdant.android.data.repository.WorkflowRepository
import app.verdant.android.data.model.SpeciesWorkflowResponse
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

    private fun batch(id: Long, quantity: Int, provider: String?) = SeedInventoryResponse(id = id, speciesId = 3, speciesName = "Pompondahlia – Lilliput",
        quantity = quantity, collectionDate = null, expirationDate = null, createdAt = "", providerName = provider)
    private val impecta = batch(9, 250, "Impecta")
    private val runabergs = batch(10, 100, "Runåbergs")
    private val empty = batch(11, 0, null)
    private var batches = listOf(impecta, runabergs, empty)
    private val species = Gson().fromJson("""{"id":3,"commonName":"Dahlia","germinationRate":80,"groups":[],"tags":[],"photos":[],"growingPositions":[],"soils":[],"providers":[]}""", SpeciesResponse::class.java)
    private val plan = SeedBatchPlanResponse(9, 3, 200, 150, "2099-05-01", listOf(
        SeedBatchPlanStep("Sådd", "SOW", "2099-01-01", 200, false),
        SeedBatchPlanStep("Avhärdning", "INSPECT", "2099-04-13", 150, true),
        SeedBatchPlanStep("Säljklar", "INSPECT", "2099-05-01", 150, false),
    ), allocations = listOf(SeedBatchAllocation(9, 200)))
    private fun step(name: String, sortOrder: Int, days: Int?) = Gson().fromJson(
        """{"id":$sortOrder,"name":"$name","daysAfterPrevious":${days ?: "null"},"isOptional":false,"isSideBranch":false,"sortOrder":$sortOrder}""",
        app.verdant.android.data.model.SpeciesWorkflowStepResponse::class.java,
    )
    private var workflowSteps = listOf(step("Sådd", 0, 0), step("Säljklar", 1, 120))
    private var catalogue = false
    private var previewResult = plan
    private val previews = mutableListOf<SeedBatchPlanRequest>()
    private val creates = mutableListOf<SeedBatchPlanRequest>()
    private val listedFor = mutableListOf<Long?>()

    private fun api(): VerdantApi = Proxy.newProxyInstance(VerdantApi::class.java.classLoader, arrayOf(VerdantApi::class.java)) { _, method, args ->
        when (method.name) {
            "getSeedInventory" -> { listedFor += args!![0] as Long?; batches }
            "getSpeciesById" -> if (catalogue) Gson().fromJson("""{"id":3,"commonName":"Dahlia","isSystem":true,"groups":[],"tags":[],"photos":[],"growingPositions":[],"soils":[],"providers":[]}""", SpeciesResponse::class.java) else species
            "getSpeciesWorkflow" -> SpeciesWorkflowResponse(templateId = null, templateName = null, steps = workflowSteps)
            "previewSeedBatchPlan" -> { previews += args!![0] as SeedBatchPlanRequest; previewResult }
            "createSeedBatchPlan" -> { creates += args!![0] as SeedBatchPlanRequest; previewResult.copy(taskIds = listOf(11, 12)) }
            else -> error(method.name)
        }
    } as VerdantApi

    private fun viewModel(batchId: Long? = null) = api().let {
        SeedPlanViewModel(SavedStateHandle(mapOf("speciesId" to 3L, "batchId" to (batchId ?: -1L))), SeedInventoryRepository(it), SpeciesRepositoryImpl(it), WorkflowRepository(it))
    }

    @Test fun `a variety with one batch uses it, previews once the inputs settle and creates the tasks`() = runTest {
        batches = listOf(impecta)
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(listOf<Long?>(3L), listedFor)
        assertEquals(listOf(9L), vm.uiState.value.selectedIds)
        assertEquals("250", vm.uiState.value.seeds)
        assertEquals(200, vm.uiState.value.suggestedPlants)

        vm.setSeeds("200")
        vm.setPlants("15")
        vm.setPlants("150")
        vm.setReadyDate(LocalDate.of(2099, 5, 1))
        advanceUntilIdle()

        val request = SeedBatchPlanRequest(200, 150, "2099-05-01", listOf(9))
        assertEquals(listOf(request), previews)
        assertEquals(2, vm.uiState.value.taskCount)

        vm.create()
        advanceUntilIdle()
        assertEquals(listOf(request), creates)
        assertEquals(listOf(11L, 12L), vm.uiState.value.created?.taskIds)
    }

    @Test fun `several batches start unchosen, and choosing two lets the plan sow more than one holds`() = runTest {
        previewResult = plan.copy(seeds = 300, allocations = listOf(SeedBatchAllocation(9, 250), SeedBatchAllocation(10, 50)))
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals("empty batches are left out", listOf(9L, 10L), vm.uiState.value.batches.map { it.id })
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
        assertEquals("Välj minst ett fröparti.", vm.uiState.value.inputError)

        vm.toggleBatch(9)
        vm.toggleBatch(10)
        assertEquals("350", vm.uiState.value.seeds)
        vm.setSeeds("300")
        vm.setPlants("220")
        vm.setReadyDate(LocalDate.of(2099, 5, 1))
        advanceUntilIdle()

        assertEquals(listOf(SeedBatchPlanRequest(300, 220, "2099-05-01", listOf(9, 10))), previews)
        assertEquals("two sowing tasks and one later task", 3, vm.uiState.value.taskCount)
    }

    @Test fun `opening from a batch starts with it and catches more seeds than it holds`() = runTest {
        val vm = viewModel(batchId = 10)
        advanceUntilIdle()
        assertEquals(listOf(10L), vm.uiState.value.selectedIds)
        vm.setSeeds("300")
        vm.setPlants("150")
        vm.setReadyDate(LocalDate.of(2099, 5, 1))
        advanceUntilIdle()

        assertEquals("Partiet har bara 100 frön.", vm.uiState.value.inputError)
        assertTrue(previews.isEmpty())
    }

    @Test fun `a plan starting before today cannot be created`() {
        val state = SeedPlanState(preview = plan.copy(steps = listOf(plan.steps.first().copy(date = "2026-10-01"))))
        assertTrue(state.startsInPast(LocalDate.of(2026, 11, 1)))
    }

    @Test fun `explains a variety that cannot be planned instead of showing the form`() = runTest {
        workflowSteps = emptyList()
        assertEquals(WorkflowProblem.None, viewModel().also { advanceUntilIdle() }.uiState.value.workflowProblem)

        workflowSteps = listOf(step("Sådd", 0, 0), step("Omskolning", 1, null))
        assertEquals(WorkflowProblem.MissingDays("Omskolning"), viewModel().also { advanceUntilIdle() }.uiState.value.workflowProblem)

        catalogue = true
        assertEquals(WorkflowProblem.Catalogue, viewModel().also { advanceUntilIdle() }.uiState.value.workflowProblem)

        catalogue = false
        workflowSteps = listOf(step("Sådd", 0, 0), step("Säljklar", 1, 120))
        assertNull(viewModel().also { advanceUntilIdle() }.uiState.value.workflowProblem)
    }
}
