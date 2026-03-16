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
    suspend fun getAllBeds() = api.getAllBeds()
    suspend fun getBed(id: Long) = api.getBed(id)
    suspend fun createBed(gardenId: Long, request: CreateBedRequest) = api.createBed(gardenId, request)
    suspend fun updateBed(id: Long, request: UpdateBedRequest) = api.updateBed(id, request)
    suspend fun deleteBed(id: Long) = api.deleteBed(id)
    suspend fun getAllPlants(status: String? = null) = api.getAllPlants(status)
    suspend fun getPlants(bedId: Long) = api.getPlants(bedId)
    suspend fun getPlant(id: Long) = api.getPlant(id)
    suspend fun createPlant(bedId: Long, request: CreatePlantRequest) = api.createPlant(bedId, request)
    suspend fun createPlantWithoutBed(request: CreatePlantRequest) = api.createPlantWithoutBed(request)
    suspend fun batchSow(request: BatchSowRequest) = api.batchSow(request)
    suspend fun getPlantGroups(status: String, trayOnly: Boolean? = null) = api.getPlantGroups(status, trayOnly)
    suspend fun batchEvent(request: BatchEventRequest) = api.batchEvent(request)
    suspend fun updatePlant(id: Long, request: UpdatePlantRequest) = api.updatePlant(id, request)
    suspend fun deletePlant(id: Long) = api.deletePlant(id)
    suspend fun getMe() = api.getMe()
    suspend fun updateMe(request: UpdateUserRequest) = api.updateMe(request)
    suspend fun deleteMe() = api.deleteMe()
    suspend fun suggestLayout(request: SuggestLayoutRequest) = api.suggestLayout(request)
    suspend fun createGardenWithLayout(request: CreateGardenWithLayoutRequest) = api.createGardenWithLayout(request)

    // Species Plant Summary
    suspend fun getSpeciesPlantSummary() = api.getSpeciesPlantSummary()
    suspend fun getSpeciesLocations(speciesId: Long) = api.getSpeciesLocations(speciesId)

    // Plant Events
    suspend fun getPlantEvents(plantId: Long) = api.getPlantEvents(plantId)
    suspend fun addPlantEvent(plantId: Long, request: CreatePlantEventRequest) = api.addPlantEvent(plantId, request)
    suspend fun deletePlantEvent(plantId: Long, eventId: Long) = api.deletePlantEvent(plantId, eventId)

    // Identification
    suspend fun identifyPlant(request: IdentifyPlantRequest) = api.identifyPlant(request)
    suspend fun extractSpeciesInfo(request: ExtractSpeciesInfoRequest) = api.extractSpeciesInfo(request)

    // Stats
    suspend fun getHarvestStats() = api.getHarvestStats()

    // Species
    suspend fun getSpecies() = api.getSpecies()
    suspend fun createSpecies(request: CreateSpeciesRequest) = api.createSpecies(request)
    suspend fun updateSpecies(id: Long, request: UpdateSpeciesRequest) = api.updateSpecies(id, request)
    suspend fun deleteSpecies(id: Long) = api.deleteSpecies(id)
    suspend fun getSpeciesGroups() = api.getSpeciesGroups()
    suspend fun createSpeciesGroup(request: CreateSpeciesGroupRequest) = api.createSpeciesGroup(request)
    suspend fun deleteSpeciesGroup(id: Long) = api.deleteSpeciesGroup(id)
    suspend fun getSpeciesTags() = api.getSpeciesTags()
    suspend fun createSpeciesTag(request: CreateSpeciesTagRequest) = api.createSpeciesTag(request)
    suspend fun deleteSpeciesTag(id: Long) = api.deleteSpeciesTag(id)

    // Frequent Comments
    suspend fun getFrequentComments() = api.getFrequentComments()
    suspend fun recordComment(request: RecordCommentRequest) = api.recordComment(request)
    suspend fun deleteComment(id: Long) = api.deleteComment(id)

    // Seed Inventory
    suspend fun getSeedInventory(speciesId: Long? = null) = api.getSeedInventory(speciesId)
    suspend fun createSeedInventory(request: CreateSeedInventoryRequest) = api.createSeedInventory(request)
    suspend fun updateSeedInventory(id: Long, request: UpdateSeedInventoryRequest) = api.updateSeedInventory(id, request)
    suspend fun decrementSeedInventory(id: Long, request: DecrementSeedInventoryRequest) = api.decrementSeedInventory(id, request)
    suspend fun deleteSeedInventory(id: Long) = api.deleteSeedInventory(id)

    // Scheduled Tasks
    suspend fun getTasks() = api.getTasks()
    suspend fun getTask(id: Long) = api.getTask(id)
    suspend fun createTask(request: CreateScheduledTaskRequest) = api.createTask(request)
    suspend fun updateTask(id: Long, request: UpdateScheduledTaskRequest) = api.updateTask(id, request)
    suspend fun completeTaskPartially(id: Long, request: CompleteTaskPartiallyRequest) = api.completeTaskPartially(id, request)
    suspend fun deleteTask(id: Long) = api.deleteTask(id)
}
