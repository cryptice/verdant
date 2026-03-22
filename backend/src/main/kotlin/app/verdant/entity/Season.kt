package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class Season(
    val id: Long? = null,
    val userId: Long,
    val name: String,
    val year: Int,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val lastFrostDate: LocalDate? = null,
    val firstFrostDate: LocalDate? = null,
    val growingDegreeBaseC: Double? = 10.0,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
