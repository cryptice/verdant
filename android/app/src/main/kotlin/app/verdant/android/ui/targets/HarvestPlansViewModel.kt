package app.verdant.android.ui.targets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.*
import app.verdant.android.data.repository.HarvestPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.util.UUID
import javax.inject.Inject

data class HarvestPlansState(
    val busy: Boolean = false,
    val needsSeason: Boolean = false,
    val error: String? = null,
    val seasons: List<SeasonResponse> = emptyList(),
    val groups: List<SpeciesGroupResponse> = emptyList(),
    val searchResults: List<SpeciesResponse> = emptyList(),
    val candidates: List<PlanningSpecies> = emptyList(),
    val preview: HarvestPlanPreview? = null,
    val plans: List<HarvestPlanResponse> = emptyList(),
    val selected: HarvestPlanResponse? = null,
    val page: Int = 0,
)

@HiltViewModel
class HarvestPlansViewModel @Inject constructor(private val repo: HarvestPlanRepository) : ViewModel() {
    private val state = MutableStateFlow(HarvestPlansState())
    val uiState = state.asStateFlow()
    private var requestKey = UUID.randomUUID().toString()

    private fun work(block: suspend () -> Unit) {
        if (state.value.busy) return
        state.value = state.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val message = if (e is HttpException) runCatching {
                    JSONObject(e.response()?.errorBody()?.string().orEmpty()).optString("message").takeIf { it.isNotBlank() }
                }.getOrNull() else null
                state.value = state.value.copy(error = message ?: e.message ?: "Kunde inte spara planen")
            } finally { state.value = state.value.copy(busy = false) }
        }
    }

    fun load(planId: Long? = null) = work {
        invalidatePreview()
        state.value = state.value.copy(candidates = emptyList(), searchResults = emptyList(), needsSeason = false)
        val seasons = repo.seasons()
        state.value = state.value.copy(seasons = seasons, needsSeason = seasons.isEmpty())
        if (seasons.isEmpty()) return@work
        val groups = repo.groups()
        val plans = repo.list()
        val selected = planId?.let { repo.get(it) }
        state.value = state.value.copy(seasons = seasons, groups = groups, plans = plans, selected = selected, page = 0)
    }
    fun search(query: String) = work { state.value = state.value.copy(searchResults = repo.search(query)) }
    fun candidates(speciesId: Long?, groupId: Long?) {
        invalidatePreview()
        state.value = state.value.copy(candidates = emptyList())
        work { state.value = state.value.copy(candidates = repo.species(speciesId, groupId)) }
    }
    fun invalidatePreview() {
        requestKey = UUID.randomUUID().toString()
        state.value = state.value.copy(preview = null, error = null)
    }
    fun profile(id: Long, profile: ProductionProfile, onSaved: () -> Unit) = work {
        val saved = repo.profile(id, profile)
        invalidatePreview()
        state.value = state.value.copy(candidates = state.value.candidates.map { if (it.speciesId == id) saved else it })
        onSaved()
    }
    fun preview(request: HarvestPlanRequest) = work {
        state.value = state.value.copy(preview = null)
        state.value = state.value.copy(preview = repo.preview(request))
    }
    fun save() = work {
        val preview = state.value.preview ?: return@work
        val plan = repo.create(requestKey, preview)
        invalidatePreview()
        state.value = state.value.copy(selected = plan, plans = listOf(plan) + state.value.plans.filter { it.id != plan.id })
    }
    fun newPlan() {
        invalidatePreview()
        state.value = state.value.copy(selected = null)
    }
    fun select(id: Long) = work { state.value = state.value.copy(selected = repo.get(id)) }
    fun page(page: Int) = work { state.value = state.value.copy(plans = repo.list(page * 50), page = page) }
    fun cancel(id: Long) = work { replace(repo.cancel(id)) }
    fun complete(plan: HarvestPlanResponse, task: HarvestPlanTask, quantity: Int) = work {
        try { replace(repo.complete(plan.id, task, quantity)) }
        catch (e: HttpException) {
            if (e.code() == 409) replace(repo.get(plan.id))
            throw e
        }
    }
    private fun replace(plan: HarvestPlanResponse) {
        state.value = state.value.copy(selected = plan, plans = state.value.plans.map { if (it.id == plan.id) plan else it })
    }
}
