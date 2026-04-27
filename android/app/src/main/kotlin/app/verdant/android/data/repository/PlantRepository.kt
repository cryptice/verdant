package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.BatchEventRequest
import app.verdant.android.data.model.BatchSowRequest
import app.verdant.android.data.model.CreatePlantEventRequest
import app.verdant.android.data.model.CreatePlantRequest
import app.verdant.android.data.model.DeleteSpeciesEventRequest
import app.verdant.android.data.model.ExtractSpeciesInfoRequest
import app.verdant.android.data.model.IdentifyPlantRequest
import app.verdant.android.data.model.MoveTrayPlantsRequest
import app.verdant.android.data.model.UpdatePlantRequest
import app.verdant.android.data.model.UpdateSpeciesEventDateRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Plant CRUD, plant events, batch sow / event, species summary, identification. */
@Singleton
class PlantRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun listAll(status: String? = null) = api.getAllPlants(status)
    suspend fun listForBed(bedId: Long) = api.getPlants(bedId)
    suspend fun get(id: Long) = api.getPlant(id)
    suspend fun create(bedId: Long, request: CreatePlantRequest) = api.createPlant(bedId, request)
    suspend fun createWithoutBed(request: CreatePlantRequest) = api.createPlantWithoutBed(request)
    suspend fun update(id: Long, request: UpdatePlantRequest) = api.updatePlant(id, request)
    suspend fun delete(id: Long) = api.deletePlant(id)
    suspend fun batchSow(request: BatchSowRequest) = api.batchSow(request)
    suspend fun batchEvent(request: BatchEventRequest) = api.batchEvent(request)
    suspend fun groupedByStatus(status: String, trayOnly: Boolean? = null) = api.getPlantGroups(status, trayOnly)
    suspend fun traySummary() = api.getTraySummary()
    suspend fun moveTrayPlants(request: MoveTrayPlantsRequest) = api.moveTrayPlants(request)

    // Events
    suspend fun events(plantId: Long) = api.getPlantEvents(plantId)
    suspend fun addEvent(plantId: Long, request: CreatePlantEventRequest) = api.addPlantEvent(plantId, request)
    suspend fun deleteEvent(plantId: Long, eventId: Long) = api.deletePlantEvent(plantId, eventId)

    // Species-scoped queries
    suspend fun speciesSummary() = api.getSpeciesPlantSummary()
    suspend fun speciesLocations(speciesId: Long) = api.getSpeciesLocations(speciesId)
    suspend fun speciesEvents(speciesId: Long, trayOnly: Boolean = false) =
        api.getSpeciesEventSummary(speciesId, trayOnly)
    suspend fun updateSpeciesEventDate(speciesId: Long, request: UpdateSpeciesEventDateRequest) =
        api.updateSpeciesEventDate(speciesId, request)
    suspend fun deleteSpeciesEvent(speciesId: Long, request: DeleteSpeciesEventRequest) =
        api.deleteSpeciesEvent(speciesId, request)

    // Identification
    suspend fun identify(request: IdentifyPlantRequest) = api.identifyPlant(request)
    suspend fun extractSpeciesInfo(request: ExtractSpeciesInfoRequest) = api.extractSpeciesInfo(request)
}
