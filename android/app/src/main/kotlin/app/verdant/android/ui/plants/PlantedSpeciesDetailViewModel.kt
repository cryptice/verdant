package app.verdant.android.ui.plants
import app.verdant.android.data.repository.BedRepository
import app.verdant.android.data.repository.PlantRepository
import app.verdant.android.data.repository.TaskRepository
import app.verdant.android.data.repository.TrayLocationRepository

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.BatchEventRequest
import app.verdant.android.data.model.BedWithGardenResponse
import app.verdant.android.data.model.DeleteSpeciesEventRequest
import app.verdant.android.data.model.MoveTrayPlantsRequest
import app.verdant.android.data.model.PlantLocationGroup
import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.data.model.SpeciesEventSummaryEntry
import app.verdant.android.data.model.TrayLocationResponse
import app.verdant.android.data.model.UpdateSpeciesEventDateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PlantedSpeciesDetailViewModel"

data class PlantedSpeciesDetailState(
    val isLoading: Boolean = true,
    val speciesName: String = "",
    val tasks: List<ScheduledTaskResponse> = emptyList(),
    val locations: List<PlantLocationGroup> = emptyList(),
    val beds: List<BedWithGardenResponse> = emptyList(),
    val trayLocations: List<TrayLocationResponse> = emptyList(),
    val trayEvents: List<SpeciesEventSummaryEntry> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class PlantedSpeciesDetailViewModel @Inject constructor(
    private val plantRepository: PlantRepository,
    private val bedRepository: BedRepository,
    private val taskRepository: TaskRepository,
    private val trayLocationRepository: TrayLocationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val speciesId: Long = savedStateHandle["speciesId"]!!
    private val _uiState = MutableStateFlow(PlantedSpeciesDetailState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun deleteEvent(
        eventType: String,
        eventDate: String,
        count: Int,
        currentStatus: String,
        trayOnly: Boolean = true,
    ) {
        viewModelScope.launch {
            try {
                plantRepository.deleteSpeciesEvent(
                    speciesId,
                    DeleteSpeciesEventRequest(
                        eventType = eventType,
                        eventDate = eventDate,
                        count = count,
                        currentStatus = currentStatus,
                        trayOnly = trayOnly,
                    ),
                )
                load()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete event", e)
            }
        }
    }

    fun updateEventDate(
        eventType: String,
        oldDate: String,
        newDate: java.time.LocalDate,
        currentStatus: String,
        trayOnly: Boolean = true,
    ) {
        viewModelScope.launch {
            try {
                plantRepository.updateSpeciesEventDate(
                    speciesId,
                    UpdateSpeciesEventDateRequest(
                        eventType = eventType,
                        oldDate = oldDate,
                        newDate = newDate.toString(),
                        currentStatus = currentStatus,
                        trayOnly = trayOnly,
                    ),
                )
                load()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update event date", e)
            }
        }
    }

    fun moveTrayPlants(
        sourceLocationId: Long?,
        targetLocationId: Long?,
        status: String,
        count: Int,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                plantRepository.moveTrayPlants(
                    MoveTrayPlantsRequest(
                        fromTrayLocationId = sourceLocationId,
                        toTrayLocationId = targetLocationId,
                        speciesId = speciesId,
                        status = status,
                        count = count,
                    ),
                )
                load()
                onDone()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to move plants", e)
            }
        }
    }

    fun batchEvent(item: PlantLocationGroup, eventType: String, count: Int, targetBedId: Long? = null, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                plantRepository.batchEvent(
                    BatchEventRequest(
                        speciesId = speciesId,
                        bedId = item.bedId,
                        plantedDate = null,
                        status = item.status,
                        eventType = eventType,
                        count = count,
                        targetBedId = targetBedId,
                    ),
                )
                load()
                onDone()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit batch event", e)
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val locations = plantRepository.speciesLocations(speciesId)
                val beds = bedRepository.listAll()
                val tasks = taskRepository.list().filter {
                    it.speciesId == speciesId && it.status == "PENDING"
                }
                val summary = plantRepository.speciesSummary().find { it.speciesId == speciesId }
                val name = summary?.let {
                    it.variantName?.let { v -> "${it.speciesName} – $v" } ?: it.speciesName
                } ?: tasks.firstOrNull()?.speciesName ?: ""
                val trayEvents = runCatching { plantRepository.speciesEvents(speciesId, trayOnly = true) }
                    .onFailure { Log.e(TAG, "Failed to load species event summary", it) }
                    .getOrDefault(emptyList())
                val trayLocations = runCatching { trayLocationRepository.list() }
                    .getOrDefault(emptyList())
                _uiState.value = PlantedSpeciesDetailState(
                    isLoading = false,
                    speciesName = name,
                    tasks = tasks,
                    locations = locations,
                    beds = beds,
                    trayLocations = trayLocations,
                    trayEvents = trayEvents,
                )
            } catch (e: Exception) {
                _uiState.value = PlantedSpeciesDetailState(isLoading = false, error = e.message)
            }
        }
    }
}
