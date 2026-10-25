package app.verdant.android.ui.activity

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.BatchSowRequest
import app.verdant.android.data.model.BatchSowResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.repository.*
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
class RegisterPlantsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private val species = Gson().fromJson("""{"id":1991,"commonName":"Chili","commonNameSv":"Chilipeppar","variantName":"Naga Black","groups":[],"tags":[],"photos":[],"growingPositions":[],"soils":[],"providers":[]} """, SpeciesResponse::class.java)
    private val sowRequests = mutableListOf<BatchSowRequest>()
    private fun api(): VerdantApi = Proxy.newProxyInstance(VerdantApi::class.java.classLoader, arrayOf(VerdantApi::class.java)) { _, method, args ->
        when (method.name) {
            "getSpecies", "getAllBeds", "getTrayLocations" -> emptyList<Any>()
            "batchSow" -> {
                val request = args!![0] as BatchSowRequest
                sowRequests += request
                BatchSowResponse((1L..request.seedCount.toLong()).toList(), request.seedCount)
            }
            else -> error(method.name)
        }
    } as VerdantApi

    @Test fun `registering plants sends the registered origin`() = runTest {
        val api = api()
        val vm = RegisterPlantsViewModel(SpeciesRepositoryImpl(api), TrayLocationRepository(api), BedRepository(api), PlantRepository(api))
        advanceUntilIdle()
        vm.register(species, 3, LocalDate.of(2026, 5, 1), null, null, 2L)
        advanceUntilIdle()
        assertEquals("REGISTERED", sowRequests.single().origin)
        assertTrue(vm.uiState.value.created)
    }
}
