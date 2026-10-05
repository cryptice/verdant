package app.verdant.android.ui.targets

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.*
import app.verdant.android.data.repository.HarvestPlanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class HarvestPlansViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private val target = HarvestPlanRequest(1, speciesId = 2, quantity = 300, sellableUnit = "FLOWER", harvestDate = "2027-08-01")
    private val preview = HarvestPlanPreview(target, emptyList(), emptyList(), 0, true, "preview-1")
    private val plan = HarvestPlanResponse(9, "ACTIVE", preview, emptyList(), "2026-10-05T00:00:00Z")

    private fun model(call: (String, Array<out Any?>) -> Any?): HarvestPlansViewModel {
        val api = Proxy.newProxyInstance(VerdantApi::class.java.classLoader, arrayOf(VerdantApi::class.java)) { _, method, args ->
            call(method.name, args ?: emptyArray())
        } as VerdantApi
        return HarvestPlansViewModel(HarvestPlanRepository(api))
    }

    @Test fun `save retries use the same key and immutable preview`() = runTest {
        val requests = mutableListOf<CreateHarvestPlanRequest>()
        val vm = model { method, args -> when (method) {
            "previewHarvestPlan" -> preview
            "createHarvestPlan" -> {
                requests.add(args[0] as CreateHarvestPlanRequest)
                if (requests.size == 1) throw RuntimeException("Connection interrupted")
                plan
            }
            else -> error(method)
        } }
        vm.preview(target); advanceUntilIdle()
        vm.save(); advanceUntilIdle()
        assertEquals("Connection interrupted", vm.uiState.value.error)
        assertEquals(preview, vm.uiState.value.preview)
        vm.save(); advanceUntilIdle()
        assertEquals(requests[0], requests[1])
        assertEquals(plan, vm.uiState.value.selected)
        assertNull(vm.uiState.value.preview)
    }

    @Test fun `editing invalidates a calculated preview`() = runTest {
        val vm = model { method, _ -> if (method == "previewHarvestPlan") preview else error(method) }
        vm.preview(target); advanceUntilIdle()
        assertNotNull(vm.uiState.value.preview)
        vm.invalidatePreview()
        assertNull(vm.uiState.value.preview)
    }

    @Test fun `reopening the screen clears a preview whose form is no longer present`() = runTest {
        val vm = model { method, _ -> when (method) {
            "previewHarvestPlan" -> preview
            "getSeasons", "getSpeciesGroups", "getHarvestPlans" -> emptyList<Any>()
            else -> error(method)
        } }
        vm.preview(target); advanceUntilIdle()
        vm.load(); advanceUntilIdle()
        assertNull(vm.uiState.value.preview)
        assertFalse(vm.uiState.value.busy)
    }

    @Test fun `double taps submit once while save is pending`() = runTest {
        var saves = 0
        val vm = model { method, _ -> when (method) {
            "previewHarvestPlan" -> preview
            "createHarvestPlan" -> { saves++; plan }
            else -> error(method)
        } }
        vm.preview(target); advanceUntilIdle()
        vm.save(); vm.save(); advanceUntilIdle()
        assertEquals(1, saves)
        assertFalse(vm.uiState.value.busy)
    }
}
