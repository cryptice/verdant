package app.verdant.dto

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
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateSeasonRequest(
    val name: String,
    val year: Int,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val lastFrostDate: LocalDate? = null,
    val firstFrostDate: LocalDate? = null,
    val growingDegreeBaseC: Double? = 10.0,
    val notes: String? = null,
    val isActive: Boolean = true,
)

data class UpdateSeasonRequest(
    val name: String? = null,
    val year: Int? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val lastFrostDate: LocalDate? = null,
    val firstFrostDate: LocalDate? = null,
    val growingDegreeBaseC: Double? = null,
    val notes: String? = null,
    val isActive: Boolean? = null,
)
