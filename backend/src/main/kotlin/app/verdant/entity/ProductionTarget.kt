package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class ProductionTarget(
    val id: Long? = null,
    val userId: Long,
    val seasonId: Long,
    val speciesId: Long,
    val stemsPerWeek: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
)
