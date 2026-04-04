package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class SuccessionSchedule(
    val id: Long? = null,
    val orgId: Long,
    val seasonId: Long,
    val speciesId: Long,
    val bedId: Long? = null,
    val firstSowDate: LocalDate,
    val intervalDays: Int,
    val totalSuccessions: Int,
    val seedsPerSuccession: Int,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
)
