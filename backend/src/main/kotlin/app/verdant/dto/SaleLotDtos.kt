package app.verdant.dto

import app.verdant.entity.SaleLotStatus
import app.verdant.entity.SourceKind
import app.verdant.entity.UnitKind
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class SaleLotResponse(
    val id: Long,
    val sourceKind: SourceKind,
    val plantId: Long?,
    val harvestEventId: Long?,
    val bouquetId: Long?,
    /** Display-friendly summary of the source: plant name, harvest event "N stems on date", or bouquet name. */
    val sourceSummary: String?,
    val unitKind: UnitKind,
    val stemsPerUnit: Int?,
    val quantityTotal: Int,
    val quantityRemaining: Int,
    val initialRequestedPriceCents: Int,
    val currentRequestedPriceCents: Int,
    val currentOutletId: Long,
    val currentOutletName: String,
    val status: SaleLotStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Detail view: lot + its sales + its audit log. */
data class SaleLotDetailResponse(
    val lot: SaleLotResponse,
    val sales: List<SaleResponse>,
    val events: List<SaleLotEventResponse>,
)

data class CreateSaleLotForPlantRequest(
    @field:NotNull
    val plantId: Long,
    @field:NotNull
    val unitKind: UnitKind,
    @field:Min(1)
    val quantityTotal: Int,
    @field:Min(0)
    val initialRequestedPriceCents: Int,
    @field:NotNull
    val currentOutletId: Long,
)

data class CreateSaleLotForHarvestRequest(
    @field:NotNull
    val harvestEventId: Long,
    /** STEM or BUNCH. */
    @field:NotNull
    val unitKind: UnitKind,
    /** Required when unitKind = BUNCH; null otherwise. */
    @field:Min(1)
    val stemsPerUnit: Int? = null,
    @field:Min(1)
    val quantityTotal: Int,
    @field:Min(0)
    val initialRequestedPriceCents: Int,
    @field:NotNull
    val currentOutletId: Long,
)

data class ChangePriceRequest(
    @field:Min(0)
    val newPriceCents: Int,
)

data class ChangeOutletRequest(
    @field:NotNull
    val newOutletId: Long,
)

data class ReturnFromOutletRequest(
    @field:NotNull
    val fromOutletId: Long,
)
