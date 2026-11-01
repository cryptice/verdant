package app.verdant.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/** "Sow [seeds] from this batch to have [plants] ready on [readyDate]." */
data class SeedBatchPlanRequest(
    @field:Min(1) val seeds: Int,
    @field:Min(1) val plants: Int,
    @field:NotNull val readyDate: LocalDate,
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
    val seedBatchId: Long,
    val speciesId: Long,
    val seeds: Int,
    val plants: Int,
    val readyDate: LocalDate,
    val steps: List<SeedBatchPlanStep>,
    /** Empty for a preview. */
    val taskIds: List<Long> = emptyList(),
)
