package app.verdant.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

data class SuccessionScheduleResponse(
    val id: Long,
    val seasonId: Long,
    val speciesId: Long,
    val speciesName: String?,
    val bedId: Long?,
    val firstSowDate: LocalDate,
    val intervalDays: Int,
    val totalSuccessions: Int,
    val seedsPerSuccession: Int,
    val notes: String?,
    val createdAt: Instant,
)

data class CreateSuccessionScheduleRequest(
    @field:NotNull
    val seasonId: Long,
    @field:NotNull
    val speciesId: Long,
    @field:NotNull
    val firstSowDate: LocalDate,
    @field:Min(1)
    val intervalDays: Int,
    @field:Min(1)
    val totalSuccessions: Int,
    @field:Min(1)
    val seedsPerSuccession: Int,
    val bedId: Long? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)

data class UpdateSuccessionScheduleRequest(
    val seasonId: Long? = null,
    val speciesId: Long? = null,
    val bedId: Long? = null,
    val firstSowDate: LocalDate? = null,
    @field:Min(1)
    val intervalDays: Int? = null,
    @field:Min(1)
    val totalSuccessions: Int? = null,
    @field:Min(1)
    val seedsPerSuccession: Int? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)
