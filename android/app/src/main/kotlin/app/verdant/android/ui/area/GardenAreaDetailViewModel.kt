package app.verdant.android.ui.area

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreateGardenAreaEventRequest
import app.verdant.android.data.model.CreateGardenAreaPhotoRequest
import app.verdant.android.data.model.GardenAreaEventResponse
import app.verdant.android.data.model.GardenAreaPhotoResponse
import app.verdant.android.data.model.GardenAreaResponse
import app.verdant.android.data.model.MaintenanceTarget
import app.verdant.android.data.model.UpdateGardenAreaRequest
import app.verdant.android.data.repository.GardenAreaRepository
import app.verdant.android.data.repository.MaintenanceRuleRepository
import app.verdant.android.ui.maintenance.MaintenanceRulesController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Three-state UI for the area detail screen — mirrors
 * [app.verdant.android.ui.bed.BedDetailUiState], but simpler: an area has
 * no plants, no harvest stats, and no supply applications.
 */
sealed interface GardenAreaDetailUiState {
    data object Loading : GardenAreaDetailUiState
    data class Error(val message: String) : GardenAreaDetailUiState
    data class Loaded(
        val area: GardenAreaResponse,
        val events: List<GardenAreaEventResponse>,
        val photos: List<GardenAreaPhotoResponse>,
        val isRefreshing: Boolean = false,
        val deleted: Boolean = false,
        val toastMessage: String? = null,
    ) : GardenAreaDetailUiState
}

@HiltViewModel
class GardenAreaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val areaRepository: GardenAreaRepository,
    ruleRepository: MaintenanceRuleRepository,
) : ViewModel() {
    private val areaId: Long = savedStateHandle.get<Long>("areaId")!!
    private val _uiState = MutableStateFlow<GardenAreaDetailUiState>(GardenAreaDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    /**
     * Shared with the bed detail screen (Task 7) — see
     * [MaintenanceRulesController]'s doc.
     *
     * Reloaded by [load] and by [logEvent], not by [refresh]: the backend
     * derives "last done" from the event log rather than a stored
     * timestamp, so a rule's `nextDueDate` changes only when work is
     * logged. Renaming an area or deleting a photo cannot move it, and
     * refetching there would be a round-trip that can only return what the
     * client already has.
     */
    val rulesController = MaintenanceRulesController(ruleRepository, MaintenanceTarget.AREA, areaId, viewModelScope)

    /** Initial load and every RESUME: the area plus its rules. */
    fun load() {
        rulesController.refresh()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current is GardenAreaDetailUiState.Loaded) {
                _uiState.value = current.copy(isRefreshing = true)
            }
            try {
                val area = areaRepository.get(areaId)
                val events = runCatching { areaRepository.events(areaId, 20) }.getOrDefault(emptyList())
                val photos = runCatching { areaRepository.photos(areaId) }.getOrDefault(emptyList())
                // Read the toast as it stands NOW, not as captured before the
                // round-trip above: the screen may have shown and consumed it
                // while these calls were in flight, and restoring the stale
                // value would fire the same snackbar a second time.
                val latest = _uiState.value as? GardenAreaDetailUiState.Loaded
                _uiState.value = GardenAreaDetailUiState.Loaded(
                    area = area,
                    events = events,
                    photos = photos,
                    isRefreshing = false,
                    deleted = latest?.deleted ?: false,
                    toastMessage = latest?.toastMessage,
                )
            } catch (e: Exception) {
                // Same reasoning as above: write from the current state, so a
                // toast raised while this was in flight is not rolled back.
                val latest = _uiState.value
                _uiState.value = if (latest is GardenAreaDetailUiState.Loaded) {
                    latest.copy(isRefreshing = false)
                } else {
                    GardenAreaDetailUiState.Error(e.message ?: "Kunde inte ladda platsen")
                }
            }
        }
    }

    /**
     * A null [description] or [sizeSqm] means the user emptied the field.
     * The server reads an omitted/null field as "keep the current value",
     * so emptying has to be an explicit flag — and the flag cannot be sent
     * alongside a replacement value. Deriving both here, from what is
     * currently loaded, keeps that contract in one place.
     */
    fun update(name: String, description: String?, category: String, sizeSqm: Double?) {
        viewModelScope.launch {
            val loaded = _uiState.value as? GardenAreaDetailUiState.Loaded
            val clearDescription = description == null && loaded?.area?.description != null
            val clearSizeSqm = sizeSqm == null && loaded?.area?.sizeSqm != null
            try {
                areaRepository.update(
                    areaId,
                    UpdateGardenAreaRequest(
                        name = name,
                        category = category,
                        description = description,
                        sizeSqm = sizeSqm,
                        clearDescription = clearDescription,
                        clearSizeSqm = clearSizeSqm,
                    ),
                )
                refresh()
            } catch (e: Exception) {
                (_uiState.value as? GardenAreaDetailUiState.Loaded)?.let {
                    _uiState.value = it.copy(toastMessage = e.message ?: "Kunde inte spara platsen")
                }
            }
        }
    }

    /**
     * Logs a maintenance event, then reloads both the event list and the
     * rules via [refresh]. The backend derives "when was this last done"
     * from the event log, not a stored timestamp — precisely so that
     * logging work by hand resets the reminder. Reloading only the event
     * list here would leave a just-logged rule showing a stale
     * `nextDueDate`, which looks broken.
     */
    fun logEvent(activityType: String, notes: String? = null) {
        viewModelScope.launch {
            val current = _uiState.value as? GardenAreaDetailUiState.Loaded ?: return@launch
            try {
                areaRepository.logEvent(
                    areaId,
                    CreateGardenAreaEventRequest(
                        activityType = activityType,
                        notes = notes?.takeIf { it.isNotBlank() },
                    ),
                )
                // Logging work is what moves a rule's next-due date, so this
                // is the one mutation that must reload the rules as well.
                rulesController.refresh()
                refresh()
            } catch (e: Exception) {
                _uiState.value = current.copy(toastMessage = e.message ?: "Kunde inte logga underhåll")
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                areaRepository.delete(areaId)
                (_uiState.value as? GardenAreaDetailUiState.Loaded)?.let {
                    _uiState.value = it.copy(deleted = true)
                }
            } catch (e: Exception) {
                (_uiState.value as? GardenAreaDetailUiState.Loaded)?.let {
                    _uiState.value = it.copy(toastMessage = e.message ?: "Kunde inte ta bort platsen")
                }
            }
        }
    }

    fun addPhoto(imageBase64: String, reason: String, description: String?) {
        viewModelScope.launch {
            val current = _uiState.value as? GardenAreaDetailUiState.Loaded ?: return@launch
            _uiState.value = current.copy(isRefreshing = true)
            try {
                areaRepository.addPhoto(
                    areaId,
                    CreateGardenAreaPhotoRequest(
                        imageBase64 = imageBase64,
                        reason = reason,
                        description = description?.takeIf { it.isNotBlank() },
                    ),
                )
                (_uiState.value as? GardenAreaDetailUiState.Loaded)?.let {
                    _uiState.value = it.copy(toastMessage = "Bild sparad")
                }
                refresh()
            } catch (e: Exception) {
                (_uiState.value as? GardenAreaDetailUiState.Loaded)?.let {
                    _uiState.value = it.copy(
                        isRefreshing = false,
                        toastMessage = e.message ?: "Kunde inte spara bilden",
                    )
                }
            }
        }
    }

    fun deletePhoto(photoId: Long) {
        viewModelScope.launch {
            val current = _uiState.value as? GardenAreaDetailUiState.Loaded ?: return@launch
            try {
                areaRepository.deletePhoto(areaId, photoId)
                refresh()
            } catch (e: Exception) {
                _uiState.value = current.copy(toastMessage = e.message ?: "Kunde inte ta bort bilden")
            }
        }
    }

    fun consumeToast() {
        (_uiState.value as? GardenAreaDetailUiState.Loaded)?.let {
            _uiState.value = it.copy(toastMessage = null)
        }
    }
}
