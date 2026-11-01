package app.verdant.android.ui.activity

import app.verdant.android.data.model.CreateSeedInventoryRequest
import app.verdant.android.data.model.SpeciesResponse
import java.time.LocalDate

/**
 * A new seed batch as typed, matching the web dialog: packages of a size make up the quantity, and price
 * per seed and per package fill each other in. Picking a variety fills in its usual package size and
 * price, and its supplier when it has only one.
 */
data class SeedBatchForm(
    val species: SpeciesResponse? = null,
    val providerId: Long? = null,
    val seedsPerPackage: String = "",
    val packages: String = "1",
    val unitType: String = "SEED",
    val collectionDate: LocalDate? = null,
    val expirationDate: LocalDate? = null,
    val costPerSeed: String = "",
    val costPerPackage: String = "",
) {
    private val packageSize: Int get() = seedsPerPackage.toIntOrNull() ?: 0
    val quantity: Int get() = packageSize * (packages.toIntOrNull() ?: 0)
    val canSave: Boolean get() = species != null && quantity > 0

    fun withSpecies(s: SpeciesResponse): SeedBatchForm {
        val size = s.seedsPerPackage ?: 0
        val perSeed = centsToSek(s.costPerSeedCents)
        return copy(
            species = s,
            providerId = s.providers.singleOrNull()?.providerId,
            unitType = s.defaultUnitType ?: "SEED",
            seedsPerPackage = if (size > 0) size.toString() else "",
            costPerSeed = perSeed,
            costPerPackage = packagePriceFor(perSeed, size),
        )
    }

    fun withSeedsPerPackage(value: String): SeedBatchForm {
        val digits = value.filter { it.isDigit() }
        val size = digits.toIntOrNull() ?: 0
        return when {
            size == 0 -> copy(seedsPerPackage = digits, costPerPackage = "")
            costPerPackage.isNotBlank() -> copy(seedsPerPackage = digits, costPerSeed = perSeedPriceFor(costPerPackage, size) ?: costPerSeed)
            else -> copy(seedsPerPackage = digits, costPerPackage = packagePriceFor(costPerSeed, size))
        }
    }

    fun withCostPerSeed(value: String): SeedBatchForm {
        val clean = value.filter { it.isDigit() || it == ',' || it == '.' }
        return copy(costPerSeed = clean, costPerPackage = packagePriceFor(clean, packageSize))
    }

    fun withCostPerPackage(value: String): SeedBatchForm {
        val clean = value.filter { it.isDigit() || it == ',' || it == '.' }
        return copy(costPerPackage = clean, costPerSeed = perSeedPriceFor(clean, packageSize) ?: costPerSeed)
    }

    fun toRequest() = CreateSeedInventoryRequest(
        speciesId = species!!.id,
        quantity = quantity,
        collectionDate = collectionDate?.toString(),
        expirationDate = expirationDate?.toString(),
        costPerUnitCents = sekToCents(costPerSeed),
        unitType = unitType,
        seedsPerPackage = packageSize.takeIf { it > 0 },
        providerId = providerId,
    )
}
