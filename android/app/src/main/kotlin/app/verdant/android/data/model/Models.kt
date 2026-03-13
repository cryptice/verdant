package app.verdant.android.data.model

data class GoogleAuthRequest(val idToken: String)
data class AuthResponse(val token: String, val user: UserResponse)

data class UserResponse(
    val id: Long,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: String,
    val createdAt: String
)

data class UpdateUserRequest(
    val displayName: String? = null,
    val avatarUrl: String? = null
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

data class PlantResponse(
    val id: Long,
    val name: String,
    val species: String?,
    val plantedDate: String?,
    val status: String,
    val seedCount: Int?,
    val survivingCount: Int?,
    val bedId: Long,
    val createdAt: String,
    val updatedAt: String
)

data class CreatePlantRequest(
    val name: String,
    val species: String? = null,
    val plantedDate: String? = null,
    val status: String = "SEEDED",
    val seedCount: Int? = null,
    val survivingCount: Int? = null,
)

data class UpdatePlantRequest(
    val name: String? = null,
    val species: String? = null,
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
    val imageBase64: String?,
    val aiSuggestions: String?,
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
)

data class IdentifyPlantRequest(
    val imageBase64: String,
)

data class PlantSuggestion(
    val species: String,
    val commonName: String,
    val confidence: Double,
)

data class HarvestStatRow(
    val species: String,
    val totalWeightGrams: Double,
    val totalQuantity: Int,
    val harvestCount: Int,
)
