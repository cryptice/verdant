package app.verdant.entity

import java.time.Instant

/**
 * A unit of stuff offered for sale, polymorphic over plant batch (plugs / tubers /
 * bulbs / seedlings), harvest event (stems / bunches), or an assembled bouquet.
 *
 * Exactly one of [plantId] / [harvestEventId] / [bouquetId] is non-null and matches
 * [sourceKind] (enforced by a CHECK constraint at the DB level).
 *
 * [stemsPerUnit] is meaningful only when [unitKind] is BUNCH (a "bunch of N stems"),
 * null otherwise. The "available for sale" derivation for HARVEST_EVENT lots
 * multiplies by stemsPerUnit when present.
 */
data class SaleLot(
    val id: Long? = null,
    val orgId: Long,
    val sourceKind: SourceKind,
    val plantId: Long? = null,
    val harvestEventId: Long? = null,
    val bouquetId: Long? = null,
    val unitKind: UnitKind,
    val stemsPerUnit: Int? = null,
    val quantityTotal: Int,
    val quantityRemaining: Int,
    val initialRequestedPriceCents: Int,
    val currentRequestedPriceCents: Int,
    val currentOutletId: Long,
    val status: SaleLotStatus = SaleLotStatus.OFFERED,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class SourceKind { PLANT, HARVEST_EVENT, BOUQUET }

enum class UnitKind { STEM, BUNCH, PLUG, BULB, TUBER, PLANT, BOUQUET }

enum class SaleLotStatus { OFFERED, SOLD_OUT, NOT_SOLD }
