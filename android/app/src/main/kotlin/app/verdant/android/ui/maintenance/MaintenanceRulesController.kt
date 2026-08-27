package app.verdant.android.ui.maintenance

import app.verdant.android.data.model.CreateMaintenanceRuleRequest
import app.verdant.android.data.model.MaintenanceRuleResponse
import app.verdant.android.data.model.MaintenanceTarget
import app.verdant.android.data.model.UpdateMaintenanceRuleRequest
import app.verdant.android.data.repository.MaintenanceRuleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MaintenanceRulesState(
    val rules: List<MaintenanceRuleResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * Loads and mutates maintenance rules for a single target — a bed or a
 * garden area. Shared by [app.verdant.android.ui.area]'s and
 * [app.verdant.android.ui.bed]'s detail ViewModels so the "exactly one of
 * bedId/areaId" and "clearSeasonWindow alone" server rules are enforced
 * in exactly one place.
 *
 * Plain class, not a ViewModel: [scope] is the host ViewModel's
 * `viewModelScope` in production and a `TestScope` in tests.
 */
class MaintenanceRulesController(
    private val repository: MaintenanceRuleRepository,
    private val target: MaintenanceTarget,
    private val targetId: Long,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(MaintenanceRulesState())
    val state = _state.asStateFlow()

    private val bedId: Long? get() = if (target == MaintenanceTarget.BED) targetId else null
    private val areaId: Long? get() = if (target == MaintenanceTarget.AREA) targetId else null

    fun refresh() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val rules = repository.list(bedId = bedId, areaId = areaId)
                _state.value = _state.value.copy(rules = rules, isLoading = false, error = null)
            } catch (e: Exception) {
                // Keep whatever rules were already loaded — matching
                // BedDetailViewModel's "once loaded, stay loaded" behaviour.
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Kunde inte ladda skötselregler",
                )
            }
        }
    }

    fun create(
        activityType: String,
        intervalDays: Int,
        anchorDate: String? = null,
        seasonStartMonth: Int? = null,
        seasonStartDay: Int? = null,
        seasonEndMonth: Int? = null,
        seasonEndDay: Int? = null,
        notes: String? = null,
    ) {
        scope.launch {
            try {
                repository.create(
                    CreateMaintenanceRuleRequest(
                        bedId = bedId,
                        gardenAreaId = areaId,
                        activityType = activityType,
                        intervalDays = intervalDays,
                        anchorDate = anchorDate,
                        seasonStartMonth = seasonStartMonth,
                        seasonStartDay = seasonStartDay,
                        seasonEndMonth = seasonEndMonth,
                        seasonEndDay = seasonEndDay,
                        notes = notes,
                    ),
                )
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Kunde inte skapa skötselregel")
            }
        }
    }

    fun update(id: Long, request: UpdateMaintenanceRuleRequest) {
        scope.launch {
            try {
                repository.update(id, request)
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Kunde inte uppdatera skötselregel")
            }
        }
    }

    /**
     * The only way to remove a season window. Sends the flag and nothing
     * season-related — combining them is a 400 server-side.
     */
    fun clearSeasonWindow(ruleId: Long) {
        update(ruleId, UpdateMaintenanceRuleRequest(clearSeasonWindow = true))
    }

    fun delete(id: Long) {
        scope.launch {
            try {
                repository.delete(id)
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Kunde inte ta bort skötselregel")
            }
        }
    }
}
