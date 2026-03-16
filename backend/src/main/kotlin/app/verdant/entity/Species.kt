package app.verdant.entity

import java.time.Instant

data class Species(
    val id: Long? = null,
    val userId: Long? = null,
    val commonName: String,
    val commonNameSv: String? = null,
    val scientificName: String? = null,
    val imageFrontUrl: String? = null,
    val imageBackUrl: String? = null,
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
    val createdAt: Instant = Instant.now(),
)

enum class GrowingPosition { SUNNY, PARTIALLY_SUNNY, SHADOWY }

enum class SoilType { CLAY, SANDY, LOAMY, CHALKY, PEATY, SILTY }

data class SpeciesGroup(
    val id: Long? = null,
    val userId: Long? = null,
    val name: String,
)

data class SpeciesTag(
    val id: Long? = null,
    val userId: Long? = null,
    val name: String,
)

data class SpeciesPhoto(
    val id: Long? = null,
    val speciesId: Long,
    val imageUrl: String,
    val sortOrder: Int = 0,
    val createdAt: Instant = Instant.now(),
)

data class Provider(
    val id: Long? = null,
    val name: String,
    val identifier: String,
)

data class SpeciesProvider(
    val id: Long? = null,
    val speciesId: Long,
    val providerId: Long,
    val imageFrontUrl: String? = null,
    val imageBackUrl: String? = null,
    val productUrl: String? = null,
    val createdAt: Instant = Instant.now(),
)

data class FrequentComment(
    val id: Long? = null,
    val userId: Long,
    val text: String,
    val useCount: Int = 1,
)
