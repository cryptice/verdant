package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HarvestPlanRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun list(offset: Int = 0) = api.getHarvestPlans(offset)
    suspend fun get(id: Long) = api.getHarvestPlan(id)
    suspend fun species(speciesId: Long?, groupId: Long?) = api.getPlanningSpecies(speciesId, groupId)
    suspend fun profile(id: Long, profile: ProductionProfile) = api.saveProductionProfile(id, profile)
    suspend fun preview(request: HarvestPlanRequest) = api.previewHarvestPlan(request)
    suspend fun create(key: String, preview: HarvestPlanPreview) = api.createHarvestPlan(CreateHarvestPlanRequest(key, preview.fingerprint, preview.request))
    suspend fun complete(id: Long, task: HarvestPlanTask, quantity: Int) = api.completeHarvestPlanTask(id, task.id, CompleteHarvestPlanTaskRequest(task.remainingCount, quantity))
    suspend fun cancel(id: Long) = api.cancelHarvestPlan(id)
    suspend fun seasons() = api.getSeasons()
    suspend fun groups() = api.getSpeciesGroups()
    suspend fun search(query: String) = api.searchPlanningSpecies(query)
}
