package app.verdant.entity

import java.time.Instant

data class Species(
    val id: Long? = null,
    val orgId: Long? = null,
    val commonName: String,
    val variantName: String? = null,
    val commonNameSv: String? = null,
    val variantNameSv: String? = null,
    val scientificName: String? = null,
    val imageFrontUrl: String? = null,
    val imageBackUrl: String? = null,
    val germinationTimeDaysMin: Int? = null,
    val germinationTimeDaysMax: Int? = null,
    val daysToHarvestMin: Int? = null,
    val daysToHarvestMax: Int? = null,
    val sowingDepthMm: Int? = null,
    val growingPositions: List<GrowingPosition> = emptyList(),
    val soils: List<SoilType> = emptyList(),
    val heightCmMin: Int? = null,
    val heightCmMax: Int? = null,
    val bloomMonths: List<Int> = emptyList(),
    val sowingMonths: List<Int> = emptyList(),
    val germinationRate: Int? = null,
    val costPerSeedCents: Int? = null,
    val expectedStemsPerPlant: Int? = null,
    val expectedVaseLifeDays: Int? = null,
    val plantType: PlantType = PlantType.ANNUAL,
    val defaultUnitType: UnitType = UnitType.SEED,
    val workflowTemplateId: Long? = null,
    val createdAt: Instant = Instant.now(),
)

enum class GrowingPosition { SUNNY, PARTIALLY_SUNNY, SHADOWY }

enum class SoilType { CLAY, SANDY, LOAMY, CHALKY, PEATY, SILTY }

data class SpeciesGroup(
    val id: Long? = null,
    val orgId: Long? = null,
    val name: String,
)

data class SpeciesTag(
    val id: Long? = null,
    val orgId: Long? = null,
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
    val costPerUnitCents: Int? = null,
    val unitType: UnitType = UnitType.SEED,
    val createdAt: Instant = Instant.now(),
)

data class FrequentComment(
    val id: Long? = null,
    val orgId: Long,
    val text: String,
    val useCount: Int = 1,
)

enum class PlantType { ANNUAL, PERENNIAL, BULB, TUBER }
enum class UnitType { SEED, PLUG, BULB, TUBER, PLANT }
