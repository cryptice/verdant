package app.verdant.android.data.api

import app.verdant.android.data.model.*
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

    @GET("api/seed-inventory")
    suspend fun getSeedInventory(@Query("speciesId") speciesId: Long? = null): List<SeedInventoryResponse>

    @POST("api/seed-inventory")
    suspend fun createSeedInventory(@Body request: CreateSeedInventoryRequest): SeedInventoryResponse

    @PUT("api/seed-inventory/{id}")
    suspend fun updateSeedInventory(@Path("id") id: Long, @Body request: UpdateSeedInventoryRequest): SeedInventoryResponse

    @POST("api/seed-inventory/{id}/decrement")
    suspend fun decrementSeedInventory(@Path("id") id: Long, @Body request: DecrementSeedInventoryRequest): SeedInventoryResponse

    @DELETE("api/seed-inventory/{id}")
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
}
