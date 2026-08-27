package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateGardenAreaEventRequest
import app.verdant.android.data.model.CreateGardenAreaPhotoRequest
import app.verdant.android.data.model.CreateGardenAreaRequest
import app.verdant.android.data.model.GardenAreaEventResponse
import app.verdant.android.data.model.GardenAreaPhotoResponse
import app.verdant.android.data.model.GardenAreaResponse
import app.verdant.android.data.model.UpdateGardenAreaRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Garden area CRUD, its event log, and its photos.
 *
 * An interface rather than a concrete class so ViewModel tests can supply a
 * hand-written fake — this module has no mocking library.
 */
interface GardenAreaRepository {
    suspend fun list(gardenId: Long): List<GardenAreaResponse>
    suspend fun get(id: Long): GardenAreaResponse
    suspend fun create(gardenId: Long, request: CreateGardenAreaRequest): GardenAreaResponse
    suspend fun update(id: Long, request: UpdateGardenAreaRequest): GardenAreaResponse
    suspend fun delete(id: Long)
    suspend fun events(id: Long, limit: Int = 50): List<GardenAreaEventResponse>
    suspend fun logEvent(id: Long, request: CreateGardenAreaEventRequest): GardenAreaEventResponse
    suspend fun photos(id: Long): List<GardenAreaPhotoResponse>
    suspend fun addPhoto(id: Long, request: CreateGardenAreaPhotoRequest): GardenAreaPhotoResponse
    suspend fun deletePhoto(id: Long, photoId: Long)
}

@Singleton
class DefaultGardenAreaRepository @Inject constructor(
    private val api: VerdantApi,
) : GardenAreaRepository {
    override suspend fun list(gardenId: Long) = api.getGardenAreas(gardenId)
    override suspend fun get(id: Long) = api.getGardenArea(id)
    override suspend fun create(gardenId: Long, request: CreateGardenAreaRequest) =
        api.createGardenArea(gardenId, request)
    override suspend fun update(id: Long, request: UpdateGardenAreaRequest) =
        api.updateGardenArea(id, request)
    override suspend fun delete(id: Long) = api.deleteGardenArea(id)
    override suspend fun events(id: Long, limit: Int) = api.getGardenAreaEvents(id, limit)
    override suspend fun logEvent(id: Long, request: CreateGardenAreaEventRequest) =
        api.logGardenAreaEvent(id, request)
    override suspend fun photos(id: Long) = api.getGardenAreaPhotos(id)
    override suspend fun addPhoto(id: Long, request: CreateGardenAreaPhotoRequest) =
        api.addGardenAreaPhoto(id, request)
    override suspend fun deletePhoto(id: Long, photoId: Long) =
        api.deleteGardenAreaPhoto(id, photoId)
}
