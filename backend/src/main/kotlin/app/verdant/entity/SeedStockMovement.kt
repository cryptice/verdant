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
    /** On a correction: the SOWN draw it reverses. Its quantity is negative and its shortfall is the part that never came from stock. */
    val correctsMovementId: Long? = null,
    val createdAt: Instant = Instant.now(),
)

enum class MovementKind { OPENING, ADDED, SOWN, ADJUSTED }
