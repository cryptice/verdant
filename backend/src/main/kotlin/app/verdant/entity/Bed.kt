package app.verdant.entity

import java.time.Instant

data class Bed(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val gardenId: Long,
    val boundaryJson: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
