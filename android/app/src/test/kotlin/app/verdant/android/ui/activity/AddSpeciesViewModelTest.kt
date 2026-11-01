package app.verdant.android.ui.activity

import androidx.lifecycle.SavedStateHandle
import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.*
import app.verdant.android.data.repository.PlantRepository
import app.verdant.android.data.repository.SpeciesRepositoryImpl
import app.verdant.android.data.repository.WorkflowRepository
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
class AddSpeciesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    private fun species(id: Long, system: Boolean = false, template: Long? = null) = Gson().fromJson(
        """{"id":$id,"commonName":"Zinnia","isSystem":$system,"workflowTemplateId":${template ?: "null"},"groups":[],"tags":[],"photos":[],"growingPositions":[],"soils":[],"providers":[]}""",
        SpeciesResponse::class.java,
    )
    private val calls = mutableListOf<String>()
    private var existing = species(7)

    private fun api(): VerdantApi = Proxy.newProxyInstance(VerdantApi::class.java.classLoader, arrayOf(VerdantApi::class.java)) { _, method, args ->
        when (method.name) {
            "getSpeciesTags" -> emptyList<Any>()
            "getProviders" -> listOf(ProviderResponse(1, "Impecta", "impecta"))
            "getWorkflowTemplates" -> listOf(WorkflowTemplateResponse(6, "Zinnia – snittblommor", null))
            "getSpeciesById" -> existing
            "createSpecies" -> { calls += "create ${(args!![0] as CreateSpeciesRequest).commonName}"; species(30) }
            "updateSpecies" -> { calls += "update ${args!![0]}"; existing }
            "addSpeciesProvider" -> { val r = args!![1] as AddSpeciesProviderRequest; calls += "provider ${args[0]} ${r.providerId} ${r.productUrl}"; null }
            "assignSpeciesWorkflow" -> { calls += "assign ${args!![0]} ${(args[1] as AssignWorkflowTemplateRequest).templateId}"; null }
            "uploadSpeciesPhoto" -> { calls += "photo ${args!![0]}"; null }
            else -> error(method.name)
        }
    } as VerdantApi

    private fun viewModel(speciesId: Long? = null) = api().let {
        AddSpeciesViewModel(SavedStateHandle(mapOf("speciesId" to (speciesId ?: -1L))), SpeciesRepositoryImpl(it), PlantRepository(it), WorkflowRepository(it))
    }

    @Test fun `a new variety is created with its supplier and workflow`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(listOf("Impecta"), vm.uiState.value.providers.map { it.name })

        vm.createSpecies(SpeciesFormValues(commonNameSv = "Zinnia").toCreateRequest(null, null), NewSpeciesExtras(1, "https://impecta.se/zinnia", 6))
        advanceUntilIdle()

        assertEquals(listOf("create Zinnia", "provider 30 1 https://impecta.se/zinnia", "assign 30 6"), calls)
        assertTrue(vm.uiState.value.created)
    }

    @Test fun `saving assigns the workflow only when it changed`() = runTest {
        existing = species(7, template = 6)
        val vm = viewModel(7)
        advanceUntilIdle()

        vm.updateSpecies(SpeciesFormValues(commonNameSv = "Zinnia").toUpdateRequest(null, null), 6)
        advanceUntilIdle()
        assertTrue(calls.none { it.startsWith("assign") })

        vm.updateSpecies(SpeciesFormValues(commonNameSv = "Zinnia").toUpdateRequest(null, null), 9)
        advanceUntilIdle()
        assertEquals("assign 7 9", calls.last())
    }

    @Test fun `a catalogue variety is read-only and a photo on an own variety uploads at once`() = runTest {
        existing = species(468, system = true)
        assertTrue(viewModel(468).also { advanceUntilIdle() }.uiState.value.readOnly)

        existing = species(7)
        val vm = viewModel(7)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.readOnly)
        vm.uploadPhoto("aGVsbG8=")
        advanceUntilIdle()
        assertEquals("photo 7", calls.last())
    }
}
