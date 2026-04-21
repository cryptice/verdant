package app.verdant.entity

import java.math.BigDecimal
import java.time.Instant

enum class SupplyApplicationScope { BED, PLANTS }

data class SupplyApplication(
    val id: Long? = null,
    val orgId: Long,
    val bedId: Long,
    val supplyInventoryId: Long,
    val supplyTypeId: Long,
    val quantity: BigDecimal,
    val targetScope: SupplyApplicationScope,
    val appliedAt: Instant = Instant.now(),
    val appliedBy: Long,
    val workflowStepId: Long? = null,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
)
