package app.verdant.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class BedResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val gardenId: Long,
    val boundaryJson: String?,
    val lengthMeters: Double?,
    val widthMeters: Double?,
    val soilType: String?,
    val soilPh: Double?,
    val sunExposure: String?,
    val drainage: String?,
    val sunDirections: List<String>,
    val irrigationType: String?,
    val protection: String?,
    val raisedBed: Boolean?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreateBedRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:Size(max = 2000)
    val description: String? = null,
    val boundaryJson: String? = null,
    val lengthMeters: Double? = null,
    val widthMeters: Double? = null,
    val soilType: String? = null,
    @field:DecimalMin("3.0") @field:DecimalMax("9.0")
    val soilPh: Double? = null,
    val sunExposure: String? = null,
    val drainage: String? = null,
    val sunDirections: List<String>? = null,
    val irrigationType: String? = null,
    val protection: String? = null,
    val raisedBed: Boolean? = null,
)

data class BedEventResponse(
    val id: Long,
    val bedId: Long,
    val eventType: String,
    val eventDate: java.time.LocalDate,
    val notes: String?,
    val plantsAffected: Int?,
    val createdAt: Instant,
)

data class BedPhotoResponse(
    val id: Long,
    val bedId: Long,
    val photoUrl: String,
    val reason: String,
    val description: String?,
    val capturedAt: Instant,
    val createdAt: Instant,
)

data class CreateBedPhotoRequest(
    @field:NotNull
    val imageBase64: String,
    @field:NotBlank @field:Size(max = 32)
    val reason: String,
    @field:Size(max = 2000)
    val description: String? = null,
    /** Optional client-supplied capture timestamp (ISO-8601). Falls back to server time. */
    val capturedAt: Instant? = null,
)

data class UpdateBedRequest(
    @field:Size(max = 255)
    val name: String? = null,
    @field:Size(max = 2000)
    val description: String? = null,
    val boundaryJson: String? = null,
    val lengthMeters: Double? = null,
    val widthMeters: Double? = null,
    val soilType: String? = null,
    @field:DecimalMin("3.0") @field:DecimalMax("9.0")
    val soilPh: Double? = null,
    val sunExposure: String? = null,
    val drainage: String? = null,
    val sunDirections: List<String>? = null,
    val irrigationType: String? = null,
    val protection: String? = null,
    val raisedBed: Boolean? = null,
)
