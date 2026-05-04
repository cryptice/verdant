package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

// ── Outlets ──

data class OutletResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("channel") val channel: String,
    @SerializedName("contactInfo") val contactInfo: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
)

data class CreateOutletRequest(
    @SerializedName("name") val name: String,
    @SerializedName("channel") val channel: String,
    @SerializedName("contactInfo") val contactInfo: String? = null,
    @SerializedName("notes") val notes: String? = null,
)

data class UpdateOutletRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("channel") val channel: String? = null,
    @SerializedName("contactInfo") val contactInfo: String? = null,
    @SerializedName("notes") val notes: String? = null,
)

// ── Sale lots ──

data class SaleLotResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("sourceKind") val sourceKind: String,
    @SerializedName("plantId") val plantId: Long?,
    @SerializedName("harvestEventId") val harvestEventId: Long?,
    @SerializedName("bouquetId") val bouquetId: Long?,
    @SerializedName("sourceSummary") val sourceSummary: String?,
    @SerializedName("unitKind") val unitKind: String,
    @SerializedName("stemsPerUnit") val stemsPerUnit: Int?,
    @SerializedName("quantityTotal") val quantityTotal: Int,
    @SerializedName("quantityRemaining") val quantityRemaining: Int,
    @SerializedName("initialRequestedPriceCents") val initialRequestedPriceCents: Int,
    @SerializedName("currentRequestedPriceCents") val currentRequestedPriceCents: Int,
    @SerializedName("currentOutletId") val currentOutletId: Long,
    @SerializedName("currentOutletName") val currentOutletName: String,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
)

data class SaleLotDetailResponse(
    @SerializedName("lot") val lot: SaleLotResponse,
    @SerializedName("sales") val sales: List<SaleResponse>,
    @SerializedName("events") val events: List<SaleLotEventResponse>,
)

data class CreateSaleLotForPlantRequest(
    @SerializedName("plantId") val plantId: Long,
    @SerializedName("unitKind") val unitKind: String,
    @SerializedName("quantityTotal") val quantityTotal: Int,
    @SerializedName("initialRequestedPriceCents") val initialRequestedPriceCents: Int,
    @SerializedName("currentOutletId") val currentOutletId: Long,
)

data class CreateSaleLotForHarvestRequest(
    @SerializedName("harvestEventId") val harvestEventId: Long,
    @SerializedName("unitKind") val unitKind: String,
    @SerializedName("stemsPerUnit") val stemsPerUnit: Int? = null,
    @SerializedName("quantityTotal") val quantityTotal: Int,
    @SerializedName("initialRequestedPriceCents") val initialRequestedPriceCents: Int,
    @SerializedName("currentOutletId") val currentOutletId: Long,
)

data class ChangePriceRequest(
    @SerializedName("newPriceCents") val newPriceCents: Int,
)

data class ChangeOutletRequest(
    @SerializedName("newOutletId") val newOutletId: Long,
)

data class ReturnFromOutletRequest(
    @SerializedName("fromOutletId") val fromOutletId: Long,
)

// ── Sales ──

data class SaleResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("saleLotId") val saleLotId: Long,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("pricePerUnitCents") val pricePerUnitCents: Int,
    @SerializedName("outletId") val outletId: Long,
    @SerializedName("outletName") val outletName: String,
    @SerializedName("customerId") val customerId: Long?,
    @SerializedName("customerName") val customerName: String?,
    @SerializedName("soldAt") val soldAt: String,
    @SerializedName("recordedByUserId") val recordedByUserId: Long,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
)

data class RecordSaleRequest(
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("pricePerUnitCents") val pricePerUnitCents: Int,
    @SerializedName("customerId") val customerId: Long? = null,
    @SerializedName("soldAt") val soldAt: String? = null,
    @SerializedName("notes") val notes: String? = null,
)

data class EditSaleRequest(
    @SerializedName("quantity") val quantity: Int? = null,
    @SerializedName("pricePerUnitCents") val pricePerUnitCents: Int? = null,
    @SerializedName("customerId") val customerId: Long? = null,
    @SerializedName("soldAt") val soldAt: String? = null,
    @SerializedName("notes") val notes: String? = null,
)

data class SaleLotEventResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("eventType") val eventType: String,
    @SerializedName("payloadJson") val payloadJson: String?,
    @SerializedName("recordedByUserId") val recordedByUserId: Long,
    @SerializedName("createdAt") val createdAt: String,
)

/** GET .../available-for-sale returns { "available": N }. */
data class AvailableForSaleResponse(
    @SerializedName("available") val available: Int,
)

// ── Constants matching backend enums ──

object SourceKind {
    const val PLANT = "PLANT"
    const val HARVEST_EVENT = "HARVEST_EVENT"
    const val BOUQUET = "BOUQUET"
}

object UnitKind {
    const val STEM = "STEM"
    const val BUNCH = "BUNCH"
    const val PLUG = "PLUG"
    const val BULB = "BULB"
    const val TUBER = "TUBER"
    const val PLANT = "PLANT"
    const val BOUQUET = "BOUQUET"
}

object SaleLotStatus {
    const val OFFERED = "OFFERED"
    const val SOLD_OUT = "SOLD_OUT"
    const val NOT_SOLD = "NOT_SOLD"
    val all = listOf(OFFERED, SOLD_OUT, NOT_SOLD)
}

object SaleLotEventType {
    const val CREATED = "CREATED"
    const val PRICE_CHANGED = "PRICE_CHANGED"
    const val OUTLET_CHANGED = "OUTLET_CHANGED"
    const val RETURNED_FROM_OUTLET = "RETURNED_FROM_OUTLET"
    const val SALE_RECORDED = "SALE_RECORDED"
    const val SALE_EDITED = "SALE_EDITED"
    const val MARKED_NOT_SOLD = "MARKED_NOT_SOLD"
    const val AUTO_SOLD_OUT = "AUTO_SOLD_OUT"
}
