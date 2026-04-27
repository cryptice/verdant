package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreatePestDiseaseLogRequest
import app.verdant.android.data.model.UpdatePestDiseaseLogRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Pest / disease logs. */
@Singleton
class PestDiseaseRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun list(seasonId: Long? = null) = api.getPestDiseaseLogs(seasonId)
    suspend fun get(id: Long) = api.getPestDiseaseLog(id)
    suspend fun create(request: CreatePestDiseaseLogRequest) = api.createPestDiseaseLog(request)
    suspend fun update(id: Long, request: UpdatePestDiseaseLogRequest) = api.updatePestDiseaseLog(id, request)
    suspend fun delete(id: Long) = api.deletePestDiseaseLog(id)
}
