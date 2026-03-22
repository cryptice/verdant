package app.verdant.dto

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
    val seasonId: Long,
    val speciesId: Long,
    val firstSowDate: LocalDate,
    val intervalDays: Int,
    val totalSuccessions: Int,
    val seedsPerSuccession: Int,
    val bedId: Long? = null,
    val notes: String? = null,
)

data class UpdateSuccessionScheduleRequest(
    val seasonId: Long? = null,
    val speciesId: Long? = null,
    val bedId: Long? = null,
    val firstSowDate: LocalDate? = null,
    val intervalDays: Int? = null,
    val totalSuccessions: Int? = null,
    val seedsPerSuccession: Int? = null,
    val notes: String? = null,
)
