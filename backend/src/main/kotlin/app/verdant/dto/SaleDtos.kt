package app.verdant.dto

import app.verdant.entity.SaleLotEventType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

data class SaleResponse(
    val id: Long,
    val saleLotId: Long,
    val quantity: Int,
    val pricePerUnitCents: Int,
    val outletId: Long,
    val outletName: String,
    val customerId: Long?,
    val customerName: String?,
    val soldAt: LocalDate,
    val recordedByUserId: Long,
    val notes: String?,
    val createdAt: Instant,
)

data class RecordSaleRequest(
    @field:Min(1)
    val quantity: Int,
    @field:Min(0)
    val pricePerUnitCents: Int,
    val customerId: Long? = null,
    val soldAt: LocalDate? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)

/** Partial update — null fields keep the current value. */
data class EditSaleRequest(
    @field:Min(1)
    val quantity: Int? = null,
    @field:Min(0)
    val pricePerUnitCents: Int? = null,
    val customerId: Long? = null,
    val soldAt: LocalDate? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)

data class SaleLotEventResponse(
    val id: Long,
    val eventType: SaleLotEventType,
    val payloadJson: String?,
    val recordedByUserId: Long,
    val createdAt: Instant,
)

data class SaleLedgerEntry(
    val id: Long,
    val saleLotId: Long,
    val sourceKind: String,
    val sourceSummary: String?,
    val unitKind: String,
    val quantity: Int,
    val pricePerUnitCents: Int,
    val totalCents: Int,
    val outletName: String,
    val customerId: Long?,
    val customerName: String?,
    val soldAt: LocalDate,
    val notes: String?,
)

data class QuickSaleRequest(
    val speciesId: Long? = null,
    val adhocLabel: String? = null,
    @field:NotBlank val unitKind: String,
    @field:Min(1) val quantity: Int,
    @field:Min(0) val pricePerUnitCents: Int,
    @field:NotNull val outletId: Long,
    val customerId: Long? = null,
    val soldAt: LocalDate? = null,
    @field:Size(max = 2000) val notes: String? = null,
)
