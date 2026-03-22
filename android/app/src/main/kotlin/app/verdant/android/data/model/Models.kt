package app.verdant.android.data.model

data class GoogleAuthRequest(val idToken: String)
data class AuthResponse(val token: String, val user: UserResponse)

data class UserResponse(
    val id: Long,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: String,
    val language: String = "sv",
    val createdAt: String
)

data class UpdateUserRequest(
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val language: String? = null,
)

data class GardenResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val emoji: String?,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val boundaryJson: String?,
    val createdAt: String,
    val updatedAt: String
)

data class CreateGardenRequest(
    val name: String,
    val description: String? = null,
    val emoji: String? = "\uD83C\uDF31",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val boundaryJson: String? = null
)

data class UpdateGardenRequest(
    val name: String? = null,
    val description: String? = null,
    val emoji: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val boundaryJson: String? = null
)

data class BedResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val gardenId: Long,
    val boundaryJson: String?,
    val lengthMeters: Double? = null,
    val widthMeters: Double? = null,
    val createdAt: String,
    val updatedAt: String
)

data class CreateBedRequest(
    val name: String,
    val description: String? = null,
    val boundaryJson: String? = null
)

data class UpdateBedRequest(
    val name: String? = null,
    val description: String? = null,
    val boundaryJson: String? = null
)

data class BedWithGardenResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val gardenId: Long,
    val gardenName: String,
    val boundaryJson: String?,
)

data class PlantResponse(
    val id: Long,
    val name: String,
    val speciesId: Long?,
    val speciesName: String?,
    val plantedDate: String?,
    val status: String,
    val seedCount: Int?,
    val survivingCount: Int?,
    val bedId: Long?,
    val createdAt: String,
    val updatedAt: String
)

data class CreatePlantRequest(
    val name: String,
    val speciesId: Long? = null,
    val plantedDate: String? = null,
    val status: String = "SEEDED",
    val seedCount: Int? = null,
    val survivingCount: Int? = null,
)

data class BatchSowRequest(
    val bedId: Long? = null,
    val speciesId: Long,
    val name: String,
    val seedCount: Int,
    val notes: String? = null,
    val imageBase64: String? = null,
)

data class BatchSowResponse(
    val plantIds: List<Long>,
    val count: Int,
)

data class TraySummaryEntry(
    val speciesName: String,
    val status: String,
    val count: Int,
)

data class PlantGroupResponse(
    val speciesId: Long,
    val speciesName: String?,
    val bedId: Long?,
    val bedName: String?,
    val gardenName: String?,
    val plantedDate: String?,
    val status: String,
    val count: Int,
)

data class BatchEventRequest(
    val speciesId: Long,
    val bedId: Long? = null,
    val plantedDate: String? = null,
    val status: String,
    val eventType: String,
    val count: Int,
    val notes: String? = null,
    val imageBase64: String? = null,
    val targetBedId: Long? = null,
)

data class BatchEventResponse(
    val updatedCount: Int,
)

data class UpdatePlantRequest(
    val name: String? = null,
    val speciesId: Long? = null,
    val plantedDate: String? = null,
    val status: String? = null,
    val seedCount: Int? = null,
    val survivingCount: Int? = null,
)

data class DashboardResponse(
    val user: UserResponse,
    val gardens: List<GardenSummary>,
    val stats: DashboardStats
)

data class GardenSummary(
    val id: Long,
    val name: String,
    val emoji: String?,
    val bedCount: Int,
    val plantCount: Int
)

data class DashboardStats(
    val totalGardens: Int,
    val totalBeds: Int,
    val totalPlants: Int
)

data class LatLng(val lat: Double, val lng: Double)

data class SuggestLayoutRequest(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
)

data class SuggestedBed(
    val name: String,
    val description: String?,
    val boundary: List<LatLng>
)

data class SuggestLayoutResponse(
    val gardenName: String,
    val boundary: List<LatLng>,
    val beds: List<SuggestedBed>
)

data class CreateGardenWithLayoutRequest(
    val name: String,
    val description: String? = null,
    val emoji: String? = "\uD83C\uDF31",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val boundaryJson: String? = null,
    val beds: List<BedLayoutItem> = emptyList()
)

data class BedLayoutItem(
    val name: String,
    val description: String? = null,
    val boundaryJson: String? = null
)

data class GardenWithBedsResponse(
    val garden: GardenResponse,
    val beds: List<BedResponse>
)

// ── Plant Events ──

data class PlantEventResponse(
    val id: Long,
    val plantId: Long,
    val eventType: String,
    val eventDate: String,
    val plantCount: Int?,
    val weightGrams: Double?,
    val quantity: Int?,
    val notes: String?,
    val imageUrl: String?,
    val aiSuggestions: String?,
    val stemCount: Int? = null,
    val stemLengthCm: Int? = null,
    val qualityGrade: String? = null,
    val vaseLifeDays: Int? = null,
    val harvestDestinationId: Long? = null,
    val customerName: String? = null,
    val createdAt: String,
)

data class CreatePlantEventRequest(
    val eventType: String,
    val eventDate: String? = null,
    val plantCount: Int? = null,
    val weightGrams: Double? = null,
    val quantity: Int? = null,
    val notes: String? = null,
    val imageBase64: String? = null,
    val aiSuggestions: String? = null,
    val stemCount: Int? = null,
    val stemLengthCm: Int? = null,
    val qualityGrade: String? = null,
    val harvestDestinationId: Long? = null,
)

data class IdentifyPlantRequest(
    val imageBase64: String,
)

data class ExtractSpeciesInfoRequest(
    val imageBase64: String,
)

data class ExtractedSpeciesInfo(
    val commonName: String? = null,
    val variantName: String? = null,
    val variantNameSv: String? = null,
    val scientificName: String? = null,
    val germinationTimeDays: Int? = null,
    val sowingDepthMm: Int? = null,
    val heightCm: Int? = null,
    val bloomMonths: List<Int>? = null,
    val sowingMonths: List<Int>? = null,
    val germinationRate: Int? = null,
    val growingPositions: List<String>? = null,
    val soils: List<String>? = null,
    val daysToSprout: Int? = null,
    val daysToHarvest: Int? = null,
    val cropBox: CropBox? = null,
)

data class PlantSuggestion(
    val species: String,
    val commonName: String,
    val confidence: Double,
    val cropBox: CropBox? = null,
)

data class CropBox(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

data class HarvestStatRow(
    val species: String,
    val totalWeightGrams: Double,
    val totalQuantity: Int,
    val harvestCount: Int,
)

// ── Species ──

data class SpeciesPhotoResponse(
    val id: Long,
    val imageUrl: String,
    val sortOrder: Int,
)

data class SpeciesResponse(
    val id: Long,
    val commonName: String,
    val commonNameSv: String?,
    val variantName: String? = null,
    val variantNameSv: String? = null,
    val scientificName: String?,
    val imageFrontUrl: String?,
    val imageBackUrl: String?,
    val photos: List<SpeciesPhotoResponse> = emptyList(),
    val daysToSprout: Int?,
    val daysToHarvest: Int?,
    val germinationTimeDays: Int?,
    val sowingDepthMm: Int?,
    val growingPositions: List<String>,
    val soils: List<String>,
    val heightCm: Int?,
    val bloomMonths: List<Int> = emptyList(),
    val sowingMonths: List<Int> = emptyList(),
    val germinationRate: Int?,
    val groupId: Long?,
    val groupName: String?,
    val tags: List<SpeciesTagResponse>,
    val providers: List<SpeciesProviderResponse> = emptyList(),
    val isSystem: Boolean = false,
    val costPerSeedCents: Int? = null,
    val expectedStemsPerPlant: Int? = null,
    val expectedVaseLifeDays: Int? = null,
    val plantType: String? = null,
    val createdAt: String,
)

data class CreateSpeciesRequest(
    val commonName: String,
    val commonNameSv: String? = null,
    val variantName: String? = null,
    val variantNameSv: String? = null,
    val scientificName: String? = null,
    val imageFrontBase64: String? = null,
    val imageBackBase64: String? = null,
    val daysToSprout: Int? = null,
    val daysToHarvest: Int? = null,
    val germinationTimeDays: Int? = null,
    val sowingDepthMm: Int? = null,
    val growingPositions: List<String> = emptyList(),
    val soils: List<String> = emptyList(),
    val heightCm: Int? = null,
    val bloomMonths: List<Int> = emptyList(),
    val sowingMonths: List<Int> = emptyList(),
    val germinationRate: Int? = null,
    val groupId: Long? = null,
    val tagIds: List<Long> = emptyList(),
)

data class UpdateSpeciesRequest(
    val commonName: String? = null,
    val commonNameSv: String? = null,
    val variantName: String? = null,
    val variantNameSv: String? = null,
    val scientificName: String? = null,
    val imageFrontBase64: String? = null,
    val imageBackBase64: String? = null,
    val daysToSprout: Int? = null,
    val daysToHarvest: Int? = null,
    val germinationTimeDays: Int? = null,
    val sowingDepthMm: Int? = null,
    val growingPositions: List<String>? = null,
    val soils: List<String>? = null,
    val heightCm: Int? = null,
    val bloomMonths: List<Int>? = null,
    val sowingMonths: List<Int>? = null,
    val germinationRate: Int? = null,
    val groupId: Long? = null,
    val tagIds: List<Long>? = null,
)

data class SpeciesGroupResponse(
    val id: Long,
    val name: String,
)

data class CreateSpeciesGroupRequest(
    val name: String,
)

data class SpeciesTagResponse(
    val id: Long,
    val name: String,
)

data class CreateSpeciesTagRequest(
    val name: String,
)

// ── Providers ──

data class ProviderResponse(
    val id: Long,
    val name: String,
    val identifier: String,
)

data class SpeciesProviderResponse(
    val id: Long,
    val providerId: Long,
    val providerName: String,
    val providerIdentifier: String,
    val imageFrontUrl: String?,
    val imageBackUrl: String?,
    val productUrl: String?,
)

data class FrequentCommentResponse(
    val id: Long,
    val text: String,
    val useCount: Int,
)

data class RecordCommentRequest(
    val text: String,
)

// ── Seed Inventory ──

data class SeedInventoryResponse(
    val id: Long,
    val speciesId: Long,
    val speciesName: String,
    val quantity: Int,
    val collectionDate: String?,
    val expirationDate: String?,
    val costPerUnitCents: Int? = null,
    val unitType: String? = null,
    val seasonId: Long? = null,
    val createdAt: String,
)

data class CreateSeedInventoryRequest(
    val speciesId: Long,
    val quantity: Int,
    val collectionDate: String? = null,
    val expirationDate: String? = null,
)

data class UpdateSeedInventoryRequest(
    val quantity: Int? = null,
    val collectionDate: String? = null,
    val expirationDate: String? = null,
)

data class DecrementSeedInventoryRequest(
    val quantity: Int,
)

// ── Species Plant Summary ──

data class SpeciesPlantSummary(
    val speciesId: Long,
    val speciesName: String,
    val scientificName: String?,
    val activePlantCount: Int,
    val totalPlantCount: Int,
)

data class PlantLocationGroup(
    val gardenName: String?,
    val bedName: String?,
    val bedId: Long?,
    val status: String,
    val count: Int,
    val year: Int,
)

// ── Scheduled Tasks ──

data class ScheduledTaskResponse(
    val id: Long,
    val speciesId: Long,
    val speciesName: String,
    val activityType: String,
    val deadline: String,
    val targetCount: Int,
    val remainingCount: Int,
    val status: String,
    val notes: String?,
    val seasonId: Long? = null,
    val successionScheduleId: Long? = null,
    val createdAt: String,
    val updatedAt: String,
)

data class CreateScheduledTaskRequest(
    val speciesId: Long,
    val activityType: String,
    val deadline: String,
    val targetCount: Int,
    val notes: String? = null,
)

data class UpdateScheduledTaskRequest(
    val speciesId: Long? = null,
    val activityType: String? = null,
    val deadline: String? = null,
    val targetCount: Int? = null,
    val notes: String? = null,
)

data class CompleteTaskPartiallyRequest(
    val processedCount: Int,
)

// ── Seasons ──

data class SeasonResponse(
    val id: Long,
    val name: String,
    val year: Int,
    val startDate: String?,
    val endDate: String?,
    val lastFrostDate: String?,
    val firstFrostDate: String?,
    val growingDegreeBaseC: Double?,
    val notes: String?,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

data class CreateSeasonRequest(
    val name: String,
    val year: Int,
    val lastFrostDate: String? = null,
    val firstFrostDate: String? = null,
    val notes: String? = null,
)

// ── Customers ──

data class CustomerResponse(
    val id: Long,
    val name: String,
    val channel: String,
    val contactInfo: String?,
    val notes: String?,
    val createdAt: String,
)

// ── Production Targets ──

data class ProductionTargetResponse(
    val id: Long,
    val seasonId: Long,
    val speciesId: Long,
    val speciesName: String,
    val stemsPerWeek: Int,
    val startDate: String,
    val endDate: String,
    val notes: String?,
    val createdAt: String,
)

data class ProductionForecastResponse(
    val targetId: Long,
    val speciesName: String,
    val totalStemsNeeded: Int,
    val plantsNeeded: Int,
    val seedsNeeded: Int,
    val suggestedSowDate: String?,
    val weeksOfDelivery: Int,
    val warnings: List<String>,
)

// ── Succession Schedules ──

data class SuccessionScheduleResponse(
    val id: Long,
    val seasonId: Long,
    val speciesId: Long,
    val speciesName: String,
    val bedId: Long?,
    val firstSowDate: String,
    val intervalDays: Int,
    val totalSuccessions: Int,
    val seedsPerSuccession: Int,
    val notes: String?,
    val createdAt: String,
)
