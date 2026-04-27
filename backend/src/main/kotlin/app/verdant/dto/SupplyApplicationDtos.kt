package app.verdant.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant

data class SupplyApplicationResponse(
    val id: Long,
    val bedId: Long?,
    val trayLocationId: Long?,
    val supplyInventoryId: Long,
    val supplyTypeId: Long,
    val supplyTypeName: String,
    val supplyUnit: String,
    val quantity: BigDecimal,
    val targetScope: String,
    val appliedAt: Instant,
    val appliedByName: String?,
    val workflowStepId: Long?,
    val notes: String?,
    val plantIds: List<Long>,
)

/** Exactly one of [bedId] or [trayLocationId] must be set. */
data class CreateSupplyApplicationRequest(
    val bedId: Long? = null,
    val trayLocationId: Long? = null,
    @field:NotNull val supplyInventoryId: Long,
    @field:NotNull @field:DecimalMin("0.01") val quantity: BigDecimal,
    @field:NotNull val targetScope: String,
    val plantIds: List<Long> = emptyList(),
    val workflowStepId: Long? = null,
    val notes: String? = null,
)
