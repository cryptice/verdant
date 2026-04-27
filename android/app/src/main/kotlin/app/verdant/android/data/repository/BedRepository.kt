package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateBedRequest
import app.verdant.android.data.model.UpdateBedRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Bed CRUD + bed-level actions (water, weed, events). */
@Singleton
class BedRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun list(gardenId: Long) = api.getBeds(gardenId)
    suspend fun listAll() = api.getAllBeds()
    suspend fun get(id: Long) = api.getBed(id)
    suspend fun create(gardenId: Long, request: CreateBedRequest) = api.createBed(gardenId, request)
    suspend fun update(id: Long, request: UpdateBedRequest) = api.updateBed(id, request)
    suspend fun delete(id: Long) = api.deleteBed(id)
    suspend fun water(id: Long) = api.waterBed(id)
    suspend fun weed(id: Long) = api.weedBed(id)
    suspend fun events(id: Long, limit: Int = 50) = api.getBedEvents(id, limit)
}
