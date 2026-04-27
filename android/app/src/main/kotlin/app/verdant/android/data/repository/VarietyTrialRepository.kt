package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateVarietyTrialRequest
import app.verdant.android.data.model.UpdateVarietyTrialRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Variety trials. */
@Singleton
class VarietyTrialRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun list(seasonId: Long? = null) = api.getVarietyTrials(seasonId)
    suspend fun get(id: Long) = api.getVarietyTrial(id)
    suspend fun create(request: CreateVarietyTrialRequest) = api.createVarietyTrial(request)
    suspend fun update(id: Long, request: UpdateVarietyTrialRequest) = api.updateVarietyTrial(id, request)
    suspend fun delete(id: Long) = api.deleteVarietyTrial(id)
}
