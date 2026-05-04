package app.verdant.entity

import java.time.Instant

/**
 * Append-only audit log for a [SaleLot]. Every state transition writes one
 * row: creation, markdown, outlet move, return-from-outlet, sale recorded,
 * sale edited, terminal NOT_SOLD, and auto-flipped SOLD_OUT.
 *
 * [payloadJson] holds before/after for edits, the from/to outlet for
 * RETURNED_FROM_OUTLET, the sale id for SALE_RECORDED, etc. — shape is
 * event-type specific. Keep the schema loose; consumers parse by event_type.
 */
data class SaleLotEvent(
    val id: Long? = null,
    val saleLotId: Long,
    val eventType: SaleLotEventType,
    val payloadJson: String? = null,
    val recordedByUserId: Long,
    val createdAt: Instant = Instant.now(),
)

enum class SaleLotEventType {
    CREATED,
    PRICE_CHANGED,
    OUTLET_CHANGED,
    RETURNED_FROM_OUTLET,
    SALE_RECORDED,
    SALE_EDITED,
    MARKED_NOT_SOLD,
    AUTO_SOLD_OUT,
}
