package app.verdant.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
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
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:Size(max = 2000)
    val description: String? = null,
    @field:Size(max = 255)
    val emoji: String? = "\uD83C\uDF31",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @field:Size(max = 255)
    val address: String? = null,
    val boundaryJson: String? = null
)

data class UpdateGardenRequest(
    @field:Size(max = 255)
    val name: String? = null,
    @field:Size(max = 2000)
    val description: String? = null,
    @field:Size(max = 255)
    val emoji: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @field:Size(max = 255)
    val address: String? = null,
    val boundaryJson: String? = null
)

// New DTOs for the wizard flow
data class CreateGardenWithLayoutRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:Size(max = 2000)
    val description: String? = null,
    @field:Size(max = 255)
    val emoji: String? = "\uD83C\uDF31",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @field:Size(max = 255)
    val address: String? = null,
    val boundaryJson: String? = null,
    @field:Size(max = 100)
    @field:Valid
    val beds: List<BedLayoutItem> = emptyList()
)

data class BedLayoutItem(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:Size(max = 2000)
    val description: String? = null,
    val boundaryJson: String? = null
)

data class GardenWithBedsResponse(
    val garden: GardenResponse,
    val beds: List<BedResponse>
)

// AI suggestion DTOs
data class SuggestLayoutRequest(
    @field:NotNull
    val latitude: Double,
    @field:NotNull
    val longitude: Double,
    @field:Size(max = 255)
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
