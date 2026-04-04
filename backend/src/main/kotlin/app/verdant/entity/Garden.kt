package app.verdant.entity

import java.time.Instant

data class Garden(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val emoji: String? = "\uD83C\uDF31",
    val orgId: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val boundaryJson: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
