package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateSupplyApplicationRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Supply applications (fertilize / soil / etc. recorded against beds and plants). */
@Singleton
class SupplyApplicationRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun create(request: CreateSupplyApplicationRequest) = api.createSupplyApplication(request)
    suspend fun listByBed(bedId: Long, limit: Int = 20) = api.listSupplyApplicationsByBed(bedId, limit)
    suspend fun listByGarden(gardenId: Long, limit: Int = 20) = api.listSupplyApplicationsByGarden(gardenId, limit)
    suspend fun get(id: Long) = api.getSupplyApplication(id)
}
