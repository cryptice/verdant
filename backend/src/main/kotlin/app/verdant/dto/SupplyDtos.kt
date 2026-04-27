package app.verdant.dto

import app.verdant.entity.SupplyCategory
import app.verdant.entity.SupplyUnit
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant

data class SupplyTypeResponse(
    val id: Long,
    val name: String,
    val category: String,
    val unit: String,
    val properties: Map<String, Any?>,
    val inexhaustible: Boolean,
    val createdAt: Instant,
)

data class CreateSupplyTypeRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:NotNull
    val category: SupplyCategory,
    @field:NotNull
    val unit: SupplyUnit,
    val properties: Map<String, Any?> = emptyMap(),
    val inexhaustible: Boolean = false,
)

data class UpdateSupplyTypeRequest(
    @field:Size(max = 255)
    val name: String? = null,
    val category: SupplyCategory? = null,
    val unit: SupplyUnit? = null,
    val properties: Map<String, Any?>? = null,
    val inexhaustible: Boolean? = null,
)

data class SupplyInventoryResponse(
    val id: Long,
    val supplyTypeId: Long,
    val supplyTypeName: String,
    val category: String,
    val unit: String,
    val properties: Map<String, Any?>,
    val quantity: BigDecimal,
    val costCents: Int?,
    val seasonId: Long?,
    val notes: String?,
    val createdAt: Instant,
)

data class CreateSupplyInventoryRequest(
    @field:NotNull
    val supplyTypeId: Long,
    @field:NotNull
    val quantity: BigDecimal,
    @field:Min(0)
    val costCents: Int? = null,
    val seasonId: Long? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)

data class UpdateSupplyInventoryRequest(
    val quantity: BigDecimal? = null,
    @field:Min(0)
    val costCents: Int? = null,
    val seasonId: Long? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)

data class DecrementSupplyRequest(
    @field:NotNull
    val quantity: BigDecimal,
)
