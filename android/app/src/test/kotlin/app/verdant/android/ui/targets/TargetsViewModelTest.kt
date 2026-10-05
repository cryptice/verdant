package app.verdant.android.ui.targets

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.SeasonResponse
import app.verdant.android.data.repository.AnalyticsRepository
import app.verdant.android.data.repository.SeasonRepositoryImpl
import app.verdant.android.data.repository.SpeciesRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class TargetsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    private fun model(call: (String) -> Any?): TargetsViewModel {
        val api = Proxy.newProxyInstance(VerdantApi::class.java.classLoader, arrayOf(VerdantApi::class.java)) { _, method, _ -> call(method.name) } as VerdantApi
        return TargetsViewModel(SeasonRepositoryImpl(api), AnalyticsRepository(api), SpeciesRepositoryImpl(api))
    }

    @Test fun `empty seasons redirect before loading weekly target data`() = runTest {
        val calls = mutableListOf<String>()
        val vm = model { method ->
            calls += method
            if (method == "getSeasons") emptyList<SeasonResponse>() else error(method)
        }
        assertFalse(vm.uiState.value.needsSeason)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.needsSeason)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
        assertEquals(listOf("getSeasons"), calls)
    }

    @Test fun `inactive season allows weekly targets`() = runTest {
        val season = SeasonResponse(1, "2026", 2026, null, null, null, null, null, null, false, "", "")
        val vm = model { method -> when (method) {
            "getSeasons" -> listOf(season)
            "getSpecies", "getProductionTargets" -> emptyList<Any>()
            else -> error(method)
        } }
        advanceUntilIdle()
        assertFalse(vm.uiState.value.needsSeason)
        assertNull(vm.uiState.value.error)
        assertEquals(listOf(season), vm.uiState.value.seasons)
    }

    @Test fun `failed season load does not redirect and supports retry`() = runTest {
        var fail = true
        val vm = model { method ->
            if (fail) error("Offline")
            if (method == "getSeasons") emptyList<SeasonResponse>() else error(method)
        }
        advanceUntilIdle()
        assertFalse(vm.uiState.value.needsSeason)
        assertEquals("Offline", vm.uiState.value.error)
        fail = false
        vm.refresh(); advanceUntilIdle()
        assertTrue(vm.uiState.value.needsSeason)
    }
}
