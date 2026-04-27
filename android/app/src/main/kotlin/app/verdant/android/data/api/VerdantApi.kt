package app.verdant.android.data.api

import app.verdant.android.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface VerdantApi {
    @POST("api/auth/google")
    suspend fun googleAuth(@Body request: GoogleAuthRequest): AuthResponse

    @GET("api/dashboard")
    suspend fun getDashboard(): DashboardResponse

    @GET("api/users/me")
    suspend fun getMe(): UserResponse

    @PUT("api/users/me")
    suspend fun updateMe(@Body request: UpdateUserRequest): UserResponse

    @DELETE("api/users/me")
    suspend fun deleteMe()

    @GET("api/gardens")
    suspend fun getGardens(): List<GardenResponse>

    @GET("api/gardens/{id}")
    suspend fun getGarden(@Path("id") id: Long): GardenResponse

    @POST("api/gardens")
    suspend fun createGarden(@Body request: CreateGardenRequest): GardenResponse

    @PUT("api/gardens/{id}")
    suspend fun updateGarden(@Path("id") id: Long, @Body request: UpdateGardenRequest): GardenResponse

    @DELETE("api/gardens/{id}")
    suspend fun deleteGarden(@Path("id") id: Long)

    @GET("api/gardens/{gardenId}/beds")
    suspend fun getBeds(@Path("gardenId") gardenId: Long): List<BedResponse>

    @GET("api/beds")
    suspend fun getAllBeds(): List<BedWithGardenResponse>

    @GET("api/beds/{id}")
    suspend fun getBed(@Path("id") id: Long): BedResponse

    @POST("api/gardens/{gardenId}/beds")
    suspend fun createBed(@Path("gardenId") gardenId: Long, @Body request: CreateBedRequest): BedResponse

    @PUT("api/beds/{id}")
    suspend fun updateBed(@Path("id") id: Long, @Body request: UpdateBedRequest): BedResponse

    @DELETE("api/beds/{id}")
    suspend fun deleteBed(@Path("id") id: Long)

    @GET("api/plants")
    suspend fun getAllPlants(@Query("status") status: String? = null): List<PlantResponse>

    @GET("api/beds/{bedId}/plants")
    suspend fun getPlants(@Path("bedId") bedId: Long): List<PlantResponse>

    @GET("api/plants/{id}")
    suspend fun getPlant(@Path("id") id: Long): PlantResponse

    @POST("api/beds/{bedId}/plants")
    suspend fun createPlant(@Path("bedId") bedId: Long, @Body request: CreatePlantRequest): PlantResponse

    @POST("api/plants")
    suspend fun createPlantWithoutBed(@Body request: CreatePlantRequest): PlantResponse

    @POST("api/plants/batch-sow")
    suspend fun batchSow(@Body request: BatchSowRequest): BatchSowResponse

    @GET("api/plants/tray-summary")
    suspend fun getTraySummary(): List<TraySummaryEntry>

    @GET("api/plants/groups")
    suspend fun getPlantGroups(@Query("status") status: String, @Query("trayOnly") trayOnly: Boolean? = null): List<PlantGroupResponse>

    @POST("api/plants/batch-event")
    suspend fun batchEvent(@Body request: BatchEventRequest): BatchEventResponse

    @POST("api/plants/move-tray")
    suspend fun moveTrayPlants(@Body request: MoveTrayPlantsRequest): BulkLocationActionResponse

    @POST("api/beds/{id}/weed")
    suspend fun weedBed(@Path("id") id: Long): BulkLocationActionResponse

    @POST("api/beds/{id}/water")
    suspend fun waterBed(@Path("id") id: Long): BulkLocationActionResponse

    @PUT("api/plants/{id}")
    suspend fun updatePlant(@Path("id") id: Long, @Body request: UpdatePlantRequest): PlantResponse

    @DELETE("api/plants/{id}")
    suspend fun deletePlant(@Path("id") id: Long)

    @POST("api/gardens/suggest-layout")
    suspend fun suggestLayout(@Body request: SuggestLayoutRequest): SuggestLayoutResponse

    @POST("api/gardens/with-layout")
    suspend fun createGardenWithLayout(@Body request: CreateGardenWithLayoutRequest): GardenWithBedsResponse

    // ── Species Plant Summary ──

    @GET("api/plants/species-summary")
    suspend fun getSpeciesPlantSummary(): List<SpeciesPlantSummary>

    @GET("api/plants/species/{speciesId}/locations")
    suspend fun getSpeciesLocations(@Path("speciesId") speciesId: Long): List<PlantLocationGroup>

    @GET("api/plants/species/{speciesId}/events")
    suspend fun getSpeciesEventSummary(
        @Path("speciesId") speciesId: Long,
        @retrofit2.http.Query("trayOnly") trayOnly: Boolean = false,
    ): List<SpeciesEventSummaryEntry>

    @retrofit2.http.PATCH("api/plants/species/{speciesId}/events/date")
    suspend fun updateSpeciesEventDate(
        @Path("speciesId") speciesId: Long,
        @retrofit2.http.Body request: UpdateSpeciesEventDateRequest,
    ): UpdateSpeciesEventDateResponse

    @POST("api/plants/species/{speciesId}/events/delete")
    suspend fun deleteSpeciesEvent(
        @Path("speciesId") speciesId: Long,
        @Body request: DeleteSpeciesEventRequest,
    ): DeleteSpeciesEventResponse

    // ── Plant Events ──

    @GET("api/plants/{id}/events")
    suspend fun getPlantEvents(@Path("id") plantId: Long): List<PlantEventResponse>

    @POST("api/plants/{id}/events")
    suspend fun addPlantEvent(@Path("id") plantId: Long, @Body request: CreatePlantEventRequest): PlantEventResponse

    @DELETE("api/plants/{id}/events/{eventId}")
    suspend fun deletePlantEvent(@Path("id") plantId: Long, @Path("eventId") eventId: Long)

    // ── Identification ──

    @POST("api/plants/identify")
    suspend fun identifyPlant(@Body request: IdentifyPlantRequest): List<PlantSuggestion>

    @POST("api/species/extract-info")
    suspend fun extractSpeciesInfo(@Body request: ExtractSpeciesInfoRequest): ExtractedSpeciesInfo

    // ── Stats ──

    @GET("api/stats/harvests")
    suspend fun getHarvestStats(): List<HarvestStatRow>

    // ── Species ──

    @GET("api/species")
    suspend fun getSpecies(): List<SpeciesResponse>

    @POST("api/species")
    suspend fun createSpecies(@Body request: CreateSpeciesRequest): SpeciesResponse

    @PUT("api/species/{id}")
    suspend fun updateSpecies(@Path("id") id: Long, @Body request: UpdateSpeciesRequest): SpeciesResponse

    @DELETE("api/species/{id}")
    suspend fun deleteSpecies(@Path("id") id: Long)

    @GET("api/species/groups")
    suspend fun getSpeciesGroups(): List<SpeciesGroupResponse>

    @POST("api/species/groups")
    suspend fun createSpeciesGroup(@Body request: CreateSpeciesGroupRequest): SpeciesGroupResponse

    @DELETE("api/species/groups/{id}")
    suspend fun deleteSpeciesGroup(@Path("id") id: Long)

    @GET("api/species/tags")
    suspend fun getSpeciesTags(): List<SpeciesTagResponse>

    @POST("api/species/tags")
    suspend fun createSpeciesTag(@Body request: CreateSpeciesTagRequest): SpeciesTagResponse

    @DELETE("api/species/tags/{id}")
    suspend fun deleteSpeciesTag(@Path("id") id: Long)

    // ── Frequent Comments ──

    @GET("api/comments")
    suspend fun getFrequentComments(): List<FrequentCommentResponse>

    @POST("api/comments")
    suspend fun recordComment(@Body request: RecordCommentRequest): FrequentCommentResponse

    @DELETE("api/comments/{id}")
    suspend fun deleteComment(@Path("id") id: Long)

    // ── Seed Inventory ──

    @GET("api/seed-stock")
    suspend fun getSeedInventory(@Query("speciesId") speciesId: Long? = null): List<SeedInventoryResponse>

    @POST("api/seed-stock")
    suspend fun createSeedInventory(@Body request: CreateSeedInventoryRequest): SeedInventoryResponse

    @PUT("api/seed-stock/{id}")
    suspend fun updateSeedInventory(@Path("id") id: Long, @Body request: UpdateSeedInventoryRequest): SeedInventoryResponse

    @POST("api/seed-stock/{id}/decrement")
    suspend fun decrementSeedInventory(@Path("id") id: Long, @Body request: DecrementSeedInventoryRequest): SeedInventoryResponse

    @DELETE("api/seed-stock/{id}")
    suspend fun deleteSeedInventory(@Path("id") id: Long)

    // ── Scheduled Tasks ──

    @GET("api/tasks")
    suspend fun getTasks(): List<ScheduledTaskResponse>

    @GET("api/tasks/{id}")
    suspend fun getTask(@Path("id") id: Long): ScheduledTaskResponse

    @POST("api/tasks")
    suspend fun createTask(@Body request: CreateScheduledTaskRequest): ScheduledTaskResponse

    @PUT("api/tasks/{id}")
    suspend fun updateTask(@Path("id") id: Long, @Body request: UpdateScheduledTaskRequest): ScheduledTaskResponse

    @POST("api/tasks/{id}/complete")
    suspend fun completeTaskPartially(@Path("id") id: Long, @Body request: CompleteTaskPartiallyRequest): ScheduledTaskResponse

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Long)

    // ── Seasons ──

    @GET("api/seasons")
    suspend fun getSeasons(): List<SeasonResponse>

    @POST("api/seasons")
    suspend fun createSeason(@Body request: CreateSeasonRequest): SeasonResponse

    @PUT("api/seasons/{id}")
    suspend fun updateSeason(@Path("id") id: Long, @Body request: Map<String, Any?>): SeasonResponse

    @DELETE("api/seasons/{id}")
    suspend fun deleteSeason(@Path("id") id: Long)

    // ── Customers ──

    @GET("api/customers")
    suspend fun getCustomers(): List<CustomerResponse>

    @POST("api/customers")
    suspend fun createCustomer(@Body request: CreateCustomerRequest): CustomerResponse

    @PUT("api/customers/{id}")
    suspend fun updateCustomer(@Path("id") id: Long, @Body request: UpdateCustomerRequest): CustomerResponse

    @DELETE("api/customers/{id}")
    suspend fun deleteCustomer(@Path("id") id: Long)

    // ── Pest/Disease Logs ──

    @GET("api/pest-disease-logs")
    suspend fun getPestDiseaseLogs(@Query("seasonId") seasonId: Long? = null): List<PestDiseaseLogResponse>

    @GET("api/pest-disease-logs/{id}")
    suspend fun getPestDiseaseLog(@Path("id") id: Long): PestDiseaseLogResponse

    @POST("api/pest-disease-logs")
    suspend fun createPestDiseaseLog(@Body request: CreatePestDiseaseLogRequest): PestDiseaseLogResponse

    @PUT("api/pest-disease-logs/{id}")
    suspend fun updatePestDiseaseLog(@Path("id") id: Long, @Body request: UpdatePestDiseaseLogRequest): PestDiseaseLogResponse

    @DELETE("api/pest-disease-logs/{id}")
    suspend fun deletePestDiseaseLog(@Path("id") id: Long)

    // ── Variety Trials ──

    @GET("api/variety-trials")
    suspend fun getVarietyTrials(@Query("seasonId") seasonId: Long? = null): List<VarietyTrialResponse>

    @GET("api/variety-trials/{id}")
    suspend fun getVarietyTrial(@Path("id") id: Long): VarietyTrialResponse

    @POST("api/variety-trials")
    suspend fun createVarietyTrial(@Body request: CreateVarietyTrialRequest): VarietyTrialResponse

    @PUT("api/variety-trials/{id}")
    suspend fun updateVarietyTrial(@Path("id") id: Long, @Body request: UpdateVarietyTrialRequest): VarietyTrialResponse

    @DELETE("api/variety-trials/{id}")
    suspend fun deleteVarietyTrial(@Path("id") id: Long)

    // ── Bouquet Recipes ──

    @GET("api/bouquet-recipes")
    suspend fun getBouquetRecipes(): List<BouquetRecipeResponse>

    @GET("api/bouquet-recipes/{id}")
    suspend fun getBouquetRecipe(@Path("id") id: Long): BouquetRecipeResponse

    @POST("api/bouquet-recipes")
    suspend fun createBouquetRecipe(@Body request: CreateBouquetRecipeRequest): BouquetRecipeResponse

    @PUT("api/bouquet-recipes/{id}")
    suspend fun updateBouquetRecipe(@Path("id") id: Long, @Body request: UpdateBouquetRecipeRequest): BouquetRecipeResponse

    @DELETE("api/bouquet-recipes/{id}")
    suspend fun deleteBouquetRecipe(@Path("id") id: Long)

    // ── Bouquets (instances) ──

    @GET("api/bouquets")
    suspend fun getBouquets(): List<BouquetResponse>

    @GET("api/bouquets/{id}")
    suspend fun getBouquet(@Path("id") id: Long): BouquetResponse

    @POST("api/bouquets")
    suspend fun createBouquet(@Body request: CreateBouquetRequest): BouquetResponse

    @PUT("api/bouquets/{id}")
    suspend fun updateBouquet(@Path("id") id: Long, @Body request: UpdateBouquetRequest): BouquetResponse

    @DELETE("api/bouquets/{id}")
    suspend fun deleteBouquet(@Path("id") id: Long)

    // ── Analytics ──

    @GET("api/analytics/seasons")
    suspend fun getSeasonSummaries(): List<SeasonSummaryResponse>

    @GET("api/analytics/species/{speciesId}/compare")
    suspend fun getSpeciesComparison(@Path("speciesId") speciesId: Long): SpeciesComparisonResponse

    @GET("api/analytics/yield-per-bed")
    suspend fun getYieldPerBed(@Query("seasonId") seasonId: Long? = null): List<YieldPerBedResponse>

    // ── Production Targets ──

    @GET("api/production-targets")
    suspend fun getProductionTargets(@Query("seasonId") seasonId: Long? = null): List<ProductionTargetResponse>

    @POST("api/production-targets")
    suspend fun createProductionTarget(@Body request: Map<String, Any?>): ProductionTargetResponse

    @GET("api/production-targets/{id}/forecast")
    suspend fun getProductionForecast(@Path("id") id: Long): ProductionForecastResponse

    @DELETE("api/production-targets/{id}")
    suspend fun deleteProductionTarget(@Path("id") id: Long)

    // ── Succession Schedules ──

    @GET("api/succession-schedules")
    suspend fun getSuccessionSchedules(@Query("seasonId") seasonId: Long? = null): List<SuccessionScheduleResponse>

    @POST("api/succession-schedules")
    suspend fun createSuccessionSchedule(@Body request: Map<String, Any?>): SuccessionScheduleResponse

    @POST("api/succession-schedules/{id}/generate-tasks")
    suspend fun generateSuccessionTasks(@Path("id") id: Long): List<Long>

    @DELETE("api/succession-schedules/{id}")
    suspend fun deleteSuccessionSchedule(@Path("id") id: Long)

    // ── Supplies ──

    @GET("api/supplies/types")
    suspend fun getSupplyTypes(): List<SupplyTypeResponse>

    @GET("api/supplies")
    suspend fun getSupplyInventory(): List<SupplyInventoryResponse>

    @POST("api/supplies/{id}/decrement")
    suspend fun decrementSupply(@Path("id") id: Long, @Body request: DecrementSupplyRequest): Response<Unit>

    // ── Supply Applications ──

    @POST("api/supply-applications")
    suspend fun createSupplyApplication(@Body req: CreateSupplyApplicationRequest): SupplyApplicationResponse

    @GET("api/supply-applications/bed/{bedId}")
    suspend fun listSupplyApplicationsByBed(@Path("bedId") bedId: Long, @Query("limit") limit: Int = 20): List<SupplyApplicationResponse>

    @GET("api/supply-applications/garden/{gardenId}")
    suspend fun listSupplyApplicationsByGarden(@Path("gardenId") gardenId: Long, @Query("limit") limit: Int = 20): List<SupplyApplicationResponse>

    @GET("api/supply-applications/{id}")
    suspend fun getSupplyApplication(@Path("id") id: Long): SupplyApplicationResponse

    // ── Workflows ──

    @GET("api/workflows/species/{speciesId}")
    suspend fun getSpeciesWorkflow(@Path("speciesId") speciesId: Long): SpeciesWorkflowResponse

    @GET("api/workflows/plants/{plantId}")
    suspend fun getPlantWorkflowProgress(@Path("plantId") plantId: Long): PlantWorkflowProgressResponse

    @POST("api/workflows/species-steps/{stepId}/complete")
    suspend fun completeWorkflowStep(@Path("stepId") stepId: Long, @Body request: CompleteWorkflowStepRequest): Response<Unit>

    @GET("api/workflows/species-steps/{stepId}/plants")
    suspend fun getPlantsAtStep(@Path("stepId") stepId: Long, @Query("speciesId") speciesId: Long): List<Long>

    // ── Tray locations ──

    @GET("api/tray-locations")
    suspend fun getTrayLocations(): List<TrayLocationResponse>

    @POST("api/tray-locations")
    suspend fun createTrayLocation(@Body request: CreateTrayLocationRequest): TrayLocationResponse

    @retrofit2.http.PATCH("api/tray-locations/{id}")
    suspend fun updateTrayLocation(
        @Path("id") id: Long,
        @Body request: UpdateTrayLocationRequest,
    ): TrayLocationResponse

    @retrofit2.http.DELETE("api/tray-locations/{id}")
    suspend fun deleteTrayLocation(@Path("id") id: Long): Response<Unit>

    @POST("api/tray-locations/{id}/water")
    suspend fun waterTrayLocation(@Path("id") id: Long): BulkLocationActionResponse

    @POST("api/tray-locations/{id}/note")
    suspend fun noteTrayLocation(
        @Path("id") id: Long,
        @Body request: BulkLocationNoteRequest,
    ): BulkLocationActionResponse

    @POST("api/tray-locations/{id}/move")
    suspend fun moveTrayLocation(
        @Path("id") id: Long,
        @Body request: MoveTrayLocationRequest,
    ): BulkLocationActionResponse
}
