package app.verdant.entity

import java.time.Instant

enum class BedPhotoReason { PROGRESS, ISSUE, HARVEST, PLANTING, OTHER }

data class BedPhoto(
    val id: Long? = null,
    val bedId: Long,
    val photoUrl: String,
    val reason: BedPhotoReason,
    val description: String? = null,
    val capturedAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now(),
)
