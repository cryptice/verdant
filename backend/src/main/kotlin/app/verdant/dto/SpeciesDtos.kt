package app.verdant.dto

import app.verdant.entity.GrowingPosition
import app.verdant.entity.SoilType
import java.time.Instant

data class SpeciesPhotoResponse(
    val id: Long,
    val imageBase64: String,
    val sortOrder: Int,
)

data class SpeciesResponse(
    val id: Long,
    val commonName: String,
    val commonNameSv: String?,
    val scientificName: String?,
    val imageFrontBase64: String?,
    val imageBackBase64: String?,
    val photos: List<SpeciesPhotoResponse>,
    val daysToSprout: Int?,
    val daysToHarvest: Int?,
    val germinationTimeDays: Int?,
    val sowingDepthMm: Int?,
    val growingPositions: List<GrowingPosition>,
    val soils: List<SoilType>,
    val heightCm: Int?,
    val bloomTime: String?,
    val germinationRate: Int?,
    val groupId: Long?,
    val groupName: String?,
    val tags: List<SpeciesTagResponse>,
    val isSystem: Boolean,
    val createdAt: Instant,
)

data class CreateSpeciesRequest(
    val commonName: String,
    val commonNameSv: String? = null,
    val scientificName: String? = null,
    val imageFrontBase64: String? = null,
    val imageBackBase64: String? = null,
    val daysToSprout: Int? = null,
    val daysToHarvest: Int? = null,
    val germinationTimeDays: Int? = null,
    val sowingDepthMm: Int? = null,
    val growingPositions: List<GrowingPosition> = emptyList(),
    val soils: List<SoilType> = emptyList(),
    val heightCm: Int? = null,
    val bloomTime: String? = null,
    val germinationRate: Int? = null,
    val groupId: Long? = null,
    val tagIds: List<Long> = emptyList(),
)

data class UpdateSpeciesRequest(
    val commonName: String? = null,
    val commonNameSv: String? = null,
    val scientificName: String? = null,
    val imageFrontBase64: String? = null,
    val imageBackBase64: String? = null,
    val daysToSprout: Int? = null,
    val daysToHarvest: Int? = null,
    val germinationTimeDays: Int? = null,
    val sowingDepthMm: Int? = null,
    val growingPositions: List<GrowingPosition>? = null,
    val soils: List<SoilType>? = null,
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

data class ExtractSpeciesInfoRequest(val imageBase64: String)

data class ExtractedSpeciesInfo(
    val commonName: String? = null,
    val scientificName: String? = null,
    val germinationTimeDays: Int? = null,
    val sowingDepthMm: Int? = null,
    val heightCm: Int? = null,
    val bloomTime: String? = null,
    val germinationRate: Int? = null,
    val growingPositions: List<String>? = null,
    val soils: List<String>? = null,
    val daysToSprout: Int? = null,
    val daysToHarvest: Int? = null,
    val cropBox: CropBox? = null,
)
