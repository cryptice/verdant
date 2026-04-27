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
    suspend fun getTraySummary() = api.getTraySummary()
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
    suspend fun getSpeciesEventSummary(speciesId: Long, trayOnly: Boolean = false) =
        api.getSpeciesEventSummary(speciesId, trayOnly)
    suspend fun updateSpeciesEventDate(speciesId: Long, request: UpdateSpeciesEventDateRequest) =
        api.updateSpeciesEventDate(speciesId, request)
    suspend fun deleteSpeciesEvent(speciesId: Long, request: DeleteSpeciesEventRequest) =
        api.deleteSpeciesEvent(speciesId, request)

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

    // Seasons
    suspend fun getSeasons() = api.getSeasons()
    suspend fun createSeason(request: CreateSeasonRequest) = api.createSeason(request)
    suspend fun updateSeason(id: Long, request: Map<String, Any?>) = api.updateSeason(id, request)
    suspend fun deleteSeason(id: Long) = api.deleteSeason(id)

    // Supplies
    suspend fun getSupplyTypes() = api.getSupplyTypes()
    suspend fun getSupplyInventory() = api.getSupplyInventory()
    suspend fun decrementSupply(id: Long, quantity: Double) = api.decrementSupply(id, DecrementSupplyRequest(quantity))

    // Supply Applications
    suspend fun createSupplyApplication(req: CreateSupplyApplicationRequest) = api.createSupplyApplication(req)
    suspend fun listSupplyApplicationsByBed(bedId: Long, limit: Int = 20) = api.listSupplyApplicationsByBed(bedId, limit)
    suspend fun listSupplyApplicationsByGarden(gardenId: Long, limit: Int = 20) = api.listSupplyApplicationsByGarden(gardenId, limit)
    suspend fun getSupplyApplication(id: Long) = api.getSupplyApplication(id)

    // Plant proxy needed by ApplySupplyViewModel
    suspend fun getBedPlants(bedId: Long) = api.getPlants(bedId)

    // Customers
    suspend fun getCustomers() = api.getCustomers()
    suspend fun createCustomer(request: CreateCustomerRequest) = api.createCustomer(request)
    suspend fun updateCustomer(id: Long, request: UpdateCustomerRequest) = api.updateCustomer(id, request)
    suspend fun deleteCustomer(id: Long) = api.deleteCustomer(id)

    // Pest / Disease Logs
    suspend fun getPestDiseaseLogs(seasonId: Long? = null) = api.getPestDiseaseLogs(seasonId)
    suspend fun getPestDiseaseLog(id: Long) = api.getPestDiseaseLog(id)
    suspend fun createPestDiseaseLog(request: CreatePestDiseaseLogRequest) = api.createPestDiseaseLog(request)
    suspend fun updatePestDiseaseLog(id: Long, request: UpdatePestDiseaseLogRequest) = api.updatePestDiseaseLog(id, request)
    suspend fun deletePestDiseaseLog(id: Long) = api.deletePestDiseaseLog(id)

    // Variety Trials
    suspend fun getVarietyTrials(seasonId: Long? = null) = api.getVarietyTrials(seasonId)
    suspend fun getVarietyTrial(id: Long) = api.getVarietyTrial(id)
    suspend fun createVarietyTrial(request: CreateVarietyTrialRequest) = api.createVarietyTrial(request)
    suspend fun updateVarietyTrial(id: Long, request: UpdateVarietyTrialRequest) = api.updateVarietyTrial(id, request)
    suspend fun deleteVarietyTrial(id: Long) = api.deleteVarietyTrial(id)

    // Bouquet Recipes
    suspend fun getBouquetRecipes() = api.getBouquetRecipes()
    suspend fun getBouquetRecipe(id: Long) = api.getBouquetRecipe(id)
    suspend fun createBouquetRecipe(request: CreateBouquetRecipeRequest) = api.createBouquetRecipe(request)
    suspend fun updateBouquetRecipe(id: Long, request: UpdateBouquetRecipeRequest) = api.updateBouquetRecipe(id, request)
    suspend fun deleteBouquetRecipe(id: Long) = api.deleteBouquetRecipe(id)

    // Bouquets (instances)
    suspend fun getBouquets() = api.getBouquets()
    suspend fun getBouquet(id: Long) = api.getBouquet(id)
    suspend fun createBouquet(request: CreateBouquetRequest) = api.createBouquet(request)
    suspend fun updateBouquet(id: Long, request: UpdateBouquetRequest) = api.updateBouquet(id, request)
    suspend fun deleteBouquet(id: Long) = api.deleteBouquet(id)

    // Analytics
    suspend fun getSeasonSummaries() = api.getSeasonSummaries()
    suspend fun getSpeciesComparison(speciesId: Long) = api.getSpeciesComparison(speciesId)
    suspend fun getYieldPerBed(seasonId: Long? = null) = api.getYieldPerBed(seasonId)

    // Production Targets
    suspend fun getProductionTargets(seasonId: Long? = null) = api.getProductionTargets(seasonId)
    suspend fun createProductionTarget(request: Map<String, Any?>) = api.createProductionTarget(request)
    suspend fun getProductionForecast(id: Long) = api.getProductionForecast(id)
    suspend fun deleteProductionTarget(id: Long) = api.deleteProductionTarget(id)

    // Succession Schedules
    suspend fun getSuccessionSchedules(seasonId: Long? = null) = api.getSuccessionSchedules(seasonId)
    suspend fun createSuccessionSchedule(request: Map<String, Any?>) = api.createSuccessionSchedule(request)
    suspend fun generateSuccessionTasks(id: Long) = api.generateSuccessionTasks(id)
    suspend fun deleteSuccessionSchedule(id: Long) = api.deleteSuccessionSchedule(id)

    // Workflows
    suspend fun getSpeciesWorkflow(speciesId: Long) = api.getSpeciesWorkflow(speciesId)
    suspend fun getPlantWorkflowProgress(plantId: Long) = api.getPlantWorkflowProgress(plantId)
    suspend fun completeWorkflowStep(stepId: Long, request: CompleteWorkflowStepRequest) = api.completeWorkflowStep(stepId, request)
    suspend fun getPlantsAtStep(stepId: Long, speciesId: Long) = api.getPlantsAtStep(stepId, speciesId)

    // ── Tray locations ──
    suspend fun getTrayLocations() = api.getTrayLocations()
    suspend fun createTrayLocation(name: String) =
        api.createTrayLocation(CreateTrayLocationRequest(name))
    suspend fun updateTrayLocation(id: Long, name: String) =
        api.updateTrayLocation(id, UpdateTrayLocationRequest(name))
    suspend fun deleteTrayLocation(id: Long) = api.deleteTrayLocation(id)
    suspend fun waterTrayLocation(id: Long) = api.waterTrayLocation(id)
    suspend fun noteTrayLocation(id: Long, text: String) =
        api.noteTrayLocation(id, BulkLocationNoteRequest(text))
    suspend fun moveTrayLocation(id: Long, request: MoveTrayLocationRequest) =
        api.moveTrayLocation(id, request)
    suspend fun moveTrayPlants(request: MoveTrayPlantsRequest) =
        api.moveTrayPlants(request)
    suspend fun weedBed(bedId: Long) = api.weedBed(bedId)
}
