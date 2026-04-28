package app.verdant.android.ui.bed
import app.verdant.android.data.repository.BedRepository
import app.verdant.android.data.repository.GardenApiRepository
import app.verdant.android.data.repository.PlantRepository
import app.verdant.android.data.repository.SupplyApplicationRepository

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.BedEventResponse
import app.verdant.android.data.model.BedResponse
import app.verdant.android.data.model.CreateBedRequest
import app.verdant.android.data.model.PlantResponse
import app.verdant.android.data.model.SupplyApplicationResponse
import app.verdant.android.data.model.UpdateBedRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BedDetailState(
    val isLoading: Boolean = true,
    val bed: BedResponse? = null,
    val gardenName: String? = null,
    val plants: List<PlantResponse> = emptyList(),
    val applications: List<SupplyApplicationResponse> = emptyList(),
    val bedEvents: List<BedEventResponse> = emptyList(),
    val error: String? = null,
    val deleted: Boolean = false,
    val expandedGroups: Set<String> = emptySet(),
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
    val toastMessage: String? = null,
)

@HiltViewModel
class BedDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bedRepository: BedRepository,
    private val plantRepository: PlantRepository,
    private val supplyApplicationRepository: SupplyApplicationRepository,
    private val gardenApiRepository: GardenApiRepository,
) : ViewModel() {
    private val bedId: Long = savedStateHandle.get<Long>("bedId")!!
    private val _uiState = MutableStateFlow(BedDetailState())
    val uiState = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            // Cold load shows the spinner; in-place refreshes keep existing
            // content so the screen doesn't flicker on resume.
            val isColdLoad = _uiState.value.bed == null
            _uiState.value = _uiState.value.copy(isLoading = isColdLoad, error = null)
            try {
                val bed = bedRepository.get(bedId)
                val plants = plantRepository.listForBed(bedId)
                val applications = runCatching { supplyApplicationRepository.listByBed(bedId, 10) }.getOrDefault(emptyList())
                val bedEvents = runCatching { bedRepository.events(bedId, 20) }.getOrDefault(emptyList())
                val gardenName = runCatching { gardenApiRepository.get(bed.gardenId).name }.getOrNull()
                _uiState.value = _uiState.value.copy(
                    isLoading = false, bed = bed, plants = plants, applications = applications,
                    bedEvents = bedEvents, gardenName = gardenName,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun update(
        name: String,
        description: String?,
        soilType: String?,
        soilPh: Double?,
        sunExposure: String?,
        drainage: String?,
        sunDirections: Set<String>,
        irrigationType: String?,
        protection: String?,
        raisedBed: Boolean?,
    ) {
        viewModelScope.launch {
            try {
                bedRepository.update(
                    bedId,
                    UpdateBedRequest(
                        name = name,
                        description = description,
                        soilType = soilType,
                        soilPh = soilPh,
                        sunExposure = sunExposure,
                        drainage = drainage,
                        sunDirections = sunDirections.toList(),
                        irrigationType = irrigationType,
                        protection = protection,
                        raisedBed = raisedBed,
                    ),
                )
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun toggleGroup(species: String) {
        val current = _uiState.value.expandedGroups
        _uiState.value = _uiState.value.copy(
            expandedGroups = if (species in current) current - species else current + species,
        )
    }

    fun saveScrollPosition(index: Int, offset: Int) {
        _uiState.value = _uiState.value.copy(scrollIndex = index, scrollOffset = offset)
    }

    fun delete() {
        viewModelScope.launch {
            try {
                bedRepository.delete(bedId)
                _uiState.value = _uiState.value.copy(deleted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun weed() {
        viewModelScope.launch {
            try {
                val r = bedRepository.weed(bedId)
                _uiState.value = _uiState.value.copy(
                    toastMessage = "Rensade ogräs · ${r.plantsAffected} plantor",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(toastMessage = "Kunde inte rensa ogräs")
            }
        }
    }

    fun water() {
        viewModelScope.launch {
            try {
                val r = bedRepository.water(bedId)
                _uiState.value = _uiState.value.copy(
                    toastMessage = "Vattnade · ${r.plantsAffected} plantor",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(toastMessage = "Kunde inte vattna")
            }
        }
    }

    fun consumeToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    /**
     * Duplicate the bed (same conditions). If the source name ends with `#<n>`,
     * the new name uses the same stem with the next free number
     * (`Bed #1` → `Bed #2`); otherwise we fall back to `{old} (kopia)`.
     */
    fun copy(onCopied: (Long) -> Unit) {
        val source = _uiState.value.bed ?: return
        viewModelScope.launch {
            try {
                val newName = nextCopyName(source.name, source.gardenId)
                val created = bedRepository.create(
                    source.gardenId,
                    CreateBedRequest(
                        name = newName,
                        description = source.description,
                        soilType = source.soilType,
                        soilPh = source.soilPh,
                        sunExposure = source.sunExposure,
                        drainage = source.drainage,
                        sunDirections = source.sunDirections,
                        irrigationType = source.irrigationType,
                        protection = source.protection,
                        raisedBed = source.raisedBed,
                    ),
                )
                onCopied(created.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private suspend fun nextCopyName(sourceName: String, gardenId: Long): String {
        val match = Regex("^(.*?)#(\\d+)\\s*$").matchEntire(sourceName) ?: return "$sourceName (kopia)"
        val stem = match.groupValues[1]
        val siblings = runCatching { bedRepository.list(gardenId) }.getOrDefault(emptyList())
        val pattern = Regex("^${Regex.escape(stem)}#(\\d+)\\s*$")
        val highest = siblings.mapNotNull { pattern.matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull() ?: 0
        return "$stem#${highest + 1}"
    }
}
