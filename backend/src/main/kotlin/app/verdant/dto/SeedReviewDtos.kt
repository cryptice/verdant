package app.verdant.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class SeedReviewResponse(
    val season: SeasonSummary,
    /** True when the season started before the seed stock ledger went live. */
    val estimated: Boolean,
    val varieties: List<VarietyReview>,
)

data class SeasonSummary(
    val id: Long,
    val name: String,
    val year: Int,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
)

data class VarietyReview(
    val speciesId: Long,
    val speciesName: String,
    val unitType: String,
    val steps: List<ReviewStep>,
    val sown: Int,
    val bought: Int,
    val harvestedStems: Int,
    val soldStems: Int,
    val revenueCents: Long,
    val seedCostCents: Long,
    val leftInStock: Int,
    val suggestedOrder: Int,
    val savedOrder: Int?,
)

data class ReviewStep(
    val name: String,
    val eventType: String,
    val count: Int,
)

data class SaveOrderListRequest(
    @field:NotNull
    @field:Size(max = 1000)
    @field:Valid
    val lines: List<OrderLine>,
)

data class OrderLine(
    @field:NotNull
    val speciesId: Long,
    @field:Min(0)
    val quantity: Int,
)
