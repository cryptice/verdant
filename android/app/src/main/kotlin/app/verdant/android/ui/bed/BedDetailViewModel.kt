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

/**
 * Three-state UI for the bed detail screen. Cold load shows a spinner;
 * the hard error case shows a retry; once we have a [Loaded] bed we stay
 * loaded — even on subsequent refresh failures — so navigation feels
 * stable.
 */
sealed interface BedDetailUiState {
    data object Loading : BedDetailUiState
    data class Error(val message: String) : BedDetailUiState
    data class Loaded(
        val bed: BedResponse,
        val gardenName: String?,
        val plants: List<PlantResponse>,
        val applications: List<SupplyApplicationResponse>,
        val bedEvents: List<BedEventResponse>,
        val isRefreshing: Boolean = false,
        val deleted: Boolean = false,
        val expandedGroups: Set<String> = emptySet(),
        val scrollIndex: Int = 0,
        val scrollOffset: Int = 0,
        val toastMessage: String? = null,
    ) : BedDetailUiState
}

@HiltViewModel
class BedDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bedRepository: BedRepository,
    private val plantRepository: PlantRepository,
    private val supplyApplicationRepository: SupplyApplicationRepository,
    private val gardenApiRepository: GardenApiRepository,
) : ViewModel() {
    private val bedId: Long = savedStateHandle.get<Long>("bedId")!!
    private val _uiState = MutableStateFlow<BedDetailUiState>(BedDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current is BedDetailUiState.Loaded) {
                _uiState.value = current.copy(isRefreshing = true)
            }
            try {
                val bed = bedRepository.get(bedId)
                val plants = plantRepository.listForBed(bedId)
                val applications = runCatching { supplyApplicationRepository.listByBed(bedId, 10) }.getOrDefault(emptyList())
                val bedEvents = runCatching { bedRepository.events(bedId, 20) }.getOrDefault(emptyList())
                val gardenName = runCatching { gardenApiRepository.get(bed.gardenId).name }.getOrNull()
                val previous = current as? BedDetailUiState.Loaded
                _uiState.value = BedDetailUiState.Loaded(
                    bed = bed,
                    gardenName = gardenName,
                    plants = plants,
                    applications = applications,
                    bedEvents = bedEvents,
                    isRefreshing = false,
                    expandedGroups = previous?.expandedGroups ?: emptySet(),
                    scrollIndex = previous?.scrollIndex ?: 0,
                    scrollOffset = previous?.scrollOffset ?: 0,
                    toastMessage = previous?.toastMessage,
                    deleted = previous?.deleted ?: false,
                )
            } catch (e: Exception) {
                _uiState.value = if (current is BedDetailUiState.Loaded) {
                    current.copy(isRefreshing = false)
                } else {
                    BedDetailUiState.Error(e.message ?: "Kunde inte ladda bädden")
                }
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
                (_uiState.value as? BedDetailUiState.Loaded)?.let {
                    _uiState.value = it.copy(toastMessage = e.message ?: "Kunde inte spara bädden")
                }
            }
        }
    }

    fun toggleGroup(species: String) {
        val current = _uiState.value as? BedDetailUiState.Loaded ?: return
        _uiState.value = current.copy(
            expandedGroups = if (species in current.expandedGroups) {
                current.expandedGroups - species
            } else {
                current.expandedGroups + species
            },
        )
    }

    fun saveScrollPosition(index: Int, offset: Int) {
        val current = _uiState.value as? BedDetailUiState.Loaded ?: return
        _uiState.value = current.copy(scrollIndex = index, scrollOffset = offset)
    }

    fun delete() {
        viewModelScope.launch {
            try {
                bedRepository.delete(bedId)
                (_uiState.value as? BedDetailUiState.Loaded)?.let {
                    _uiState.value = it.copy(deleted = true)
                }
            } catch (e: Exception) {
                (_uiState.value as? BedDetailUiState.Loaded)?.let {
                    _uiState.value = it.copy(toastMessage = e.message ?: "Kunde inte ta bort bädden")
                }
            }
        }
    }

    fun weed() {
        viewModelScope.launch {
            val current = _uiState.value as? BedDetailUiState.Loaded ?: return@launch
            try {
                val r = bedRepository.weed(bedId)
                _uiState.value = current.copy(toastMessage = "Rensade ogräs · ${r.plantsAffected} plantor")
            } catch (e: Exception) {
                _uiState.value = current.copy(toastMessage = "Kunde inte rensa ogräs")
            }
        }
    }

    fun water() {
        viewModelScope.launch {
            val current = _uiState.value as? BedDetailUiState.Loaded ?: return@launch
            try {
                val r = bedRepository.water(bedId)
                _uiState.value = current.copy(toastMessage = "Vattnade · ${r.plantsAffected} plantor")
            } catch (e: Exception) {
                _uiState.value = current.copy(toastMessage = "Kunde inte vattna")
            }
        }
    }

    fun consumeToast() {
        (_uiState.value as? BedDetailUiState.Loaded)?.let {
            _uiState.value = it.copy(toastMessage = null)
        }
    }

    /**
     * Duplicate the bed (same conditions). If the source name ends with `#<n>`,
     * the new name uses the same stem with the next free number
     * (`Bed #1` → `Bed #2`); otherwise we fall back to `{old} (kopia)`.
     */
    fun copy(onCopied: (Long) -> Unit) {
        val current = _uiState.value as? BedDetailUiState.Loaded ?: return
        val source = current.bed
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
                _uiState.value = current.copy(toastMessage = e.message ?: "Kunde inte kopiera bädden")
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
