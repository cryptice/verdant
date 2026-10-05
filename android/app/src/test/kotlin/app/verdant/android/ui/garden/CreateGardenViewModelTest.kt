package app.verdant.android.ui.garden

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.GardenResponse
import app.verdant.android.data.model.GardenWithBedsResponse
import app.verdant.android.data.repository.GardenApiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class CreateGardenViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private var existingGardens = emptyList<GardenResponse>()
    private var failList = false
    private var failCreate = false
    private var createCalls = 0
    private val garden = GardenResponse(7, "Garden", null, null, null, null, null, null, "", "")

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun viewModel(): CreateGardenViewModel {
        val api = Proxy.newProxyInstance(VerdantApi::class.java.classLoader, arrayOf(VerdantApi::class.java)) { _, method, _ ->
            when (method.name) {
                "getGardens" -> if (failList) error("Could not load gardens") else existingGardens
                "createGardenWithLayout" -> {
                    createCalls++
                    if (failCreate) error("Could not create garden")
                    GardenWithBedsResponse(garden, emptyList())
                }
                else -> error("Unexpected API call: ${method.name}")
            }
        } as VerdantApi
        return CreateGardenViewModel(GardenApiRepository(api)).apply { gardenName = "Garden" }
    }

    @Test fun `first garden signals dashboard navigation and cannot be submitted twice`() = runTest {
        val vm = viewModel()
        vm.createGarden()
        vm.createGarden()
        advanceUntilIdle()
        assertEquals(7L, vm.createdGardenId)
        assertTrue(vm.createdFirstGarden)
        vm.createGarden()
        advanceUntilIdle()
        assertEquals(1, createCalls)
        assertFalse(vm.isCreating)
    }

    @Test fun `subsequent garden keeps existing navigation`() = runTest {
        existingGardens = listOf(garden.copy(id = 1))
        val vm = viewModel()
        vm.createGarden()
        advanceUntilIdle()
        assertEquals(7L, vm.createdGardenId)
        assertFalse(vm.createdFirstGarden)
    }

    @Test fun `failed count read does not create a garden or navigate`() = runTest {
        failList = true
        val vm = viewModel()
        vm.createGarden()
        advanceUntilIdle()
        assertNull(vm.createdGardenId)
        assertEquals(0, createCalls)
        assertNotNull(vm.error)
        assertFalse(vm.isCreating)
        failList = false
        vm.createGarden()
        advanceUntilIdle()
        assertTrue(vm.createdFirstGarden)
        assertEquals(1, createCalls)
    }

    @Test fun `failed create does not signal navigation`() = runTest {
        failCreate = true
        val vm = viewModel()
        vm.createGarden()
        advanceUntilIdle()
        assertNull(vm.createdGardenId)
        assertFalse(vm.createdFirstGarden)
        assertNotNull(vm.error)
    }
}
