package app.verdant.dto

import java.time.Instant
import java.time.LocalDate

data class SeedInventoryResponse(
    val id: Long,
    val speciesId: Long,
    val speciesName: String,
    val quantity: Int,
    val collectionDate: LocalDate?,
    val expirationDate: LocalDate?,
    val costPerUnitCents: Int?,
    val unitType: String?,
    val seasonId: Long?,
    val createdAt: Instant,
)

data class CreateSeedInventoryRequest(
    val speciesId: Long,
    val quantity: Int,
    val collectionDate: LocalDate? = null,
    val expirationDate: LocalDate? = null,
    val costPerUnitCents: Int? = null,
    val unitType: String? = null,
    val seasonId: Long? = null,
)

data class UpdateSeedInventoryRequest(
    val quantity: Int? = null,
    val collectionDate: LocalDate? = null,
    val expirationDate: LocalDate? = null,
    val costPerUnitCents: Int? = null,
    val unitType: String? = null,
    val seasonId: Long? = null,
)

data class DecrementSeedInventoryRequest(
    val quantity: Int,
)
