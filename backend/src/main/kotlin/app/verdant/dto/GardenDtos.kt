package app.verdant.dto

import java.time.Instant

data class GardenResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val emoji: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreateGardenRequest(
    val name: String,
    val description: String? = null,
    val emoji: String? = "\uD83C\uDF31"
)

data class UpdateGardenRequest(
    val name: String? = null,
    val description: String? = null,
    val emoji: String? = null
)
