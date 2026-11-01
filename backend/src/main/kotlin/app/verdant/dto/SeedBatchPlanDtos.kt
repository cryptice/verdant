package app.verdant.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/** "Sow [seeds] from this batch to have [plants] ready on [readyDate]." */
data class SeedBatchPlanRequest(
    @field:Min(1) val seeds: Int,
    @field:Min(1) val plants: Int,
    @field:NotNull val readyDate: LocalDate,
    /** The batches to sow from, in the order to draw from them; all of one variety. Only read by the plan endpoints without a batch in the path. */
    val seedBatchIds: List<Long>? = null,
)

/** How many of the plan's seeds come from one batch. */
data class SeedBatchAllocation(
    val seedBatchId: Long,
    val seeds: Int,
)

data class SeedBatchPlanStep(
    val name: String,
    val activityType: String,
    val date: LocalDate,
    val count: Int,
    /** Optional workflow steps keep their place in the timeline but get no task. */
    val optional: Boolean,
)

data class SeedBatchPlanResponse(
    /** The first batch drawn from. */
    val seedBatchId: Long,
    val speciesId: Long,
    val seeds: Int,
    val plants: Int,
    val readyDate: LocalDate,
    val steps: List<SeedBatchPlanStep>,
    /** The batches the seeds are drawn from; each gets its own sowing task. */
    val allocations: List<SeedBatchAllocation> = emptyList(),
    /** Empty for a preview. */
    val taskIds: List<Long> = emptyList(),
)
