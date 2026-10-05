package app.verdant.dto

import app.verdant.entity.UnitType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class SellableUnit { STEM, FLOWER }
enum class PlanningQuantityBasis { START, PLANT, OUTPUT }

data class LifecycleStep(
    val key: String,
    val name: String,
    val activityType: String,
    val daysBeforeHarvest: Int?,
    val quantityBasis: PlanningQuantityBasis,
)

/** Yield is output available on the target date, never a season's total. */
data class ProductionProfile(
    val sellableUnit: SellableUnit = SellableUnit.STEM,
    val outputPerPlant: BigDecimal? = null,
    val establishmentPercent: BigDecimal? = null,
    val startingUnit: UnitType = UnitType.SEED,
    val steps: List<LifecycleStep>,
)

data class PlanningSpecies(
    val speciesId: Long,
    val speciesName: String,
    val profile: ProductionProfile,
    val customized: Boolean,
    val defaultSchedule: DefaultScheduleInfo? = null,
)

data class PlanningSource(val title: String, val url: String)
data class DefaultScheduleInfo(
    val key: String, val name: String, val version: String, val climate: String,
    val description: String, val reviewRequired: Boolean,
    val harvestMonths: List<Int>, val plantingMonths: List<Int>,
    val sources: List<PlanningSource>,
)

data class HarvestPlanRequest(
    val seasonId: Long,
    val speciesId: Long? = null,
    val groupId: Long? = null,
    val quantity: Int,
    val sellableUnit: SellableUnit,
    val harvestDate: LocalDate,
    /** Missing = suggested share; zero = excluded; positive = fixed output quantity. */
    val allocations: Map<Long, Int> = emptyMap(),
)

data class PlannedStep(
    val key: String, val name: String, val activityType: String,
    val date: LocalDate, val quantity: Int, val unit: String,
)
data class SpeciesAllocation(
    val speciesId: Long, val speciesName: String, val outputQuantity: Int,
    val plantsNeeded: Int, val startingQuantity: Int,
    val profile: ProductionProfile, val steps: List<PlannedStep>,
)
data class PlanningIssue(val speciesId: Long?, val message: String)
data class HarvestPlanPreview(
    val request: HarvestPlanRequest,
    val allocations: List<SpeciesAllocation>,
    val issues: List<PlanningIssue>,
    val unallocatedQuantity: Int,
    val canSave: Boolean,
    val fingerprint: String = "",
)
data class CreateHarvestPlanRequest(
    val requestKey: UUID, val fingerprint: String, val target: HarvestPlanRequest,
)
data class HarvestPlanTask(
    val id: Long, val speciesId: Long, val stepKey: String,
    val remainingCount: Int, val status: String,
)
data class HarvestPlanResponse(
    val id: Long, val status: String, val snapshot: HarvestPlanPreview,
    val tasks: List<HarvestPlanTask>, val createdAt: Instant,
)
data class CompleteHarvestPlanTaskRequest(val expectedRemaining: Int, val processedCount: Int)
