package app.verdant.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

data class SeedInventoryResponse(
    val id: Long,
    val speciesId: Long,
    val speciesName: String,
    val quantity: Int,
    val collectionDate: LocalDate?,
    val expirationDate: LocalDate?,
    val costPerUnitSek: Double?,
    val unitType: String?,
    val seasonId: Long?,
    val speciesProviderId: Long?,
    val providerName: String?,
    val createdAt: Instant,
)

data class CreateSeedInventoryRequest(
    @field:NotNull
    val speciesId: Long,
    @field:Min(0)
    val quantity: Int,
    val collectionDate: LocalDate? = null,
    val expirationDate: LocalDate? = null,
    @field:Min(0)
    val costPerUnitSek: Double? = null,
    @field:NotBlank @field:Size(max = 255)
    val unitType: String,
    val seasonId: Long? = null,
    val speciesProviderId: Long? = null,
)

data class UpdateSeedInventoryRequest(
    @field:Min(0)
    val quantity: Int? = null,
    val collectionDate: LocalDate? = null,
    val expirationDate: LocalDate? = null,
    @field:Min(0)
    val costPerUnitSek: Double? = null,
    @field:Size(max = 255)
    val unitType: String? = null,
    val seasonId: Long? = null,
    val speciesProviderId: Long? = null,
)

data class DecrementSeedInventoryRequest(
    @field:Min(1)
    val quantity: Int,
)
