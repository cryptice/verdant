package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class SeedStockMovement(
    val id: Long? = null,
    val orgId: Long,
    val seedInventoryId: Long?,
    val speciesId: Long,
    val kind: MovementKind,
    val quantity: Int,
    val shortfall: Int = 0,
    val unitCostCents: Int?,
    val occurredOn: LocalDate,
    val seasonId: Long?,
    val firstPlantId: Long? = null,
    val plantCount: Int? = null,
    val createdAt: Instant = Instant.now(),
)

enum class MovementKind { OPENING, ADDED, SOWN, ADJUSTED }
