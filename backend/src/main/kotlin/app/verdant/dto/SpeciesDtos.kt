package app.verdant.dto

import app.verdant.entity.GrowingPosition
import app.verdant.entity.SoilType
import java.time.Instant

data class SpeciesResponse(
    val id: Long,
    val commonName: String,
    val scientificName: String?,
    val imageBase64: String?,
    val daysToSprout: Int?,
    val daysToHarvest: Int?,
    val germinationTimeDays: Int?,
    val sowingDepthMm: Int?,
    val growingPosition: GrowingPosition?,
    val soil: SoilType?,
    val heightCm: Int?,
    val bloomTime: String?,
    val germinationRate: Int?,
    val groupId: Long?,
    val groupName: String?,
    val tags: List<SpeciesTagResponse>,
    val createdAt: Instant,
)

data class CreateSpeciesRequest(
    val commonName: String,
    val scientificName: String? = null,
    val imageBase64: String? = null,
    val daysToSprout: Int? = null,
    val daysToHarvest: Int? = null,
    val germinationTimeDays: Int? = null,
    val sowingDepthMm: Int? = null,
    val growingPosition: GrowingPosition? = null,
    val soil: SoilType? = null,
    val heightCm: Int? = null,
    val bloomTime: String? = null,
    val germinationRate: Int? = null,
    val groupId: Long? = null,
    val tagIds: List<Long> = emptyList(),
)

data class UpdateSpeciesRequest(
    val commonName: String? = null,
    val scientificName: String? = null,
    val imageBase64: String? = null,
    val daysToSprout: Int? = null,
    val daysToHarvest: Int? = null,
    val germinationTimeDays: Int? = null,
    val sowingDepthMm: Int? = null,
    val growingPosition: GrowingPosition? = null,
    val soil: SoilType? = null,
    val heightCm: Int? = null,
    val bloomTime: String? = null,
    val germinationRate: Int? = null,
    val groupId: Long? = null,
    val tagIds: List<Long>? = null,
)

data class SpeciesGroupResponse(
    val id: Long,
    val name: String,
)

data class CreateSpeciesGroupRequest(
    val name: String,
)

data class SpeciesTagResponse(
    val id: Long,
    val name: String,
)

data class CreateSpeciesTagRequest(
    val name: String,
)

data class FrequentCommentResponse(
    val id: Long,
    val text: String,
    val useCount: Int,
)

data class RecordCommentRequest(
    val text: String,
)

data class BedWithGardenResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val gardenId: Long,
    val gardenName: String,
    val boundaryJson: String?,
)
