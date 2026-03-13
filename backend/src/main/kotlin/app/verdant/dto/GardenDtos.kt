package app.verdant.dto

import java.time.Instant

data class GardenResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val emoji: String?,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val boundaryJson: String?,
    val createdAt: Instant,
    val updatedAt: Instant
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

// New DTOs for the wizard flow
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

// AI suggestion DTOs
data class SuggestLayoutRequest(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
)

data class LatLng(
    val lat: Double,
    val lng: Double
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
