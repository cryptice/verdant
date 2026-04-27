package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class BedEvent(
    val id: Long? = null,
    val bedId: Long,
    val eventType: PlantEventType,
    val eventDate: LocalDate = LocalDate.now(),
    val notes: String? = null,
    val plantsAffected: Int? = null,
    val createdAt: Instant = Instant.now(),
)
