package app.verdant.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

data class SeasonResponse(
    val id: Long,
    val name: String,
    val year: Int,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val lastFrostDate: LocalDate?,
    val firstFrostDate: LocalDate?,
    val growingDegreeBaseC: Double?,
    val notes: String?,
    @get:JsonProperty("isActive") val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateSeasonRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:NotNull
    val year: Int,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val lastFrostDate: LocalDate? = null,
    val firstFrostDate: LocalDate? = null,
    val growingDegreeBaseC: Double? = 10.0,
    @field:Size(max = 2000)
    val notes: String? = null,
    @JsonProperty("isActive") val isActive: Boolean = true,
)

data class UpdateSeasonRequest(
    @field:Size(max = 255)
    val name: String? = null,
    val year: Int? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val lastFrostDate: LocalDate? = null,
    val firstFrostDate: LocalDate? = null,
    val growingDegreeBaseC: Double? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
    @JsonProperty("isActive") val isActive: Boolean? = null,
)
