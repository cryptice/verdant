package app.verdant.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

data class GardenAreaResponse(
    val id: Long,
    val gardenId: Long,
    val gardenName: String?,
    val name: String,
    val description: String?,
    val category: String,
    val boundaryJson: String?,
    val sizeSqm: Double?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateGardenAreaRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:Size(max = 2000)
    val description: String? = null,
    @field:NotBlank
    val category: String,
    val boundaryJson: String? = null,
    @field:Positive
    val sizeSqm: Double? = null,
)

data class UpdateGardenAreaRequest(
    @field:Size(max = 255)
    val name: String? = null,
    @field:Size(max = 2000)
    val description: String? = null,
    val category: String? = null,
    val boundaryJson: String? = null,
    @field:Positive
    val sizeSqm: Double? = null,
)

data class GardenAreaEventResponse(
    val id: Long,
    val gardenAreaId: Long,
    val eventType: String,
    val eventDate: LocalDate,
    val notes: String?,
    val createdAt: Instant,
)

data class CreateGardenAreaEventRequest(
    @field:NotBlank
    val activityType: String,
    val eventDate: LocalDate? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)

data class GardenAreaPhotoResponse(
    val id: Long,
    val gardenAreaId: Long,
    val photoUrl: String,
    val reason: String,
    val description: String?,
    val capturedAt: Instant,
    val createdAt: Instant,
)

data class CreateGardenAreaPhotoRequest(
    @field:NotBlank
    val photoUrl: String,
    @field:NotBlank
    val reason: String,
    @field:Size(max = 2000)
    val description: String? = null,
    val capturedAt: Instant? = null,
)
