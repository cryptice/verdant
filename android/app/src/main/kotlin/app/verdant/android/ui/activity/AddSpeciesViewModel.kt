package app.verdant.android.ui.activity

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreateSpeciesGroupRequest
import app.verdant.android.data.model.CreateSpeciesRequest
import app.verdant.android.data.model.CreateSpeciesTagRequest
import app.verdant.android.data.model.ExtractSpeciesInfoRequest
import app.verdant.android.data.model.ExtractedSpeciesInfo
import app.verdant.android.data.model.IdentifyPlantRequest
import app.verdant.android.data.model.PlantSuggestion
import app.verdant.android.data.model.SpeciesGroupResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.SpeciesTagResponse
import app.verdant.android.data.model.UpdateSpeciesRequest
import app.verdant.android.data.repository.PlantRepository
import app.verdant.android.data.repository.SpeciesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AddSpeciesViewModel"

data class AddSpeciesState(
    val isLoading: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
    val groups: List<SpeciesGroupResponse> = emptyList(),
    val tags: List<SpeciesTagResponse> = emptyList(),
    val identifying: Boolean = false,
    val extracting: Boolean = false,
    val suggestions: List<PlantSuggestion> = emptyList(),
    val extractedInfo: ExtractedSpeciesInfo? = null,
    val existingSpecies: SpeciesResponse? = null,
)

@HiltViewModel
class AddSpeciesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val speciesRepository: SpeciesRepository,
    private val plantRepository: PlantRepository
) : ViewModel() {
    val speciesId: Long? = savedStateHandle.get<Long>("speciesId")?.takeIf { it > 0 }
    private val _uiState = MutableStateFlow(AddSpeciesState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val groups = speciesRepository.listGroups()
                val tags = speciesRepository.listTags()
                val existing = speciesId?.let { speciesRepository.list().find { s -> s.id == it } }
                _uiState.value = _uiState.value.copy(groups = groups, tags = tags, existingSpecies = existing)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load species data", e)
            }
        }
    }

    fun createSpecies(request: CreateSpeciesRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                speciesRepository.create(request)
                _uiState.value = _uiState.value.copy(isLoading = false, created = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun updateSpecies(request: UpdateSpeciesRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                speciesRepository.update(speciesId!!, request)
                _uiState.value = _uiState.value.copy(isLoading = false, created = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun createGroup(name: String) {
        viewModelScope.launch {
            try {
                speciesRepository.createGroup(CreateSpeciesGroupRequest(name))
                loadData()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create species group", e)
            }
        }
    }

    fun createTag(name: String) {
        viewModelScope.launch {
            try {
                speciesRepository.createTag(CreateSpeciesTagRequest(name))
                loadData()
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
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(extracting = false, error = "Kunde inte extrahera information")
            }
        }
    }
}
