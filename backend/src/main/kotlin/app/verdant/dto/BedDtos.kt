package app.verdant.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
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
    val soilType: String?,
    val soilPh: Double?,
    val sunExposure: String?,
    val drainage: String?,
    val aspect: String?,
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
    val aspect: String? = null,
    val irrigationType: String? = null,
    val protection: String? = null,
    val raisedBed: Boolean? = null,
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
    val aspect: String? = null,
    val irrigationType: String? = null,
    val protection: String? = null,
    val raisedBed: Boolean? = null,
)
