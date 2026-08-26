package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class GardenArea(
    val id: Long? = null,
    val gardenId: Long,
    val name: String,
    val description: String? = null,
    val category: GardenAreaCategory,
    val boundaryJson: String? = null,
    val sizeSqm: Double? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

/** [eventType] is a [MaintenanceActivity] name or [AREA_EVENT_NOTE]. */
data class GardenAreaEvent(
    val id: Long? = null,
    val gardenAreaId: Long,
    val eventType: String,
    val eventDate: LocalDate = LocalDate.now(),
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
)

data class GardenAreaPhoto(
    val id: Long? = null,
    val gardenAreaId: Long,
    val photoUrl: String,
    val reason: BedPhotoReason,
    val description: String? = null,
    val capturedAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now(),
)
