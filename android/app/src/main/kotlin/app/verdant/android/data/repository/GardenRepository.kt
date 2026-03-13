package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GardenRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun getDashboard() = api.getDashboard()
    suspend fun getGardens() = api.getGardens()
    suspend fun getGarden(id: Long) = api.getGarden(id)
    suspend fun createGarden(request: CreateGardenRequest) = api.createGarden(request)
    suspend fun updateGarden(id: Long, request: UpdateGardenRequest) = api.updateGarden(id, request)
    suspend fun deleteGarden(id: Long) = api.deleteGarden(id)
    suspend fun getBeds(gardenId: Long) = api.getBeds(gardenId)
    suspend fun getBed(id: Long) = api.getBed(id)
    suspend fun createBed(gardenId: Long, request: CreateBedRequest) = api.createBed(gardenId, request)
    suspend fun updateBed(id: Long, request: UpdateBedRequest) = api.updateBed(id, request)
    suspend fun deleteBed(id: Long) = api.deleteBed(id)
    suspend fun getPlants(bedId: Long) = api.getPlants(bedId)
    suspend fun getPlant(id: Long) = api.getPlant(id)
    suspend fun createPlant(bedId: Long, request: CreatePlantRequest) = api.createPlant(bedId, request)
    suspend fun updatePlant(id: Long, request: UpdatePlantRequest) = api.updatePlant(id, request)
    suspend fun deletePlant(id: Long) = api.deletePlant(id)
    suspend fun getMe() = api.getMe()
    suspend fun updateMe(request: UpdateUserRequest) = api.updateMe(request)
    suspend fun deleteMe() = api.deleteMe()
    suspend fun suggestLayout(request: SuggestLayoutRequest) = api.suggestLayout(request)
    suspend fun createGardenWithLayout(request: CreateGardenWithLayoutRequest) = api.createGardenWithLayout(request)

    // Plant Events
    suspend fun getPlantEvents(plantId: Long) = api.getPlantEvents(plantId)
    suspend fun addPlantEvent(plantId: Long, request: CreatePlantEventRequest) = api.addPlantEvent(plantId, request)
    suspend fun deletePlantEvent(plantId: Long, eventId: Long) = api.deletePlantEvent(plantId, eventId)

    // Identification
    suspend fun identifyPlant(request: IdentifyPlantRequest) = api.identifyPlant(request)

    // Stats
    suspend fun getHarvestStats() = api.getHarvestStats()
}
