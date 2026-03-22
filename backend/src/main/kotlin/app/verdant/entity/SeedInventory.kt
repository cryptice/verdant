package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class SeedInventory(
    val id: Long? = null,
    val userId: Long,
    val speciesId: Long,
    val quantity: Int,
    val collectionDate: LocalDate? = null,
    val expirationDate: LocalDate? = null,
    val costPerUnitCents: Int? = null,
    val unitType: UnitType = UnitType.SEED,
    val seasonId: Long? = null,
    val createdAt: Instant = Instant.now(),
)
