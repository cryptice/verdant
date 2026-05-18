package app.verdant.android.ui.plant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreateOutletRequest
import app.verdant.android.data.model.CreateSaleLotForHarvestRequest
import app.verdant.android.data.model.CreateSaleLotForPlantRequest
import app.verdant.android.data.model.OutletResponse
import app.verdant.android.data.model.PlantEventResponse
import app.verdant.android.data.model.PlantResponse
import app.verdant.android.data.model.PlantWorkflowProgressResponse
import app.verdant.android.data.repository.OutletRepository
import app.verdant.android.data.repository.PlantRepository
import app.verdant.android.data.repository.SaleLotRepository
import app.verdant.android.data.repository.WorkflowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlantDetailState(
    val isLoading: Boolean = true,
    val plant: PlantResponse? = null,
    val events: List<PlantEventResponse> = emptyList(),
    val workflowProgress: PlantWorkflowProgressResponse? = null,
    val outlets: List<OutletResponse> = emptyList(),
    val availableForSale: Int = 0,
    val toastMessage: String? = null,
    val error: String? = null,
    val deleted: Boolean = false
)

@HiltViewModel
class PlantDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val plantRepository: PlantRepository,
    private val workflowRepository: WorkflowRepository,
    private val outletRepository: OutletRepository,
    private val saleLotRepository: SaleLotRepository,
) : ViewModel() {
    private val plantId: Long = savedStateHandle.get<Long>("plantId")!!
    private val _uiState = MutableStateFlow(PlantDetailState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = PlantDetailState(isLoading = true)
            try {
                val plant = plantRepository.get(plantId)
                val events = plantRepository.events(plantId)
                val workflowProgress = try {
                    workflowRepository.plantProgress(plantId)
                } catch (_: Exception) {
                    null
                }
                val outlets = runCatching { outletRepository.list() }.getOrDefault(emptyList())
                val available = runCatching { saleLotRepository.availableForPlant(plantId) }.getOrDefault(0)
                _uiState.value = PlantDetailState(
                    isLoading = false,
                    plant = plant,
                    events = events,
                    workflowProgress = workflowProgress,
                    outlets = outlets,
                    availableForSale = available,
                )
            } catch (e: Exception) {
                _uiState.value = PlantDetailState(isLoading = false, error = e.message)
            }
        }
    }

    fun createSaleLot(
        unitKind: String,
        quantityTotal: Int,
        initialRequestedPriceCents: Int,
        outletId: Long,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                saleLotRepository.createForPlant(
                    CreateSaleLotForPlantRequest(
                        plantId = plantId,
                        unitKind = unitKind,
                        quantityTotal = quantityTotal,
                        initialRequestedPriceCents = initialRequestedPriceCents,
                        currentOutletId = outletId,
                    ),
                )
                _uiState.value = _uiState.value.copy(toastMessage = "Lade ut $quantityTotal till försäljning")
                onDone()
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Kunde inte lägga ut till försäljning")
            }
        }
    }

    fun createOutlet(name: String, channel: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val created = outletRepository.create(
                    CreateOutletRequest(name = name, channel = channel),
                )
                _uiState.value = _uiState.value.copy(outlets = _uiState.value.outlets + created)
                onCreated(created.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun consumeToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    /** Fetches available-for-sale for a harvest event on demand (for the listing dialog). */
    suspend fun fetchHarvestAvailable(eventId: Long): Int =
        runCatching { saleLotRepository.availableForHarvestEvent(eventId) }.getOrDefault(0)

    fun createSaleLotForHarvest(
        harvestEventId: Long,
        unitKind: String,
        stemsPerUnit: Int?,
        quantityTotal: Int,
        initialRequestedPriceCents: Int,
        outletId: Long,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                saleLotRepository.createForHarvest(
                    CreateSaleLotForHarvestRequest(
                        harvestEventId = harvestEventId,
                        unitKind = unitKind,
                        stemsPerUnit = stemsPerUnit,
                        quantityTotal = quantityTotal,
                        initialRequestedPriceCents = initialRequestedPriceCents,
                        currentOutletId = outletId,
                    ),
                )
                _uiState.value = _uiState.value.copy(toastMessage = "Lade ut $quantityTotal till försäljning")
                onDone()
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Kunde inte lägga ut till försäljning")
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                plantRepository.delete(plantId)
                _uiState.value = _uiState.value.copy(deleted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                plantRepository.deleteEvent(plantId, eventId)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
