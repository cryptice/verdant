package app.verdant.android.ui.activity

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreateSpeciesRequest
import app.verdant.android.data.model.CreateSpeciesTagRequest
import app.verdant.android.data.model.ExtractSpeciesInfoRequest
import app.verdant.android.data.model.ExtractedSpeciesInfo
import app.verdant.android.data.model.IdentifyPlantRequest
import app.verdant.android.data.model.PlantSuggestion
import app.verdant.android.data.model.ProviderResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.SpeciesTagResponse
import app.verdant.android.data.model.UpdateSpeciesRequest
import app.verdant.android.data.model.VarietyNames
import app.verdant.android.data.model.WorkflowTemplateResponse
import app.verdant.android.data.model.duplicateSearchText
import app.verdant.android.data.model.findDuplicateVarieties
import app.verdant.android.data.repository.PlantRepository
import app.verdant.android.data.repository.SpeciesRepository
import app.verdant.android.data.repository.WorkflowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

private const val TAG = "AddSpeciesViewModel"

data class AddSpeciesState(
    val isLoading: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
    val tags: List<SpeciesTagResponse> = emptyList(),
    /** Seed suppliers to pick from, for a new variety and for linking more to an existing one. */
    val providers: List<ProviderResponse> = emptyList(),
    val templates: List<WorkflowTemplateResponse> = emptyList(),
    val identifying: Boolean = false,
    val extracting: Boolean = false,
    val suggestions: List<PlantSuggestion> = emptyList(),
    val extractedInfo: ExtractedSpeciesInfo? = null,
    val existingSpecies: SpeciesResponse? = null,
    val duplicates: List<SpeciesResponse> = emptyList(),
    /** A photo or supplier change on an existing variety is in flight. */
    val updatingDetails: Boolean = false,
) {
    /** A catalogue variety is shown read-only, like on the web. */
    val readOnly: Boolean get() = existingSpecies?.isSystem == true
}

/** What a new variety is created with besides the variety itself; suppliers and workflows are separate resources. */
data class NewSpeciesExtras(val providerId: Long? = null, val productUrl: String? = null, val workflowTemplateId: Long? = null)

/**
 * The variety form, matching the web's: names, growth, months, conditions, tags, commercial figures and a
 * workflow, plus a supplier when creating and photos and supplier links when editing. Photos and seed
 * packets can still fill in the form through plant identification and packet reading.
 */
@HiltViewModel
class AddSpeciesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val speciesRepository: SpeciesRepository,
    private val plantRepository: PlantRepository,
    private val workflowRepository: WorkflowRepository,
) : ViewModel() {
    val speciesId: Long? = savedStateHandle.get<Long>("speciesId")?.takeIf { it > 0 }
    private val _uiState = MutableStateFlow(AddSpeciesState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val tags = speciesRepository.listTags()
                val providers = runCatching { speciesRepository.providers() }.getOrDefault(emptyList())
                val templates = runCatching { workflowRepository.templates() }.getOrDefault(emptyList())
                val existing = speciesId?.let { speciesRepository.get(it) }
                _uiState.value = _uiState.value.copy(tags = tags, providers = providers, templates = templates, existingSpecies = existing)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load species data", e)
            }
        }
    }

    private var duplicateCheck: Job? = null

    /** Looks for an existing variety with the typed names, so a new one that already exists gets a warning. */
    fun checkDuplicates(names: VarietyNames) {
        duplicateCheck?.cancel()
        val query = duplicateSearchText(names)
        if (speciesId != null || query.length < 2) {
            _uiState.value = _uiState.value.copy(duplicates = emptyList())
            return
        }
        duplicateCheck = viewModelScope.launch {
            delay(400)
            try {
                val candidates = speciesRepository.search(query)
                _uiState.value = _uiState.value.copy(duplicates = findDuplicateVarieties(names, candidates))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check for existing varieties", e)
            }
        }
    }

    fun createSpecies(request: CreateSpeciesRequest, extras: NewSpeciesExtras) = save {
        val created = speciesRepository.create(request)
        extras.providerId?.let { speciesRepository.addProvider(created.id, it, extras.productUrl) }
        // Assigning copies the template's steps to the variety; the template id alone does not.
        extras.workflowTemplateId?.let { workflowRepository.assignTemplate(created.id, it) }
    }

    /** Saves the variety, and assigns [workflowTemplateId] when it changes from the current one. */
    fun updateSpecies(request: UpdateSpeciesRequest, workflowTemplateId: Long?) = save {
        val id = speciesId!!
        speciesRepository.update(id, request)
        if (workflowTemplateId != null && workflowTemplateId != _uiState.value.existingSpecies?.workflowTemplateId) {
            workflowRepository.assignTemplate(id, workflowTemplateId)
        }
    }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                block()
                _uiState.value = _uiState.value.copy(isLoading = false, created = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = serverMessage(e) ?: e.message)
            }
        }
    }

    fun uploadPhoto(imageBase64: String) = changeDetails("Kunde inte ladda upp fotot") { speciesRepository.uploadPhoto(it, imageBase64) }
    fun deletePhoto(photoId: Long) = changeDetails("Kunde inte ta bort fotot") { speciesRepository.deletePhoto(it, photoId) }
    fun addProvider(providerId: Long, productUrl: String?) =
        changeDetails("Kunde inte lägga till leverantören") { speciesRepository.addProvider(it, providerId, productUrl?.ifBlank { null }) }
    fun removeProvider(speciesProviderId: Long) =
        changeDetails("Kunde inte ta bort leverantören") { speciesRepository.removeProvider(it, speciesProviderId) }

    /** Photos and supplier links save at once, like on the web, and the variety is read back to show them. */
    private fun changeDetails(failure: String, block: suspend (Long) -> Unit) {
        val id = speciesId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updatingDetails = true, error = null)
            try {
                block(id)
                _uiState.value = _uiState.value.copy(updatingDetails = false, existingSpecies = speciesRepository.get(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(updatingDetails = false, error = serverMessage(e) ?: failure)
            }
        }
    }

    fun createTag(name: String) {
        viewModelScope.launch {
            try {
                speciesRepository.createTag(CreateSpeciesTagRequest(name))
                _uiState.value = _uiState.value.copy(tags = speciesRepository.listTags())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create species tag", e)
            }
        }
    }

    fun identifyPlant(imageBase64: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(identifying = true, suggestions = emptyList(), error = null)
            try {
                val suggestions = plantRepository.identify(IdentifyPlantRequest(imageBase64))
                _uiState.value = _uiState.value.copy(identifying = false, suggestions = suggestions)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(identifying = false, error = "Kunde inte identifiera bilden")
            }
        }
    }

    fun extractSpeciesInfo(imageBase64: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(extracting = true, extractedInfo = null, error = null)
            try {
                val info = plantRepository.extractSpeciesInfo(ExtractSpeciesInfoRequest(imageBase64))
                _uiState.value = _uiState.value.copy(extracting = false, extractedInfo = info)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(extracting = false, error = "Kunde inte extrahera information")
            }
        }
    }

    private fun serverMessage(e: Exception): String? = if (e is HttpException) runCatching {
        JSONObject(e.response()?.errorBody()?.string().orEmpty()).optString("message").takeIf { it.isNotBlank() }
    }.getOrNull() else null
}
