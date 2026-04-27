package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

// ── Species ──

data class SpeciesPhotoResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("sortOrder") val sortOrder: Int,
)

data class SpeciesResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("commonName") val commonName: String,
    @SerializedName("commonNameSv") val commonNameSv: String?,
    @SerializedName("variantName") val variantName: String? = null,
    @SerializedName("variantNameSv") val variantNameSv: String? = null,
    @SerializedName("scientificName") val scientificName: String?,
    @SerializedName("imageFrontUrl") val imageFrontUrl: String?,
    @SerializedName("imageBackUrl") val imageBackUrl: String?,
    @SerializedName("photos") val photos: List<SpeciesPhotoResponse> = emptyList(),
    @SerializedName("germinationTimeDaysMin") val germinationTimeDaysMin: Int?,
    @SerializedName("germinationTimeDaysMax") val germinationTimeDaysMax: Int?,
    @SerializedName("daysToHarvestMin") val daysToHarvestMin: Int?,
    @SerializedName("daysToHarvestMax") val daysToHarvestMax: Int?,
    @SerializedName("sowingDepthMm") val sowingDepthMm: Int?,
    @SerializedName("growingPositions") val growingPositions: List<String>,
    @SerializedName("soils") val soils: List<String>,
    @SerializedName("heightCmMin") val heightCmMin: Int?,
    @SerializedName("heightCmMax") val heightCmMax: Int?,
    @SerializedName("bloomMonths") val bloomMonths: List<Int> = emptyList(),
    @SerializedName("sowingMonths") val sowingMonths: List<Int> = emptyList(),
    @SerializedName("germinationRate") val germinationRate: Int?,
    @SerializedName("groups") val groups: List<SpeciesGroupRef> = emptyList(),
    @SerializedName("tags") val tags: List<SpeciesTagResponse>,
    @SerializedName("providers") val providers: List<SpeciesProviderResponse> = emptyList(),
    @SerializedName("isSystem") val isSystem: Boolean = false,
    @SerializedName("costPerSeedCents") val costPerSeedCents: Int? = null,
    @SerializedName("expectedStemsPerPlant") val expectedStemsPerPlant: Int? = null,
    @SerializedName("expectedVaseLifeDays") val expectedVaseLifeDays: Int? = null,
    @SerializedName("plantType") val plantType: String? = null,
    @SerializedName("workflowTemplateId") val workflowTemplateId: Long? = null,
    @SerializedName("defaultUnitType") val defaultUnitType: String? = null,
    @SerializedName("createdAt") val createdAt: String,
)

data class CreateSpeciesRequest(
    @SerializedName("commonName") val commonName: String,
    @SerializedName("commonNameSv") val commonNameSv: String? = null,
    @SerializedName("variantName") val variantName: String? = null,
    @SerializedName("variantNameSv") val variantNameSv: String? = null,
    @SerializedName("scientificName") val scientificName: String? = null,
    @SerializedName("imageFrontBase64") val imageFrontBase64: String? = null,
    @SerializedName("imageBackBase64") val imageBackBase64: String? = null,
    @SerializedName("germinationTimeDaysMin") val germinationTimeDaysMin: Int? = null,
    @SerializedName("germinationTimeDaysMax") val germinationTimeDaysMax: Int? = null,
    @SerializedName("daysToHarvestMin") val daysToHarvestMin: Int? = null,
    @SerializedName("daysToHarvestMax") val daysToHarvestMax: Int? = null,
    @SerializedName("sowingDepthMm") val sowingDepthMm: Int? = null,
    @SerializedName("growingPositions") val growingPositions: List<String> = emptyList(),
    @SerializedName("soils") val soils: List<String> = emptyList(),
    @SerializedName("heightCmMin") val heightCmMin: Int? = null,
    @SerializedName("heightCmMax") val heightCmMax: Int? = null,
    @SerializedName("bloomMonths") val bloomMonths: List<Int> = emptyList(),
    @SerializedName("sowingMonths") val sowingMonths: List<Int> = emptyList(),
    @SerializedName("germinationRate") val germinationRate: Int? = null,
    @SerializedName("tagIds") val tagIds: List<Long> = emptyList(),
    @SerializedName("groupId") val groupId: Long? = null,
    @SerializedName("workflowTemplateId") val workflowTemplateId: Long? = null,
)

data class UpdateSpeciesRequest(
    @SerializedName("commonName") val commonName: String? = null,
    @SerializedName("commonNameSv") val commonNameSv: String? = null,
    @SerializedName("variantName") val variantName: String? = null,
    @SerializedName("variantNameSv") val variantNameSv: String? = null,
    @SerializedName("scientificName") val scientificName: String? = null,
    @SerializedName("imageFrontBase64") val imageFrontBase64: String? = null,
    @SerializedName("imageBackBase64") val imageBackBase64: String? = null,
    @SerializedName("germinationTimeDaysMin") val germinationTimeDaysMin: Int? = null,
    @SerializedName("germinationTimeDaysMax") val germinationTimeDaysMax: Int? = null,
    @SerializedName("daysToHarvestMin") val daysToHarvestMin: Int? = null,
    @SerializedName("daysToHarvestMax") val daysToHarvestMax: Int? = null,
    @SerializedName("sowingDepthMm") val sowingDepthMm: Int? = null,
    @SerializedName("growingPositions") val growingPositions: List<String>? = null,
    @SerializedName("soils") val soils: List<String>? = null,
    @SerializedName("heightCmMin") val heightCmMin: Int? = null,
    @SerializedName("heightCmMax") val heightCmMax: Int? = null,
    @SerializedName("bloomMonths") val bloomMonths: List<Int>? = null,
    @SerializedName("sowingMonths") val sowingMonths: List<Int>? = null,
    @SerializedName("germinationRate") val germinationRate: Int? = null,
    @SerializedName("tagIds") val tagIds: List<Long>? = null,
    @SerializedName("groupId") val groupId: Long? = null,
    @SerializedName("workflowTemplateId") val workflowTemplateId: Long? = null,
)

data class SpeciesGroupRef(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
)

data class SpeciesGroupResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
)

data class CreateSpeciesGroupRequest(
    @SerializedName("name") val name: String,
)

data class SpeciesTagResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
)

data class CreateSpeciesTagRequest(
    @SerializedName("name") val name: String,
)

// ── Providers ──

data class ProviderResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("identifier") val identifier: String,
)

data class SpeciesProviderResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("providerId") val providerId: Long,
    @SerializedName("providerName") val providerName: String,
    @SerializedName("providerIdentifier") val providerIdentifier: String,
    @SerializedName("imageFrontUrl") val imageFrontUrl: String?,
    @SerializedName("imageBackUrl") val imageBackUrl: String?,
    @SerializedName("productUrl") val productUrl: String?,
)

data class FrequentCommentResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("text") val text: String,
    @SerializedName("useCount") val useCount: Int,
)

data class RecordCommentRequest(
    @SerializedName("text") val text: String,
)

// ── Seed Inventory ──

data class SeedInventoryResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("collectionDate") val collectionDate: String?,
    @SerializedName("expirationDate") val expirationDate: String?,
    @SerializedName("costPerUnitCents") val costPerUnitCents: Int? = null,
    @SerializedName("unitType") val unitType: String? = null,
    @SerializedName("seasonId") val seasonId: Long? = null,
    @SerializedName("createdAt") val createdAt: String,
)

data class CreateSeedInventoryRequest(
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("collectionDate") val collectionDate: String? = null,
    @SerializedName("expirationDate") val expirationDate: String? = null,
    @SerializedName("costPerUnitCents") val costPerUnitCents: Int? = null,
    @SerializedName("unitType") val unitType: String? = null,
    @SerializedName("seasonId") val seasonId: Long? = null,
)

data class UpdateSeedInventoryRequest(
    @SerializedName("quantity") val quantity: Int? = null,
    @SerializedName("collectionDate") val collectionDate: String? = null,
    @SerializedName("expirationDate") val expirationDate: String? = null,
    @SerializedName("costPerUnitCents") val costPerUnitCents: Int? = null,
    @SerializedName("unitType") val unitType: String? = null,
    @SerializedName("seasonId") val seasonId: Long? = null,
)

data class DecrementSeedInventoryRequest(
    @SerializedName("quantity") val quantity: Int,
)
