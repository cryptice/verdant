package app.verdant.android.ui.activity

import app.verdant.android.data.model.CreateSpeciesRequest
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.UpdateSpeciesRequest

/** What the variety form holds, as typed. Matches the web form; only a name is required. */
data class SpeciesFormValues(
    val commonNameSv: String = "",
    val commonName: String = "",
    val variantNameSv: String = "",
    val variantName: String = "",
    val scientificName: String = "",
    val germinationTimeDaysMin: String = "",
    val germinationTimeDaysMax: String = "",
    val daysToHarvestMin: String = "",
    val daysToHarvestMax: String = "",
    val heightCmMin: String = "",
    val heightCmMax: String = "",
    val sowingDepthMm: String = "",
    val germinationRate: String = "",
    val sowingMonths: Set<Int> = emptySet(),
    val bloomMonths: Set<Int> = emptySet(),
    val positions: Set<String> = emptySet(),
    val soils: Set<String> = emptySet(),
    val tagIds: Set<Long> = emptySet(),
    val seedsPerPackage: String = "",
    val costPerSeed: String = "",
    val costPerPackage: String = "",
    val expectedStemsPerPlant: String = "",
    val expectedVaseLifeDays: String = "",
    val plantType: String? = null,
    val defaultUnitType: String? = null,
    val workflowTemplateId: Long? = null,
) {
    val hasName: Boolean get() = commonNameSv.isNotBlank() || commonName.isNotBlank()
    private val packageSize: Int get() = seedsPerPackage.toIntOrNull() ?: 0

    /** The English name falls back to the Swedish one, as on the web. */
    private val commonNameOrSv: String get() = commonName.trim().ifBlank { commonNameSv.trim() }

    fun copyToEnglish() = copy(
        commonName = commonName.ifBlank { commonNameSv },
        variantName = variantName.ifBlank { variantNameSv },
    )

    fun withSeedsPerPackage(value: String): SpeciesFormValues {
        val digits = value.filter { it.isDigit() }
        val size = digits.toIntOrNull() ?: 0
        return when {
            size == 0 -> copy(seedsPerPackage = digits, costPerPackage = "")
            costPerPackage.isNotBlank() -> copy(seedsPerPackage = digits, costPerSeed = perSeedPriceFor(costPerPackage, size) ?: costPerSeed)
            else -> copy(seedsPerPackage = digits, costPerPackage = packagePriceFor(costPerSeed, size))
        }
    }

    fun withCostPerSeed(value: String): SpeciesFormValues {
        val clean = value.filter { it.isDigit() || it == ',' || it == '.' }
        return copy(costPerSeed = clean, costPerPackage = packagePriceFor(clean, packageSize))
    }

    fun withCostPerPackage(value: String): SpeciesFormValues {
        val clean = value.filter { it.isDigit() || it == ',' || it == '.' }
        return copy(costPerPackage = clean, costPerSeed = perSeedPriceFor(clean, packageSize) ?: costPerSeed)
    }

    fun toCreateRequest(imageFrontBase64: String?, imageBackBase64: String?) = CreateSpeciesRequest(
        commonName = commonNameOrSv,
        commonNameSv = commonNameSv.trim().ifBlank { null },
        variantName = variantName.trim().ifBlank { null },
        variantNameSv = variantNameSv.trim().ifBlank { null },
        scientificName = scientificName.trim().ifBlank { null },
        imageFrontBase64 = imageFrontBase64,
        imageBackBase64 = imageBackBase64,
        germinationTimeDaysMin = germinationTimeDaysMin.toIntOrNull(),
        germinationTimeDaysMax = germinationTimeDaysMax.toIntOrNull(),
        daysToHarvestMin = daysToHarvestMin.toIntOrNull(),
        daysToHarvestMax = daysToHarvestMax.toIntOrNull(),
        sowingDepthMm = sowingDepthMm.toIntOrNull(),
        growingPositions = positions.toList(),
        soils = soils.toList(),
        heightCmMin = heightCmMin.toIntOrNull(),
        heightCmMax = heightCmMax.toIntOrNull(),
        bloomMonths = bloomMonths.sorted(),
        sowingMonths = sowingMonths.sorted(),
        germinationRate = germinationRate.toIntOrNull(),
        tagIds = tagIds.toList(),
        costPerSeedCents = sekToCents(costPerSeed),
        seedsPerPackage = seedsPerPackage.toIntOrNull()?.takeIf { it > 0 },
        expectedStemsPerPlant = expectedStemsPerPlant.toIntOrNull(),
        expectedVaseLifeDays = expectedVaseLifeDays.toIntOrNull(),
        plantType = plantType,
        defaultUnitType = defaultUnitType,
    )

    fun toUpdateRequest(imageFrontBase64: String?, imageBackBase64: String?): UpdateSpeciesRequest = toCreateRequest(imageFrontBase64, imageBackBase64).let {
        UpdateSpeciesRequest(
            commonName = it.commonName, commonNameSv = it.commonNameSv, variantName = it.variantName, variantNameSv = it.variantNameSv,
            scientificName = it.scientificName, imageFrontBase64 = it.imageFrontBase64, imageBackBase64 = it.imageBackBase64,
            germinationTimeDaysMin = it.germinationTimeDaysMin, germinationTimeDaysMax = it.germinationTimeDaysMax,
            daysToHarvestMin = it.daysToHarvestMin, daysToHarvestMax = it.daysToHarvestMax, sowingDepthMm = it.sowingDepthMm,
            growingPositions = it.growingPositions, soils = it.soils, heightCmMin = it.heightCmMin, heightCmMax = it.heightCmMax,
            bloomMonths = it.bloomMonths, sowingMonths = it.sowingMonths, germinationRate = it.germinationRate, tagIds = it.tagIds,
            costPerSeedCents = it.costPerSeedCents, seedsPerPackage = it.seedsPerPackage,
            expectedStemsPerPlant = it.expectedStemsPerPlant, expectedVaseLifeDays = it.expectedVaseLifeDays,
            plantType = it.plantType, defaultUnitType = it.defaultUnitType,
        )
    }

    companion object {
        fun from(s: SpeciesResponse): SpeciesFormValues {
            val costPerSeed = centsToSek(s.costPerSeedCents)
            return SpeciesFormValues(
                commonNameSv = s.commonNameSv ?: "",
                commonName = s.commonName,
                variantNameSv = s.variantNameSv ?: "",
                variantName = s.variantName ?: "",
                scientificName = s.scientificName ?: "",
                germinationTimeDaysMin = s.germinationTimeDaysMin?.toString() ?: "",
                germinationTimeDaysMax = s.germinationTimeDaysMax?.toString() ?: "",
                daysToHarvestMin = s.daysToHarvestMin?.toString() ?: "",
                daysToHarvestMax = s.daysToHarvestMax?.toString() ?: "",
                heightCmMin = s.heightCmMin?.toString() ?: "",
                heightCmMax = s.heightCmMax?.toString() ?: "",
                sowingDepthMm = s.sowingDepthMm?.toString() ?: "",
                germinationRate = s.germinationRate?.toString() ?: "",
                sowingMonths = s.sowingMonths.toSet(),
                bloomMonths = s.bloomMonths.toSet(),
                positions = s.growingPositions.toSet(),
                soils = s.soils.toSet(),
                tagIds = s.tags.map { it.id }.toSet(),
                seedsPerPackage = s.seedsPerPackage?.toString() ?: "",
                costPerSeed = costPerSeed,
                costPerPackage = packagePriceFor(costPerSeed, s.seedsPerPackage ?: 0),
                expectedStemsPerPlant = s.expectedStemsPerPlant?.toString() ?: "",
                expectedVaseLifeDays = s.expectedVaseLifeDays?.toString() ?: "",
                plantType = s.plantType,
                defaultUnitType = s.defaultUnitType,
                workflowTemplateId = s.workflowTemplateId,
            )
        }
    }
}
