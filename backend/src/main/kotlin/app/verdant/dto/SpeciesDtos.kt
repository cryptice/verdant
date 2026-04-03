package app.verdant.dto

import app.verdant.entity.GrowingPosition
import app.verdant.entity.SoilType
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class SpeciesPhotoResponse(
    val id: Long,
    val imageUrl: String,
    val sortOrder: Int,
)

data class SpeciesResponse(
    val id: Long,
    val commonName: String,
    val variantName: String?,
    val commonNameSv: String?,
    val variantNameSv: String?,
    val scientificName: String?,
    val imageFrontUrl: String?,
    val imageBackUrl: String?,
    val photos: List<SpeciesPhotoResponse>,
    val daysToSprout: Int?,
    val daysToHarvest: Int?,
    val germinationTimeDays: Int?,
    val sowingDepthMm: Int?,
    val growingPositions: List<GrowingPosition>,
    val soils: List<SoilType>,
    val heightCm: Int?,
    val bloomMonths: List<Int>,
    val sowingMonths: List<Int>,
    val germinationRate: Int?,
    val groupId: Long?,
    val groupName: String?,
    val tags: List<SpeciesTagResponse>,
    val providers: List<SpeciesProviderResponse>,
    val costPerSeedSek: Int?,
    val expectedStemsPerPlant: Int?,
    val expectedVaseLifeDays: Int?,
    val plantType: String?,
    @get:JsonProperty("isSystem") val isSystem: Boolean,
    val createdAt: Instant,
)

data class CreateSpeciesRequest(
    @field:NotBlank @field:Size(max = 255)
    val commonName: String,
    @field:Size(max = 255)
    val variantName: String? = null,
    @field:Size(max = 255)
    val commonNameSv: String? = null,
    @field:Size(max = 255)
    val variantNameSv: String? = null,
    @field:Size(max = 255)
    val scientificName: String? = null,
    val imageFrontBase64: String? = null,
    val imageBackBase64: String? = null,
    @field:Min(0)
    val daysToSprout: Int? = null,
    @field:Min(0)
    val daysToHarvest: Int? = null,
    @field:Min(0)
    val germinationTimeDays: Int? = null,
    @field:Min(0)
    val sowingDepthMm: Int? = null,
    @field:Size(max = 100)
    val growingPositions: List<GrowingPosition>? = null,
    @field:Size(max = 100)
    val soils: List<SoilType>? = null,
    @field:Min(0)
    val heightCm: Int? = null,
    @field:Size(max = 12)
    val bloomMonths: List<Int>? = null,
    @field:Size(max = 12)
    val sowingMonths: List<Int>? = null,
    @field:Min(0)
    val germinationRate: Int? = null,
    val groupId: Long? = null,
    @field:Size(max = 100)
    val tagIds: List<Long>? = null,
    @field:Min(0)
    val costPerSeedSek: Int? = null,
    @field:Min(0)
    val expectedStemsPerPlant: Int? = null,
    @field:Min(0)
    val expectedVaseLifeDays: Int? = null,
    @field:Size(max = 255)
    val plantType: String? = null,
)

data class UpdateSpeciesRequest(
    @field:Size(max = 255)
    val commonName: String? = null,
    @field:Size(max = 255)
    val variantName: String? = null,
    @field:Size(max = 255)
    val commonNameSv: String? = null,
    @field:Size(max = 255)
    val variantNameSv: String? = null,
    @field:Size(max = 255)
    val scientificName: String? = null,
    val imageFrontBase64: String? = null,
    val imageBackBase64: String? = null,
    @field:Min(0)
    val daysToSprout: Int? = null,
    @field:Min(0)
    val daysToHarvest: Int? = null,
    @field:Min(0)
    val germinationTimeDays: Int? = null,
    @field:Min(0)
    val sowingDepthMm: Int? = null,
    @field:Size(max = 100)
    val growingPositions: List<GrowingPosition>? = null,
    @field:Size(max = 100)
    val soils: List<SoilType>? = null,
    @field:Min(0)
    val heightCm: Int? = null,
    @field:Size(max = 12)
    val bloomMonths: List<Int>? = null,
    @field:Size(max = 12)
    val sowingMonths: List<Int>? = null,
    @field:Min(0)
    val germinationRate: Int? = null,
    val groupId: Long? = null,
    @field:Size(max = 100)
    val tagIds: List<Long>? = null,
    @field:Min(0)
    val costPerSeedSek: Int? = null,
    @field:Min(0)
    val expectedStemsPerPlant: Int? = null,
    @field:Min(0)
    val expectedVaseLifeDays: Int? = null,
    @field:Size(max = 255)
    val plantType: String? = null,
)

data class SpeciesGroupResponse(
    val id: Long,
    val name: String,
)

data class CreateSpeciesGroupRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
)

data class SpeciesTagResponse(
    val id: Long,
    val name: String,
)

data class CreateSpeciesTagRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
)

data class FrequentCommentResponse(
    val id: Long,
    val text: String,
    val useCount: Int,
)

data class RecordCommentRequest(
    @field:NotBlank @field:Size(max = 2000)
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

// ── Providers ──

data class ProviderResponse(
    val id: Long,
    val name: String,
    val identifier: String,
)

data class CreateProviderRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:NotBlank @field:Size(max = 255)
    val identifier: String,
)

data class UpdateProviderRequest(
    @field:Size(max = 255)
    val name: String? = null,
    @field:Size(max = 255)
    val identifier: String? = null,
)

data class SpeciesProviderResponse(
    val id: Long,
    val providerId: Long,
    val providerName: String,
    val providerIdentifier: String,
    val imageFrontUrl: String?,
    val imageBackUrl: String?,
    val productUrl: String?,
    val costPerUnitSek: Int?,
    val unitType: String?,
)

data class AddSpeciesProviderRequest(
    @field:NotNull
    val providerId: Long,
    val imageFrontBase64: String? = null,
    val imageBackBase64: String? = null,
    @field:Size(max = 255)
    val productUrl: String? = null,
    @field:Min(0)
    val costPerUnitSek: Int? = null,
    @field:Size(max = 255)
    val unitType: String? = null,
)

data class UpdateSpeciesProviderRequest(
    val imageFrontBase64: String? = null,
    val imageBackBase64: String? = null,
    @field:Size(max = 255)
    val productUrl: String? = null,
    @field:Min(0)
    val costPerUnitSek: Int? = null,
    @field:Size(max = 255)
    val unitType: String? = null,
)

data class UploadPhotoRequest(
    @field:NotNull
    val imageBase64: String
)

data class SpeciesExportProvider(
    val providerName: String,
    val providerIdentifier: String,
    val imageFrontUrl: String? = null,
    val imageBackUrl: String? = null,
    val productUrl: String? = null,
    val costPerUnitSek: Int? = null,
    val unitType: String? = null,
)

data class SpeciesExportEntry(
    val commonName: String,
    val variantName: String? = null,
    val commonNameSv: String? = null,
    val variantNameSv: String? = null,
    val scientificName: String? = null,
    val imageFrontUrl: String? = null,
    val imageBackUrl: String? = null,
    val daysToSprout: Int? = null,
    val daysToHarvest: Int? = null,
    val germinationTimeDays: Int? = null,
    val sowingDepthMm: Int? = null,
    val growingPositions: List<String> = emptyList(),
    val soils: List<String> = emptyList(),
    val heightCm: Int? = null,
    val bloomMonths: List<Int> = emptyList(),
    val sowingMonths: List<Int> = emptyList(),
    val germinationRate: Int? = null,
    val groupName: String? = null,
    val tagNames: List<String> = emptyList(),
    val providers: List<SpeciesExportProvider> = emptyList(),
)

data class ImportResult(val created: Int, val updated: Int, val skipped: Int)

data class ExtractSpeciesInfoRequest(
    @field:NotNull
    val imageBase64: String
)

data class ExtractedFrontInfo(
    val commonName: String? = null,
    val commonNameSv: String? = null,
    val variantName: String? = null,
    val variantNameSv: String? = null,
    val scientificName: String? = null,
    val cropBox: CropBox? = null,
)

data class ExtractedSpeciesInfo(
    val commonName: String? = null,
    val scientificName: String? = null,
    val germinationTimeDays: Int? = null,
    val sowingDepthMm: Int? = null,
    val heightCm: Int? = null,
    val bloomMonths: List<Int>? = null,
    val sowingMonths: List<Int>? = null,
    val germinationRate: Int? = null,
    val growingPositions: List<String>? = null,
    val soils: List<String>? = null,
    val daysToSprout: Int? = null,
    val daysToHarvest: Int? = null,
    val cropBox: CropBox? = null,
)
