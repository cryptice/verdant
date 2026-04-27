package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateGardenRequest
import app.verdant.android.data.model.CreateGardenWithLayoutRequest
import app.verdant.android.data.model.SuggestLayoutRequest
import app.verdant.android.data.model.UpdateGardenRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Garden CRUD + layout. */
@Singleton
class GardenApiRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun list() = api.getGardens()
    suspend fun get(id: Long) = api.getGarden(id)
    suspend fun create(request: CreateGardenRequest) = api.createGarden(request)
    suspend fun update(id: Long, request: UpdateGardenRequest) = api.updateGarden(id, request)
    suspend fun delete(id: Long) = api.deleteGarden(id)
    suspend fun suggestLayout(request: SuggestLayoutRequest) = api.suggestLayout(request)
    suspend fun createWithLayout(request: CreateGardenWithLayoutRequest) = api.createGardenWithLayout(request)
}
