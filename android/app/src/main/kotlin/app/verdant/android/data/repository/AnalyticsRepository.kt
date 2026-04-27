package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import javax.inject.Inject
import javax.inject.Singleton

/** Dashboard, analytics, harvest stats, production targets, succession schedules,
 *  workflows. Aggregations live here because they read across multiple domains. */
@Singleton
class AnalyticsRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun dashboard() = api.getDashboard()
    suspend fun harvestStats() = api.getHarvestStats()
    suspend fun seasonSummaries() = api.getSeasonSummaries()
    suspend fun speciesComparison(speciesId: Long) = api.getSpeciesComparison(speciesId)
    suspend fun yieldPerBed(seasonId: Long? = null) = api.getYieldPerBed(seasonId)

    suspend fun productionTargets(seasonId: Long? = null) = api.getProductionTargets(seasonId)
    suspend fun createProductionTarget(request: Map<String, Any?>) = api.createProductionTarget(request)
    suspend fun productionForecast(id: Long) = api.getProductionForecast(id)
    suspend fun deleteProductionTarget(id: Long) = api.deleteProductionTarget(id)

    suspend fun successionSchedules(seasonId: Long? = null) = api.getSuccessionSchedules(seasonId)
    suspend fun createSuccessionSchedule(request: Map<String, Any?>) = api.createSuccessionSchedule(request)
    suspend fun generateSuccessionTasks(id: Long) = api.generateSuccessionTasks(id)
    suspend fun deleteSuccessionSchedule(id: Long) = api.deleteSuccessionSchedule(id)
}
