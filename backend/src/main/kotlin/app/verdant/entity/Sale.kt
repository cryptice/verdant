package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

/**
 * One sale transaction against a [SaleLot]. Decrements the lot's
 * [SaleLot.quantityRemaining]; auto-flips the lot to [SaleLotStatus.SOLD_OUT]
 * when remaining hits zero.
 *
 * [outletId] is a snapshot at insert time — it stays attributed to the outlet
 * the lot was at when the sale happened, even if the lot later moves.
 *
 * [customerId] is optional: anonymous walk-up market sales have an outlet but
 * no specific customer.
 */
data class Sale(
    val id: Long? = null,
    val saleLotId: Long,
    val quantity: Int,
    val pricePerUnitCents: Int,
    val outletId: Long,
    val customerId: Long? = null,
    val soldAt: LocalDate = LocalDate.now(),
    val recordedByUserId: Long,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
)
