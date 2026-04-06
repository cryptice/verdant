package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

data class GoogleAuthRequest(@SerializedName("idToken") val idToken: String)
data class AuthResponse(@SerializedName("token") val token: String, @SerializedName("user") val user: UserResponse)

data class UserResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("email") val email: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("avatarUrl") val avatarUrl: String?,
    @SerializedName("role") val role: String,
    @SerializedName("language") val language: String = "sv",
    @SerializedName("onboarding") val onboarding: String? = null,
    @SerializedName("createdAt") val createdAt: String
)

data class UpdateUserRequest(
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("language") val language: String? = null,
)

data class GardenResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("emoji") val emoji: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("address") val address: String?,
    @SerializedName("boundaryJson") val boundaryJson: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class CreateGardenRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("emoji") val emoji: String? = "\uD83C\uDF31",
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("boundaryJson") val boundaryJson: String? = null
)

data class UpdateGardenRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("emoji") val emoji: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("boundaryJson") val boundaryJson: String? = null
)

data class BedResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("gardenId") val gardenId: Long,
    @SerializedName("boundaryJson") val boundaryJson: String?,
    @SerializedName("lengthMeters") val lengthMeters: Double? = null,
    @SerializedName("widthMeters") val widthMeters: Double? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class CreateBedRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("boundaryJson") val boundaryJson: String? = null
)

data class UpdateBedRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("boundaryJson") val boundaryJson: String? = null
)

data class BedWithGardenResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("gardenId") val gardenId: Long,
    @SerializedName("gardenName") val gardenName: String,
    @SerializedName("boundaryJson") val boundaryJson: String?,
)

data class PlantResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("speciesId") val speciesId: Long?,
    @SerializedName("speciesName") val speciesName: String?,
    @SerializedName("plantedDate") val plantedDate: String?,
    @SerializedName("status") val status: String,
    @SerializedName("seedCount") val seedCount: Int?,
    @SerializedName("survivingCount") val survivingCount: Int?,
    @SerializedName("bedId") val bedId: Long?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class CreatePlantRequest(
    @SerializedName("name") val name: String,
    @SerializedName("speciesId") val speciesId: Long? = null,
    @SerializedName("plantedDate") val plantedDate: String? = null,
    @SerializedName("status") val status: String = "SEEDED",
    @SerializedName("seedCount") val seedCount: Int? = null,
    @SerializedName("survivingCount") val survivingCount: Int? = null,
)

data class BatchSowRequest(
    @SerializedName("bedId") val bedId: Long? = null,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("seedCount") val seedCount: Int,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("imageBase64") val imageBase64: String? = null,
)

data class BatchSowResponse(
    @SerializedName("plantIds") val plantIds: List<Long>,
    @SerializedName("count") val count: Int,
)

data class TraySummaryEntry(
    @SerializedName("speciesName") val speciesName: String,
    @SerializedName("status") val status: String,
    @SerializedName("count") val count: Int,
)

data class PlantGroupResponse(
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String?,
    @SerializedName("bedId") val bedId: Long?,
    @SerializedName("bedName") val bedName: String?,
    @SerializedName("gardenName") val gardenName: String?,
    @SerializedName("plantedDate") val plantedDate: String?,
    @SerializedName("status") val status: String,
    @SerializedName("count") val count: Int,
)

data class BatchEventRequest(
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("bedId") val bedId: Long? = null,
    @SerializedName("plantedDate") val plantedDate: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("eventType") val eventType: String,
    @SerializedName("count") val count: Int,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("imageBase64") val imageBase64: String? = null,
    @SerializedName("targetBedId") val targetBedId: Long? = null,
)

data class BatchEventResponse(
    @SerializedName("updatedCount") val updatedCount: Int,
)

data class UpdatePlantRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("speciesId") val speciesId: Long? = null,
    @SerializedName("plantedDate") val plantedDate: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("seedCount") val seedCount: Int? = null,
    @SerializedName("survivingCount") val survivingCount: Int? = null,
)

data class DashboardResponse(
    @SerializedName("user") val user: UserResponse,
    @SerializedName("gardens") val gardens: List<GardenSummary>,
    @SerializedName("stats") val stats: DashboardStats
)

data class GardenSummary(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("emoji") val emoji: String?,
    @SerializedName("bedCount") val bedCount: Int,
    @SerializedName("plantCount") val plantCount: Int
)

data class DashboardStats(
    @SerializedName("totalGardens") val totalGardens: Int,
    @SerializedName("totalBeds") val totalBeds: Int,
    @SerializedName("totalPlants") val totalPlants: Int
)

data class LatLng(@SerializedName("lat") val lat: Double, @SerializedName("lng") val lng: Double)

data class SuggestLayoutRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("address") val address: String? = null
)

data class SuggestedBed(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("boundary") val boundary: List<LatLng>
)

data class SuggestLayoutResponse(
    @SerializedName("gardenName") val gardenName: String,
    @SerializedName("boundary") val boundary: List<LatLng>,
    @SerializedName("beds") val beds: List<SuggestedBed>
)

data class CreateGardenWithLayoutRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("emoji") val emoji: String? = "\uD83C\uDF31",
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("boundaryJson") val boundaryJson: String? = null,
    @SerializedName("beds") val beds: List<BedLayoutItem> = emptyList()
)

data class BedLayoutItem(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("boundaryJson") val boundaryJson: String? = null
)

data class GardenWithBedsResponse(
    @SerializedName("garden") val garden: GardenResponse,
    @SerializedName("beds") val beds: List<BedResponse>
)

// ── Plant Events ──

data class PlantEventResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("plantId") val plantId: Long,
    @SerializedName("eventType") val eventType: String,
    @SerializedName("eventDate") val eventDate: String,
    @SerializedName("plantCount") val plantCount: Int?,
    @SerializedName("weightGrams") val weightGrams: Double?,
    @SerializedName("quantity") val quantity: Int?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("aiSuggestions") val aiSuggestions: String?,
    @SerializedName("stemCount") val stemCount: Int? = null,
    @SerializedName("stemLengthCm") val stemLengthCm: Int? = null,
    @SerializedName("qualityGrade") val qualityGrade: String? = null,
    @SerializedName("vaseLifeDays") val vaseLifeDays: Int? = null,
    @SerializedName("harvestDestinationId") val harvestDestinationId: Long? = null,
    @SerializedName("customerName") val customerName: String? = null,
    @SerializedName("createdAt") val createdAt: String,
)

data class CreatePlantEventRequest(
    @SerializedName("eventType") val eventType: String,
    @SerializedName("eventDate") val eventDate: String? = null,
    @SerializedName("plantCount") val plantCount: Int? = null,
    @SerializedName("weightGrams") val weightGrams: Double? = null,
    @SerializedName("quantity") val quantity: Int? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("imageBase64") val imageBase64: String? = null,
    @SerializedName("aiSuggestions") val aiSuggestions: String? = null,
    @SerializedName("stemCount") val stemCount: Int? = null,
    @SerializedName("stemLengthCm") val stemLengthCm: Int? = null,
    @SerializedName("qualityGrade") val qualityGrade: String? = null,
    @SerializedName("vaseLifeDays") val vaseLifeDays: Int? = null,
    @SerializedName("harvestDestinationId") val harvestDestinationId: Long? = null,
)

data class IdentifyPlantRequest(
    @SerializedName("imageBase64") val imageBase64: String,
)

data class ExtractSpeciesInfoRequest(
    @SerializedName("imageBase64") val imageBase64: String,
)

data class ExtractedSpeciesInfo(
    @SerializedName("commonName") val commonName: String? = null,
    @SerializedName("variantName") val variantName: String? = null,
    @SerializedName("variantNameSv") val variantNameSv: String? = null,
    @SerializedName("scientificName") val scientificName: String? = null,
    @SerializedName("germinationTimeDaysMin") val germinationTimeDaysMin: Int? = null,
    @SerializedName("germinationTimeDaysMax") val germinationTimeDaysMax: Int? = null,
    @SerializedName("sowingDepthMm") val sowingDepthMm: Int? = null,
    @SerializedName("heightCmMin") val heightCmMin: Int? = null,
    @SerializedName("heightCmMax") val heightCmMax: Int? = null,
    @SerializedName("bloomMonths") val bloomMonths: List<Int>? = null,
    @SerializedName("sowingMonths") val sowingMonths: List<Int>? = null,
    @SerializedName("germinationRate") val germinationRate: Int? = null,
    @SerializedName("growingPositions") val growingPositions: List<String>? = null,
    @SerializedName("soils") val soils: List<String>? = null,
    @SerializedName("daysToHarvestMin") val daysToHarvestMin: Int? = null,
    @SerializedName("daysToHarvestMax") val daysToHarvestMax: Int? = null,
    @SerializedName("cropBox") val cropBox: CropBox? = null,
)

data class PlantSuggestion(
    @SerializedName("species") val species: String,
    @SerializedName("commonName") val commonName: String,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("cropBox") val cropBox: CropBox? = null,
)

data class CropBox(
    @SerializedName("x") val x: Double,
    @SerializedName("y") val y: Double,
    @SerializedName("width") val width: Double,
    @SerializedName("height") val height: Double,
)

data class HarvestStatRow(
    @SerializedName("species") val species: String,
    @SerializedName("totalWeightGrams") val totalWeightGrams: Double,
    @SerializedName("totalQuantity") val totalQuantity: Int,
    @SerializedName("harvestCount") val harvestCount: Int,
    @SerializedName("totalStems") val totalStems: Int = 0,
)

// ── Species ──

data class SpeciesPhotoResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("sortOrder") val sortOrder: Int,
)

data class SpeciesResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("commonName") val commonName: String,
    @SerializedName("commonNameSv") val commonNameSv: String?,
    @SerializedName("variantName") val variantName: String? = null,
    @SerializedName("variantNameSv") val variantNameSv: String? = null,
    @SerializedName("scientificName") val scientificName: String?,
    @SerializedName("imageFrontUrl") val imageFrontUrl: String?,
    @SerializedName("imageBackUrl") val imageBackUrl: String?,
    @SerializedName("photos") val photos: List<SpeciesPhotoResponse> = emptyList(),
    @SerializedName("germinationTimeDaysMin") val germinationTimeDaysMin: Int?,
    @SerializedName("germinationTimeDaysMax") val germinationTimeDaysMax: Int?,
    @SerializedName("daysToHarvestMin") val daysToHarvestMin: Int?,
    @SerializedName("daysToHarvestMax") val daysToHarvestMax: Int?,
    @SerializedName("sowingDepthMm") val sowingDepthMm: Int?,
    @SerializedName("growingPositions") val growingPositions: List<String>,
    @SerializedName("soils") val soils: List<String>,
    @SerializedName("heightCmMin") val heightCmMin: Int?,
    @SerializedName("heightCmMax") val heightCmMax: Int?,
    @SerializedName("bloomMonths") val bloomMonths: List<Int> = emptyList(),
    @SerializedName("sowingMonths") val sowingMonths: List<Int> = emptyList(),
    @SerializedName("germinationRate") val germinationRate: Int?,
    @SerializedName("groups") val groups: List<SpeciesGroupRef> = emptyList(),
    @SerializedName("tags") val tags: List<SpeciesTagResponse>,
    @SerializedName("providers") val providers: List<SpeciesProviderResponse> = emptyList(),
    @SerializedName("isSystem") val isSystem: Boolean = false,
    @SerializedName("costPerSeedSek") val costPerSeedSek: Int? = null,
    @SerializedName("expectedStemsPerPlant") val expectedStemsPerPlant: Int? = null,
    @SerializedName("expectedVaseLifeDays") val expectedVaseLifeDays: Int? = null,
    @SerializedName("plantType") val plantType: String? = null,
    @SerializedName("workflowTemplateId") val workflowTemplateId: Long? = null,
    @SerializedName("defaultUnitType") val defaultUnitType: String? = null,
    @SerializedName("createdAt") val createdAt: String,
)

data class CreateSpeciesRequest(
    @SerializedName("commonName") val commonName: String,
    @SerializedName("commonNameSv") val commonNameSv: String? = null,
    @SerializedName("variantName") val variantName: String? = null,
    @SerializedName("variantNameSv") val variantNameSv: String? = null,
    @SerializedName("scientificName") val scientificName: String? = null,
    @SerializedName("imageFrontBase64") val imageFrontBase64: String? = null,
    @SerializedName("imageBackBase64") val imageBackBase64: String? = null,
    @SerializedName("germinationTimeDaysMin") val germinationTimeDaysMin: Int? = null,
    @SerializedName("germinationTimeDaysMax") val germinationTimeDaysMax: Int? = null,
    @SerializedName("daysToHarvestMin") val daysToHarvestMin: Int? = null,
    @SerializedName("daysToHarvestMax") val daysToHarvestMax: Int? = null,
    @SerializedName("sowingDepthMm") val sowingDepthMm: Int? = null,
    @SerializedName("growingPositions") val growingPositions: List<String> = emptyList(),
    @SerializedName("soils") val soils: List<String> = emptyList(),
    @SerializedName("heightCmMin") val heightCmMin: Int? = null,
    @SerializedName("heightCmMax") val heightCmMax: Int? = null,
    @SerializedName("bloomMonths") val bloomMonths: List<Int> = emptyList(),
    @SerializedName("sowingMonths") val sowingMonths: List<Int> = emptyList(),
    @SerializedName("germinationRate") val germinationRate: Int? = null,
    @SerializedName("tagIds") val tagIds: List<Long> = emptyList(),
    @SerializedName("workflowTemplateId") val workflowTemplateId: Long? = null,
)

data class UpdateSpeciesRequest(
    @SerializedName("commonName") val commonName: String? = null,
    @SerializedName("commonNameSv") val commonNameSv: String? = null,
    @SerializedName("variantName") val variantName: String? = null,
    @SerializedName("variantNameSv") val variantNameSv: String? = null,
    @SerializedName("scientificName") val scientificName: String? = null,
    @SerializedName("imageFrontBase64") val imageFrontBase64: String? = null,
    @SerializedName("imageBackBase64") val imageBackBase64: String? = null,
    @SerializedName("germinationTimeDaysMin") val germinationTimeDaysMin: Int? = null,
    @SerializedName("germinationTimeDaysMax") val germinationTimeDaysMax: Int? = null,
    @SerializedName("daysToHarvestMin") val daysToHarvestMin: Int? = null,
    @SerializedName("daysToHarvestMax") val daysToHarvestMax: Int? = null,
    @SerializedName("sowingDepthMm") val sowingDepthMm: Int? = null,
    @SerializedName("growingPositions") val growingPositions: List<String>? = null,
    @SerializedName("soils") val soils: List<String>? = null,
    @SerializedName("heightCmMin") val heightCmMin: Int? = null,
    @SerializedName("heightCmMax") val heightCmMax: Int? = null,
    @SerializedName("bloomMonths") val bloomMonths: List<Int>? = null,
    @SerializedName("sowingMonths") val sowingMonths: List<Int>? = null,
    @SerializedName("germinationRate") val germinationRate: Int? = null,
    @SerializedName("tagIds") val tagIds: List<Long>? = null,
    @SerializedName("workflowTemplateId") val workflowTemplateId: Long? = null,
)

data class SpeciesGroupRef(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
)

data class SpeciesGroupResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
)

data class CreateSpeciesGroupRequest(
    @SerializedName("name") val name: String,
)

data class SpeciesTagResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
)

data class CreateSpeciesTagRequest(
    @SerializedName("name") val name: String,
)

// ── Providers ──

data class ProviderResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("identifier") val identifier: String,
)

data class SpeciesProviderResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("providerId") val providerId: Long,
    @SerializedName("providerName") val providerName: String,
    @SerializedName("providerIdentifier") val providerIdentifier: String,
    @SerializedName("imageFrontUrl") val imageFrontUrl: String?,
    @SerializedName("imageBackUrl") val imageBackUrl: String?,
    @SerializedName("productUrl") val productUrl: String?,
)

data class FrequentCommentResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("text") val text: String,
    @SerializedName("useCount") val useCount: Int,
)

data class RecordCommentRequest(
    @SerializedName("text") val text: String,
)

// ── Seed Inventory ──

data class SeedInventoryResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("collectionDate") val collectionDate: String?,
    @SerializedName("expirationDate") val expirationDate: String?,
    @SerializedName("costPerUnitSek") val costPerUnitSek: Int? = null,
    @SerializedName("unitType") val unitType: String? = null,
    @SerializedName("seasonId") val seasonId: Long? = null,
    @SerializedName("createdAt") val createdAt: String,
)

data class CreateSeedInventoryRequest(
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("collectionDate") val collectionDate: String? = null,
    @SerializedName("expirationDate") val expirationDate: String? = null,
    @SerializedName("costPerUnitSek") val costPerUnitSek: Int? = null,
    @SerializedName("unitType") val unitType: String? = null,
    @SerializedName("seasonId") val seasonId: Long? = null,
)

data class UpdateSeedInventoryRequest(
    @SerializedName("quantity") val quantity: Int? = null,
    @SerializedName("collectionDate") val collectionDate: String? = null,
    @SerializedName("expirationDate") val expirationDate: String? = null,
    @SerializedName("costPerUnitSek") val costPerUnitSek: Int? = null,
    @SerializedName("unitType") val unitType: String? = null,
    @SerializedName("seasonId") val seasonId: Long? = null,
)

data class DecrementSeedInventoryRequest(
    @SerializedName("quantity") val quantity: Int,
)

// ── Species Plant Summary ──

data class SpeciesPlantSummary(
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String,
    @SerializedName("scientificName") val scientificName: String?,
    @SerializedName("activePlantCount") val activePlantCount: Int,
    @SerializedName("totalPlantCount") val totalPlantCount: Int,
)

data class PlantLocationGroup(
    @SerializedName("gardenName") val gardenName: String?,
    @SerializedName("bedName") val bedName: String?,
    @SerializedName("bedId") val bedId: Long?,
    @SerializedName("status") val status: String,
    @SerializedName("count") val count: Int,
    @SerializedName("year") val year: Int,
)

// ── Scheduled Tasks ──

data class ScheduledTaskResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("speciesId") val speciesId: Long?,
    @SerializedName("speciesName") val speciesName: String?,
    @SerializedName("activityType") val activityType: String,
    @SerializedName("deadline") val deadline: String,
    @SerializedName("targetCount") val targetCount: Int,
    @SerializedName("remainingCount") val remainingCount: Int,
    @SerializedName("status") val status: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("seasonId") val seasonId: Long? = null,
    @SerializedName("successionScheduleId") val successionScheduleId: Long? = null,
    @SerializedName("originGroupId") val originGroupId: Long? = null,
    @SerializedName("originGroupName") val originGroupName: String? = null,
    @SerializedName("acceptableSpecies") val acceptableSpecies: List<AcceptableSpeciesEntry> = emptyList(),
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
)

data class AcceptableSpeciesEntry(
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String,
    @SerializedName("commonName") val commonName: String,
    @SerializedName("variantName") val variantName: String?,
    @SerializedName("commonNameSv") val commonNameSv: String?,
    @SerializedName("variantNameSv") val variantNameSv: String?,
)

data class CreateScheduledTaskRequest(
    @SerializedName("speciesId") val speciesId: Long? = null,
    @SerializedName("speciesGroupId") val speciesGroupId: Long? = null,
    @SerializedName("speciesIds") val speciesIds: List<Long>? = null,
    @SerializedName("activityType") val activityType: String,
    @SerializedName("deadline") val deadline: String,
    @SerializedName("targetCount") val targetCount: Int,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("seasonId") val seasonId: Long? = null,
    @SerializedName("successionScheduleId") val successionScheduleId: Long? = null,
)

data class UpdateScheduledTaskRequest(
    @SerializedName("speciesId") val speciesId: Long? = null,
    @SerializedName("activityType") val activityType: String? = null,
    @SerializedName("deadline") val deadline: String? = null,
    @SerializedName("targetCount") val targetCount: Int? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("seasonId") val seasonId: Long? = null,
)

data class CompleteTaskPartiallyRequest(
    @SerializedName("processedCount") val processedCount: Int,
    @SerializedName("speciesId") val speciesId: Long,
)

// ── Seasons ──

data class SeasonResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("year") val year: Int,
    @SerializedName("startDate") val startDate: String?,
    @SerializedName("endDate") val endDate: String?,
    @SerializedName("lastFrostDate") val lastFrostDate: String?,
    @SerializedName("firstFrostDate") val firstFrostDate: String?,
    @SerializedName("growingDegreeBaseC") val growingDegreeBaseC: Double?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
)

data class CreateSeasonRequest(
    @SerializedName("name") val name: String,
    @SerializedName("year") val year: Int,
    @SerializedName("lastFrostDate") val lastFrostDate: String? = null,
    @SerializedName("firstFrostDate") val firstFrostDate: String? = null,
    @SerializedName("notes") val notes: String? = null,
)

// ── Customers ──

data class CustomerResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("channel") val channel: String,
    @SerializedName("contactInfo") val contactInfo: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
)

// ── Production Targets ──

data class ProductionTargetResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("seasonId") val seasonId: Long,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String?,
    @SerializedName("stemsPerWeek") val stemsPerWeek: Int,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
)

data class ProductionForecastResponse(
    @SerializedName("targetId") val targetId: Long,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String?,
    @SerializedName("totalWeeks") val totalWeeks: Long,
    @SerializedName("totalStemsNeeded") val totalStemsNeeded: Long,
    @SerializedName("stemsPerPlant") val stemsPerPlant: Int,
    @SerializedName("plantsNeeded") val plantsNeeded: Long,
    @SerializedName("germinationRate") val germinationRate: Int,
    @SerializedName("seedsNeeded") val seedsNeeded: Long,
    @SerializedName("daysToHarvest") val daysToHarvest: Int,
    @SerializedName("suggestedSowDate") val suggestedSowDate: String?,
    @SerializedName("warnings") val warnings: List<String>,
)

// ── Succession Schedules ──

data class SuccessionScheduleResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("seasonId") val seasonId: Long,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String,
    @SerializedName("bedId") val bedId: Long?,
    @SerializedName("firstSowDate") val firstSowDate: String,
    @SerializedName("intervalDays") val intervalDays: Int,
    @SerializedName("totalSuccessions") val totalSuccessions: Int,
    @SerializedName("seedsPerSuccession") val seedsPerSuccession: Int,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
)

// ── Supplies ──

data class SupplyTypeResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("category") val category: String,
    @SerializedName("unit") val unit: String,
    @SerializedName("properties") val properties: Map<String, Any?>,
    @SerializedName("createdAt") val createdAt: String,
)

data class SupplyInventoryResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("supplyTypeId") val supplyTypeId: Long,
    @SerializedName("supplyTypeName") val supplyTypeName: String,
    @SerializedName("category") val category: String,
    @SerializedName("unit") val unit: String,
    @SerializedName("properties") val properties: Map<String, Any?>,
    @SerializedName("quantity") val quantity: Double,
    @SerializedName("costSek") val costSek: Int?,
    @SerializedName("seasonId") val seasonId: Long?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
)

data class DecrementSupplyRequest(
    @SerializedName("quantity") val quantity: Double,
)

// ── Workflows ──

data class SpeciesWorkflowResponse(
    @SerializedName("templateId") val templateId: Long?,
    @SerializedName("templateName") val templateName: String?,
    @SerializedName("steps") val steps: List<SpeciesWorkflowStepResponse>,
)

data class SpeciesWorkflowStepResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("templateStepId") val templateStepId: Long?,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("eventType") val eventType: String?,
    @SerializedName("daysAfterPrevious") val daysAfterPrevious: Int?,
    @SerializedName("isOptional") val isOptional: Boolean,
    @SerializedName("isSideBranch") val isSideBranch: Boolean,
    @SerializedName("sideBranchName") val sideBranchName: String?,
    @SerializedName("sortOrder") val sortOrder: Int,
)

data class PlantWorkflowProgressResponse(
    @SerializedName("steps") val steps: List<SpeciesWorkflowStepResponse>,
    @SerializedName("completedStepIds") val completedStepIds: List<Long>,
    @SerializedName("currentStepId") val currentStepId: Long?,
    @SerializedName("activeSideBranches") val activeSideBranches: List<String>,
)

data class CompleteWorkflowStepRequest(
    @SerializedName("plantIds") val plantIds: List<Long>,
    @SerializedName("notes") val notes: String? = null,
)
