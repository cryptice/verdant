package app.verdant.entity

import java.math.BigDecimal
import java.time.Instant

enum class SupplyCategory { SOIL, POT, FERTILIZER, TOOL, LABEL, TRAY, OTHER }

enum class SupplyUnit { COUNT, LITERS, KILOGRAMS, GRAMS, METERS, PACKETS }

data class SupplyType(
    val id: Long? = null,
    val orgId: Long,
    val name: String,
    val category: SupplyCategory,
    val unit: SupplyUnit,
    val properties: String = "{}",
    val createdAt: Instant = Instant.now(),
)

data class SupplyInventory(
    val id: Long? = null,
    val orgId: Long,
    val supplyTypeId: Long,
    val quantity: BigDecimal,
    val costSek: Int? = null,
    val seasonId: Long? = null,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
)
