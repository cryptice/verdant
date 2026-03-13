package app.verdant.dto

import java.time.Instant

data class BedResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val gardenId: Long,
    val boundaryJson: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreateBedRequest(
    val name: String,
    val description: String? = null,
    val boundaryJson: String? = null
)

data class UpdateBedRequest(
    val name: String? = null,
    val description: String? = null,
    val boundaryJson: String? = null
)
