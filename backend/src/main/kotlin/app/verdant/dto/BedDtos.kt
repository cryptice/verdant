package app.verdant.dto

import jakarta.validation.constraints.NotBlank
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
    val widthMeters: Double? = null
)

data class UpdateBedRequest(
    @field:Size(max = 255)
    val name: String? = null,
    @field:Size(max = 2000)
    val description: String? = null,
    val boundaryJson: String? = null,
    val lengthMeters: Double? = null,
    val widthMeters: Double? = null
)
