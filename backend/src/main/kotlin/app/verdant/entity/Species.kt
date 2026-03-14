package app.verdant.entity

import java.time.Instant

data class Species(
    val id: Long? = null,
    val userId: Long,
    val commonName: String,
    val commonNameSv: String? = null,
    val scientificName: String? = null,
    val imageFrontBase64: String? = null,
    val imageBackBase64: String? = null,
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
    val createdAt: Instant = Instant.now(),
)

enum class GrowingPosition { SUNNY, PARTIALLY_SUNNY, SHADOWY }

enum class SoilType { CLAY, SANDY, LOAMY, CHALKY, PEATY, SILTY }

data class SpeciesGroup(
    val id: Long? = null,
    val userId: Long,
    val name: String,
)

data class SpeciesTag(
    val id: Long? = null,
    val userId: Long,
    val name: String,
)

data class SpeciesPhoto(
    val id: Long? = null,
    val speciesId: Long,
    val imageBase64: String,
    val sortOrder: Int = 0,
    val createdAt: Instant = Instant.now(),
)

data class FrequentComment(
    val id: Long? = null,
    val userId: Long,
    val text: String,
    val useCount: Int = 1,
)
